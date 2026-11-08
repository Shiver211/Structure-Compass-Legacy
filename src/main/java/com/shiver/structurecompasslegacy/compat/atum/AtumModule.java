package com.shiver.structurecompasslegacy.compat.atum;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

public final class AtumModule implements CompatModule {

    private static final String PROVIDER = "com.teammetallurgy.atum.world.WorldProviderAtum";
    private static final String GENERATOR = "com.teammetallurgy.atum.world.ChunkGeneratorAtum";
    private static final String STRUCTURES = "com.teammetallurgy.atum.world.gen.structure.";

    @Override
    public String modId() {
        return "atum";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                GENERATOR, IChunkGenerator.class, World.class, long.class, boolean.class, String.class);
        GeneratorFactories.register(PROVIDER, world -> terrain.create(
                world, world.getSeed(), false, world.getWorldInfo().getGeneratorOptions()));

        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        registerLandmark(entries, terrain.type(), "pyramid", "pyramid.MapGenPyramid");
        registerLandmark(entries, terrain.type(), "tomb", "tomb.MapGenTomb");
        registerLandmark(entries, terrain.type(), "girafi_tomb", "girafitomb.MapGenGirafiTomb");
        registerLandmark(entries, terrain.type(), "lighthouse", "lighthouse.MapGenLighthouse");

        ModConstructor<MapGenStructure> mineshaft = ModConstructor.bind(
                STRUCTURES + "mineshaft.MapGenAtumMineshaft", MapGenStructure.class);
        entries.map("mineshaft", world -> mineshaft.create(), false);
    }

    private void registerLandmark(
            ModuleRegistration entries,
            Class<?> terrainType,
            String id,
            String generatorClass) {
        ModConstructor<MapGenStructure> structure = ModConstructor.bind(
                STRUCTURES + generatorClass, MapGenStructure.class, terrainType);
        entries.map(id,
                world -> structure.create(world.simulationChunks().chunkGenerator), true);
    }
}
