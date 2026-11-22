package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.util.Collections;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import com.shiver.structurecompasslegacy.simulation.VanillaBiomeSafety;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Partial generator: a rejected chunk invalidates the whole candidate, including cross-chunk checks.
 */
final class LostTerrain implements IChunkGenerator {
    // Many BOP biomes inherit Biome's read-only implementation unchanged. An override
    // (e.g. mesa's seed-dependent clay bands) is not safe merely because the world is private.
    private static final ClassValue<Boolean> VANILLA_SURFACE = new ClassValue<Boolean>() {
        final Method surface = ReflectionHelper.findMethod(Biome.class, "genTerrainBlocks", "func_180622_a",
            World.class, Random.class, ChunkPrimer.class, int.class, int.class, double.class);

        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod(surface.getName(), surface.getParameterTypes()).getDeclaringClass() ==
                    Biome.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    };

    static final class UnknownChunk extends RuntimeException {
        UnknownChunk() {
            super(null, null, false, false);
        }
    }

    private final SimulationWorld world;
    private final LostWilderness layout;
    private final BiPredicate<Integer, Integer> generated;
    private final ChunkGeneratorSettings settings = new ChunkGeneratorSettings.Factory().build();

    LostTerrain(SimulationWorld world, LostWilderness layout, BiPredicate<Integer, Integer> generated) {
        this.world = world;
        this.layout = layout;
        this.generated = generated;
        world.setSeaLevel(63);
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        if (generated.test(x, z) || !layout.wilderness(x, z)) {
            throw new UnknownChunk();
        }
        // BiomeInfo in 3.0.0 samples one chunk WEST of vanilla's density window.
        Biome[] coarse = world.getBiomeProvider().getBiomesForGeneration(null, (x - 1) * 4 - 2, z * 4 - 2, 10, 10);
        if (bridgeBiome(coarse[54]) || bridgeBiome(coarse[55]) || bridgeBiome(coarse[56])) {
            throw new UnknownChunk();
        }
        Biome[] biomes = world.getBiomeProvider().getBiomes(null, x * 16, z * 16, 16, 16);
        for (Biome b : biomes) {
            if (!VanillaBiomeSafety.supports(b) && !VANILLA_SURFACE.get(b.getClass())) {
                throw new UnknownChunk();
            }
        }
        ChunkPrimer primer = new ChunkPrimer();
        fill(primer, density(x, z, coarse));
        double[] noise = layout.surface.getRegion(null, x * 16, z * 16, 16, 16, 0.0625, 0.0625, 1);
        // generate() reseeds before explosions/debris; neither consumes a draw in admitted chunks.
        Random rand = new Random(x * 257017164707L + z * 101754694003L);
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int i = dz + dx * 16;
                biomes[i].genTerrainBlocks(world, rand, primer, x * 16 + dx, z * 16 + dz, noise[i]);
            }
        }
        if (layout.caves != null) {
            layout.caves.generate(world, x, z, primer);
        }
        if (layout.ravines != null) {
            layout.ravines.generate(world, x, z, primer);
        }
        Chunk chunk = new Chunk(world, primer, x, z);
        for (int i = 0; i < 256; i++) {
            chunk.getBiomeArray()[i] = (byte) Biome.getIdForBiome(biomes[i]);
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    private static boolean bridgeBiome(Biome b) {
        return b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN || b == Biomes.FROZEN_OCEAN || b == Biomes.RIVER
            || b == Biomes.FROZEN_RIVER || b == Biomes.BEACH || b == Biomes.COLD_BEACH;
    }

    private double[] density(int cx, int cz, Biome[] b) {
        int x = cx * 4, z = cz * 4;
        float f = settings.coordinateScale, h = settings.heightScale;
        double[] depth =
            layout.depth.generateNoiseOctaves(null, x, z, 5, 5, settings.depthNoiseScaleX, settings.depthNoiseScaleZ,
                settings.depthNoiseScaleExponent);
        double[] mix = layout.main.generateNoiseOctaves(null, x, 0, z, 5, 33, 5, f / settings.mainNoiseScaleX,
            h / settings.mainNoiseScaleY, f / settings.mainNoiseScaleZ);
        double[] low = layout.low.generateNoiseOctaves(null, x, 0, z, 5, 33, 5, f, h, f);
        double[] high = layout.high.generateNoiseOctaves(null, x, 0, z, 5, 33, 5, f, h, f);
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                int c = gx + 2 + (gz + 2) * 10, col = gx * 5 + gz;
                float scale = 0, base = 0, weights = 0;
                for (int a = -2; a <= 2; a++) {
                    for (int k = -2; k <= 2; k++) {
                        Biome n = b[c + a + k * 10];
                        float v = n.getBaseHeight();
                        float weight = (float) (10.0 / Math.sqrt(a * a + k * k + 0.2F)) / (v + 2);
                        if (v > b[c].getBaseHeight()) {
                            weight /= 2;
                        }
                        scale += Math.abs(n.getHeightVariation()) * weight;
                        base += v * weight;
                        weights += weight;
                    }
                }
                scale = scale / weights * 0.9F + 0.1F;
                base = (base / weights * 4 - 1) / 8;
                double d = depth[col] / 8000;
                d = (d < 0 ? -d * 0.3 : d) * 3 - 2;
                d = d < 0 ? Math.max(-1, d / 2) / 1.4 / 2 : Math.min(1, d) / 8;
                double mid = settings.baseSize + ((base + d * 0.2) * settings.baseSize / 8) * 4;
                for (int y = 0; y < 33; y++) {
                    int i = col * 33 + y;
                    double slope = (y - mid) * settings.stretchY * 128 / 256 / scale;
                    double value =
                        MathHelper.clampedLerp(low[i] / settings.lowerLimitScale, high[i] / settings.upperLimitScale,
                            (mix[i] / 10 + 1) / 2) - (slope < 0 ? slope * 4 : slope);
                    double fade = (y - 29) / 3F;
                    mix[i] = y > 29 ? value * (1 - fade) - 10 * fade : value;
                }
            }
        }
        return mix;
    }

    private static void fill(ChunkPrimer primer, double[] grid) {
        for (int gx = 0; gx < 4; gx++) {
            for (int gz = 0; gz < 4; gz++) {
                for (int gy = 0; gy < 32; gy++) {
                    int i = (gx * 5 + gz) * 33 + gy;
                    double a = grid[i], b = grid[i + 33], c = grid[i + 165], d = grid[i + 198];
                    double da = (grid[i + 1] - a) / 8, db = (grid[i + 34] - b) / 8, dc = (grid[i + 166] - c) / 8, dd =
                        (grid[i + 199] - d) / 8;
                    for (int dy = 0; dy < 8; dy++, a += da, b += db, c += dc, d += dd) {
                        int y = gy * 8 + dy;
                        double left = a, right = b, dl = (c - a) / 4, dr = (d - b) / 4;
                        for (int dx = 0; dx < 4; dx++, left += dl, right += dr) {
                            double step = (right - left) / 4, v = left - step;
                            for (int dz = 0; dz < 4; dz++) {
                                if ((v += step) > 0) {
                                    primer.setBlockState(gx * 4 + dx, y, gz * 4 + dz, Blocks.STONE.getDefaultState());
                                } else if (y < 63) {
                                    primer.setBlockState(gx * 4 + dx, y, gz * 4 + dz, Blocks.WATER.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void populate(int x, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean generateStructures(Chunk c, int x, int z) {
        return false;
    }

    @Override
    public void recreateStructures(Chunk c, int x, int z) {
    }

    @Override
    public BlockPos getNearestStructurePos(World w, String n, BlockPos p, boolean u) {
        return null;
    }

    @Override
    public boolean isInsideStructure(World w, String n, BlockPos p) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType t, BlockPos p) {
        return Collections.emptyList();
    }
}
