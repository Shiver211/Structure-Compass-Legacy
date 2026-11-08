package com.shiver.structurecompasslegacy.simulation;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationBlockTicksTest {

    @BeforeAll
    static void initializeBlocks() {
        Bootstrap.register();
    }

    @Test
    void updatesPreserveTimePriorityAndDuplicateSemantics() {
        SimulationBlockTicks ticks = new SimulationBlockTicks();
        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = first.east();
        ticks.schedule(first, Blocks.STONE, 10, 2);
        ticks.schedule(second, Blocks.STONE, 10, 1);
        ticks.schedule(first, Blocks.STONE, 1, 0);
        assertEquals(second, ticks.poll().position);
        assertEquals(10, ticks.poll().scheduledTime);
        assertTrue(ticks.isEmpty());
    }

    @Test
    void removingARegionAndClearingCandidatesRemoveBothQueueIndexes() {
        SimulationBlockTicks ticks = new SimulationBlockTicks();
        ticks.schedule(new BlockPos(-1, 64, -1), Blocks.STONE, 1, 0);
        ticks.schedule(new BlockPos(20, 64, 20), Blocks.STONE, 1, 0);
        assertEquals(1, ticks.within(new StructureBoundingBox(-16, 0, -16, -1, 255, -1), true).size());
        assertFalse(ticks.contains(new BlockPos(-1, 64, -1), Blocks.STONE));
        ticks.clear();
        assertTrue(ticks.isEmpty());
        assertFalse(ticks.contains(new BlockPos(20, 64, 20), Blocks.STONE));
    }
}
