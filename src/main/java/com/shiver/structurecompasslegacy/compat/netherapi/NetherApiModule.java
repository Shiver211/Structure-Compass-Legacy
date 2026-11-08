package com.shiver.structurecompasslegacy.compat.netherapi;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import com.shiver.structurecompasslegacy.simulation.WorldInfoCopies;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.WorldInfo;

/** 末地城的高度检查必须使用 Nether API 的生成器和独立群系图层。 */
public final class NetherApiModule implements CompatModule {

    private static final String WORLD_PACKAGE = "git.jbredwards.nether_api.mod.common.world.";

    @Override
    public String modId() {
        return "nether_api";
    }

    @Override
    public void register(StructureRegistry registry) {
        GeneratorFactories.registerBiomes(WORLD_PACKAGE + "WorldProviderNether");
        Class<? extends WorldInfo> infoType = ModConstructor.type(WORLD_PACKAGE + "DerivedWorldInfoMulti")
                .asSubclass(WorldInfo.class);
        NetherWorldInfo info = new NetherWorldInfo(
                infoType, ModConstructor.type(WORLD_PACKAGE + "DerivedWorldInfoMulti$Data"));
        WorldInfoCopies.register(infoType, info::snapshot);

        Class<?> biomeProvider = ModConstructor.type(WORLD_PACKAGE + "biome.BiomeProviderTheEnd");
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                WORLD_PACKAGE + "gen.ChunkGeneratorTheEnd", IChunkGenerator.class,
                World.class, boolean.class, biomeProvider, BlockPos.class);

        GeneratorFactories.register(WORLD_PACKAGE + "WorldProviderTheEnd", world -> terrain.create(
                world, false, world.getBiomeProvider(), world.provider.getSpawnCoordinate()));
    }
}
