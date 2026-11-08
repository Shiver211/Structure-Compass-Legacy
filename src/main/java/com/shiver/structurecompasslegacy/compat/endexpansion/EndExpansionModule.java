package com.shiver.structurecompasslegacy.compat.endexpansion;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

public final class EndExpansionModule implements CompatModule {

    private static final String CONFIG = "com.jboymercs.endexpansion.config.WorldConfig";

    @Override
    public String modId() {
        return "endexpansion";
    }

    @Override
    public void register(StructureRegistry registry) {
        BooleanSupplier structures = ModFields.flag(CONFIG, "does_structure_spawn");
        BooleanSupplier islands = ModFields.flag(CONFIG, "lamented_islands_enabled");
        Field excludedTypes = ModFields.bind(CONFIG, "biome_types_lamented_islands");

        // 原自然入口按维度 ID 判断，兼容 Nether API 替换的末地 provider。
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), 1);
        entries.prediction("lamented_islands", LamentedIslandsTask.locator(
                ModFields.integer(CONFIG, "structureFrequency"),
                () -> (String[]) ModFields.read(excludedTypes, null)),
                () -> structures.getAsBoolean() && islands.getAsBoolean());
    }
}
