package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.List;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.search.ChunkDistanceOrder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.layer.GenLayer;

/** 原生记录与区域预测统一按水平距离比较，不因目标已加载而优先返回。 */
final class BetweenlandsSpiritTreeTask implements SearchTask {

    // 候选相对装饰参考点为 [-16, +14]，导航点再偏移至多 32 格。
    // 仅在有候选的触发区块返回区域，故到排序参考点最多 sqrt(48²+48²) < 68 格。
    private static final int MAX_REFERENCE_OFFSET = 68;

    private final SearchContext context;
    private final ChunkDistanceOrder chunks;
    private final BetweenlandsSpiritTreeLocations locations;
    private BetweenlandsSpiritTreeRegion regionFinder;
    private List<BlockPos> knownPositions;
    private BlockPos nearestPosition;
    private long nearestDistanceSquared;
    private long stopDistanceSquared;
    private long longestCallNanos;

    BetweenlandsSpiritTreeTask(SearchContext context, GenLayer biomes, int clearingId,
            BetweenlandsSpiritTreeLocations locations) {
        this.context = context;
        this.locations = locations;
        regionFinder = new BetweenlandsSpiritTreeRegion(biomes, clearingId);

        // 按区块的 (+16, +16) 参考点排序，并为半径边缘的偏移候选保留余量。
        chunks = new ChunkDistanceOrder(context.origin.getX() - 8, context.origin.getZ() - 8,
                context.radius + MAX_REFERENCE_OFFSET);
        nearestDistanceSquared = (long) context.radius * context.radius + 1;
        stopDistanceSquared = (long) (context.radius + MAX_REFERENCE_OFFSET)
                * (context.radius + MAX_REFERENCE_OFFSET);
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        while (!budget.exhausted()) {
            long before = System.nanoTime();

            if (knownPositions == null) {
                knownPositions = locations.read(context.source);
                for (BlockPos position : knownPositions) {
                    considerCandidate(position);
                }
            } else {
                if (!chunks.hasNext()) {
                    return searchResult();
                }

                long dx = (long) chunks.x() * 16 + 16 - context.origin.getX();
                long dz = (long) chunks.z() * 16 + 16 - context.origin.getZ();
                // 后续参考点扣除最大偏移后仍不可能更近，才可结束。
                if (dx * dx + dz * dz >= stopDistanceSquared) {
                    return searchResult();
                }

                considerCandidate(findCandidate(chunks.x(), chunks.z()));
                chunks.next();
            }

            long elapsed = System.nanoTime() - before;
            longestCallNanos = Math.max(longestCallNanos, elapsed);
            if (elapsed > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }

        return SearchStep.running();
    }

    private BlockPos findCandidate(int chunkX, int chunkZ) {
        if (!canStillGenerate(new ChunkPos(chunkX, chunkZ))) {
            return null;
        }

        BetweenlandsSpiritTreeRegion.Region region = regionFinder.find(chunkX, chunkZ);
        if (region == null || !region.hasCandidateInChunk(chunkX, chunkZ)) {
            return null;
        }

        // 原生记录已参与距离比较；同一区域不再另报预测中心。
        for (BlockPos position : knownPositions) {
            if (region.containsCandidatePosition(position)) {
                return null;
            }
        }

        return region.navigationPoint(this::canStillGenerate);
    }

    private boolean canStillGenerate(ChunkPos position) {
        Chunk chunk = context.source.getChunkProvider().getLoadedChunk(position.x, position.z);
        // 已完成装饰的触发区块不再生成；未加载的区块保留预测，不为核验而加载。
        return chunk == null || !chunk.isTerrainPopulated();
    }

    private void considerCandidate(BlockPos position) {
        if (position == null) {
            return;
        }

        long dx = (long) position.getX() - context.origin.getX();
        long dz = (long) position.getZ() - context.origin.getZ();
        long distanceSquared = dx * dx + dz * dz;

        if (distanceSquared < nearestDistanceSquared) {
            nearestPosition = position;
            nearestDistanceSquared = distanceSquared;
            long stopDistance = (long) Math.ceil(Math.sqrt(distanceSquared)) + MAX_REFERENCE_OFFSET;
            stopDistanceSquared = stopDistance * stopDistance;
        }
    }

    private SearchStep searchResult() {
        return nearestPosition == null
                ? SearchStep.exhausted()
                : SearchStep.found(new SearchTarget(nearestPosition));
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
        return longestCallNanos;
    }

    @Override
    public void close() {
        regionFinder = null;
        knownPositions = null;
        nearestPosition = null;
    }
}
