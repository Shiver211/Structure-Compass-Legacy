package com.shiver.structurecompasslegacy.compat.dungeonadditions;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

import com.shiver.structurecompasslegacy.compat.ModFields;
import net.minecraftforge.common.BiomeDictionary;

/** 只读生成参数和群系类型缓存；不读取结构坐标或模板计数。 */
final class DungeonArenaSettings {

    private static final String ROOT = "com.dungeon_additions.da.";

    static int obsidilithSpacing() {
        Object generator = ModFields.read(privateField(ROOT + "world.ModWorldGen", "obsidilithArena"), null);
        Field spacing = privateField(generator.getClass().getName(), "spacing");
        return ((Number) ModFields.read(spacing, generator)).intValue();
    }

    static Supplier<String[]> biomeTypes(boolean nether) {
        String generator = ROOT + "world." + (nether
                ? "nether_arena.WorldGenNetherArena" : "obsidilith_arena.WorldGenObsidilithArena");
        Field cached = privateField(generator, nether ? "burningFlameBiomeTypes" : "highCityBiomeTypes");
        Field configured = ModFields.bind(ROOT + "config.WorldConfig", nether
                ? "biome_types_blacklist_burning_arena" : "obsdilith_arena_whitelist");
        return () -> {
            @SuppressWarnings("unchecked")
            List<BiomeDictionary.Type> types = (List<BiomeDictionary.Type>) ModFields.read(cached, null);
            return types == null ? ((String[]) ModFields.read(configured, null)).clone()
                    : types.stream().map(BiomeDictionary.Type::getName).toArray(String[]::new);
        };
    }

    private static Field privateField(String owner, String name) {
        try {
            Field field = Class.forName(owner).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind Dungeon Additions field: " + owner + "." + name, exception);
        }
    }
}
