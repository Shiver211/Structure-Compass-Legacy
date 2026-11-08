package com.shiver.structurecompasslegacy.simulation;

import java.util.function.Supplier;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;

/** 每个候选创建一个计划；各阶段捕获同一随机源和临时状态。 */
public final class SimulationPlan implements AutoCloseable {

    public enum Phase {
        TERRAIN,
        PREREQUISITES,
        PLACEMENT,
        COLLECT
    }

    private final SimulationStage[] stages;
    private final Runnable beginPlacement;
    private final Supplier<SearchTarget> collect;
    private final Runnable cleanup;
    private int phase;
    private boolean capturing;
    private boolean closed;
    private long longestCallNanos;
    private String longestOperation = "plan";
    private SearchStep terminal;

    public SimulationPlan(
            SimulationStage terrain,
            SimulationStage prerequisites,
            SimulationStage placement,
            Runnable beginPlacement,
            Supplier<SearchTarget> collect,
            Runnable cleanup) {
        this.stages = new SimulationStage[] {terrain, prerequisites, placement};
        this.beginPlacement = beginPlacement;
        this.collect = collect;
        this.cleanup = cleanup;
    }

    public SearchStep advance(SearchBudget budget) throws Exception {
        if (closed) {
            throw new IllegalStateException("Simulation plan is closed");
        }
        if (terminal != null) {
            return terminal;
        }

        while (!budget.exhausted()) {
            long started = budget.nanoTime();
            SimulationStage.Result result = SimulationStage.Result.COMPLETE;
            try {
                if (phase == Phase.COLLECT.ordinal()) {
                    SearchTarget target = collect.get();
                    terminal = target == null ? SearchStep.exhausted() : SearchStep.found(target);
                } else {
                    if (phase == Phase.PLACEMENT.ordinal() && !capturing) {
                        beginPlacement.run();
                        capturing = true;
                    }
                    result = stages[phase].advance(budget);
                }
            } finally {
                long elapsed = budget.nanoTime() - started;
                if (elapsed > longestCallNanos) {
                    longestCallNanos = elapsed;
                    longestOperation = "plan." + phase().name().toLowerCase(java.util.Locale.ROOT);
                }
            }

            if (terminal != null && terminal.state == SearchStep.State.FOUND) {
                return terminal;
            }
            if (longestCallNanos > CompassConfig.slowCallMillis * 1_000_000L) {
                terminal = SearchStep.limit("slow_call");
                return terminal;
            }
            if (terminal != null) {
                return terminal;
            }
            if (result == SimulationStage.Result.REJECTED) {
                terminal = SearchStep.exhausted();
                return terminal;
            }
            if (result == SimulationStage.Result.RUNNING) {
                return SearchStep.running();
            }
            phase++;
        }
        return SearchStep.running();
    }

    public Phase phase() {
        return Phase.values()[phase];
    }

    public long longestCallNanos() {
        return longestCallNanos;
    }

    public String longestOperation() {
        return longestOperation;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            cleanup.run();
        }
    }
}
