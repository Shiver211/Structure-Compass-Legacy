package com.shiver.structurecompasslegacy.verification;

import com.shiver.structurecompasslegacy.simulation.VanillaBiomeSafety;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.ISaveHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class DungeonPopulationTest {

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void replayMatchesVanillaPopulateBeforeDecoration() throws Exception {
        // 以实际原版 populate 为参考，不在测试中再抄一份随机公式。
        for (long seed : new long[] {0, 1, -17, 8675309}) {
            for (int lakes = 0; lakes < 4; lakes++) {
                ChunkGeneratorSettings.Factory options = new ChunkGeneratorSettings.Factory();
                options.useWaterLakes = (lakes & 1) != 0;
                options.useLavaLakes = (lakes & 2) != 0;
                options.waterLakeChance = 1;
                options.lavaLakeChance = 10;
                ChunkGeneratorSettings settings = options.build();
                Fixture vanilla = new Fixture(seed, false);
                Random referenceRandom = runVanilla(vanilla, settings, -7, 11);

                Fixture simulated = new Fixture(seed, false);
                DungeonPopulation replay = runReplay(simulated, settings, -7, 11);
                assertEquals(vanilla.reads, simulated.reads, "block reads: seed=" + seed + ", lakes=" + lakes);
                assertEquals(vanilla.writes, simulated.writes, "block writes: seed=" + seed + ", lakes=" + lakes);
                assertEquals(referenceRandom.nextLong(), randomOf(replay).nextLong(), "remaining random state");
            }
        }
    }

    @Test
    void failedDungeonsConsumeTheSameRandomAsVanilla() throws Exception {
        ChunkGeneratorSettings settings = new ChunkGeneratorSettings.Factory().build();
        Fixture vanilla = new Fixture(-987654321, true);
        Random referenceRandom = runVanilla(vanilla, settings, 9, -13);
        Fixture simulated = new Fixture(-987654321, true);
        DungeonPopulation replay = runReplay(simulated, settings, 9, -13);
        assertTrue(simulated.writes.isEmpty(), "empty terrain must not produce a dungeon");
        assertEquals(vanilla.reads, simulated.reads);
        assertEquals(referenceRandom.nextLong(), randomOf(replay).nextLong());
    }

    @Test
    void sharedMutableOrUnreviewedBiomesAreRejected() {
        assertTrue(VanillaBiomeSafety.supports(Biomes.PLAINS));
        assertTrue(VanillaBiomeSafety.supports(Biomes.SWAMPLAND));
        assertFalse(VanillaBiomeSafety.supports(Biomes.EXTREME_HILLS));
        assertFalse(VanillaBiomeSafety.supports(Biomes.MESA));
        assertFalse(VanillaBiomeSafety.supports(Biomes.TAIGA));
        assertFalse(VanillaBiomeSafety.supports(Biomes.MUTATED_SAVANNA));
        assertFalse(VanillaBiomeSafety.supports(mock(Biome.class)));
    }

    @Test
    void naturalTerrainProducesTheSameDungeonAsVanillaPopulate() throws Exception {
        // 固定的自然命中样例：原版默认地形，关闭大型结构；没有手动挖洞或增加尝试次数。
        Fixture vanilla = new Fixture(0, false);
        vanilla.useNaturalTerrain();
        Fixture simulated = new Fixture(0, false);
        simulated.useNaturalTerrain();
        for (Biome biome : simulated.biomeProvider.getBiomes(null, -242 * 16, 0, 32, 32)) {
            assertTrue(VanillaBiomeSafety.supports(biome));
        }
        ChunkGeneratorSettings settings = new ChunkGeneratorSettings.Factory().build();
        Random referenceRandom = runVanilla(vanilla, settings, -242, 0);
        DungeonPopulation replay = runReplay(simulated, settings, -242, 0);

        BlockPos spawner = new BlockPos(-3854, 25, 18);
        assertEquals(Blocks.MOB_SPAWNER, simulated.blocks.get(spawner).getBlock());
        assertTrue(simulated.tiles.get(spawner) instanceof TileEntityMobSpawner);
        assertTrue(simulated.tiles.values().stream().anyMatch(tile -> tile instanceof TileEntityChest));
        assertEquals(vanilla.reads, simulated.reads);
        assertEquals(vanilla.writes, simulated.writes);
        assertEquals(referenceRandom.nextLong(), randomOf(replay).nextLong());
        assertEquals(vanilla.tiles.keySet(), simulated.tiles.keySet());
        for (BlockPos position : vanilla.tiles.keySet()) {
            assertEquals(vanilla.tiles.get(position).serializeNBT(), simulated.tiles.get(position).serializeNBT());
        }
    }

    @Test
    void reportedHmclMissIsComparedAgainstVanillaPopulation() throws Exception {
        long seed = 9010613875193237759L;
        Fixture vanilla = new Fixture(seed, false);
        vanilla.useNaturalTerrain();
        Fixture simulated = new Fixture(seed, false);
        simulated.useNaturalTerrain();
        ChunkGeneratorSettings settings = new ChunkGeneratorSettings.Factory().build();
        Random reference = runVanilla(vanilla, settings, -59, -56);
        DungeonPopulation replay = runReplay(simulated, settings, -59, -56);
        BlockPos reported = new BlockPos(-924, 18, -888);
        assertTrue(vanilla.tiles.get(reported) instanceof TileEntityMobSpawner,
                "Reported hit differs even from isolated vanilla population");
        assertEquals(vanilla.reads, simulated.reads);
        assertEquals(vanilla.writes, simulated.writes);
        assertEquals(reference.nextLong(), randomOf(replay).nextLong());
    }

    private static DungeonPopulation runReplay(Fixture fixture, ChunkGeneratorSettings settings, int x, int z) {
        DungeonPopulation replay = new DungeonPopulation(fixture.world, settings, x, z);
        replay.generateWaterLake();
        replay.generateLavaLake();
        while (replay.hasNextDungeon()) {
            replay.generateNextDungeon();
        }
        return replay;
    }

    private static Random runVanilla(Fixture fixture, ChunkGeneratorSettings settings, int x, int z) throws Exception {
        ChunkGeneratorOverworld generator = mock(ChunkGeneratorOverworld.class, CALLS_REAL_METHODS);
        Random random = new Random();
        setField(ChunkGeneratorOverworld.class, generator, "world", fixture.world);
        setField(ChunkGeneratorOverworld.class, generator, "settings", settings);
        setField(ChunkGeneratorOverworld.class, generator, "rand", random);
        setField(ChunkGeneratorOverworld.class, generator, "mapFeaturesEnabled", false);
        boolean falling = BlockFalling.fallInstantly;
        try {
            assertThrows(ReachedDecoration.class, () -> generator.populate(x, z));
        } finally {
            BlockFalling.fallInstantly = falling;
        }
        return random;
    }

    private static Random randomOf(DungeonPopulation replay) throws Exception {
        Field field = DungeonPopulation.class.getDeclaredField("random");
        field.setAccessible(true);
        return (Random) field.get(replay);
    }

    private static void setField(Class<?> type, Object object, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static final class ReachedDecoration extends RuntimeException {
    }

    /** 两条路径使用独立、相同的临时方块空间，允许原生生成器读取自己的写入。 */
    private static final class Fixture {

        private final Map<BlockPos, IBlockState> blocks = new HashMap<>();
        private final Map<BlockPos, TileEntity> tiles = new HashMap<>();
        private final List<BlockPos> reads = new ArrayList<>();
        private final List<String> writes = new ArrayList<>();
        private final Biome biome = mock(Biome.class);
        private final World world;
        private final long seed;
        private final boolean empty;
        private final Map<Long, Chunk> chunks = new HashMap<>();
        private BiomeProvider biomeProvider;
        private WorldInfo info;
        private ChunkGeneratorOverworld terrain;

        private Fixture(long seed, boolean empty) {
            this.seed = seed;
            this.empty = empty;
            biome.topBlock = Blocks.GRASS.getDefaultState();
            biome.fillerBlock = Blocks.DIRT.getDefaultState();
            doThrow(new ReachedDecoration()).when(biome).decorate(any(), any(), any());
            world = mock(World.class, withSettings().stubOnly().defaultAnswer(this::call));
        }

        private void useNaturalTerrain() throws Exception {
            TerrainWorld terrainWorld = new TerrainWorld(seed);
            info = terrainWorld.getWorldInfo();
            biomeProvider = terrainWorld.getBiomeProvider();
            terrain = new ChunkGeneratorOverworld(terrainWorld, seed, false, "");
        }

        private Object call(InvocationOnMock call) throws Throwable {
            switch (call.getMethod().getName()) {
                case "getSeed":
                    return seed;
                case "getSeaLevel":
                    return 63;
                case "getBiome":
                    return biome;
                case "getBiomeProvider":
                    return biomeProvider;
                case "getWorldInfo":
                    return info;
                case "getWorldType":
                    return WorldType.DEFAULT;
                case "getBlockState":
                    return state(call.getArgument(0));
                case "isAirBlock":
                    return state(call.getArgument(0)).getBlock() == Blocks.AIR;
                case "getLightFromNeighborsFor":
                    return 15;
                case "setBlockToAir":
                    return write(call.getArgument(0), Blocks.AIR.getDefaultState());
                case "setBlockState":
                    return write(call.getArgument(0), call.getArgument(1));
                case "getTileEntity":
                    return tiles.get(call.getArgument(0));
                default:
                    return RETURNS_DEFAULTS.answer(call);
            }
        }

        private IBlockState state(BlockPos position) {
            reads.add(position.toImmutable());
            if (terrain != null) {
                int x = position.getX() >> 4;
                int z = position.getZ() >> 4;
                long key = net.minecraft.util.math.ChunkPos.asLong(x, z);
                Chunk chunk = chunks.computeIfAbsent(key, ignored -> terrain.generateChunk(x, z));
                return blocks.getOrDefault(position, chunk.getBlockState(position));
            }
            IBlockState initial = empty || position.getY() > 70 || position.getY() < 0
                    ? Blocks.AIR.getDefaultState() : Blocks.STONE.getDefaultState();
            return blocks.getOrDefault(position, initial);
        }

        private boolean write(BlockPos position, IBlockState state) {
            BlockPos key = position.toImmutable();
            blocks.put(key, state);
            writes.add(key.toString() + ":" + state);
            TileEntity tile = null;
            if (state.getBlock() == Blocks.CHEST) {
                tile = new TileEntityChest();
            } else if (state.getBlock() == Blocks.MOB_SPAWNER) {
                tile = new TileEntityMobSpawner();
            }
            if (tile != null) {
                tile.setWorld(world);
                tile.setPos(key);
                tiles.put(key, tile);
            } else {
                tiles.remove(key);
            }
            return true;
        }
    }

    /** 地形生成的热循环不经过 Mockito；该世界不提供区块加载或持久化入口。 */
    private static final class TerrainWorld extends World {

        private TerrainWorld(long seed) {
            super(mock(ISaveHandler.class),
                    new WorldInfo(new WorldSettings(seed, GameType.SURVIVAL, false, false, WorldType.DEFAULT), "fixture"),
                    new WorldProviderSurface(), new Profiler(), false);
            provider.setWorld(this);
        }

        @Override
        public Biome getBiome(BlockPos position) {
            return getBiomeProvider().getBiome(position);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            throw new UnsupportedOperationException("Terrain fixture does not populate chunks");
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return false;
        }
    }
}
