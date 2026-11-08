package com.shiver.structurecompasslegacy.search;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

/** 每个模块先筛候选，再执行经过审核的自然放置流程。 */
public abstract class SimulationSearchTask extends ChunkSearchTask {

    private SimulationSession simulation;

    protected SimulationSearchTask(SearchContext context) {
        super(context);
    }

    protected abstract boolean isCandidate(int chunkX, int chunkZ);

    protected abstract void generate(SimulationWorld world, int chunkX, int chunkZ);

    @Override
    protected final SearchTarget inspect(int chunkX, int chunkZ) {
        if (!isCandidate(chunkX, chunkZ)) {
            return null;
        }

        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
            }
            simulation.beginCandidate();
            SimulationWorld world = simulation.world();
            world.beginPlacement();
            generate(world, chunkX, chunkZ);
            return world.finishPlacement();
        }
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
