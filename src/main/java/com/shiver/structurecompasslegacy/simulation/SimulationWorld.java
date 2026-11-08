package com.shiver.structurecompasslegacy.simulation;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.search.SearchLimitException;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.World;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;

/** 仅由 SimulationSession 持有，不加入服务器世界列表，也不执行世界 tick。 */
public final class SimulationWorld extends WorldServer implements SimulationMarker {

    private final Queue<ListenableFutureTask<Object>> scheduledTasks = new ArrayDeque<>();
    private final SimulationBlockTicks blockTicks = new SimulationBlockTicks();
    private final Map<UUID, Entity> simulationEntities = new HashMap<>();
    private final Map<Class<?>, Object> candidateState = new HashMap<>();
    private final WorldInfo initialInfo;
    private NextTickListEntry executingTick;
    private boolean capturingPlacement;
    private StructureBoundingBox placementBounds;

    SimulationWorld(WorldServer source) {
        super(source.getMinecraftServer(), new MemorySaveHandler(), copyInfo(source),
                source.provider.getDimension(), new Profiler());
        mapStorage = new MapStorage((ISaveHandler) null);
        perWorldStorage = new MapStorage((ISaveHandler) null);
        villageCollection = new VillageCollection(this);
        disableLevelSaving = true;
        rand.setSeed(getSeed());
        eventListeners.clear();
        initialInfo = copyInfo(source);
    }

    private static WorldInfo copyInfo(WorldServer source) {
        return new WorldInfo(source.getWorldInfo().cloneNBTCompound(null).copy());
    }

    @Override
    @MethodsReturnNonnullByDefault
    protected IChunkProvider createChunkProvider() {
        // 父类构造期间执行：这里只使用 World 已初始化的 provider 和 worldInfo。
        return new SimulationChunkProvider(this, GeneratorFactories.create(this));
    }

    @Override
    @MethodsReturnNonnullByDefault
    public World init() {
        return this;
    }

    @Override
    public boolean setBlockState(@Nonnull BlockPos position, @Nonnull IBlockState state, int flags) {
        if (!isValid(position)) {
            return false;
        }

        Chunk chunk = getChunk(position);
        IBlockState previous = chunk.getBlockState(position);
        if (previous == state) {
            return false;
        }

        // 直接保存临时方块，避免 breakBlock/onBlockAdded 调用未知模组的全局逻辑。
        int sectionIndex = position.getY() >> 4;
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        if (sections[sectionIndex] == Chunk.NULL_BLOCK_STORAGE) {
            sections[sectionIndex] = new ExtendedBlockStorage(sectionIndex << 4, provider.hasSkyLight());
        }

        sections[sectionIndex].set(position.getX() & 15, position.getY() & 15, position.getZ() & 15, state);
        if (previous.getBlock() != state.getBlock()) {
            chunk.getTileEntityMap().remove(position);
        }
        updateColumnHeight(chunk, position);
        if (capturingPlacement) {
            capture(position);
        }
        return true;
    }

    private void capture(BlockPos position) {
        StructureBoundingBox block = new StructureBoundingBox(
                position.getX(), position.getY(), position.getZ(),
                position.getX(), position.getY(), position.getZ());
        if (placementBounds == null) {
            placementBounds = block;
        } else {
            placementBounds.expandTo(block);
        }
    }

    public void beginPlacement() {
        placementBounds = null;
        capturingPlacement = true;
    }

    public SearchTarget finishPlacement() {
        capturingPlacement = false;
        if (placementBounds == null) {
            return null;
        }
        BlockPos center = new BlockPos(
                (placementBounds.minX + placementBounds.maxX) / 2,
                (placementBounds.minY + placementBounds.maxY) / 2,
                (placementBounds.minZ + placementBounds.maxZ) / 2);
        return new SearchTarget(center, placementBounds);
    }

    private void updateColumnHeight(Chunk chunk, BlockPos position) {
        int localX = position.getX() & 15;
        int localZ = position.getZ() & 15;
        int column = localZ << 4 | localX;
        int height = Math.max(chunk.getHeightMap()[column], position.getY() + 1);
        while (height > 0 && chunk.getBlockState(localX, height - 1, localZ).getLightOpacity() == 0) {
            height--;
        }

        chunk.getHeightMap()[column] = height;
    }

    @Override
    public void notifyNeighborsOfStateChange(@Nonnull BlockPos position, @Nonnull Block block, boolean updateObservers) {
        // Template 会在放置完成后显式通知邻居，不能只拦截 setBlockState 的 flags。
    }

