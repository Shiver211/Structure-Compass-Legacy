package com.shiver.structurecompasslegacy.api;

import java.util.function.LongSupplier;

public final class SearchBudget {

    private final long deadline;
    private final LongSupplier clock;

    public SearchBudget(long now, long nanoseconds) {
        this(now, nanoseconds, System::nanoTime);
    }

    public SearchBudget(long now, long nanoseconds, LongSupplier clock) {
        this.deadline = now + nanoseconds;
        this.clock = clock;
    }

    public boolean exhausted() {
        return clock.getAsLong() >= deadline;
    }

    public long nanoTime() {
        return clock.getAsLong();
    }
}
