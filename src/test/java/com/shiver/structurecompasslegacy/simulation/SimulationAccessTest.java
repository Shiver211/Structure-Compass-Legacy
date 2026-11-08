package com.shiver.structurecompasslegacy.simulation;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class SimulationAccessTest {

    @Test
    void scopeRejectsRealWorldAndDiskBackedStorageButAllowsSimulation() {
        World real = mock(World.class);
        World simulation = mock(World.class, withSettings().extraInterfaces(SimulationMarker.class));
        assertDoesNotThrow(() -> SimulationAccess.requireWorld(real));
        try (SimulationScope ignored = SimulationScope.enter()) {
            assertThrows(UnsupportedOperationException.class, () -> SimulationAccess.requireWorld(real));
            assertThrows(UnsupportedOperationException.class, () -> SimulationAccess.requireMemoryStorage(false));
            assertDoesNotThrow(() -> SimulationAccess.requireWorld(simulation));
            assertDoesNotThrow(() -> SimulationAccess.requireMemoryStorage(true));
        }
        assertDoesNotThrow(() -> SimulationAccess.requireWorld(real));
    }

    @Test
    void fileEntryPointsFailWithoutChangingFiles(@TempDir Path directory) throws Exception {
        Path sentinel = directory.resolve("level.dat");
        Files.write(sentinel, new byte[] {7, 8, 9});
        ISaveHandler storage = new MemorySaveHandler();
        assertThrows(UnsupportedOperationException.class, storage::getWorldDirectory);
        assertThrows(UnsupportedOperationException.class, () -> storage.getMapFileFromName("probe"));
        assertThrows(UnsupportedOperationException.class, () -> storage.saveWorldInfo(mock(WorldInfo.class)));
        assertThrows(UnsupportedOperationException.class, () -> storage.getChunkLoader(null));
        storage.flush();
        assertEquals(3, Files.size(sentinel));
        assertEquals(7, Files.readAllBytes(sentinel)[0]);
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void temporaryIdsRemainDistinctAcrossCalls() {
        int entity = SimulationIds.entity();
        assertEquals(entity + 1, SimulationIds.entity());
        long tick = SimulationIds.blockTick();
        assertEquals(tick + 1, SimulationIds.blockTick());
        assertTrue(entity < 0);
        assertTrue(tick < 0);
    }
}
