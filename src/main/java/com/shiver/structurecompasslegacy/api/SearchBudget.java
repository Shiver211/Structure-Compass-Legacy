package com.shiver.structurecompasslegacy.api;

public final class SearchBudget {

    private final long deadline;

    public SearchBudget(long now, long nanoseconds) {
        this.deadline = now + nanoseconds;
    }

    public boolean exhausted() {
        return System.nanoTime() >= deadline;
    }
}
