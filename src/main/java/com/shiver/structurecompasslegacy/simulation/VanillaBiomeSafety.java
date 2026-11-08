package com.shiver.structurecompasslegacy.simulation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeBeach;
import net.minecraft.world.biome.BiomeDesert;
import net.minecraft.world.biome.BiomeForest;
import net.minecraft.world.biome.BiomeForestMutated;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.biome.BiomeMushroomIsland;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.biome.BiomePlains;
import net.minecraft.world.biome.BiomeRiver;
import net.minecraft.world.biome.BiomeSavanna;
import net.minecraft.world.biome.BiomeSnow;
import net.minecraft.world.biome.BiomeStoneBeach;
import net.minecraft.world.biome.BiomeSwamp;

/** 注册群系对象是共享的，只有已确认地形方法不改自身字段的精确类可以调用。 */
public final class VanillaBiomeSafety {

    private static final Set<Class<?>> READ_ONLY = new HashSet<>(Arrays.asList(
            BiomeBeach.class, BiomeDesert.class, BiomeForest.class, BiomeForestMutated.class,
            BiomeJungle.class, BiomeMushroomIsland.class, BiomeOcean.class, BiomePlains.class,
            BiomeRiver.class, BiomeSavanna.class, BiomeSnow.class, BiomeStoneBeach.class, BiomeSwamp.class));

    private VanillaBiomeSafety() {
    }

    public static boolean supports(Biome biome) {
        return READ_ONLY.contains(biome.getClass());
    }

    public static void requireSafe(Biome[] biomes) {
        for (Biome biome : biomes) {
            if (!supports(biome)) {
                throw new UnsupportedOperationException("Unisolated biome terrain: " + biome.getClass().getName());
            }
        }
    }
}
