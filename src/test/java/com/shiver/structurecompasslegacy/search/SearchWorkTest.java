package com.shiver.structurecompasslegacy.search;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchWorkTest {

    @Test
    void namesAndMeasuresFailingCallsWithoutHidingTheFailure() {
        AtomicLong clock = new AtomicLong();
        SearchWork work = new SearchWork(clock::get);
        work.run("quick", () -> clock.addAndGet(5_000));
        IllegalStateException failure = new IllegalStateException("fixture");
        assertEquals(failure, assertThrows(IllegalStateException.class, () -> work.run("terrain", () -> {
            clock.addAndGet(70_000_000);
            throw failure;
        })));
        assertEquals("terrain", work.longestOperation());
        assertEquals(70_000_000, work.longestNanos());
        assertTrue(work.summary().contains("terrain[calls=1, totalUs=70000, maxUs=70000]"));
    }
}
