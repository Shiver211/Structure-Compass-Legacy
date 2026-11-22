package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

/** 只采样区块中心；邻域检查复用结果，不为一次查询计算整块 256 个群系。 */
final class IceBiomes {
    private static final int CACHE_SIZE = 1024;
    private final BiomeProvider provider;
    private Biome[] sample;
    private final Map<Long, Biome> cache = new LinkedHashMap<Long, Biome>(CACHE_SIZE, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Biome> entry) {
            return size() > CACHE_SIZE;
        }
    };

    IceBiomes(BiomeProvider provider) {
        this.provider = provider;
    }

    Biome at(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        Biome biome = cache.get(key);
        if (biome == null) {
            sample = provider.getBiomes(sample, x * 16 + 8, z * 16 + 8, 1, 1, false);
            biome = sample[0];
            cache.put(key, biome);
        }
        return biome;
    }

    void clear() {
        cache.clear();
    }
}
