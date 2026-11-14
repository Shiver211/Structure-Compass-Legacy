package com.shiver.structurecompasslegacy.compat.gctadditions;

import java.util.function.IntSupplier;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.IChunkGenerator;

public final class GctAdditionsModule implements CompatModule {

    private static final String DIMENSIONS = "com.shiver.gct_additions.common.world.dimension.";
    static final String WARPED_PROVIDER = DIMENSIONS + "WorldWarpedRuin$WorldProviderMod";
    static final String ALFHEIM_PROVIDER = DIMENSIONS + "WorldAlfheim$WorldProviderMod";

    @Override
    public String modId() {
        return "gct_additions";
    }

    @Override
    public void register(StructureRegistry registry) {
        registerGenerator(
                DIMENSIONS + "WorldBesideVoid$WorldProviderMod",
                DIMENSIONS + "WorldBesideVoid",
                DIMENSIONS + "WorldBesideVoid$ChunkProviderModded");
        registerGenerator(
                WARPED_PROVIDER,
                DIMENSIONS + "WorldWarpedRuin",
                DIMENSIONS + "WorldWarpedRuin$ChunkProviderModded");
        registerGenerator(
                ALFHEIM_PROVIDER,
                DIMENSIONS + "WorldAlfheim",
                DIMENSIONS + "WorldAlfheim$ChunkProviderModded");

        ModuleRegistration warped = new ModuleRegistration(registry, modId(), WARPED_PROVIDER);
        ModuleRegistration alfheim = new ModuleRegistration(registry, modId(), ALFHEIM_PROVIDER);

        for (GctAdditionsStructure structure : GctAdditionsStructure.values()) {
            Biome requiredBiome = structure.biomeClass == null
                    ? null : (Biome) ModFields.read(ModFields.bind(structure.biomeClass, "biome"), null);
            ModuleRegistration targetDimension = WARPED_PROVIDER.equals(structure.providerClass) ? warped : alfheim;
            targetDimension.prediction(structure.id, GctAdditionsTask.locator(structure, requiredBiome));
        }
    }

    private static void registerGenerator(String providerClass, String dimensionClass, String generatorClass) {
        IntSupplier dimId = ModFields.integer(dimensionClass, "DIMID");
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                generatorClass, IChunkGenerator.class, World.class, long.class);
        GeneratorFactories.register(
                providerClass, world -> terrain.create(world, world.getSeed() - dimId.getAsInt()));
    }
}
