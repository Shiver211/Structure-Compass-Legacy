package com.shiver.structurecompasslegacy.compat.lostaether;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

/** 只绑定经过审核的天境生成入口；每个候选使用独立生成器和装饰器。 */
final class PlatinumPopulation {

    private final ModConstructor<BiomeDecorator> decorators = ModConstructor.bind(
            "com.gildedgames.the_aether.world.biome.AetherBiomeDecorator", BiomeDecorator.class);
    private final Field random;
    private final Field[] structures;
    private final Method bronze;

    PlatinumPopulation(Class<?> provider) {
        try {
            random = field(provider, "rand");
            structures = new Field[] {
                    field(provider, "largeColdAercloudStructure"),
                    field(provider, "silverDungeonStructure"),
                    field(provider, "goldenDungeonStructure")
            };
            bronze = provider.getDeclaredMethod("generateBronzeDungeon", BlockPos.class);
            bronze.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind Aether population", exception);
        }
    }

    BiomeDecorator newDecorator() {
        return decorators.create();
    }

    void populate(SimulationWorld world, BiomeDecorator decorator, int x, int z) {
        IChunkGenerator generator = world.simulationChunks().chunkGenerator;
        Random rand = (Random) ModFields.read(random, generator);
        rand.setSeed(world.getSeed());
        long seedX = rand.nextLong() / 2 * 2 + 1;
        long seedZ = rand.nextLong() / 2 * 2 + 1;
        rand.setSeed((x * 16L) * seedX + (z * 16L) * seedZ ^ world.getSeed());
        BlockPos origin = new BlockPos(x * 16, 0, z * 16);
        ChunkPos chunk = new ChunkPos(x, z);
        for (Field structure : structures) {
            ((MapGenStructure) ModFields.read(structure, generator)).generateStructure(world, rand, chunk);
        }
        try {
            bronze.invoke(generator, origin);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Aether bronze generation failed", cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke Aether bronze generation", exception);
        }
        Biome biome = world.getBiome(origin.add(16, 0, 16));
        // 不能调用 biome.decorate：群系及其 decorator 是真实世界共享对象。
        decorator.decorate(world, rand, biome, origin);
        // 原生流程剩余的动物生成不改变中心方块，省略以免执行实体初始化。
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
