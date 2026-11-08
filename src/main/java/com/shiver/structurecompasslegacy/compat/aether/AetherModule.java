package com.shiver.structurecompasslegacy.compat.aether;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

public final class AetherModule implements CompatModule {

    private static final String PROVIDER = "com.gildedgames.the_aether.world.AetherWorldProvider";
    private static final String GENERATOR = "com.gildedgames.the_aether.world.ChunkProviderAether";
    private static final String STRUCTURES = "com.gildedgames.the_aether.world.gen.";

    @Override
    public String modId() {
        return "aether_legacy";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                GENERATOR, IChunkGenerator.class, World.class, long.class);
        ModConstructor<MapGenStructure> silver = ModConstructor.bind(
                STRUCTURES + "MapGenSilverDungeon", MapGenStructure.class);
        ModConstructor<MapGenStructure> gold = ModConstructor.bind(
                STRUCTURES + "MapGenGoldenDungeon", MapGenStructure.class);

        GeneratorFactories.register(PROVIDER, world -> terrain.create(world, world.getSeed()));
        ModuleRegistration entries = new ModuleRegistration(registry, modId(), PROVIDER);
        entries.map("silver_dungeon", world -> silver.create(), true);
        entries.map("golden_dungeon", world -> gold.create(), true);
    }
}
