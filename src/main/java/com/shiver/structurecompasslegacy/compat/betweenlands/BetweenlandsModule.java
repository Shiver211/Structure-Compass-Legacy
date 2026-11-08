package com.shiver.structurecompasslegacy.compat.betweenlands;

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
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        entries.prediction("floating_island", locator(Landmark.FLOATING_ISLAND, layers, raisedIsles, coarseIslands),
                () -> true, "structurecompasslegacy.limitation.betweenlands_island_anchor");
        entries.prediction("giant_root", locator(Landmark.GIANT_ROOT, layers, raisedIsles, coarseIslands),
                () -> true, "structurecompasslegacy.limitation.betweenlands_root_anchor");
    }

    private StructureLocator locator(Landmark landmark, BetweenlandsBiomeLayers layers,
            Biome raisedIsles, Biome coarseIslands) {
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                GenLayer biomes = layers.create(context.seed, context.source.getWorldInfo().getTerrainType(),
                        context.source.getBiomeProvider());
                return new BetweenlandsLandmarkTask(context, landmark, biomes, raisedIsles, coarseIslands);
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
