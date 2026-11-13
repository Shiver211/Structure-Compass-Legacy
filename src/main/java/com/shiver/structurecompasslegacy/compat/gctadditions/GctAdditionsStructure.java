package com.shiver.structurecompasslegacy.compat.gctadditions;

import java.util.Random;

enum GctAdditionsStructure {

    SEEK_ALTAR(
            "seek_altar",
            GctAdditionsModule.WARPED_PROVIDER,
            null,
            18,
            4,
            55001L,
            1,
            1,
            0,
            29,
            11,
            29,
            14,
            3,
            14),
    REVERSED_DUNGEON(
            "reversed_dungeon",
            GctAdditionsModule.ALFHEIM_PROVIDER,
            "com.shiver.gct_additions.common.world.biome.BiomeReversedForest",
            14,
            3,
            42001L,
            14,
            15,
            -50,
            17,
            54,
            16,
            4,
            0,
            3),
    ELVEN_POST(
            "elven_post",
            GctAdditionsModule.ALFHEIM_PROVIDER,
            "com.shiver.gct_additions.common.world.biome.BiomeAlfheimPlain",
            10,
            2,
            42002L,
            18,
            17,
            0,
            13,
            21,
            14,
            6,
            1,
            7);

    private static final long REGION_PRIME_X = 341873128712L;
    private static final long REGION_PRIME_Z = 132897987541L;

    final String id;
    final String providerClass;
    final String biomeClass;
    final int spacing;
    final int separation;
    final long salt;
    final int originRangeX;
    final int originRangeZ;
    final int yOffset;
    final int sizeX;
    final int maxRelY;
    final int sizeZ;
    final int targetOffsetX;
    final int targetOffsetY;
    final int targetOffsetZ;

    GctAdditionsStructure(
            String id,
            String providerClass,
            String biomeClass,
            int spacing,
            int separation,
            long salt,
            int originRangeX,
            int originRangeZ,
            int yOffset,
            int sizeX,
            int maxRelY,
            int sizeZ,
            int targetOffsetX,
            int targetOffsetY,
            int targetOffsetZ) {
        this.id = id;
        this.providerClass = providerClass;
        this.biomeClass = biomeClass;
        this.spacing = spacing;
        this.separation = separation;
        this.salt = salt;
        this.originRangeX = originRangeX;
        this.originRangeZ = originRangeZ;
        this.yOffset = yOffset;
        this.sizeX = sizeX;
        this.maxRelY = maxRelY;
        this.sizeZ = sizeZ;
        this.targetOffsetX = targetOffsetX;
        this.targetOffsetY = targetOffsetY;
        this.targetOffsetZ = targetOffsetZ;
    }

    int minTargetX(int regionX) {
        return regionX * spacing * 16 + 1 + targetOffsetX;
    }

    int maxTargetX(int regionX) {
        int bound = spacing - separation;
        return (regionX * spacing + bound - 1) * 16 + originRangeX + targetOffsetX;
    }

    int minTargetZ(int regionZ) {
        return regionZ * spacing * 16 + 1 + targetOffsetZ;
    }

    int maxTargetZ(int regionZ) {
        int bound = spacing - separation;
        return (regionZ * spacing + bound - 1) * 16 + originRangeZ + targetOffsetZ;
    }

    Candidate candidate(long seed, int regionX, int regionZ) {
        long regionSeed = (long) regionX * REGION_PRIME_X + (long) regionZ * REGION_PRIME_Z + seed + salt;
        Random rng = new Random(regionSeed);
        int bound = spacing - separation;
        int chunkX = regionX * spacing + rng.nextInt(bound);
        int chunkZ = regionZ * spacing + rng.nextInt(bound);
        int localX = 1 + (originRangeX > 1 ? rng.nextInt(originRangeX) : 0);
        int localZ = 1 + (originRangeZ > 1 ? rng.nextInt(originRangeZ) : 0);
        int originX = chunkX * 16 + localX;
        int originZ = chunkZ * 16 + localZ;
        return new Candidate(originX, originZ, originX + targetOffsetX, originZ + targetOffsetZ);
    }

    static final class Candidate {

        final int originX;
        final int originZ;
        final int targetX;
        final int targetZ;

        Candidate(int originX, int originZ, int targetX, int targetZ) {
            this.originX = originX;
            this.originZ = originZ;
            this.targetX = targetX;
            this.targetZ = targetZ;
        }
    }
}
