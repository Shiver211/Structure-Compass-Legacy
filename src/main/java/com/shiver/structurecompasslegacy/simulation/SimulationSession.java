package com.shiver.structurecompasslegacy.simulation;

import java.util.function.Function;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.IChunkGenerator;
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
        this(source, checkedFactory(source, terrainEnabled));
    }

    private static Function<SimulationWorld, IChunkGenerator> checkedFactory(WorldServer source, boolean terrain) {
        if (!(terrain ? GeneratorFactories.supports(source) : GeneratorFactories.supportsBiomes(source))) {
            throw new UnsupportedOperationException("No isolated generator for " + source.provider.getClass().getName());
        }
        return terrain ? GeneratorFactories::create : null;
    }

    public SimulationSession(WorldServer source, Function<SimulationWorld, IChunkGenerator> terrain) {
        if (source.getMinecraftServer() != null && !source.getMinecraftServer().isCallingFromMinecraftThread()) {
            throw new IllegalStateException("Simulation must run on the server thread");
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            world = new SimulationWorld(source, terrain);
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
