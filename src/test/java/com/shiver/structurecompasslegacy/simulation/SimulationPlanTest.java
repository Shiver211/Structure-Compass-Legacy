package com.shiver.structurecompasslegacy.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationPlanTest {

    @Test
    void stagesYieldAndCaptureOnlyAfterPrerequisites() throws Exception {
        List<String> calls = new ArrayList<>();
        AtomicLong clock = new AtomicLong();
        AtomicInteger placementCalls = new AtomicInteger();
        SimulationPlan plan = new SimulationPlan(
                SimulationStage.once(() -> {
                    calls.add("terrain");
                    clock.set(2);
                    return true;
                }),
                SimulationStage.once(() -> calls.add("prerequisites")),
                budget -> {
                    calls.add("placement");
                    return placementCalls.incrementAndGet() == 1
                            ? SimulationStage.Result.RUNNING : SimulationStage.Result.COMPLETE;
                },
                () -> calls.add("capture"),
                () -> new SearchTarget(BlockPos.ORIGIN),
                () -> calls.add("close"));

        assertEquals(SearchStep.State.RUNNING, plan.advance(new SearchBudget(0, 1, clock::get)).state);
        assertEquals(Arrays.asList("terrain"), calls);
        assertEquals(SearchStep.State.RUNNING, plan.advance(unlimited()).state);
        assertEquals(SearchStep.State.FOUND, plan.advance(unlimited()).state);
        plan.close();
        plan.close();
        assertEquals(Arrays.asList("terrain", "prerequisites", "capture", "placement", "placement", "close"), calls);
    }

    @Test
    void rejectedPlacementCannotTurnPartialWritesIntoAHit() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        SimulationPlan plan = new SimulationPlan(SimulationStage.skip(), SimulationStage.skip(),
                SimulationStage.once(() -> {
                    writes.incrementAndGet();
                    return false;
                }), () -> {},
                () -> {
                    throw new AssertionError("Rejected placement must not collect a result");
                },
                () -> writes.set(0));
        assertEquals(SearchStep.State.EXHAUSTED, plan.advance(unlimited()).state);
        assertEquals(1, writes.get());
        plan.close();
        assertEquals(0, writes.get());
    }

    @Test
    void closingAfterAnExceptionReleasesResourcesExactlyOnce() {
        AtomicInteger closed = new AtomicInteger();
        SimulationPlan plan = new SimulationPlan(budget -> {
            throw new IllegalStateException("generation failed");
        },
                SimulationStage.skip(), SimulationStage.skip(), () -> {}, () -> null, closed::incrementAndGet);
        assertThrows(IllegalStateException.class, () -> plan.advance(unlimited()));
        plan.close();
        plan.close();
        assertEquals(1, closed.get());
    }

    @Test
    void slowStageStopsBeforePlacement() throws Exception {
        AtomicLong clock = new AtomicLong();
        SimulationPlan plan = new SimulationPlan(SimulationStage.once(() -> {
            clock.set((CompassConfig.slowCallMillis + 1L) * 1_000_000L);
            return true;
        }), SimulationStage.skip(), budget -> {
            throw new AssertionError("Slow terrain must not start placement");
        }, () -> {}, () -> null, () -> {});
        SearchStep result = plan.advance(new SearchBudget(0, Long.MAX_VALUE, clock::get));
        assertEquals(SearchStep.State.LIMIT, result.state);
        assertEquals("slow_call", result.reason);
        assertEquals(clock.get(), plan.longestCallNanos());
        plan.close();
    }

    private SearchBudget unlimited() {
        return new SearchBudget(0, Long.MAX_VALUE, () -> 0);
    }
}
