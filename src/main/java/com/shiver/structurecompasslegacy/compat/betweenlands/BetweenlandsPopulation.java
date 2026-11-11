package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Method;
import java.util.Random;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.IChunkGenerator;

final class BetweenlandsPopulation {

    private static final String PACKAGE = "thebetweenlands.common.world.gen.biome.decorator.";
    private static final float FORTRESS_ATTEMPTS = 0.05F;

    // 保持 super.decorate → 洞穴各子项 → 地下结构 → 火焰 → 要塞的原生顺序。
    // 前两项的次数依赖噪声或随机数，在开始该项时再计算。
    // 每项只保护它使用的共享工具；刷怪笼也会调用蘑菇、草和钟乳石工具。
    private final Decoration[] decorations = {
            new Decoration("generateSpeleothemCluster", 0, "GEN_SPELEOTHEM"),
            new Decoration("generateCavePotsCluster", 0, "GEN_CAVE_POTS"),
            new Decoration("generateLyestone", 100, "LYESTONE"),
            new Decoration("generateCaveThornsCluster", 140, "GEN_CAVE_THORNS"),
            new Decoration("generateCaveMossCluster", 120, "GEN_CAVE_MOSS"),
            new Decoration("generateCaveHangersCluster", 120, "GEN_CAVE_HANGERS"),
            new Decoration("generateUndergroundMossCluster", 25, "GEN_MOSS_UNDERGROUND"),
            new Decoration("generateUndergroundLichenCluster", 5, "GEN_LICHEN_UNDERGROUND"),
            new Decoration("generateCaveGrassCluster", 120, "GEN_CAVE_GRASS"),
            new Decoration("generateDeepmanSimulacrum", 5, "GEN_DEEPMAN_SIMULACRUM"),
            new Decoration("generateLakeCavernSimulacrum", 0.6F, "GEN_LAKE_CAVERN_SIMULACRUM"),
            new Decoration("generateUndergroundPebbleClusterLand", 8, "GEN_PEBBLE_CLUSTER_LAND"),
            new Decoration("generateUndergroundPebbleClusterWater", 8, "GEN_PEBBLE_CLUSTER_WATER"),
            new Decoration("generateStagnantWaterPool", 2, "GEN_STAGNANT_WATER_POOL"),
            new Decoration("generateUndergroundRuins", 200, "GEN_UNDERGROUND_RUINS"),
            new Decoration("generateSpawner", 1, "GEN_SPAWNER", "GEN_BIG_BULB_CAPPED_MUSHROOM",
                    "GEN_BULB_CAPPED_MUSHROOMS", "GEN_SWAMP_TALLGRASS", "GEN_SPELEOTHEM"),
            new Decoration("generateTarPoolDungeon", 1, "GEN_TAR_POOL_DUNGEON")
    };

    // 邻区块核验需要要塞之后的装饰：树木、遗迹和植物会影响目标起点及随机消耗。
    private final Decoration[] surfaceDecorations = {
            new Decoration("generateGiantTree", 0.25F, "GEN_GIANT_TREE"),
            new Decoration("generateWeedwoodTree", 1, "GEN_WEEDWOOD_TREE"),
            new Decoration("generateSpawnerStructure", 0.2F, "GEN_SPAWNER_STRUCTURE"),
            new Decoration("generateSmallRuinsCluster", 2.2F, "GEN_SMALL_RUINS"),
            new Decoration("generatePhragmites", 300),
            new Decoration("generateSmallHollowLog", 5, "GEN_SMALL_HOLLOW_LOG"),
            new Decoration("generateSwampTallgrassCluster", 30, "GEN_SWAMP_TALLGRASS"),
            new Decoration("generateSundew", 5),
            new Decoration("generateNettlesCluster", 2, "GEN_NETTLES"),
            new Decoration("generateWeepingBlue", 3),
            new Decoration("generateWisp", 4),
            new Decoration("generateArrowArumCluster", 2, "GEN_ARROW_ARUM"),
            new Decoration("generatePickerelWeedCluster", 2, "GEN_PICKEREL_WEED"),
            new Decoration("generateMarshHibiscusCluster", 2, "GEN_MARSH_HIBISCUS"),
            new Decoration("generateMarshMallowCluster", 2, "GEN_MARSH_MALLOW"),
            new Decoration("generateButtonBushCluster", 2, "GEN_BUTTON_BUSH"),
            new Decoration("generateSoftRushCluster", 90, "GEN_SOFT_RUSH"),
            new Decoration("generateBroomsedge", 40),
            new Decoration("generateBottleBrushGrassCluster", 5, "GEN_BOTTLE_BRUSH_GRASS"),
            new Decoration("generatePebbleClusterLand", 2, "GEN_PEBBLE_CLUSTER_LAND"),
            new Decoration("generatePebbleClusterWater", 4, "GEN_PEBBLE_CLUSTER_WATER")
    };

    private final BetweenlandsSharedGenerators shared;
    private final Method ores;
    private final Method init;
    private final Method randomPosition;
    private final Method paddedPosition;
    private final Method additive;
    private final Method ground;
    private final Method canGenerate;
    private final Method validBiome;
    private final ModConstructor<Object> decorators = ModConstructor.bind(
            PACKAGE + "BiomeDecoratorMarsh", Object.class, Biome.class);
    private final ModConstructor<Object> fortresses = ModConstructor.bind(
            "thebetweenlands.common.world.gen.feature.structure.WorldGenWightFortress", Object.class);
    private final Object mixedGround;
    private final Block peat;

    private BetweenlandsTerrain terrain;
    private Object decorator;
    private Random random;
    private int group;
    private int remaining;
    private int fires;
    private boolean needsOres;
    private boolean completeDecoration;

