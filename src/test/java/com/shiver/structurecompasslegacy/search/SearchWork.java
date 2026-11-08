package com.shiver.structurecompasslegacy.search;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** 只记录操作名称和耗时，不包含种子或坐标。 */
public final class SearchWork {

    private final LongSupplier clock;
    private final Map<String, Timing> timings = new LinkedHashMap<>();
    private long longestNanos;
    private String longestOperation = "";

    public SearchWork() {
        this(System::nanoTime);
    }

    public SearchWork(LongSupplier clock) {
        this.clock = clock;
    }

    public <T> T measure(String operation, Supplier<T> action) {
        long started = clock.getAsLong();
        try {
            return action.get();
        } finally {
            long elapsed = clock.getAsLong() - started;
            Timing timing = timings.computeIfAbsent(operation, ignored -> new Timing());
            timing.calls++;
            timing.totalNanos += elapsed;
            timing.longestNanos = Math.max(timing.longestNanos, elapsed);
            if (elapsed > longestNanos) {
                longestNanos = elapsed;
                longestOperation = operation;
            }
        }
    }

    public void run(String operation, Runnable action) {
        measure(operation, () -> {
            action.run();
            return null;
        });
    }

    public long longestNanos() {
        return longestNanos;
    }

    public String longestOperation() {
        return longestOperation;
    }

    public String summary() {
        StringBuilder result = new StringBuilder();
        timings.forEach((name, timing) -> {
            if (result.length() > 0) {
                result.append("; ");
            }
            result.append(name).append("[calls=").append(timing.calls)
                    .append(", totalUs=").append(timing.totalNanos / 1_000)
                    .append(", maxUs=").append(timing.longestNanos / 1_000).append(']');
        });
        return result.toString();
    }

    private static final class Timing {
        private int calls;
        private long totalNanos;
        private long longestNanos;
    }
}
