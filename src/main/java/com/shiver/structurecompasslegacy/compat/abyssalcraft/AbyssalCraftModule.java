package com.shiver.structurecompasslegacy.compat.abyssalcraft;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.search.MapStructureTask;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

public final class AbyssalCraftModule implements CompatModule {

    private static final String PACKAGE = "com.shinoow.abyssalcraft.common.";
    private static final String PROVIDERS = PACKAGE + "world.WorldProvider";

    @Override
    public String modId() {
        return "abyssalcraft";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                PACKAGE + "world.ChunkProviderAbyss", IChunkGenerator.class,
                World.class, long.class, boolean.class);
        ModConstructor<MapGenStructure> stronghold = ModConstructor.bind(
                PACKAGE + "structures.abyss.stronghold.MapGenAbyStronghold", MapGenStructure.class);

        GeneratorFactories.register(PROVIDERS + "Abyss", world -> terrain.create(world, world.getSeed(), false));

        ModuleRegistration wastelands = new ModuleRegistration(registry, modId(), PROVIDERS + "Abyss");
        wastelands.prediction("abyssal_stronghold",
                MapStructureTask.locator(world -> stronghold.create(), null, false),
                () -> true, "structurecompasslegacy.limitation.reference_height");

        ModuleRegistration dreadlands = new ModuleRegistration(registry, modId(), PROVIDERS + "Dreadlands");
        dreadlands.prediction("dreadlands_mineshaft", DreadlandsMineshaftTask::new,
                () -> true, "structurecompasslegacy.limitation.reference_height");

        ModuleRegistration omothol = new ModuleRegistration(registry, modId(), PROVIDERS + "Omothol");
        omothol.prediction("jzahar_temple", JzaharTempleTask::new,
                () -> true, "structurecompasslegacy.limitation.reference_height");
    }
}
