package com.shiver.structurecompasslegacy.search;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.simulation.SimulationPlan;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

public abstract class SimulationSearchTask implements SearchTask {

    protected final SearchContext context;
    private final ChunkSpiral chunks;
    private SimulationSession simulation;
    private SimulationPlan plan;
    private long longestCallNanos;
    private String longestOperation = "candidate.initialize";

    protected SimulationSearchTask(SearchContext context) {
        this.context = context;
        this.chunks = new ChunkSpiral(context.origin.getX() >> 4, context.origin.getZ() >> 4,
                (context.radius + 15) / 16);
    }

    protected abstract boolean isCandidate(int chunkX, int chunkZ);

    protected abstract SimulationPlan createPlan(SimulationWorld world, int chunkX, int chunkZ);

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        try (SimulationScope ignored = SimulationScope.enter()) {
            while (chunks.hasNext() && !budget.exhausted()) {
                if (plan == null) {
                    long started = budget.nanoTime();
                    try {
                        if (isCandidate(chunks.x(), chunks.z())) {
                            if (simulation == null) {
                                simulation = new SimulationSession(context.source);
                            }
                            simulation.beginCandidate();
                            plan = createPlan(simulation.world(), chunks.x(), chunks.z());
                        }
                    } finally {
                        recordCall(budget.nanoTime() - started, "candidate.initialize");
                    }
                    if (longestCallNanos > CompassConfig.slowCallMillis * 1_000_000L) {
                        return SearchStep.limit("slow_call");
                    }
                    if (plan == null) {
                        chunks.next();
                        continue;
                    }
                }

                SearchStep step;
                try {
                    step = plan.advance(budget);
                } finally {
                    recordCall(plan.longestCallNanos(), plan.longestOperation());
                }
                if (step.state == SearchStep.State.RUNNING || step.state == SearchStep.State.LIMIT
                        || step.state == SearchStep.State.ERROR) {
                    return step;
                }
                releaseCandidate();
                chunks.next();
                if (step.target != null && context.contains(step.target.position)) {
                    return step;
                }
            }
            return chunks.hasNext() ? SearchStep.running() : SearchStep.exhausted();
        } catch (SearchLimitException limit) {
            return SearchStep.limit(limit.getMessage());
        }
    }

    private void releaseCandidate() {
        SimulationPlan finished = plan;
        plan = null;
        try {
            if (finished != null) {
                finished.close();
            }
        } finally {
            if (simulation != null) {
                simulation.world().clearCandidate();
            }
        }
    }

    private void recordCall(long elapsed, String operation) {
        if (elapsed > longestCallNanos) {
            longestCallNanos = elapsed;
            longestOperation = operation;
        }
    }

    @Override
    public String longestOperation() {
        return longestOperation;
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
    public int generatedChunks() {
        return simulation == null ? 0 : simulation.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return simulation == null ? 0 : simulation.peakCachedChunks();
    }

    @Override
    public long longestCallNanos() {
        return longestCallNanos;
    }

    @Override
    public void close() {
        try (SimulationScope ignored = SimulationScope.enter()) {
            try {
                releaseCandidate();
            } finally {
                if (simulation != null) {
                    simulation.close();
                    simulation = null;
                }
            }
        }
    }
}
