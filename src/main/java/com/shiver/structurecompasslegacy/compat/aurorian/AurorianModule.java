package com.shiver.structurecompasslegacy.compat.aurorian;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

public final class AurorianModule implements CompatModule {

    private static final String ROOT = "com.shiroroku.theaurorian.";
    private static final String PROVIDER = ROOT + "World.AurorianWorldProvider";
    private static final String STRUCTURES = ROOT + "World.Structures.";

    @Override
    public String modId() {
        return "theaurorian";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        IntSupplier density = ModFields.integer(ROOT + "AurorianConfig", "Config_DungeonDensity");
        registerGrid(entries, "moon_temple", "MoonTempleWorldGenerator",
                "CHUNKS_BETWEEN_TEMPLES", "GENERATE_TEMPLES", () -> density.getAsInt() + density.getAsInt() / 2, 200);
        registerGrid(entries, "darkstone_dungeon", "DarkstoneDungeonWorldGenerator",
                "CHUNKS_BETWEEN_DUNGEONS", "GENERATE_DUNGEON", () -> density.getAsInt() / 2, 64);
        registerGrid(entries, "runestone_tower", "RunestoneTowerWorldGenerator",
                "CHUNKS_BETWEEN_TOWERS", "GENERATE_TOWERS", () -> 0, 64);

        // 四种遗迹的概率不由世界种子决定，保留目录中的不支持条目，不绑定模拟器。
    }

    private void registerGrid(
            ModuleRegistration entries,
            String id,
            String generator,
            String spacingField,
            String enabledField,
            IntSupplier offset,
            int height) {
        IntSupplier spacing = ModFields.integer(STRUCTURES + generator, spacingField);
        BooleanSupplier enabled = ModFields.flag(STRUCTURES + generator, enabledField);
        String limitation = height == 64 ? "structurecompasslegacy.limitation.reference_height" : "";
        entries.prediction(id,
                context -> new AurorianGridTask(context, spacing.getAsInt(), offset.getAsInt(), height), enabled, limitation);
    }
}
