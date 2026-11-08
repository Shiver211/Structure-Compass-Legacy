package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.shiver.structurecompasslegacy.compat.betweenlands.BetweenlandsNativeTypes.Biomes;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.compat.betweenlands.BetweenlandsLandmarkTask.Landmark;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerZoom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class BetweenlandsPlacementTest {

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void candidatesMatchNativeMapGenAcrossSeedsAndBiomeBoundaries() throws Exception {
        BetweenlandsNativeFixture nativeCode = new BetweenlandsNativeFixture();
        int islands = 0;
        int roots = 0;
        Function<BlockPos, Biome> biomes = position -> {
            int band = Math.floorMod(Math.floorDiv(position.getX(), 37) + Math.floorDiv(position.getZ(), 53), 3);
            return band == 0 ? Biomes.RAISED_ISLES : band == 1 ? Biomes.COARSE_ISLANDS : Biomes.SWAMPLANDS;
        };
        for (long seed : new long[] {0L, 72931L, -1L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            nativeCode.world(seed, biomes);
            BetweenlandsPlacement prediction = new BetweenlandsPlacement(seed);
            BetweenlandsNativeFixture.Generator island = nativeCode.generator(true);
            BetweenlandsNativeFixture.Generator root = nativeCode.generator(false);
            for (int x = -40; x <= 40; x++) {
                for (int z = -40; z <= 40; z++) {
                    BlockPos expectedIsland = island.candidate(x, z);
                    BlockPos expectedRoot = root.candidate(x, z);
                    assertEquals(expectedIsland, prediction.floatingIsland(x, z, p -> biomes.apply(p) == Biomes.RAISED_ISLES));
                    assertEquals(expectedRoot, prediction.giantRoot(x, z,
                            p -> biomes.apply(p) == Biomes.RAISED_ISLES || biomes.apply(p) == Biomes.COARSE_ISLANDS));
                    islands += expectedIsland == null ? 0 : 1;
                    roots += expectedRoot == null ? 0 : 1;
                }
            }
        }
        assertTrue(islands > 100);
        assertTrue(roots > 1000);
        System.out.println("Betweenlands native comparison: 65610 candidates, islands=" + islands + ", roots=" + roots);
    }

    @Test
    void biomeQueriesUsePrivateLayersAndRejectChangedRules() throws Exception {
        BetweenlandsNativeFixture nativeCode = new BetweenlandsNativeFixture();
        BetweenlandsBiomeLayers binding = nativeCode.binding();
        for (WorldType type : new WorldType[] {WorldType.DEFAULT, WorldType.LARGE_BIOMES}) {
            GenLayer original = nativeCode.biomeLayer(72931L, type);
            BiomeProvider source = mock(BiomeProvider.class);
            BetweenlandsNativeFixture.field(BiomeProvider.class, "biomeIndexLayer").set(source, original);
            GenLayer copy = binding.create(72931L, type, source);
            assertNotNull(copy);
            Map<Object, List<Object>> before = snapshot(original);
            for (int x = -90; x <= 90; x += 9) {
                for (int z = -90; z <= 90; z += 9) {
                    copy.getInts(x * 16, z * 16, 1, 1);
                }
            }
            Map<Object, List<Object>> after = snapshot(original);
            assertEquals(before.size(), after.size());
            for (Map.Entry<Object, List<Object>> entry : before.entrySet()) {
                assertEquals(entry.getValue(), after.get(entry.getKey()),
                        "Queries must not modify original layer state: " + entry.getKey().getClass().getSimpleName());
            }
            assertTrue(binding.matches(original, copy), "Query cursors do not change generation rules");
            for (int x = -31; x <= 31; x += 7) {
                int[] expected = original.getInts(x * 16, -x * 32, 1, 1);
                assertEquals(expected[0], copy.getInts(x * 16, -x * 32, 1, 1)[0]);
            }
            assertNull(binding.create(72932L, type, source));
            assertFalse(binding.matches(original, new GenLayerZoom(1L, copy)));
            BetweenlandsNativeFixture.field(GenLayer.class, "baseSeed").setLong(original, 123456L);
            assertFalse(binding.matches(original, copy));
        }
    }

    @Test
    void tasksUseBatchedBiomesRespectBudgetsAndReleaseLayers() throws Exception {
        BetweenlandsNativeFixture nativeCode = new BetweenlandsNativeFixture();
        WorldServer source = mock(WorldServer.class, withSettings().stubOnly().defaultAnswer(call -> {
            if (call.getMethod().getName().equals("getSeed")) {
                return 0L;
            }
            throw new AssertionError("Search accessed the real world: " + call.getMethod());
        }));
        BetweenlandsNativeFixture.field(World.class, "provider").set(source, new WorldProviderSurface());
        SearchContext context = new SearchContext(source, new BlockPos(-960, 70, -256));
        for (Landmark landmark : Landmark.values()) {
            GenLayer layers = nativeCode.biomeLayer(0L, WorldType.DEFAULT);
            GenLayer reference = nativeCode.biomeLayer(0L, WorldType.DEFAULT);
            nativeCode.world(0L, p -> Biome.getBiome(reference.getInts(p.getX(), p.getZ(), 1, 1)[0]));
            BetweenlandsNativeFixture.Generator generator = nativeCode.generator(landmark == Landmark.FLOATING_ISLAND);
            BetweenlandsLandmarkTask task = new BetweenlandsLandmarkTask(context, landmark, layers,
                    Biomes.RAISED_ISLES, Biomes.COARSE_ISLANDS);
            assertEquals(SearchStep.State.RUNNING, task.advance(new SearchBudget(0, 0, () -> 0)).state);
            assertEquals(0, task.checkedCandidates());
            int found = 0;
            for (int x = -60; x <= -56; x++) {
                for (int z = -20; z <= -10; z++) {
                    BlockPos expected = generator.candidate(x, z);
                    SearchTarget target = task.inspect(x, z);
                    assertEquals(expected, target == null ? null : target.position);
                    found += target == null ? 0 : 1;
                }
            }
            assertTrue(found > 0);
            assertEquals(0, task.generatedChunks());
            assertEquals(0, task.peakCachedChunks());
            task.close();
            assertNull(BetweenlandsNativeFixture.field(BetweenlandsLandmarkTask.class, "biomes").get(task));
        }
        BetweenlandsLandmarkTask rejected = new BetweenlandsLandmarkTask(context, Landmark.FLOATING_ISLAND,
                null, Biomes.RAISED_ISLES, Biomes.COARSE_ISLANDS);
        assertEquals("betweenlands_biome_layers", rejected.advance(new SearchBudget(0, 0, () -> 0)).reason);
        rejected.close();
    }

    @Test
    void naturalBiomeCandidatesContainNativeIslandAndRootBlocks() throws Exception {
        BetweenlandsNativeFixture nativeCode = new BetweenlandsNativeFixture();
        for (long seed : new long[] {0L, -72931L}) {
            GenLayer layers = nativeCode.biomeLayer(seed, WorldType.DEFAULT);
            Function<BlockPos, Biome> biomes = p -> Biome.getBiome(layers.getInts(p.getX(), p.getZ(), 1, 1)[0]);
            nativeCode.world(seed, biomes);
            BetweenlandsPlacement prediction = new BetweenlandsPlacement(seed);
            BlockPos island = null;
            BlockPos root = null;
            for (int x = -60; x <= 60 && (island == null || root == null); x++) {
                for (int z = -60; z <= 60 && (island == null || root == null); z++) {
                    if (island == null) {
                        island = prediction.floatingIsland(x, z, p -> biomes.apply(p) == Biomes.RAISED_ISLES);
                    }
                    if (root == null) {
                        root = prediction.giantRoot(x, z,
                                p -> biomes.apply(p) == Biomes.RAISED_ISLES || biomes.apply(p) == Biomes.COARSE_ISLANDS);
                    }
                }
            }
            assertNotNull(island);
            assertNotNull(root);
            ChunkPrimer islandChunk = nativeCode.generator(true).generateLandmark(island);
            boolean solidIsland = false;
            for (int y = island.getY() - 8; y <= island.getY() + 4; y++) {
                solidIsland |= islandChunk.getBlockState(island.getX() & 15, y, island.getZ() & 15).getMaterial().isSolid();
            }
            assertTrue(solidIsland, "The predicted island column must contain native island blocks");
            ChunkPrimer rootChunk = nativeCode.generator(false).generateLandmark(root);
            assertEquals(Blocks.LOG, rootChunk.getBlockState(root.getX() & 15, root.getY(), root.getZ() & 15).getBlock());
            System.out.println("Betweenlands native geometry: seed=" + seed + ", island=" + island + ", root=" + root);
        }
    }

    private static Map<Object, List<Object>> snapshot(GenLayer root) throws Exception {
        Map<Object, List<Object>> state = new IdentityHashMap<>();
        snapshot(root, state);
        return state;
    }

    private static void snapshot(Object node, Map<Object, List<Object>> state) throws Exception {
        if (node == null || state.containsKey(node)) {
            return;
        }
        List<Object> values = new ArrayList<>();
        state.put(node, values);
        for (Class<?> type = node.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(node);
                values.add(value instanceof List ? new ArrayList<>((List<?>) value) : value);
                if (value instanceof GenLayer || field.getName().equals("cache")) {
                    snapshot(value, state);
                }
            }
        }
    }
}
