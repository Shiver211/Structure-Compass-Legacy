package com.shiver.structurecompasslegacy.api;

import net.minecraft.world.WorldServer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StructureAvailabilityTest {

    @Test
    void missingTerrainAdapterIsReportedBeforeCreatingATask() {
        StructureLocator locator = new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                throw new AssertionError("An unavailable locator must not start a task");
            }

            @Override
            public String unavailableReason(WorldServer world) {
                return "structurecompasslegacy.reason.generator_not_adapted";
            }
        };
        StructureDefinition definition = new StructureDefinition(
                "test:terrain", "Test", "Terrain", "地形", SearchMethod.PREDICTION,
                world -> true, locator, "");

        assertFalse(definition.supports(null));
        assertEquals("structurecompasslegacy.reason.generator_not_adapted", definition.unavailableReason(null));
    }

    @Test
    void missingLocatorCannotBeAdvertisedAsAvailable() {
        StructureDefinition definition = new StructureDefinition(
                "test:missing", "Test", "Missing", "未支持", SearchMethod.UNSUPPORTED,
                world -> true, null, "");
        assertFalse(definition.supports(null));
    }

    @Test
    void dimensionAndGenerationSwitchHaveDifferentReasons() {
        StructureDefinition wrongDimension = new StructureDefinition(
                "test:dimension", "Test", "Dimension", "维度", SearchMethod.PREDICTION,
                world -> false, context -> null, "");
        StructureDefinition disabled = new StructureDefinition(
                "test:disabled", "Test", "Disabled", "关闭", SearchMethod.PREDICTION,
                world -> true, context -> null, "", () -> false);

        assertEquals("structurecompasslegacy.reason.wrong_dimension", wrongDimension.unavailableReason(null));
        assertEquals("structurecompasslegacy.reason.generation_disabled", disabled.unavailableReason(null));
    }
}
