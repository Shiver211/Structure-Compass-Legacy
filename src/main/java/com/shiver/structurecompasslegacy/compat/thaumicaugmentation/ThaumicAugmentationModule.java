package com.shiver.structurecompasslegacy.compat.thaumicaugmentation;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.search.MapStructureTask;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

public final class ThaumicAugmentationModule implements CompatModule {

    private static final String WORLD = "thecodex6824.thaumicaugmentation.common.world.";

    @Override
    public String modId() {
        return "thaumicaugmentation";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                WORLD + "ChunkGeneratorEmptiness", IChunkGenerator.class, World.class);
        ModConstructor<MapGenStructure> spire = ModConstructor.bind(
                WORLD + "structure.MapGenEldritchSpire", MapGenStructure.class,
                ModConstructor.type(WORLD + "ITAChunkGenerator"));
        Field generation = ModFields.bind("thecodex6824.thaumicaugmentation.api.TAConfig", "generateSpires");
        ModMethod enabled = ModMethod.bind(generation.getType(), "getValue");

        GeneratorFactories.register(WORLD + "WorldProviderEmptiness", world -> terrain.create(world));
        ModuleRegistration entries = new ModuleRegistration(
                registry, modId(), WORLD + "WorldProviderEmptiness");
        entries.prediction("eldritch_spire",
                MapStructureTask.locator(world -> spire.create(world.simulationChunks().chunkGenerator), null, true),
                () -> (Boolean) enabled.call(ModFields.read(generation, null)));
    }
}
