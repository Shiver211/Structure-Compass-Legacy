package com.shiver.structurecompasslegacy.compat.gctcore;

import java.util.Random;

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

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

final class GctCoreTask implements SearchTask {

    private final SearchContext context;
    private final GctCoreStructure structure;
    private final GctCorePlacement placement;
    private final ChunkDistanceOrder chunks;
    private SimulationSession simulation;
    private SearchTarget nearest;
    private long nearestDistanceSquared;
    private long stopDistanceSquared;
    private long longestCall;

    GctCoreTask(SearchContext context, GctCoreStructure structure, GctCorePlacement placement) {
        this.context = context;
        this.structure = structure;
        this.placement = placement;
        chunks = new ChunkDistanceOrder(context.origin.getX(), context.origin.getZ(),
                context.radius + structure.targetMargin);
        nearestDistanceSquared = (long) context.radius * context.radius + 1;
        long stopDistance = context.radius + structure.targetMargin;
        stopDistanceSquared = stopDistance * stopDistance;
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        while (chunks.hasNext() && !budget.exhausted()) {
            long dx = (long) chunks.x() * 16 + 8 - context.origin.getX();
            long dz = (long) chunks.z() * 16 + 8 - context.origin.getZ();
            // 旋转后的模板中心可能跨区块，扣除最大偏移后才允许提前结束。
            if (dx * dx + dz * dz > stopDistanceSquared) {
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
            if (target != null) {
                long distanceSquared = distanceSquared(target.position);
                if (distanceSquared < nearestDistanceSquared) {
                    nearest = target;
                    nearestDistanceSquared = distanceSquared;
                    long stopDistance = (long) Math.ceil(Math.sqrt(distanceSquared)) + structure.targetMargin;
                    stopDistanceSquared = stopDistance * stopDistance;
                }
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
        Random random = placement.candidate(context.seed, chunkX, chunkZ);
        if (random == null) {
            return null;
        }
        BlockPos recorded = placement.recordedOrigin(context.source, chunkX, chunkZ);
        if (recorded != null) {
            return placement.locate(random, chunkX, chunkZ, null, recorded);
        }
        Chunk loaded = context.source.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
        if (loaded != null && loaded.isTerrainPopulated()) {
            return null;
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
            }
            simulation.beginCandidate();
            try {
                return placement.locate(random, chunkX, chunkZ, simulation.world(), null);
            } finally {
                simulation.world().clearCandidate();
            }
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
