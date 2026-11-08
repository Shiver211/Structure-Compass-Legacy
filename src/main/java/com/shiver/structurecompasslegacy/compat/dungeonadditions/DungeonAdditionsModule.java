package com.shiver.structurecompasslegacy.compat.dungeonadditions;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

public final class DungeonAdditionsModule implements CompatModule {

    private static final String CONFIG = "com.dungeon_additions.da.config.WorldConfig";

    @Override
    public String modId() {
        return "da";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModuleRegistration nether = new ModuleRegistration(registry, modId(), -1);
        nether.prediction("nether_arena", DungeonArenaTask.locator(
                DungeonArenaTask.Arena.NETHER,
                ModFields.integer(CONFIG, "burning_arena_weight"),
                ModFields.integer(CONFIG, "burning_arena_y_level"),
                DungeonArenaSettings.biomeTypes(true)),
                ModFields.flag(CONFIG, "burning_flame_arena_enabled"));

        int[] dimensions = (int[]) ModFields.read(ModFields.bind(CONFIG, "list_of_dimensions_obsidilith_arena"), null);
        ModuleRegistration obsidilith = new ModuleRegistration(registry, modId(), dimensions);
        int spacing = DungeonArenaSettings.obsidilithSpacing();
        obsidilith.prediction("obsidilith_arena", DungeonArenaTask.locator(
                DungeonArenaTask.Arena.OBSIDILITH,
                () -> spacing,
                ModFields.integer(CONFIG, "obsidilith_y_height"),
                DungeonArenaSettings.biomeTypes(false)),
                ModFields.flag(CONFIG, "obsidilith_arena_enabled"));
    }

}
