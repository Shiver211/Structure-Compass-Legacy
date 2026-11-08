package com.shiver.structurecompasslegacy.compat.divinerpg;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

public final class DivineRpgModule implements CompatModule {

    @Override
    public String modId() {
        return "divinerpg";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModuleRegistration iceika = new ModuleRegistration(registry, modId(),
                "divinerpg.dimensions.iceika.WorldProviderIceika");
        ModuleRegistration vethea = new ModuleRegistration(registry, modId(),
                "divinerpg.dimensions.vethea.VetheaWorldProvider");

        for (DivineStructure structure : DivineStructure.values()) {
            ModuleRegistration dimension = structure.layer == 0 ? iceika : vethea;
            dimension.prediction(structure.id, context -> new DivineStructureTask(context, structure));
        }
    }
}
