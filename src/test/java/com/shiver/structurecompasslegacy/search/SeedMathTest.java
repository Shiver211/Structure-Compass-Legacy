package com.shiver.structurecompasslegacy.search;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedMathTest {

    @Test
    void eachRegionContainsOneCandidateIncludingNegativeRegions() {
        for (int regionX = -3; regionX <= 3; regionX++) {
            for (int regionZ = -3; regionZ <= 3; regionZ++) {
                int hits = 0;
                for (int x = regionX * 32; x < (regionX + 1) * 32; x++) {
                    for (int z = regionZ * 32; z < (regionZ + 1) * 32; z++) {
                        if (SeedMath.grid(917341L, x, z, 32, 8, 10387312, false)) {
                            hits++;
                        }
                    }
                }
                assertEquals(1, hits);
            }
        }
    }

    @Test
    void matchesForgeRandomInitializationIncludingItsShiftPrecedence() {
        Random reference = new Random(-17429003L);
        long seedX = reference.nextLong() >> 2 + 1L;
        long seedZ = reference.nextLong() >> 2 + 1L;
        reference.setSeed((seedX * -17 + seedZ * 41) ^ -17429003L);
        Random actual = SeedMath.forge(-17429003L, -17, 41);

        for (int index = 0; index < 20; index++) {
            assertEquals(reference.nextLong(), actual.nextLong());
        }
    }
}
