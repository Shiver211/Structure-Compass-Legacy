package com.shiver.structurecompasslegacy.search;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;

public abstract class ChunkSearchTask implements SearchTask {

    protected final SearchContext context;
    protected final ChunkSpiral chunks;
    private long longestCallNanos;

    protected ChunkSearchTask(SearchContext context) {
        this.context = context;
        this.chunks = new ChunkSpiral(
                context.origin.getX() >> 4,
                context.origin.getZ() >> 4,
                (context.radius + 15) / 16);
    }

    protected abstract SearchTarget inspect(int chunkX, int chunkZ) throws Exception;

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        while (chunks.hasNext() && !budget.exhausted()) {
            long before = System.nanoTime();
            long elapsed;
            SearchTarget target;

            try {
                target = inspect(chunks.x(), chunks.z());
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                elapsed = System.nanoTime() - before;
                recordCallNanos(elapsed);
            }

            chunks.next();

            if (target != null && context.contains(target.position)) {
                return SearchStep.found(target);
            }

            if (elapsed > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }

        return chunks.hasNext() ? SearchStep.running() : SearchStep.exhausted();
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

    protected final void recordCallNanos(long nanos) {
        longestCallNanos = Math.max(longestCallNanos, nanos);
    }

    @Override
    public void close() {
    }
}
