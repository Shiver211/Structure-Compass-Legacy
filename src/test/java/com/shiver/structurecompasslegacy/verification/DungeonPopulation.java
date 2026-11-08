package com.shiver.structurecompasslegacy.verification;

import java.util.Random;

import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;

/** 原版 populate 中大型结构之后、群系装饰之前的顺序；调用方须先排除大型结构。 */
final class DungeonPopulation {

    private final World world;
    private final ChunkGeneratorSettings settings;
    private final BlockPos origin;
    private final Random random;
    private int attempts;

    DungeonPopulation(World world, ChunkGeneratorSettings settings, int chunkX, int chunkZ) {
        this.world = world;
        this.settings = settings;
        this.origin = new BlockPos(chunkX * 16, 0, chunkZ * 16);
        this.random = new Random(world.getSeed());
        // 保留原式：负数的 / 2 * 2 + 1 与按位 | 1 不等价。
        long seedX = random.nextLong() / 2L * 2L + 1L;
        long seedZ = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) chunkX * seedX + (long) chunkZ * seedZ ^ world.getSeed());
    }

    void generateWaterLake() {
        Biome biome = world.getBiome(origin.add(16, 0, 16));
        if (biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS
                && settings.useWaterLakes && random.nextInt(settings.waterLakeChance) == 0) {
            int x = random.nextInt(16) + 8;
            int y = random.nextInt(256);
            int z = random.nextInt(16) + 8;
            new WorldGenLakes(Blocks.WATER).generate(world, random, origin.add(x, y, z));
        }
    }

    void generateLavaLake() {
        // 原版在 useLavaLakes 之前消耗概率随机数，关闭熔岩湖也不能省略它。
        if (random.nextInt(settings.lavaLakeChance / 10) == 0 && settings.useLavaLakes) {
            int x = random.nextInt(16) + 8;
            int y = random.nextInt(random.nextInt(248) + 8);
            int z = random.nextInt(16) + 8;
            if (y < world.getSeaLevel() || random.nextInt(settings.lavaLakeChance / 8) == 0) {
                new WorldGenLakes(Blocks.LAVA).generate(world, random, origin.add(x, y, z));
            }
        }
    }

    boolean hasNextDungeon() {
        return settings.useDungeons && attempts < settings.dungeonChance;
    }

    BlockPos generateNextDungeon() {
        int x = random.nextInt(16) + 8;
        int y = random.nextInt(256);
        int z = random.nextInt(16) + 8;
        BlockPos position = origin.add(x, y, z);
        attempts++;
        // 失败生成同样保留随机消耗及临时修改，下一次尝试继续读取它们。
        return new WorldGenDungeons().generate(world, random, position) ? position : null;
    }
}
