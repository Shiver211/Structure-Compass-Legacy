package com.shiver.structurecompasslegacy.compat.gctcore;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;

public final class GctCoreModule implements CompatModule {

    @Override
    public String modId() {
        return "gctcore";
    }

    @Override
    public void register(StructureRegistry registry) {
        for (GctCoreStructure structure : GctCoreStructure.values()) {
            GctCorePlacement placement = new GctCorePlacement(structure);
            new ModuleRegistration(registry, modId(), structure.dimension).prediction(structure.id,
                    new StructureLocator() {
                        @Override
                        public SearchTask createTask(SearchContext context) {
                            return new GctCoreTask(context, structure, placement);
                        }

                        @Override
                        public String unavailableReason(WorldServer world) {
                            return GeneratorFactories.supports(world)
                                    ? "" : "structurecompasslegacy.reason.generator_not_adapted";
                        }
                    });
        }
        if (Loader.isModLoaded("aether_legacy")) {
            registerDiamondDungeon(registry);
        }
    }

    private void registerDiamondDungeon(StructureRegistry registry) {
        try {
            Class.forName("com.smd.gctcore.common.world.AetherDiamondDungeonGenerator$DiamondDungeonMapGen",
                    false, getClass().getClassLoader());
        } catch (ClassNotFoundException olderGctCore) {
            return;
        }

        String config = "com.gildedgames.the_aether.AetherConfig";
        Field dimension = ModFields.bind(config, "dimension");
        Field dimensionId = ModFields.bind(dimension.getType().getName(), "aether_dimension_id");
        int id = (Integer) ModFields.read(dimensionId, ModFields.read(dimension, null));
        Field worldGen = ModFields.bind(config, "world_gen");
        String options = worldGen.getType().getName();
        Field primary = ModFields.bind(options, "golden_dungeon_primary_chance");
        Field secondary = ModFields.bind(options, "golden_dungeon_secondary_chance");
        new ModuleRegistration(registry, modId(), id).prediction("diamond_dungeon", context -> {
            Object settings = ModFields.read(worldGen, null);
            return new DiamondDungeonTask(context, (Integer) ModFields.read(primary, settings),
                    (Integer) ModFields.read(secondary, settings));
        }, () -> true, "structurecompasslegacy.limitation.gctcore_diamond_dungeon");
    }
}
