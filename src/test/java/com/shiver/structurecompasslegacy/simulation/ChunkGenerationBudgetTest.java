package com.shiver.structurecompasslegacy.simulation;

import com.shiver.structurecompasslegacy.search.SearchLimitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkGenerationBudgetTest {

    @Test
    void nestedGenerationReservesCacheSlotsBeforeChunksExist() {
        ChunkGenerationBudget budget = new ChunkGenerationBudget(2, 10);
        budget.begin(10, 0);
        budget.begin(11, 0);
        assertEquals("cached_chunks", assertThrows(SearchLimitException.class,
                () -> budget.begin(12, 0)).getMessage());
        assertEquals(2, budget.peak());
    }

    @Test
    void aFailedCallReleasesItsSlotButStillConsumesGenerationBudget() {
        ChunkGenerationBudget budget = new ChunkGenerationBudget(1, 2);
        budget.begin(10, 0);
        budget.finish(10);
        budget.begin(11, 0);
        budget.finish(11);
        assertEquals("generated_chunks", assertThrows(SearchLimitException.class,
                () -> budget.begin(12, 0)).getMessage());
        assertEquals(2, budget.generated());
    }

    @Test
    void recursiveRequestsStopBeforeCallingTheGeneratorAgain() {
        ChunkGenerationBudget budget = new ChunkGenerationBudget(128, 512);
        budget.begin(10, 0);
        assertEquals("recursive_chunk", assertThrows(SearchLimitException.class,
                () -> budget.begin(10, 0)).getMessage());
    }
}