    BetweenlandsPopulation() {
        Class<?> positions = ModConstructor.type(PACKAGE + "DecoratorPositionProvider");
        ores = BetweenlandsReflection.method(PACKAGE + "BiomeDecoratorBetweenlands", "generateOres");
        init = BetweenlandsReflection.method(positions.getName(), "init",
                World.class, Biome.class, IChunkGenerator.class, Random.class, int.class, int.class);
        randomPosition = BetweenlandsReflection.method(positions.getName(), "getRandomPos");
        paddedPosition = BetweenlandsReflection.method(positions.getName(), "getRandomPos", int.class);
        additive = BetweenlandsReflection.method(PACKAGE + "DecorationHelper", "getSpeleothemAttemptAdditive", positions);
        canGenerate = BetweenlandsReflection.method(fortresses.type().getName(), "canGenerateAt",
                World.class, Random.class, BlockPos.class);
        validBiome = BetweenlandsReflection.method(fortresses.type().getName(), "isBiomeValid", Biome.class);
        Class<?> surface = ModConstructor.type(PACKAGE + "SurfaceType");
        mixedGround = ModFields.read(ModFields.bind(surface.getName(), "MIXED_GROUND"), null);
        ground = BetweenlandsReflection.method(surface.getName(), "matches", World.class, BlockPos.class);
        peat = (Block) ModFields.read(ModFields.bind("thebetweenlands.common.registries.BlockRegistry", "PEAT"), null);
        shared = new BetweenlandsSharedGenerators();
    }

    void begin(BetweenlandsTerrain terrain, int chunkX, int chunkZ) {
        begin(terrain, chunkX, chunkZ, false);
    }

    void begin(BetweenlandsTerrain terrain, int chunkX, int chunkZ, boolean completeDecoration) {
        this.terrain = terrain;
        this.completeDecoration = completeDecoration;
        BetweenlandsStorage.attach(terrain.world);
        // 与原生 populate 共用生成器 Random，保留级联地形生成的随机重置行为。
        random = terrain.random();
        random.setSeed(terrain.world.getSeed());
        long seedX = random.nextLong() / 2L * 2L + 1L;
        long seedZ = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) chunkX * seedX + (long) chunkZ * seedZ ^ terrain.world.getSeed());
        Biome biome = terrain.world.getBiome(new BlockPos(chunkX * 16 + 16, 0, chunkZ * 16 + 16));
        decorator = decorators.create(biome);
        BetweenlandsReflection.call(init, decorator,
                terrain.world, biome, terrain.nativeGenerator, random, chunkX * 16, chunkZ * 16);
        group = 0;
        remaining = -1;
        fires = 0;
        needsOres = true;
    }

    /** 每次至多一个原生尝试，不把数百次洞穴装饰放在一个 tick 内。 */
    boolean step() {
        if (needsOres) {
            shared.run(() -> BetweenlandsReflection.call(ores, decorator));
            random.nextFloat(); // super.decorate 的 generate(1, populateCaves)。
            needsOres = false;
            return false;
        }
        if (group == decorations.length && fires < 10) {
            BlockPos position = (BlockPos) BetweenlandsReflection.call(randomPosition, decorator);
            if (terrain.world.getBlockState(position).getBlock() == peat) {
                terrain.world.setBlockState(position.up(), Blocks.FIRE.getDefaultState());
            }
            fires++;
            return false;
        }
        if (group > decorations.length + (completeDecoration ? surfaceDecorations.length : 0)) {
            decorator = null;
            terrain = null;
            return true;
        }
        if (remaining < 0) {
            float tries = group == decorations.length ? FORTRESS_ATTEMPTS : decoration().attempts;
            if (group == 0) {
                tries = 2 + ((Number) BetweenlandsReflection.call(additive, null, decorator)).intValue();
            } else if (group == 1) {
                tries = random.nextInt(3) == 0 ? 2 : 1;
            }
            int whole = (int) Math.floor(tries);
            // 原生用 <=；即使次数是整数，也必须消耗一次 nextFloat。
            remaining = whole + (random.nextFloat() <= tries - whole ? 1 : 0);
            return false;
        }
        if (remaining > 0) {
            if (group == decorations.length) {
                verifyFortress();
            } else {
                Decoration current = decoration();
                current.shared.run(() -> BetweenlandsReflection.call(current.method, null, decorator));
            }
            remaining--;
        } else {
            group++;
            remaining = -1;
        }
        return false;
    }

    private Decoration decoration() {
        return group < decorations.length ? decorations[group]
                : surfaceDecorations[group - decorations.length - 1];
    }

    private void verifyFortress() {
        BlockPos position = (BlockPos) BetweenlandsReflection.call(paddedPosition, decorator, 1);
        if (!terrain.world.isAirBlock(position)
                || !(Boolean) BetweenlandsReflection.call(ground, mixedGround, terrain.world, position.down())) {
            return;
        }
        Object fortress = fortresses.create();
        if ((Boolean) BetweenlandsReflection.call(validBiome, fortress, terrain.world.getBiome(position))
                && (Boolean) BetweenlandsReflection.call(canGenerate, fortress, terrain.world, random, position)) {
            // 本体布局没有额外存在性条件，导航无需创建保护区、战利品或 Boss。
            terrain.found(position);
        }
    }

    private static final class Decoration {

        final Method method;
        final float attempts;
        final BetweenlandsSharedGenerators shared;

        Decoration(String name, float attempts, String... generators) {
            Class<?> positions = ModConstructor.type(PACKAGE + "DecoratorPositionProvider");
            method = BetweenlandsReflection.method(PACKAGE + "DecorationHelper", name, positions);
            this.attempts = attempts;
            shared = new BetweenlandsSharedGenerators(generators);
        }
    }
}
