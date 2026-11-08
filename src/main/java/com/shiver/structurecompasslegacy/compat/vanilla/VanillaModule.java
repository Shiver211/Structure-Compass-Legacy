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

import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenEndCity;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
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
        register(registry, "village", 0,
                world -> new MapGenVillage(flatOptions(world, "village")), null);
        register(registry, "mineshaft", 0,
                world -> new MapGenMineshaft(flatOptions(world, "mineshaft")), null);
        register(registry, "stronghold", 0,
                world -> new MapGenStronghold(flatOptions(world, "stronghold")), null);
        register(registry, "ocean_monument", 0,
                world -> new StructureOceanMonument(flatOptions(world, "oceanmonument")), null);
        register(registry, "desert_pyramid", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.DesertPyramid.class::isInstance);
        register(registry, "jungle_temple", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.JunglePyramid.class::isInstance);
        register(registry, "swamp_hut", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.SwampHut.class::isInstance);
        register(registry, "igloo", 0,
                VanillaModule::temple, ComponentScatteredFeaturePieces.Igloo.class::isInstance);
        register(registry, "woodland_mansion", 0,
                VanillaModule::mansion, null);
        register(registry, "nether_fortress", -1,
                world -> new MapGenNetherBridge(), null);
        register(registry, "end_city", 1,
                world -> new MapGenEndCity((ChunkGeneratorEnd) world.simulationChunks().chunkGenerator), null);
    }

    private void register(
            StructureRegistry registry,
            String id,
            int dimension,
            Function<SimulationWorld, MapGenStructure> factory,
            Predicate<StructureComponent> componentFilter) {
        boolean validateLayout = componentFilter != null || id.equals("village") || id.equals("woodland_mansion");
        registry.activate("minecraft:" + id,
                DimensionRule.ofIds(dimension),
                MapStructureTask.locator(factory, componentFilter, validateLayout), "", () -> true);
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

    private static Map<String, String> flatOptions(SimulationWorld world, String feature) {
        if (world.getWorldType() != WorldType.FLAT) {
            return Collections.emptyMap();
        }

        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(world.getWorldInfo().getGeneratorOptions());
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
