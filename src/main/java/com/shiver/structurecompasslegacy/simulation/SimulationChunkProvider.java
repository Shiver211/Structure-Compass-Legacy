package com.shiver.structurecompasslegacy.simulation;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.search.SearchLimitException;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;

/** 不加载磁盘、休眠区块，不调用 onLoad 或自动 populate。 */
public final class SimulationChunkProvider extends ChunkProviderServer {

    private int generatedChunks;
    private int peakChunks;

    public SimulationChunkProvider(WorldServer world, IChunkGenerator generator) {
        super(world, null, generator);
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        return getLoadedChunk(x, z);
    }

    @Override
    public Chunk loadChunk(int x, int z, Runnable callback) {
        Chunk chunk = getLoadedChunk(x, z);
        if (callback != null) {
            callback.run();
        }

        return chunk;
    }

    @Override
    @MethodsReturnNonnullByDefault
    public Chunk provideChunk(int x, int z) {
        Chunk chunk = getLoadedChunk(x, z);
        if (chunk != null) {
            return chunk;
        }

        if (loadedChunks.size() >= CompassConfig.cachedChunks) {
            throw new SearchLimitException("cached_chunks");
        }
        if (generatedChunks >= CompassConfig.generatedChunks) {
            throw new SearchLimitException("generated_chunks");
        }

        generatedChunks++;
        chunk = chunkGenerator.generateChunk(x, z);
        loadedChunks.put(ChunkPos.asLong(x, z), chunk);
        peakChunks = Math.max(peakChunks, loadedChunks.size());
        return chunk;
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        return getLoadedChunk(x, z) != null;
    }

    @Override
    public boolean saveChunks(boolean all) {
        return true;
    }

    @Override
    public void flushToDisk() {
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    public boolean tick() {
        return false;
    }

    public int generatedChunks() {
        return generatedChunks;
    }

    public int peakChunks() {
        return peakChunks;
    }

    public void clear() {
        loadedChunks.clear();
    }
}
