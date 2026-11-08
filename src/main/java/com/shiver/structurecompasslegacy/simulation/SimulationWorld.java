package com.shiver.structurecompasslegacy.simulation;

import java.util.ArrayDeque;
import java.util.Queue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.shiver.structurecompasslegacy.api.SearchTarget;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.World;
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
        chunk.getTileEntityMap().remove(position);
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
        entity.setWorld(this);
        loadedEntityList.add(entity);
        return true;
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
        loadedTileEntityList.clear();
        tickableTileEntities.clear();
        for (ListenableFutureTask<Object> task : scheduledTasks) {
            task.cancel(false);
        }
        scheduledTasks.clear();
    }
}
