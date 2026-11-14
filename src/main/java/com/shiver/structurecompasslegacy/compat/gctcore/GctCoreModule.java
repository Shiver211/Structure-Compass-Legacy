package com.shiver.structurecompasslegacy.compat.gctcore;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.WorldServer;

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
    }
}
