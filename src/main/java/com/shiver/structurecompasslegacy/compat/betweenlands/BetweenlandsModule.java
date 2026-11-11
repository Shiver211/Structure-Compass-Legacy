package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.function.BiFunction;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.betweenlands.BetweenlandsLandmarkTask.Landmark;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;

public final class BetweenlandsModule implements CompatModule {

    private static final String PROVIDER = "thebetweenlands.common.world.WorldProviderBetweenlands";
    private static final String BIOMES = "thebetweenlands.common.registries.BiomeRegistry";

    @Override
    public String modId() {
        return "thebetweenlands";
    }

    @Override
    public void register(StructureRegistry registry) {
        BetweenlandsBiomeLayers layers = new BetweenlandsBiomeLayers();
        Biome raisedIsles = (Biome) ModFields.read(ModFields.bind(BIOMES, "RAISED_ISLES"), null);
        Biome coarseIslands = (Biome) ModFields.read(ModFields.bind(BIOMES, "COARSE_ISLANDS"), null);
        int clearingId = Biome.getIdForBiome((Biome) ModFields.read(ModFields.bind(BIOMES, "SLUDGE_PLAINS_CLEARING"), null));
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        int marsh0 = Biome.getIdForBiome((Biome) ModFields.read(ModFields.bind(BIOMES, "MARSH_0"), null));
        int marsh1 = Biome.getIdForBiome((Biome) ModFields.read(ModFields.bind(BIOMES, "MARSH_1"), null));
        GeneratorFactories.register(PROVIDER, BetweenlandsTerrain::new);
        entries.prediction("wight_fortress", locator(layers, (context, biomes) ->
                new BetweenlandsFortressTask(context, biomes, marsh0, marsh1)),
                () -> true, "structurecompasslegacy.limitation.betweenlands_fortress");
        entries.prediction("floating_island", locator(layers, (context, biomes) ->
                new BetweenlandsLandmarkTask(context, Landmark.FLOATING_ISLAND, biomes, raisedIsles, coarseIslands)),
                () -> true, "structurecompasslegacy.limitation.betweenlands_island_anchor");
        entries.prediction("giant_root", locator(layers, (context, biomes) ->
                new BetweenlandsLandmarkTask(context, Landmark.GIANT_ROOT, biomes, raisedIsles, coarseIslands)),
                () -> true, "structurecompasslegacy.limitation.betweenlands_root_anchor");
        entries.prediction("sludge_worm_dungeon", locator(layers, (context, biomes) ->
                new BetweenlandsDungeonTask(context, biomes, clearingId)),
                () -> true, "structurecompasslegacy.limitation.betweenlands_dungeon_region");
    }

    private StructureLocator locator(BetweenlandsBiomeLayers layers,
            BiFunction<SearchContext, GenLayer, SearchTask> factory) {
        return context -> {
            GenLayer biomes = layers.create(context.seed, context.source.getWorldInfo().getTerrainType());
            return factory.apply(context, biomes);
        };
    }
}
