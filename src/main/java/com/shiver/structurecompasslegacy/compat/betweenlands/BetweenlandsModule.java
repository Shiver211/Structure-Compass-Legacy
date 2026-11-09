package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.function.BiFunction;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.compat.betweenlands.BetweenlandsLandmarkTask.Landmark;
import net.minecraft.world.WorldServer;
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
        int clearingId = Biome.getIdForBiome((Biome) ModFields.read(
                ModFields.bind(BIOMES, "SLUDGE_PLAINS_CLEARING"), null));
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
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
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                GenLayer biomes = layers.create(context.seed, context.source.getWorldInfo().getTerrainType(),
                        context.source.getBiomeProvider());
                return factory.apply(context, biomes);
            }

            @Override
            public String unavailableReason(WorldServer world) {
                return world.getBiomeProvider().getClass().getName().equals(BetweenlandsBiomeLayers.PROVIDER)
                        && !world.getBiomeProvider().isFixedBiome()
                        ? "" : "structurecompasslegacy.reason.betweenlands_biome_layers";
            }
        };
    }
}
