package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.DimensionRule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class IceAndFireModule implements CompatModule {

    private static final String ROOT = "com.github.alexthe666.iceandfire.";
    private final Map<String, Field> settings = new HashMap<>();
    private final Map<String, Field> positions = new HashMap<>();
    private Field config;
    private Object generator;

    @Override
    public String modId() {
        return "iceandfire";
    }

    @Override
    public void register(StructureRegistry registry) {
        try {
            Class<?> events = Class.forName(ROOT + "event.WorldGenEvents");
            // The original 1.12 release shares a mod id but not the RotN placement rules.
            events.getDeclaredField("LIGHTNING_DRAGON_CAVE");
            Field registered = GameRegistry.class.getDeclaredField("worldGenerators");
            registered.setAccessible(true);
            for (Object item : (Iterable<?>) registered.get(null)) {
                if (events.isInstance(item)) {
                    generator = item;
                    break;
                }
            }
            if (generator == null) {
                throw new IllegalStateException("Ice and Fire world generator not registered");
            }
            config = ModFields.bind(ROOT + "IceAndFire", "CONFIG");
            for (Field field : config.getType().getFields()) {
                settings.put(field.getName(), field);
            }
            for (Field field : events.getDeclaredFields()) {
                if (field.getType() == BlockPos.class && field.getName().startsWith("last")) {
                    field.setAccessible(true);
                    positions.put(field.getName(), field);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported Ice and Fire generator", e);
        }
        for (IceLandmark kind : IceLandmark.values()) {
            boolean dragons = kind.material != null;
            DimensionRule dimensions = DimensionRule.matchingIds(id -> allows(id, dragons));
            new ModuleRegistration(registry, modId(), dimensions).prediction(kind.id,
                ctx -> new IceSearch(ctx, kind, this), () -> flag(kind.setting));
        }
    }

    Object value(String key) {
        return ModFields.read(settings.get(key), ModFields.read(config, null));
    }

    Map<String, Object> snapshot() {
        Object current = ModFields.read(config, null);
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Field> setting : settings.entrySet()) {
            values.put(setting.getKey(), ModFields.read(setting.getValue(), current));
        }
        return values;
    }

    boolean flag(String key) {
        return (Boolean) value(key);
    }

    int integer(String key) {
        return (Integer) value(key);
    }

    boolean allows(int id, boolean dragons) {
        if (id == integer("dreadlandsDimensionId")) {
            return false;
        }
        boolean blacklist = flag("useDimensionBlackList");
        int[] list = (int[]) value((dragons ? "dragon" : "structure")
            + (blacklist ? "BlacklistedDimensions" : "WhitelistedDimensions"));
        return blacklist != Arrays.stream(list).anyMatch(n -> n == id);
    }

    BlockPos last(String key) {
        // Live generator instance; never instantiate a replacement or write its spacing state.
        return (BlockPos) ModFields.read(positions.get(key), generator);
    }
}
