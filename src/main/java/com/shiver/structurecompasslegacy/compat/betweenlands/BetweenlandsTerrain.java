package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;

/** 保留注册群系的身份，只重建可变地形工具，不执行群系构造器。 */
final class BetweenlandsTerrain implements IChunkGenerator {

    private static final Class<?> BIOME_TYPE = ModConstructor.type("thebetweenlands.common.world.biome.BiomeBetweenlands");
    private static final Class<?> GENERATOR_TYPE = ModConstructor.type(
            "thebetweenlands.common.world.gen.biome.generator.BiomeGenerator");
    private static final Field BIOME_GENERATOR = BetweenlandsReflection.field(BIOME_TYPE, "biomeGenerator");
    private static final Field FEATURES = BetweenlandsReflection.field(GENERATOR_TYPE, "biomeFeatures");
    private static final ModConstructor<IChunkGenerator> FACTORY = ModConstructor.bind(
            "thebetweenlands.common.world.gen.ChunkGeneratorBetweenlands",
            IChunkGenerator.class, World.class, long.class, Block.class, Block.class, int.class);
    private static final Field RANDOM = BetweenlandsReflection.field(FACTORY.type(), "rand");
    private static final String[] SETTINGS = {
            "bottomBlockState", "underLayerTopBlockState", "baseBlockState", "bottomBlockHeight", "bottomBlockFuzz",
            "fillerBlockHeight", "underLayerBlockHeight", "hasBaseBlockPatches"
    };

    private final Map<Biome, Object> generators = new IdentityHashMap<>();
    private boolean inUse;

    final SimulationWorld world;
    final IChunkGenerator nativeGenerator;
    BlockPos fortress;

    BetweenlandsTerrain(SimulationWorld world) {
        this.world = world;
        Block base = (Block) ModFields.read(ModFields.bind(
                "thebetweenlands.common.registries.BlockRegistry", "BETWEENSTONE"), null);
        Block water = (Block) ModFields.read(ModFields.bind(
                "thebetweenlands.common.registries.BlockRegistry", "SWAMP_WATER"), null);
        nativeGenerator = FACTORY.create(world, world.getSeed(), base, water, 120);
    }

    Random random() {
        return (Random) BetweenlandsReflection.read(RANDOM, nativeGenerator);
    }

    void run(Runnable action) {
        if (inUse) {
            action.run();
            return;
        }
        Map<Biome, Object> previous = new IdentityHashMap<>();
        try {
            // 服务器主线程同步事务，每一步及异常退出都恢复原生字段引用。
            for (Biome biome : Biome.REGISTRY) {
                if (BIOME_TYPE.isInstance(biome)) {
                    Object original = BetweenlandsReflection.read(BIOME_GENERATOR, biome);
                    previous.put(biome, original);
                    Object copy = generators.computeIfAbsent(biome, ignored -> copyGenerator(original));
                    BetweenlandsReflection.write(BIOME_GENERATOR, biome, copy);
                }
            }
            inUse = true;
            action.run();
        } finally {
            inUse = false;
            for (Map.Entry<Biome, Object> entry : previous.entrySet()) {
                BetweenlandsReflection.write(BIOME_GENERATOR, entry.getKey(), entry.getValue());
            }
        }
    }

    private Object copyGenerator(Object original) {
        if (original.getClass() != GENERATOR_TYPE) {
            throw new UnsupportedOperationException("Unknown Betweenlands biome generator: " + original.getClass());
        }
        Object copy = BetweenlandsReflection.construct(GENERATOR_TYPE, new Class<?>[]{Biome.class},
                BetweenlandsReflection.readSetting(original, "biome"));
        for (String name : SETTINGS) {
            Field field = BetweenlandsReflection.field(GENERATOR_TYPE, name);
            BetweenlandsReflection.write(field, copy, BetweenlandsReflection.read(field, original));
        }
        List<Object> features = new ArrayList<>();
        for (Object feature : (List<?>) BetweenlandsReflection.read(FEATURES, original)) {
            features.add(copyFeature(feature));
        }
        BetweenlandsReflection.write(FEATURES, copy, features);
        return copy;
    }

    private Object copyFeature(Object feature) {
        Class<?> type = feature.getClass();
        switch (type.getSimpleName()) {
            case "PatchFeature":
                return BetweenlandsReflection.construct(type,
                        new Class<?>[]{double.class, double.class, IBlockState.class, double.class, double.class},
                        BetweenlandsReflection.readSetting(feature, "scaleX"),
                        BetweenlandsReflection.readSetting(feature, "scaleY"),
                        BetweenlandsReflection.readSetting(feature, "block"), BetweenlandsReflection.readSetting(feature, "mult"),
                        BetweenlandsReflection.readSetting(feature, "offset"));
            case "FlatLandFeature":
                return BetweenlandsReflection.construct(type, new Class<?>[]{int.class, int.class},
                        BetweenlandsReflection.readSetting(feature, "waterHeight"),
                        BetweenlandsReflection.readSetting(feature, "terrainOffset"));
            case "SiltBeachFeature":
                return BetweenlandsReflection.construct(type, new Class<?>[]{float.class},
                        BetweenlandsReflection.readSetting(feature, "terrainWeightThreshold"));
            default:
                if (!type.getName().startsWith("thebetweenlands.common.world.gen.biome.feature.")) {
                    throw new UnsupportedOperationException("Unknown Betweenlands terrain feature: " + type);
                }
                return BetweenlandsReflection.construct(type, new Class<?>[0]);
        }
    }

    void beginCandidate() {
        fortress = null;
    }

    void found(BlockPos position) {
        fortress = position.add(16, 0, 16).toImmutable();
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        Chunk[] result = new Chunk[1];
        run(() -> result[0] = nativeGenerator.generateChunk(x, z));
        return result[0];
    }

    @Override
    public void populate(int x, int z) {
        throw new UnsupportedOperationException("Betweenlands prediction uses staged population");
    }

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos position) {
        return nativeGenerator.getPossibleCreatures(type, position);
    }

    @Override
    public BlockPos getNearestStructurePos(World world, String name, BlockPos position, boolean unexplored) {
        return null;
    }

    @Override
    public boolean isInsideStructure(World world, String name, BlockPos position) {
        return false;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
    }
}
