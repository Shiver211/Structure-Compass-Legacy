package com.shiver.structurecompasslegacy.compat.vanilla;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.DimensionRule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.search.MapStructureTask;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenEndCity;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraft.world.gen.structure.WoodlandMansion;

public final class VanillaModule implements CompatModule {

    private static final Map<String, Function<SimulationWorld, MapGenStructure>> TEMPLES = new HashMap<>();
    private static final Map<String, Function<SimulationWorld, MapGenStructure>> MANSIONS = new HashMap<>();

    /** 世界类型可替换原版结构入口，兼容逻辑仍留在对应模组内。 */
    public static void registerTerrainStructures(
            String worldTypeClass,
            Function<SimulationWorld, MapGenStructure> temple,
            Function<SimulationWorld, MapGenStructure> mansion) {
        TEMPLES.put(worldTypeClass, temple);
        MANSIONS.put(worldTypeClass, mansion);
    }

    @Override
    public String modId() {
        return "minecraft";
    }

    @Override
    public void register(StructureRegistry registry) {
        Predicate<WorldServer> templeNeedsTerrain = world -> TEMPLES.containsKey(world.getWorldType().getClass().getName());
        register(registry, "village", 0,
                world -> new MapGenVillage(flatOptions(world, "village")), null, world -> false);
        register(registry, "mineshaft", 0,
                world -> new MapGenMineshaft(flatOptions(world, "mineshaft")), null, world -> false);
        registry.activate("minecraft:stronghold",
                DimensionRule.ofIds(0),
                StrongholdTask.locator(), "", () -> true);
        register(registry, "ocean_monument", 0,
                world -> new StructureOceanMonument(flatOptions(world, "oceanmonument")), null, world -> false);
        register(registry, "desert_pyramid", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.DesertPyramid.class::isInstance,
                templeNeedsTerrain);
        register(registry, "jungle_temple", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.JunglePyramid.class::isInstance,
                templeNeedsTerrain);
        register(registry, "swamp_hut", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.SwampHut.class::isInstance,
                templeNeedsTerrain);
        register(registry, "igloo", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.Igloo.class::isInstance,
                templeNeedsTerrain);
        register(registry, "woodland_mansion", 0,
                VanillaModule::mansion, null, world -> true);
        register(registry, "nether_fortress", -1,
                world -> new MapGenNetherBridge(), null, world -> false);
        register(registry, "end_city", 1,
                world -> new MapGenEndCity((ChunkGeneratorEnd) world.simulationChunks().chunkGenerator), null,
                world -> true);
    }

    private void register(
            StructureRegistry registry,
            String id,
            int dimension,
            Function<SimulationWorld, MapGenStructure> factory,
            Predicate<StructureComponent> componentFilter,
            Predicate<WorldServer> needsTerrain) {
        boolean validateLayout = componentFilter != null || id.equals("village") || id.equals("woodland_mansion");
        registry.activate("minecraft:" + id,
                DimensionRule.ofIds(dimension),
                MapStructureTask.locator(factory, componentFilter, validateLayout, needsTerrain), "", () -> true);
    }

    private static MapGenStructure temple(SimulationWorld world) {
        Function<SimulationWorld, MapGenStructure> factory = TEMPLES.get(world.getWorldType().getClass().getName());
        if (factory != null) {
            return factory.apply(world);
        }
        return new MapGenScatteredFeature(flatOptions(world, "biome_1"));
    }

    private static MapGenStructure mansion(SimulationWorld world) {
        Function<SimulationWorld, MapGenStructure> factory = MANSIONS.get(world.getWorldType().getClass().getName());
        return factory == null
                ? new WoodlandMansion((ChunkGeneratorOverworld) world.simulationChunks().chunkGenerator)
                : factory.apply(world);
    }

    static boolean hasFlatFeature(World world, String feature) {
        if (world.getWorldType() != WorldType.FLAT) {
            return true;
        }

        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(
                world.getWorldInfo().getGeneratorOptions());
        return info.getWorldFeatures().containsKey(feature);
    }

    static Map<String, String> flatOptions(World world, String feature) {
        if (world.getWorldType() != WorldType.FLAT) {
            return Collections.emptyMap();
        }

        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(
                world.getWorldInfo().getGeneratorOptions());
        Map<String, String> options = info.getWorldFeatures().get(feature);
        if (options == null) {
            throw new UnsupportedOperationException("Flat world does not generate " + feature);
        }

        if (feature.equals("village")) {
            options.putIfAbsent("size", "1");
        }
        return options;
    }
}
