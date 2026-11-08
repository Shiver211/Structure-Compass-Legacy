package com.shiver.structurecompasslegacy.api;

public interface SearchTask extends AutoCloseable {

    SearchStep advance(SearchBudget budget) throws Exception;

    long checkedCandidates();

    long totalCandidates();

    default int generatedChunks() {
        return 0;
    }

    default int peakCachedChunks() {
        return 0;
    }

    default long longestCallNanos() {
        return 0;
    }

    default String longestOperation() {
        return "task";
    }

    @Override
    void close();
}
