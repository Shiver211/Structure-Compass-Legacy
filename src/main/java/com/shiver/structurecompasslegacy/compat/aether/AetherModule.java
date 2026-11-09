package com.shiver.structurecompasslegacy.compat.aether;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
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

        ModMethod foundation = ModMethod.bind(terrain.type(), "setBlocksInChunk", int.class, int.class, ChunkPrimer.class);
        Field settings = ModFields.bind("com.gildedgames.the_aether.AetherConfig", "world_gen");
        String[] names = {"bronze", "silver", "golden"};
        Field[] chances = new Field[6];
        for (int i = 0; i < names.length; i++) {
            String type = "com.gildedgames.the_aether.AetherConfig$WorldGenOptions";
            chances[i * 2] = ModFields.bind(type, names[i] + "_dungeon_primary_chance");
            chances[i * 2 + 1] = ModFields.bind(type, names[i] + "_dungeon_secondary_chance");
        }
        entries.prediction("bronze_dungeon", context -> {
            Object options = ModFields.read(settings, null);
            int[] snapshot = new int[chances.length];
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = ((Number) ModFields.read(chances[i], options)).intValue();
            }
            return new BronzeDungeonTask(context, snapshot, terrain, foundation);
        }, () -> true, "structurecompasslegacy.limitation.aether_bronze_region");
    }
}
