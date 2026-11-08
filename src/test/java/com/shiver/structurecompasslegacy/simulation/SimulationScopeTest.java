package com.shiver.structurecompasslegacy.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationScopeTest {

    @Test
    void restoresEventDeliveryAfterNestedCallsAndFailure() {
        assertFalse(SimulationScope.isActive());
        try (SimulationScope outer = SimulationScope.enter()) {
            assertTrue(SimulationScope.isActive());
            assertThrows(IllegalStateException.class, () -> {
                try (SimulationScope inner = SimulationScope.enter()) {
                    throw new IllegalStateException("Simulated generator failure");
                }
            });
            assertTrue(SimulationScope.isActive());
        }
        assertFalse(SimulationScope.isActive());
    }
}
