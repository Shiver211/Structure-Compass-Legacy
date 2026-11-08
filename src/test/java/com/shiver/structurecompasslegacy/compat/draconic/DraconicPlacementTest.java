package com.shiver.structurecompasslegacy.compat.draconic;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderSurface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraconicPlacementTest {

    @Test
    void replacementEndProvidersFollowTheNativeDimensionRule() {
        WorldProvider replacement = new WorldProviderEnd() {
        };
        replacement.setDimension(1);
        assertTrue(DraconicModule.generatesIn(replacement));

        // 原生成器只检查维度 ID，甚至不要求替代 Provider 继承 WorldProviderEnd。
        WorldProvider anotherReplacement = new WorldProviderSurface();
        anotherReplacement.setDimension(1);
        assertTrue(DraconicModule.generatesIn(anotherReplacement));
    }

    @Test
    void endLikeProvidersInOtherDimensionsAreRejected() {
        WorldProvider provider = new WorldProviderEnd();
        provider.setDimension(42);
        assertFalse(DraconicModule.generatesIn(provider));
    }

    @Test
    void chaosGridAssignsPositiveAndNegativeCentersToTheirContainingChunk() {
        for (int multiple = -8; multiple <= 8; multiple++) {
            int coordinate = multiple * 10000;
            int chunk = Math.floorDiv(coordinate, 16);
            assertEquals(coordinate, ChaosIslandTask.multipleInChunk(chunk, 10000));
        }
    }

    @Test
    void cometsRespectCentralExclusionAndNativeOverflow() {
        assertNull(EnderCometTask.predict(8912, 0, 0, 1));
        assertNull(EnderCometTask.predict(8912, 12, 0, 1));
        assertNull(EnderCometTask.predict(8912, 3000, 0, 1));

        BlockPos comet = EnderCometTask.predict(8912, -16, 24, 1);
        assertNotNull(comet);
        assertEquals(-248, comet.getX());
        assertEquals(392, comet.getZ());
        assertTrue(comet.getY() >= 20 && comet.getY() < 190);
    }
}
