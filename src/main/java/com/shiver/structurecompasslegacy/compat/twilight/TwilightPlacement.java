package com.shiver.structurecompasslegacy.compat.twilight;

import java.util.Random;

import net.minecraft.util.math.BlockPos;

/** 对应当前暮色分支的区域规则，保留其整数溢出与负坐标偏移。 */
final class TwilightPlacement {

    private TwilightPlacement() {
    }

    static BlockPos center(int chunkX, int chunkZ) {
        int regionX = (chunkX + 8) >> 4;
        int regionZ = (chunkZ + 8) >> 4;
        long regionSeed = (long) (regionX * 3129871) ^ (long) regionZ * 116129781L;
        regionSeed = regionSeed * regionSeed * 42317861L + regionSeed * 7L;
        int offsetX = 8 + (int) (regionSeed >> 12 & 3) - (int) (regionSeed >> 15 & 3);
        int offsetZ = 8 + (int) (regionSeed >> 18 & 3) - (int) (regionSeed >> 21 & 3);
        return new BlockPos(centerCoordinate(regionX, offsetX), 31, centerCoordinate(regionZ, offsetZ));
    }

    private static int centerCoordinate(int region, int offset) {
        if (region < 0) {
            return (region * 16 + 8 - offset) * 16 + 9;
        }
        return (region * 16 + offset - 8) * 16 + 8;
    }

    static int biomeSample(int chunk) {
        return Math.round(chunk / 16.0F) * 16;
    }

    static String defaultFeature(long seed, int sampleX, int sampleZ) {
        int offsetX = Math.abs(((sampleX + 64) >> 4) % 8);
        int offsetZ = Math.abs(((sampleZ + 64) >> 4) % 8);
        if (offsetX == 4 && (offsetZ == 3 || offsetZ == 5)) {
            return "LICH_TOWER";
        }
        if (offsetZ == 4 && (offsetX == 3 || offsetX == 5)) {
            return "NAGA_COURTYARD";
        }

        int choice = new Random(seed + (long) (sampleX * 25117) + (long) (sampleZ * 151121)).nextInt(16);
        if (choice < 6) {
            return "SMALL_HILL";
        }
        if (choice < 9) {
            return "MEDIUM_HILL";
        }
        if (choice == 9) {
            return "LARGE_HILL";
        }
        if (choice < 12) {
            return "HEDGE_MAZE";
        }
        return choice < 14 ? "NAGA_COURTYARD" : "LICH_TOWER";
    }
}
