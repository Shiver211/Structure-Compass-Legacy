package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableCollection;
import com.shiver.structurecompasslegacy.compat.ModConstructor;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.WorldGenerator;

final class BetweenlandsSharedGenerators {

    private static final String[] GENERATORS = {
            "GEN_SPELEOTHEM", "GEN_CAVE_POTS", "LYESTONE", "GEN_CAVE_THORNS", "GEN_CAVE_MOSS",
            "GEN_CAVE_HANGERS", "GEN_MOSS_UNDERGROUND", "GEN_LICHEN_UNDERGROUND", "GEN_CAVE_GRASS",
            "GEN_DEEPMAN_SIMULACRUM", "GEN_LAKE_CAVERN_SIMULACRUM", "GEN_PEBBLE_CLUSTER_LAND",
            "GEN_PEBBLE_CLUSTER_WATER", "GEN_STAGNANT_WATER_POOL", "GEN_UNDERGROUND_RUINS",
            "GEN_SPAWNER", "GEN_TAR_POOL_DUNGEON", "GEN_GIANT_TREE", "GEN_WEEDWOOD_TREE",
            "GEN_SPAWNER_STRUCTURE", "GEN_SMALL_RUINS", "GEN_SMALL_HOLLOW_LOG", "GEN_SWAMP_TALLGRASS",
            "GEN_NETTLES", "GEN_ARROW_ARUM", "GEN_PICKEREL_WEED", "GEN_MARSH_HIBISCUS", "GEN_MARSH_MALLOW",
            "GEN_BUTTON_BUSH", "GEN_SOFT_RUSH", "GEN_BOTTLE_BRUSH_GRASS", "GEN_BIG_BULB_CAPPED_MUSHROOM",
            "GEN_BULB_CAPPED_MUSHROOMS"
    };

    private final List<Slot> slots = new ArrayList<>();

    BetweenlandsSharedGenerators() {
        this(GENERATORS);
    }

    BetweenlandsSharedGenerators(String... generators) {
        Class<?> helper = ModConstructor.type("thebetweenlands.common.world.gen.biome.decorator.DecorationHelper");
        for (String name : generators) {
            Object generator = BetweenlandsReflection.read(BetweenlandsReflection.field(helper, name), null);
            for (Class<?> type = generator.getClass(); WorldGenerator.class.isAssignableFrom(type); type = type.getSuperclass()) {
                addSlots(generator, type);
            }
        }
    }

    private void addSlots(Object owner, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Modifier.isFinal(field.getModifiers()) || List.class.isAssignableFrom(field.getType())
                    || field.getType().isArray() || field.getType() == BlockPos.MutableBlockPos.class) {
                field.setAccessible(true);
                slots.add(new Slot(owner, field));
                // 地下刷怪笼会复用内部位置提供器，恢复引用还不足以恢复其坐标、世界和随机源。
                if (field.getType().getName().equals(
                        "thebetweenlands.common.world.gen.biome.decorator.DecoratorPositionProvider")) {
                    addSlots(BetweenlandsReflection.read(field, owner), field.getType());
                }
            }
        }
    }

    /** 原生工具是静态单例；每个同步步骤结束后恢复引用和可变内容。 */
    void run(Runnable action) {
        if (slots.isEmpty()) {
            action.run();
            return;
        }
        List<Snapshot> snapshots = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            snapshots.add(new Snapshot(slot));
        }
        try {
            action.run();
        } finally {
            for (Snapshot snapshot : snapshots) {
                snapshot.restore();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restoreList(List target, List previous) {
        target.clear();
        target.addAll(previous);
    }

    private static final class Slot {

        final Object owner;
        final Field field;

        Slot(Object owner, Field field) {
            this.owner = owner;
            this.field = field;
        }
    }

    private static final class Snapshot {

        final Slot slot;
        final Object value;
        final Object contents;

        Snapshot(Slot slot) {
            this.slot = slot;
            value = BetweenlandsReflection.read(slot.field, slot.owner);
            if (value instanceof List && !(value instanceof ImmutableCollection)) {
                contents = new ArrayList<>((List<?>) value);
            } else if (value instanceof BlockPos.MutableBlockPos) {
                contents = ((BlockPos) value).toImmutable();
            } else if (value != null && value.getClass().isArray()) {
                int length = Array.getLength(value);
                contents = Array.newInstance(value.getClass().getComponentType(), length);
                System.arraycopy(value, 0, contents, 0, length);
            } else {
                contents = null;
            }
        }

        void restore() {
            BetweenlandsReflection.write(slot.field, slot.owner, value);
            if (value instanceof List && contents != null) {
                restoreList((List<?>) value, (List<?>) contents);
            } else if (value instanceof BlockPos.MutableBlockPos) {
                ((BlockPos.MutableBlockPos) value).setPos((BlockPos) contents);
            } else if (contents != null) {
                System.arraycopy(contents, 0, value, 0, Array.getLength(value));
            }
        }
    }
}
