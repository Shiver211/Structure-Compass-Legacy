package com.shiver.structurecompasslegacy.compat.biomesoplenty;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.compat.vanilla.VanillaModule;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** BOP 主世界的群系和基础地形；不执行装饰与 populate。 */
public final class BiomesOPlentyModule implements CompatModule {

    private static final String WORLD = "biomesoplenty.common.world.";
    private static ModConstructor<Object> settings;
    private static ModMethod biomeLayers;

    @Override
    public String modId() {
        return "biomesoplenty";
    }

    @Override
    public void register(StructureRegistry registry) {
        settings = ModConstructor.bind(WORLD + "BOPWorldSettings", Object.class, String.class);
        biomeLayers = ModMethod.bind(ModConstructor.type(WORLD + "BiomeProviderBOP"),
                "setupBOPGenLayers", long.class, settings.type());
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                WORLD + "ChunkGeneratorOverworldBOP", IChunkGenerator.class,
                World.class, long.class, boolean.class, String.class);
        Field temple = ReflectionHelper.findField(terrain.type(), "scatteredFeatureGenerator");
        Field mansion = ReflectionHelper.findField(terrain.type(), "woodlandMansionGenerator");

        GeneratorFactories.registerWorldType(WORLD + "WorldTypeBOP", world -> terrain.create(
                world, world.getSeed(), false, world.getWorldInfo().getGeneratorOptions()));
        VanillaModule.registerTerrainStructures(WORLD + "WorldTypeBOP",
                world -> (MapGenStructure) ModFields.read(temple, world.simulationChunks().chunkGenerator),
                world -> (MapGenStructure) ModFields.read(mansion, world.simulationChunks().chunkGenerator));
    }

    public static BiomeProvider createBiomes(World world) {
        Object options = settings.create(world.getWorldInfo().getGeneratorOptions());
        GenLayer[] layers = (GenLayer[]) biomeLayers.call(null, world.getSeed(), options);
        return new BopBiomeProvider(layers);
    }
}
