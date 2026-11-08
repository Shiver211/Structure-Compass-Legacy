package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** 原生测试的资源替身：只替换方块材质与注册数据，不替换概率、图层或几何算法。 */
public final class BetweenlandsNativeTypes {

    public interface Weight {
        short getWeight();
    }

    public static final class WeightedBiome extends Biome implements Weight {

        private final short weight;

        WeightedBiome(String name, int weight) {
            super(new BiomeProperties(name));
            this.weight = (short) weight;
            ResourceLocation key = new ResourceLocation("scl_betweenlands_test", name);
            setRegistryName(key);
            ForgeRegistries.BIOMES.register(this);
        }

        @Override
        public short getWeight() {
            return weight;
        }
    }

    public static final class Biomes {

        public static final List<WeightedBiome> REGISTERED_BIOMES = new ArrayList<>();
        public static final WeightedBiome PATCHY_ISLANDS = biome("patchy_islands", 20);
        public static final WeightedBiome SWAMPLANDS = biome("swamplands", 25);
        public static final WeightedBiome DEEP_WATERS = biome("deep_waters", 12);
        public static final WeightedBiome COARSE_ISLANDS = biome("coarse_islands", 16);
        public static final WeightedBiome RAISED_ISLES = biome("raised_isles", 16);
        public static final WeightedBiome SLUDGE_PLAINS = biome("sludge_plains", 5);
        public static final WeightedBiome MARSH_0 = biome("marsh_0", 10);
        public static final WeightedBiome MARSH_1 = biome("marsh_1", 4);
        public static final WeightedBiome SWAMPLANDS_CLEARING = biome("swamplands_clearing", 0);
        public static final WeightedBiome SLUDGE_PLAINS_CLEARING = biome("sludge_plains_clearing", 0);

        private static WeightedBiome biome(String name, int weight) {
            WeightedBiome biome = new WeightedBiome(name, weight);
            REGISTERED_BIOMES.add(biome);
            return biome;
        }
    }

    public static final class Config {
        public static final Debug DEBUG = new Debug();
    }

    public static final class Debug {
        public boolean debug;
        public final HashSet<String> biomeList = new HashSet<>();
    }

    public static final class CoarseFeature {
        public void initializeGenerators(long seed, Biome biome) {
            // 两个被测 MapGen 都没有读取这个字段生成的噪声。
        }
    }

    public static Block block(String name) {
        switch (name) {
            case "SWAMP_WATER": return Blocks.WATER;
            case "CRAGROCK": return Blocks.COBBLESTONE;
            case "BETWEENSTONE": return Blocks.STONE;
            case "MUD": return Blocks.CLAY;
            case "SWAMP_GRASS": return Blocks.GRASS;
            case "SWAMP_DIRT": return Blocks.DIRT;
            case "ROOT": return Blocks.PLANKS;
            case "HANGER": return Blocks.VINE;
            case "GIANT_ROOT": return Blocks.LOG;
            case "LEAVES_WEEDWOOD_TREE": return Blocks.LEAVES;
            case "SHELF_FUNGUS": return Blocks.BROWN_MUSHROOM_BLOCK;
            case "MOSS": return Blocks.CARPET;
            default: throw new AssertionError("Unexpected native block: " + name);
        }
    }

    public static IBlockState property(IBlockState state, IProperty<?> property, Comparable<?> value) {
        return state;
    }
}
