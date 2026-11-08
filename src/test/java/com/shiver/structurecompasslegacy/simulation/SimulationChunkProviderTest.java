package com.shiver.structurecompasslegacy.simulation;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimulationChunkProviderTest {

    @Test
    void directTerrainIsCountedAndWorldGenerationDoesNotDoubleCountIt() {
        IChunkGenerator generator = mock(IChunkGenerator.class);
        SimulationChunkProvider chunks = new SimulationChunkProvider(mock(WorldServer.class), generator);
        when(generator.generateChunk(anyInt(), anyInt())).thenAnswer(call -> {
            chunks.prepareTerrain(call.getArgument(0), call.getArgument(1), () -> {});
            return mock(Chunk.class);
        });
        chunks.prepareTerrain(0, 0, () -> {});
        chunks.provideChunk(1, 0);
        chunks.provideChunk(1, 0);
        assertEquals(2, chunks.generatedChunks());
        assertEquals(1, chunks.peakChunks());
        chunks.clear();
        assertEquals(0, chunks.getLoadedChunkCount());
        assertEquals(2, chunks.generatedChunks());
    }

    @Test
    void terrainExceptionReleasesReservationAndCanBeRetried() {
        SimulationChunkProvider chunks = new SimulationChunkProvider(mock(WorldServer.class), mock(IChunkGenerator.class));
        assertThrows(IllegalStateException.class, () -> chunks.prepareTerrain(0, 0, () -> {
            throw new IllegalStateException("fixture failure");
        }));
        chunks.prepareTerrain(0, 0, () -> {});
        assertEquals(2, chunks.generatedChunks());
    }
}
