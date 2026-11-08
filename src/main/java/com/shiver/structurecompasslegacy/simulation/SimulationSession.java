package com.shiver.structurecompasslegacy.simulation;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public final class SimulationSession implements AutoCloseable {

    private SimulationWorld world;

    public SimulationSession(WorldServer source) {
        this(source, true);
    }

    public static SimulationSession forBiomes(WorldServer source) {
        return new SimulationSession(source, false);
    }

    private SimulationSession(WorldServer source, boolean terrainEnabled) {
        if (source.getMinecraftServer() != null && !source.getMinecraftServer().isCallingFromMinecraftThread()) {
            throw new IllegalStateException("Simulation must run on the server thread");
        }
        if (!(terrainEnabled ? GeneratorFactories.supports(source) : GeneratorFactories.supportsBiomes(source))) {
            throw new UnsupportedOperationException("No isolated generator for " + source.provider.getClass().getName());
        }

        try (SimulationScope ignored = SimulationScope.enter()) {
            world = new SimulationWorld(source, terrainEnabled);
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
        world.beginCandidate();
    }

    @Override
    public void close() {
        if (world != null) {
            try (SimulationScope ignored = SimulationScope.enter()) {
                world.clearCandidate();
            } finally {
                world = null;
            }
        }
    }
}
