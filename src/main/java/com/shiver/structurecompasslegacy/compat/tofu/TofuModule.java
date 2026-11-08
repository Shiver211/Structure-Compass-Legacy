package com.shiver.structurecompasslegacy.compat.tofu;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

public final class TofuModule implements CompatModule {

    private static final String PROVIDER = "cn.mcmod.tofucraft.world.WorldProviderTofu";
    private static final String GENERATOR = "cn.mcmod.tofucraft.world.ChunkProviderTofu";
    private static final String STRUCTURES = "cn.mcmod.tofucraft.world.gen.structure.";

    @Override
    public String modId() {
        return "tofucraft";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                GENERATOR, IChunkGenerator.class, World.class, long.class);
        ModConstructor<MapGenStructure> village = ModConstructor.bind(
                STRUCTURES + "MapGenTofuVillage", MapGenStructure.class);
        ModConstructor<MapGenStructure> castle = ModConstructor.bind(
                STRUCTURES + "tofucastle.MapGenTofuCastle", MapGenStructure.class, terrain.type());
        ModConstructor<MapGenStructure> mineshaft = ModConstructor.bind(
                STRUCTURES + "tofumineshaft.MapGenTofuMineshaft", MapGenStructure.class);

        GeneratorFactories.register(PROVIDER, world -> terrain.create(world, world.getSeed()));
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        entries.map("village", world -> village.create(), true);
        entries.map("castle",
                world -> castle.create(world.simulationChunks().chunkGenerator), true);
        entries.map("mineshaft", world -> mineshaft.create(), false);
    }
}
