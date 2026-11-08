package com.shiver.structurecompasslegacy.compat.draconic;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

import net.minecraft.world.WorldProvider;

public final class DraconicModule implements CompatModule {

    private static final String CONFIG = "com.brandon3055.draconicevolution.DEConfig";

    @Override
    public String modId() {
        return "draconicevolution";
    }

    @Override
    public void register(StructureRegistry registry) {
        IntSupplier separation = ModFields.integer(CONFIG, "chaosIslandSeparation");
        IntSupplier heightOffset = ModFields.integer(CONFIG, "chaosIslandYOffset");
        BooleanSupplier worldGeneration = ModFields.flag(CONFIG, "worldGenEnabled");
        BooleanSupplier islands = ModFields.flag(CONFIG, "generateChaosIslands");
        IntSupplier cometRarity = ModFields.integer(CONFIG, "cometRarity");
        BooleanSupplier comets = ModFields.flag(CONFIG, "generateEnderComets");

        ModuleRegistration entries = new ModuleRegistration(
                registry, modId(), world -> generatesIn(world.provider));
        entries.prediction("chaos_island",
                context -> new ChaosIslandTask(context, separation.getAsInt(), 80 + heightOffset.getAsInt()),
                () -> worldGeneration.getAsBoolean() && islands.getAsBoolean());
        entries.prediction("ender_comet",
                context -> new EnderCometTask(context, cometRarity.getAsInt()),
                () -> worldGeneration.getAsBoolean() && comets.getAsBoolean());
    }

    static boolean generatesIn(WorldProvider provider) {
        return provider.getDimension() == 1;
    }
}
