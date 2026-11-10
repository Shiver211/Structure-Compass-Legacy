package com.shiver.structurecompasslegacy.compat.lostaether;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.IChunkGenerator;

public final class LostAetherModule implements CompatModule {

    private static final String PROVIDER = "com.gildedgames.the_aether.world.AetherWorldProvider";
    private static final String CONFIG = "com.gildedgames.the_aether.AetherConfig";

    @Override
    public String modId() {
        return "lost_aether";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                "com.gildedgames.the_aether.world.ChunkProviderAether", IChunkGenerator.class, World.class, long.class);
        PlatinumPopulation population = new PlatinumPopulation(terrain.type());
        Object dimension = ModFields.read(ModFields.bind(CONFIG, "dimension"), null);
        int dimensionId = ((Number) ModFields.read(
                ModFields.bind(CONFIG + "$Dimension", "aether_dimension_id"), dimension)).intValue();

        new ModuleRegistration(registry, modId(), dimensionId).prediction("platinum_dungeon", new StructureLocator() {
            @Override
            public SearchTask createTask(SearchContext context) {
                return new PlatinumDungeonTask(context, population);
            }

            @Override
            public String unavailableReason(WorldServer world) {
                return world.provider.getClass().getName().equals(PROVIDER)
                        ? "" : "structurecompasslegacy.reason.generator_not_adapted";
            }
        }, () -> true, "structurecompasslegacy.limitation.lost_aether_platinum_center");
    }
}
