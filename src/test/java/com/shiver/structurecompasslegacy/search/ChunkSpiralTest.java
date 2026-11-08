package com.shiver.structurecompasslegacy.search;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSpiralTest {

    @Test
    void visitsEveryChunkOnceInExpandingRingsAtNegativeCoordinates() {
        ChunkSpiral spiral = new ChunkSpiral(-19, -3, 16);
        Set<Long> seen = new HashSet<>();
        int previousRing = 0;

        while (spiral.hasNext()) {
            int deltaX = Math.abs(spiral.x() + 19);
            int deltaZ = Math.abs(spiral.z() + 3);
            int ring = Math.max(deltaX, deltaZ);
            long key = ((long) spiral.x() << 32) ^ (spiral.z() & 0xFFFFFFFFL);
            assertTrue(ring >= previousRing);
            assertTrue(ring <= 16);
            assertTrue(seen.add(key));
            previousRing = ring;
            spiral.next();
        }

        assertEquals(33 * 33, seen.size());
        assertEquals(spiral.total(), spiral.visited());
        assertThrows(IllegalStateException.class, spiral::next);
    }
}
