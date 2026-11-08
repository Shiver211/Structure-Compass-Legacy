package com.shiver.structurecompasslegacy.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.ChunkGeneratorHell;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;

/** 模组模块按 provider 类注册工厂，禁止回退到真实世界的生成器。 */
public final class GeneratorFactories {

    private static final Map<String, Function<SimulationWorld, IChunkGenerator>> FACTORIES = new HashMap<>();

    static {
        register("net.minecraft.world.WorldProviderSurface", GeneratorFactories::overworld);
        register("net.minecraft.world.WorldProviderHell", world -> new ChunkGeneratorHell(world, false, world.getSeed()));
        register("net.minecraft.world.WorldProviderEnd", world -> new ChunkGeneratorEnd(
                world, false, world.getSeed(), world.provider.getSpawnCoordinate()));
    }

    private GeneratorFactories() {
    }

    public static void register(String providerClass, Function<SimulationWorld, IChunkGenerator> factory) {
        FACTORIES.put(providerClass, factory);
    }

    public static boolean supports(WorldServer world) {
        return FACTORIES.containsKey(world.provider.getClass().getName());
    }

    public static IChunkGenerator create(SimulationWorld world) {
        Function<SimulationWorld, IChunkGenerator> factory = FACTORIES.get(world.provider.getClass().getName());
        if (factory == null) {
            throw new UnsupportedOperationException("No isolated generator for " + world.provider.getClass().getName());
        }

        return factory.apply(world);
    }

    private static IChunkGenerator overworld(SimulationWorld world) {
        WorldType type = world.getWorldType();
        String options = world.getWorldInfo().getGeneratorOptions();
        if (type == WorldType.FLAT) {
            return new ChunkGeneratorFlat(world, world.getSeed(), false, options);
        }

        if (type == WorldType.DEFAULT || type == WorldType.DEFAULT_1_1 || type == WorldType.LARGE_BIOMES
                || type == WorldType.AMPLIFIED || type == WorldType.CUSTOMIZED) {
            return new ChunkGeneratorOverworld(world, world.getSeed(), false, options);
        }

        throw new UnsupportedOperationException("No isolated terrain factory for " + type.getName());
    }
}
