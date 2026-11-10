package com.shiver.structurecompasslegacy.compat.erebus;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.layer.GenLayer;

/** 原版结构受探索顺序影响，只预测候选区域，不改变自然生成。 */
public final class ErebusModule implements CompatModule {

    private static final String PROVIDER = "erebus.world.WorldProviderErebus";

    @Override
    public String modId() {
        return "erebus";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                "erebus.world.ChunkProviderErebus", IChunkGenerator.class, World.class, long.class);
        ModMethod layers = ModMethod.bind(ModConstructor.type("erebus.world.genlayer.GenLayerErebus"),
                "initializeAllBiomeGenerators", long.class, WorldType.class);
        ErebusDecoration decoration = new ErebusDecoration();
        GeneratorFactories.register(PROVIDER, world -> terrain.create(world, world.getSeed()));

        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        register(entries, "giant_eucalyptus", "ULTERIOR_OUTBACK", "GiantEucalyptus",
                "erebus_giant_eucalyptus_structures", 100, layers, decoration);
        register(entries, "antlion_pyramid", "VOLCANIC_DESERT", "AntlionMaze",
                "erebus_antlion_maze_structures", 800, layers, null);
    }

    private void register(ModuleRegistration entries, String id, String biomeField, String dataClass,
            String dataKey, int exclusion, ModMethod layers, ErebusDecoration decoration) {
        Biome biome = (Biome) ModFields.read(ModFields.bind("erebus.ModBiomes", biomeField), null);
        ErebusLocations locations = new ErebusLocations(dataClass, dataKey, exclusion);
        entries.prediction(id, context -> {
            GenLayer layer;
            try (SimulationScope ignored = SimulationScope.enter()) {
                layer = ((GenLayer[]) layers.call(null, context.seed, context.source.getWorldType()))[1];
            }
            return new ErebusCandidateTask(context, layer, Biome.getIdForBiome(biome), locations, decoration);
        }, () -> true, "structurecompasslegacy.limitation.erebus_" + id);
    }
}