    @Override
    public void neighborChanged(@Nonnull BlockPos position, @Nonnull Block block, @Nonnull BlockPos fromPosition) {
        // 目标生成流程需要的物理更新由对应适配器显式复演。
    }

    @Override
    public void observedNeighborChanged(@Nonnull BlockPos position, @Nonnull Block block, @Nonnull BlockPos fromPosition) {
    }

    @Override
    public void updateComparatorOutputLevel(@Nonnull BlockPos position, @Nonnull Block block) {
        // 箱子等方块实体的 markDirty 也会走到这里。
    }

    @Override
    public void setTileEntity(@Nonnull BlockPos position, TileEntity tile) {
        if (tile == null || !isValid(position)) {
            return;
        }
        if (tile.getWorld() != null && tile.getWorld() != this) {
            throw new IllegalArgumentException("Cannot reuse a tile entity from another world");
        }
        tile.setWorld(this);
        tile.setPos(position.toImmutable());
        getChunk(position).getTileEntityMap().put(position.toImmutable(), tile);
    }

    @Override
    public void removeTileEntity(BlockPos position) {
        Chunk chunk = getChunkProvider().getLoadedChunk(position.getX() >> 4, position.getZ() >> 4);
        if (chunk != null) {
            chunk.getTileEntityMap().remove(position);
        }
    }

    @Override
    public boolean spawnEntity(Entity entity) {
        if (entity.world != this) {
            throw new IllegalArgumentException("Cannot reuse an entity from another world");
        }
        if (simulationEntities.containsKey(entity.getUniqueID())) {
            return false;
        }
        getChunk(entity.getPosition()).addEntity(entity);
        loadedEntityList.add(entity);
        simulationEntities.put(entity.getUniqueID(), entity);
        entitiesById.addKey(entity.getEntityId(), entity);
        return true;
    }

    @Override
    public Entity getEntityFromUuid(@Nonnull UUID uuid) {
        return simulationEntities.get(uuid);
    }

    @Override
    public void loadEntities(Collection<Entity> entities) {
        entities.forEach(this::spawnEntity);
    }

    @Override
    public void setEntityState(@Nonnull Entity entity, byte state) {
    }

    @Override
    public boolean addWeatherEffect(@Nonnull Entity entity) {
        throw new UnsupportedOperationException("Weather entities need an explicit simulation adapter");
    }

    @Override
    public void addBlockEvent(@Nonnull BlockPos position, @Nonnull Block block, int eventId, int eventParameter) {
        throw new UnsupportedOperationException("Block events need an explicit simulation adapter");
    }

    @Override
    public void spawnParticle(
            @Nonnull EntityPlayerMP player, @Nonnull EnumParticleTypes particle, boolean longDistance,
            double x, double y, double z, int count, double xOffset, double yOffset, double zOffset,
            double speed, @Nonnull int... arguments) {
    }

    @Override
    public void removeEntity(Entity entity) {
        if (entity.world != this) {
            throw new IllegalArgumentException("Cannot remove an entity from another world");
        }
        Chunk chunk = simulationChunks().getLoadedChunk(entity.chunkCoordX, entity.chunkCoordZ);
        if (chunk != null && entity.addedToChunk) {
            chunk.removeEntity(entity);
        }
        entity.addedToChunk = false;
        entity.isDead = true;
        loadedEntityList.remove(entity);
        simulationEntities.remove(entity.getUniqueID());
        entitiesById.removeObject(entity.getEntityId());
    }

    @Override
    public void removeEntityDangerously(@Nonnull Entity entity) {
        removeEntity(entity);
    }

    @Override
    public boolean addTileEntity(@Nonnull TileEntity tile) {
        setTileEntity(tile.getPos(), tile);
        return true;
    }

    @Override
    public void addTileEntities(Collection<TileEntity> tiles) {
        tiles.forEach(this::addTileEntity);
    }

    @Override
    public void markTileEntityForRemoval(TileEntity tile) {
        removeTileEntity(tile.getPos());
    }

    @Override
    public void scheduleUpdate(@Nonnull BlockPos position, @Nonnull Block block, int delay) {
        updateBlockTick(position, block, delay, 0);
    }

    @Override
    public void updateBlockTick(@Nonnull BlockPos position, @Nonnull Block block, int delay, int priority) {
        blockTicks.schedule(position, block, getTotalWorldTime() + delay, priority);
    }

    @Override
    public void scheduleBlockUpdate(@Nonnull BlockPos position, @Nonnull Block block, int delay, int priority) {
        updateBlockTick(position, block, delay, priority);
    }

