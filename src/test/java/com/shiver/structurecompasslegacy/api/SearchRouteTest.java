package com.shiver.structurecompasslegacy.api;

import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchRouteTest {

    @Test
    void predictionHasPriorityAndOnlyUnavailabilitySelectsSimulation() {
        SearchRoute prediction = route(SearchMethod.PREDICTION, "");
        SearchRoute simulation = route(SearchMethod.SIMULATION, "");
        assertSame(prediction, definition(prediction, simulation).selectRoute(null));

        SearchRoute unavailable = route(SearchMethod.PREDICTION, "terrain_unavailable");
        assertSame(simulation, definition(unavailable, simulation).selectRoute(null));
        SearchRoute verifiedSimulation = new SearchRoute(SearchMethod.SIMULATION, simulation.locator, false, "");
        assertEquals(SearchMethod.SIMULATION.translationKey(), verifiedSimulation.translationKey());
        assertEquals("structurecompasslegacy.method.experimental_simulation", simulation.translationKey());
    }

    @Test
    void unavailableRoutesKeepTheConcreteReason() {
        StructureDefinition definition = definition(route(SearchMethod.PREDICTION, "missing_terrain"),
                route(SearchMethod.SIMULATION, "missing_adapter"));
        assertEquals("missing_terrain", definition.unavailableReason(null));
    }

    private StructureDefinition definition(SearchRoute prediction, SearchRoute simulation) {
        return new StructureDefinition("test:routes", "Test", "Routes", "路线", world -> true,
                prediction, simulation, "", () -> true);
    }

    private SearchRoute route(SearchMethod method, String reason) {
        return new SearchRoute(method, new StructureLocator() {
            @Override
            public SearchTask createTask(SearchContext context) {
                throw new AssertionError("Route selection must not create a task");
            }

            @Override
            public String unavailableReason(WorldServer world) {
                return reason;
            }
        }, "");
    }
}
