package com.shiver.structurecompasslegacy.verification;

import java.util.concurrent.Future;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import com.shiver.structurecompasslegacy.simulation.ChunkGenerationBudget;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.WorldSavedData;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;

/** 分 tick 的开发断言；不生成可导航目标。 */
final class FoundationTask implements SearchTask {

    enum Exit {
        COMPLETE,
        HOLD,
        ERROR,
        LIMIT
    }

    private final SearchContext context;
    private final boolean checkRecurrent;
    private final BlockPos position;
    private final Exit exit;
    private SimulationSession session;
    private RecurrentTerrainProbe recurrent;
    private EntityArmorStand entity;
    private Future<?> pendingTask;
    private int phase;
    private long longestCallNanos;
    private String longestOperation = "";

    FoundationTask(SearchContext context, boolean checkRecurrent) {
        this(context, checkRecurrent, Exit.COMPLETE);
    }

    FoundationTask(SearchContext context, boolean checkRecurrent, Exit exit) {
        this.context = context;
        this.checkRecurrent = checkRecurrent;
        this.exit = exit;
        this.position = new BlockPos((context.origin.getX() >> 4) * 16 + 4, 80,
                (context.origin.getZ() >> 4) * 16 + 4);
    }

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        if (budget.exhausted()) {
            return SearchStep.running();
        }
        long started = budget.nanoTime();
        int currentPhase = phase;
        // 快照检查失败时作为 suppressed 异常保留，不能盖住生成器的原始异常。
        try (WorldStateSnapshot snapshot = new WorldStateSnapshot(context.source);
                SimulationScope ignored = SimulationScope.enter()) {
            switch (phase) {
                case 0:
                    initialize();
                    break;
                case 1:
                    session.world().getChunk(position);
                    break;
                case 2:
                    if (checkRecurrent) {
                        recurrent.verify(session.world(), position);
                    } else {
                        prepareState();
                    }
                    break;
                case 3:
                    if (exit == Exit.HOLD) {
                        break;
                    }
                    if (exit == Exit.ERROR) {
                        throw new VerificationFailure();
                    }
                    if (exit == Exit.LIMIT) {
                        // 用真正的预算拒绝路径验证清理，不修改服务器全局配置。
                        ChunkGenerationBudget limit = new ChunkGenerationBudget(1, 1);
                        limit.begin(0, 0);
                        limit.finish(0);
                        limit.begin(1, 0);
                    }
                    if (!checkRecurrent) {
                        verifyStateAndReset();
                    }
                    break;
                default:
                    return SearchStep.exhausted();
            }
            if (exit != Exit.HOLD || phase != 3) {
                phase++;
            }
        } finally {
            long elapsed = budget.nanoTime() - started;
            if (elapsed > longestCallNanos) {
                longestCallNanos = elapsed;
                longestOperation = phaseName(currentPhase);
            }
        }
        if (longestCallNanos > CompassConfig.slowCallMillis * 1_000_000L) {
            return SearchStep.limit("slow_call");
        }
        return phase == 4 ? SearchStep.exhausted() : SearchStep.running();
    }

    boolean holdingResources() {
        return exit == Exit.HOLD && phase == 3;
    }

    private void initialize() throws ReflectiveOperationException {
        session = new SimulationSession(context.source);
        session.beginCandidate();
        SimulationWorld world = session.world();
        require(world.provider != context.source.provider, "shared provider");
        require(world.getBiomeProvider() != context.source.getBiomeProvider(), "shared biome provider");
        require(world.getChunkProvider().chunkGenerator != context.source.getChunkProvider().chunkGenerator,
                "shared chunk generator");
        require(world.getMapStorage() != context.source.getMapStorage(), "shared MapStorage");
        require(world.getPerWorldStorage() != context.source.getPerWorldStorage(), "shared per-world storage");
        require(world.rand != context.source.rand, "shared random");
        if (checkRecurrent) {
            recurrent = new RecurrentTerrainProbe();
        }
    }

    private void prepareState() {
        SimulationWorld world = session.world();
        world.beginPlacement();
        world.setBlockState(position, Blocks.CHEST.getDefaultState(), 2);
        TileEntityChest chest = new TileEntityChest();
        world.setTileEntity(position, chest);
        require(world.getTileEntity(position) == chest, "temporary tile entity is not readable");
        require(world.finishPlacement() != null, "placement bounds are missing");

        entity = new EntityArmorStand(world, position.getX(), position.getY(), position.getZ());
        require(world.spawnEntity(entity), "temporary entity was rejected");
        require(world.getEntitiesWithinAABB(EntityArmorStand.class, entity.getEntityBoundingBox()).contains(entity),
                "temporary entity is absent from spatial queries");

        world.scheduleUpdate(position, Blocks.CHEST, 10);
        pendingTask = world.addScheduledTask(() -> {
            throw new IllegalStateException("Discarded candidate task must never execute");
        });
        world.getPerWorldStorage().setData("scl_verification", new ProbeData());
        world.candidateState(ProbeData.class, ProbeData::new);
    }

    private void verifyStateAndReset() {
        SimulationWorld world = session.world();
        require(world.getBlockState(position).getBlock() == Blocks.CHEST, "block state lost between ticks");
        require(world.getTileEntity(position) instanceof TileEntityChest, "tile entity lost between ticks");
        require(world.getEntityFromUuid(entity.getUniqueID()) == entity, "entity UUID index lost");
        require(world.isUpdateScheduled(position, Blocks.CHEST), "block tick lost between stages");
        ProbeData previous = world.candidateState(ProbeData.class, ProbeData::new);

        world.beginCandidate();
        require(world.simulationChunks().getLoadedChunkCount() == 0, "candidate chunks were retained");
        require(world.loadedEntityList.isEmpty(), "candidate entities were retained");
        require(world.getEntityFromUuid(entity.getUniqueID()) == null, "candidate UUID index was retained");
        require(!world.isUpdateScheduled(position, Blocks.CHEST), "candidate block ticks were retained");
        require(pendingTask.isCancelled(), "candidate task was not cancelled");
        require(world.getPerWorldStorage().getOrLoadData(ProbeData.class, "scl_verification") == null,
                "candidate saved data was retained");
        require(world.candidateState(ProbeData.class, ProbeData::new) != previous, "candidate state was retained");
    }

    @Override
    public long checkedCandidates() {
        return phase;
    }

    @Override
    public long totalCandidates() {
        return 4;
    }

    @Override
    public int generatedChunks() {
        return session == null ? 0 : session.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return session == null ? 0 : session.peakCachedChunks();
    }

    @Override
    public long longestCallNanos() {
        return longestCallNanos;
    }

    @Override
    public String longestOperation() {
        return longestOperation;
    }

    private String phaseName(int currentPhase) {
        switch (currentPhase) {
            case 0:
                return "verify.initialize";
            case 1:
                return "verify.terrain";
            case 2:
                return checkRecurrent ? "verify.rc" : "verify.prepare";
            default:
                return "verify.reset_or_exit";
        }
    }

    @Override
    public void close() {
        if (session == null) {
            return;
        }
        SimulationWorld world = session.world();
        try (WorldStateSnapshot snapshot = new WorldStateSnapshot(context.source)) {
            session.close();
            SimulationCleanupCheck.verify(world);
            require(pendingTask == null || pendingTask.isCancelled(), "session task was not cancelled");
            require(world.getPerWorldStorage().getOrLoadData(ProbeData.class, "scl_verification") == null,
                    "session saved data was retained");
        } finally {
            session = null;
            recurrent = null;
            entity = null;
            pendingTask = null;
        }
    }

    static final class VerificationFailure extends RuntimeException {
        VerificationFailure() {
            super("intentional cleanup test");
        }
    }

    private static final class ProbeData extends WorldSavedData {

        private ProbeData() {
            super("scl_verification");
        }

        @Override
        public void readFromNBT(NBTTagCompound compound) {
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            return compound;
        }
    }
}
