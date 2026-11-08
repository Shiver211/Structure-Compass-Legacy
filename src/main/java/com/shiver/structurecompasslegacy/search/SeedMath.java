package com.shiver.structurecompasslegacy.search;

import java.util.Random;

public final class SeedMath {

    private SeedMath() {
    }

    public static Random region(long seed, int x, int z, int salt) {
        return new Random((long) x * 341873128712L + (long) z * 132897987541L + seed + salt);
    }

    public static Random mapGen(long seed, int x, int z) {
        Random random = new Random(seed);
        long seedX = random.nextLong();
        long seedZ = random.nextLong();
        random.setSeed((long) x * seedX ^ (long) z * seedZ ^ seed);
        random.nextInt(); // MapGenStructure.recursiveGenerate 的首次随机消耗。
        return random;
    }

    public static Random forge(long seed, int x, int z) {
        Random random = new Random(seed);
        // Forge 1.12.2 原式的移位优先级为 >> (2 + 1)。
        long seedX = random.nextLong() >> 3;
        long seedZ = random.nextLong() >> 3;
        return new Random((seedX * x + seedZ * z) ^ seed);
    }

    public static boolean grid(
            long seed,
            int x,
            int z,
            int spacing,
            int separation,
            int salt,
            boolean triangular) {
        int regionX = Math.floorDiv(x, spacing);
        int regionZ = Math.floorDiv(z, spacing);
        Random random = region(seed, regionX, regionZ, salt);
        int bound = spacing - separation;
        int offsetX = gridOffset(random, bound, triangular);
        int offsetZ = gridOffset(random, bound, triangular);
        return x == regionX * spacing + offsetX && z == regionZ * spacing + offsetZ;
    }

    private static int gridOffset(Random random, int bound, boolean triangular) {
        if (triangular) {
            return (random.nextInt(bound) + random.nextInt(bound)) / 2;
        }

        return random.nextInt(bound);
    }
}
