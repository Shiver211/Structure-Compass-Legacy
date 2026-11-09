package com.shiver.structurecompasslegacy.compat.betweenlands;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.layer.GenLayer;

final class BetweenlandsBiomeLayers {

    private static final String PACKAGE = "thebetweenlands.common.world.gen.layer.";
    private final ModMethod factory;

    BetweenlandsBiomeLayers() {
        Class<?> baseType = ModConstructor.type(PACKAGE + "GenLayerBetweenlands");
        factory = ModMethod.bind(baseType, "initializeAllBiomeGenerators", long.class, WorldType.class);
    }

    GenLayer create(long seed, WorldType worldType) {
        GenLayer[] layers;
        try (SimulationScope ignored = SimulationScope.enter()) {
            layers = (GenLayer[]) factory.call(null, seed, worldType);
        }
        return layers[1];
    }
}
