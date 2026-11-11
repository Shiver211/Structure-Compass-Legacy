package com.shiver.structurecompasslegacy.compat.divinerpg;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.search.ChunkDistanceOrder;
import com.shiver.structurecompasslegacy.search.SearchLimitException;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

final class DivineAltarTask implements SearchTask {

    private final SearchContext context;
    private final ChunkDistanceOrder chunks;
    private final DivineAltar altar;
    private final Block grass;
    private final Block spawner;
    private final DivineAltarPlacement placement;
    private SimulationSession simulation;
    private SearchTarget nearest;
    private long nearestDistanceSquared;
    private long stopDistanceSquared;
    private long longestCall;

    DivineAltarTask(SearchContext context, DivineAltar altar, Block grass, Block spawner) {
        this.context = context;
        // 刷怪笼在触发区块的 (+8..+24,+8..+24)，以 (+16,+16) 排序。
        // 它到排序中心最多相差 sqrt(8²+8²) < 12 格，搜索边界也需扩大。
        chunks = new ChunkDistanceOrder(context.origin.getX() - 8, context.origin.getZ() - 8,
                context.radius + 12);
        this.altar = altar;
        this.grass = grass;
        this.spawner = spawner;
        placement = new DivineAltarPlacement(context.seed);
        nearestDistanceSquared = (long) context.radius * context.radius + 1;
        stopDistanceSquared = (long) (context.radius + 12) * (context.radius + 12);
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        while (chunks.hasNext() && !budget.exhausted()) {
            long dx = (long) chunks.x() * 16 + 16 - context.origin.getX();
            long dz = (long) chunks.z() * 16 + 16 - context.origin.getZ();
            // 后续中心只会更远；扣除最大偏移后仍不可能更近，才结束搜索。
            if (dx * dx + dz * dz >= stopDistanceSquared) {
                return result();
            }
            long before = System.nanoTime();
            SearchTarget target;
            try {
                target = inspect(chunks.x(), chunks.z());
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                longestCall = Math.max(longestCall, System.nanoTime() - before);
            }
            chunks.next();
            if (target != null && distanceSquared(target.position) < nearestDistanceSquared) {
                nearest = target;
                nearestDistanceSquared = distanceSquared(target.position);
                long stopDistance = (long) Math.ceil(Math.sqrt(nearestDistanceSquared)) + 12;
                stopDistanceSquared = stopDistance * stopDistance;
            }
            if (System.nanoTime() - before > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }
        return chunks.hasNext() ? SearchStep.running() : result();
    }

    private SearchStep result() {
        return nearest == null ? SearchStep.exhausted() : SearchStep.found(nearest);
    }

    private long distanceSquared(BlockPos position) {
        long dx = (long) position.getX() - context.origin.getX();
        long dz = (long) position.getZ() - context.origin.getZ();
        return dx * dx + dz * dz;
    }

    private SearchTarget inspect(int chunkX, int chunkZ) {
        SearchTarget target = placement.layout(altar, chunkX, chunkZ);
        if (target == null || distanceSquared(target.position) >= nearestDistanceSquared) {
            return null;
        }

        if (DivineAltarLoaded.ready(context.source, target, chunkX, chunkZ)) {
            return DivineAltarLoaded.find(context.source, altar, target, spawner);
        }

        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
            }
            simulation.beginCandidate();
            SimulationWorld world = simulation.world();
            int x = target.bounds.minX;
            int z = target.bounds.minZ;
            // 原版 getHeight 对未加载区块返回 0，先准备该列的私人区块。
            world.getChunk(x >> 4, z >> 4);
            int height = world.getHeight(x, z);
            // 原生条件检查 (+3,+3)，而不是刷怪笼下方或起点正下方。
            if (world.getBlockState(new BlockPos(x + 3, height - 1, z + 3)).getBlock() != grass) {
                return null;
            }
            target.bounds.offset(0, height, 0);
            return new SearchTarget(target.position.up(height), target.bounds);
        }
    }

    @Override
    public long checkedCandidates() {
        return chunks.visited();
    }

    @Override
    public long totalCandidates() {
        return chunks.total();
    }

    @Override
    public long longestCallNanos() {
        return longestCall;
    }

    @Override
    public int generatedChunks() {
        return simulation == null ? 0 : simulation.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return simulation == null ? 0 : simulation.peakCachedChunks();
    }

    @Override
    public void close() {
        if (simulation != null) {
            simulation.close();
            simulation = null;
        }
    }
}
