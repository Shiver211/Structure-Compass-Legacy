package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.util.function.BiFunction;

import com.shiver.structurecompasslegacy.api.SearchTarget;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/** 原生间距只看最后生成点；拒绝附近可能先生成的竞争点，降低探索顺序导致的误报。 */
final class IceStability {
    final SearchTarget target;
    final BlockPos center;
    final BlockPos prediction;
    final Biome biome;
    private final IcePlacement placement;
    private final IceLandmark kind;
    private final int radius;
    private int x;
    private int z;

    IceStability(IcePlacement placement, IceLandmark kind, Biome biome, BlockPos center,
            BlockPos prediction, SearchTarget target) {
        this.placement = placement;
        this.kind = kind;
        this.biome = biome;
        this.center = center;
        this.prediction = prediction;
        this.target = target;
        radius = (placement.spacing() + 15) / 16;
        x = z = -radius;
    }

    boolean hasNext() {
        return z <= radius;
    }

    // 每次最多查询一个群系，由搜索任务在相邻调用之间检查时间预算。
    boolean inspectNext(BiFunction<Integer, Integer, Biome> biomes) {
        int dx = x, dz = z;
        if (++x > radius) {
            x = -radius;
            z++;
        }
        if (dx == 0 && dz == 0 || !withinSpacing(dx, dz, placement.spacing())) {
            return true;
        }
        BlockPos other = center.add(dx * 16, 0, dz * 16);
        if (!placement.mayCompete(kind, biome, other)) {
            return true;
        }
        Biome otherBiome = biomes.apply(other.getX() >> 4, other.getZ() >> 4);
        return otherBiome == null || !placement.competes(kind, biome, otherBiome, other);
    }

    static boolean withinSpacing(int dx, int dz, int spacing) {
        long x = dx * 16L, z = dz * 16L;
        return x * x + z * z < (long) spacing * spacing;
    }
}
