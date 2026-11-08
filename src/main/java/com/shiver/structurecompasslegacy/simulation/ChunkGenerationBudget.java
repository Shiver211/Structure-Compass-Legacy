package com.shiver.structurecompasslegacy.simulation;

import java.util.HashSet;
import java.util.Set;

import com.shiver.structurecompasslegacy.search.SearchLimitException;

public final class ChunkGenerationBudget {

    private final int cacheLimit;
    private final int generationLimit;
    private final Set<Long> generating = new HashSet<>();
    private int generated;
    private int peak;

    public ChunkGenerationBudget(int cacheLimit, int generationLimit) {
        this.cacheLimit = cacheLimit;
        this.generationLimit = generationLimit;
    }

    public void begin(long chunk, int cached) {
        if (generating.contains(chunk)) {
            throw new SearchLimitException("recursive_chunk");
        }
        if (cached + generating.size() >= cacheLimit) {
            throw new SearchLimitException("cached_chunks");
        }
        if (generated >= generationLimit) {
            throw new SearchLimitException("generated_chunks");
        }
        generating.add(chunk);
        generated++;
        peak = Math.max(peak, cached + generating.size());
    }

    public void finish(long chunk) {
        generating.remove(chunk);
    }

    public boolean isGenerating(long chunk) {
        return generating.contains(chunk);
    }

    public int generated() {
        return generated;
    }

    public int peak() {
        return peak;
    }
}