    @Override
    public boolean isUpdateScheduled(@Nonnull BlockPos position, @Nonnull Block block) {
        return blockTicks.contains(position, block);
    }

    @Override
    public boolean isBlockTickPending(@Nonnull BlockPos position, @Nonnull Block block) {
        return executingTick != null && executingTick.position.equals(position) && executingTick.getBlock() == block;
    }

    @Override
    public List<NextTickListEntry> getPendingBlockUpdates(@Nonnull StructureBoundingBox bounds, boolean remove) {
        return blockTicks.within(bounds, remove);
    }

    @Override
    public void immediateBlockTick(@Nonnull BlockPos position, @Nonnull IBlockState state, @Nonnull Random random) {
        throw new UnsupportedOperationException("Immediate block ticks need an explicit simulation stage");
    }

    @Override
    public boolean tickUpdates(boolean runAll) {
        throw new UnsupportedOperationException("Use runScheduledBlockTicks with an audited block filter");
    }

    @Override
    public void tick() {
        throw new UnsupportedOperationException("Simulation does not tick the whole world");
    }

    @Override
    public void updateEntities() {
        throw new UnsupportedOperationException("Entity ticking needs an explicit simulation stage");
    }

    @Override
    public BlockPos findNearestStructure(String name, BlockPos position, boolean unexplored) {
        throw new UnsupportedOperationException("Native locate calls must be replaced by an audited adapter");
    }

    public boolean runScheduledBlockTicks(SearchBudget budget, Predicate<Block> allowed) {
        try (SimulationScope ignored = SimulationScope.enter()) {
            while (!blockTicks.isEmpty() && !budget.exhausted()) {
                long started = System.nanoTime();
                executingTick = blockTicks.poll();
                try {
                    if (!allowed.test(executingTick.getBlock())) {
                        throw new UnsupportedOperationException("Block tick has not been adapted");
                    }
                    setTotalWorldTime(Math.max(getTotalWorldTime(), executingTick.scheduledTime));
                    IBlockState state = getBlockState(executingTick.position);
                    if (state.getBlock() == executingTick.getBlock()) {
                        state.getBlock().updateTick(this, executingTick.position, state, rand);
                    }
                } finally {
                    executingTick = null;
                }
                checkSlowCall(started);
            }
            return blockTicks.isEmpty();
        }
    }

    public boolean runScheduledTasks(SearchBudget budget) {
        try (SimulationScope ignored = SimulationScope.enter()) {
            while (!scheduledTasks.isEmpty() && !budget.exhausted()) {
                long started = System.nanoTime();
                ListenableFutureTask<Object> task = scheduledTasks.remove();
                task.run();
                try {
                    task.get();
                } catch (Exception exception) {
                    throw new IllegalStateException("Simulation task failed", exception);
                }
                checkSlowCall(started);
            }
            return scheduledTasks.isEmpty();
        }
    }

    private void checkSlowCall(long started) {
        if (System.nanoTime() - started > CompassConfig.slowCallMillis * 1_000_000L) {
            throw new SearchLimitException("slow_call");
        }
    }

    public <T> T candidateState(Class<T> type, Supplier<T> factory) {
        return type.cast(candidateState.computeIfAbsent(type, key -> factory.get()));
    }

    public void beginCandidate() {
        clearCandidate();
        worldInfo = new WorldInfo(initialInfo.cloneNBTCompound(null).copy());
        rand.setSeed(getSeed());
    }

    @Override
    @MethodsReturnNonnullByDefault
    public ListenableFuture<Object> addScheduledTask(@Nonnull Runnable task) {
        ListenableFutureTask<Object> future = ListenableFutureTask.create(task, null);
        scheduledTasks.add(future);
        return future;
    }

    public SimulationChunkProvider simulationChunks() {
        return (SimulationChunkProvider) chunkProvider;
    }

    public void clearCandidate() {
        capturingPlacement = false;
        placementBounds = null;
        simulationChunks().clear();
        loadedEntityList.clear();
        unloadedEntityList.clear();
        simulationEntities.clear();
        entitiesById.clearMap();
        weatherEffects.clear();
        loadedTileEntityList.clear();
        tickableTileEntities.clear();
        for (ListenableFutureTask<Object> task : scheduledTasks) {
            task.cancel(false);
        }
        scheduledTasks.clear();
        blockTicks.clear();
        executingTick = null;
        candidateState.clear();
        mapStorage = new MapStorage((ISaveHandler) null);
        perWorldStorage = new MapStorage((ISaveHandler) null);
        villageCollection = new VillageCollection(this);
    }

}
