package com.shiver.structurecompasslegacy.compat.twilight;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwilightPlacementTest {

    @Test
    void centersRemainInTheirRegionOnBothSidesOfZero() {
        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                BlockPos center = TwilightPlacement.center(x, z);
                assertEquals(center, TwilightPlacement.center(center.getX() >> 4, center.getZ() >> 4));
            }
        }
        assertEquals(new BlockPos(8, 31, 8), TwilightPlacement.center(0, 0));
    }

    @Test
    void biomeSamplesRetainNativeRoundingAtNegativeHalfRegions() {
        assertEquals(0, TwilightPlacement.biomeSample(-8));
        assertEquals(-16, TwilightPlacement.biomeSample(-9));
        assertEquals(16, TwilightPlacement.biomeSample(8));
        assertEquals(0, TwilightPlacement.biomeSample(7));
    }

    @Test
    void guaranteedBossRegionsOverrideSeededMinorLandmarks() {
        for (long seed : new long[]{0, 1, -1, Long.MAX_VALUE}) {
            assertEquals("LICH_TOWER", TwilightPlacement.defaultFeature(seed, 0, 16));
            assertEquals("LICH_TOWER", TwilightPlacement.defaultFeature(seed, 0, -16));
            assertEquals("NAGA_COURTYARD", TwilightPlacement.defaultFeature(seed, 16, 0));
            assertEquals("NAGA_COURTYARD", TwilightPlacement.defaultFeature(seed, -16, 0));
        }
    }
}
