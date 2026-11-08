package com.shiver.structurecompasslegacy.verification;

import java.util.List;
import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenStronghold;

/** 原版新世界的 128 个强要塞候选；每次只计算一个，不读取结构存档。 */
final class StrongholdRing {

    private final Random random;
    private final List<Biome> allowedBiomes;
    private final ChunkPos[] positions = new ChunkPos[128];
    private double angle;
    private int ring;
    private int inRing;
    private int spread = 3;
    private int calculated;

    StrongholdRing(long seed) {
        random = new Random(seed);
        angle = random.nextDouble() * Math.PI * 2.0;
        allowedBiomes = new MapGenStronghold().allowedBiomes;
    }

    boolean complete() {
        return calculated == positions.length;
    }

    void advance(BiomeProvider biomes) {
        double distance = 128.0 + 32.0 * ring * 6.0 + (random.nextDouble() - 0.5) * 80.0;
        int x = (int) Math.round(Math.cos(angle) * distance);
        int z = (int) Math.round(Math.sin(angle) * distance);
        BlockPos biomePosition = biomes.findBiomePosition((x << 4) + 8, (z << 4) + 8, 112, allowedBiomes, random);
        if (biomePosition != null) {
            x = biomePosition.getX() >> 4;
            z = biomePosition.getZ() >> 4;
        }
        positions[calculated] = new ChunkPos(x, z);
        angle += Math.PI * 2.0 / spread;
        inRing++;
        if (inRing == spread) {
            ring++;
            inRing = 0;
            spread += 2 * spread / (ring + 1);
            spread = Math.min(spread, positions.length - calculated);
            angle += random.nextDouble() * Math.PI * 2.0;
        }
        calculated++;
    }

    boolean intersects(int minX, int minZ, int maxX, int maxZ) {
        if (!complete()) {
            throw new IllegalStateException("Stronghold candidates are not ready");
        }
        for (ChunkPos position : positions) {
            if (position.x >= minX && position.x <= maxX && position.z >= minZ && position.z <= maxZ) {
                return true;
            }
        }
        return false;
    }
}
