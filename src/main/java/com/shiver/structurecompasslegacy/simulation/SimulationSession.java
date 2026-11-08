package com.shiver.structurecompasslegacy.simulation;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public final class SimulationSession implements AutoCloseable {

    private SimulationWorld world;

    public SimulationSession(WorldServer source) {
        if (!GeneratorFactories.supports(source)) {
            throw new UnsupportedOperationException("No isolated generator for " + source.provider.getClass().getName());
        }

        try (SimulationScope ignored = SimulationScope.enter()) {
            world = new SimulationWorld(source);
        }

        if (DimensionManager.getWorld(source.provider.getDimension()) != source) {
            throw new IllegalStateException("Simulation dimension isolation failed");
        }
    }

    public SimulationWorld world() {
        return world;
    }

    public int generatedChunks() {
        return world.simulationChunks().generatedChunks();
    }

    public int peakCachedChunks() {
        return world.simulationChunks().peakChunks();
    }

    public void beginCandidate() {
        world.clearCandidate();
    }

    @Override
    public void close() {
        if (world != null) {
            world.clearCandidate();
            world = null;
        }
    }
}
