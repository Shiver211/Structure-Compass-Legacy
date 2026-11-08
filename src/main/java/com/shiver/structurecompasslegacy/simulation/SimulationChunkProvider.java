package com.shiver.structurecompasslegacy.simulation;

import java.util.HashSet;
import java.util.Set;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.search.SearchLimitException;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nonnull;

/** 不加载磁盘、休眠区块，不调用 onLoad 或自动 populate。 */
public final class SimulationChunkProvider extends ChunkProviderServer {

    private final ChunkGenerationBudget budget;
    private final Set<Long> preparingTerrain = new HashSet<>();

    public SimulationChunkProvider(WorldServer world, IChunkGenerator generator) {
        super(world, null, generator);
        budget = new ChunkGenerationBudget(CompassConfig.cachedChunks, CompassConfig.generatedChunks);
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

        long key = ChunkPos.asLong(x, z);
        budget.begin(key, loadedChunks.size());
        try {
            chunk = chunkGenerator.generateChunk(x, z);
            loadedChunks.put(key, chunk);
            return chunk;
        } finally {
            budget.finish(key);
        }
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        return getLoadedChunk(x, z) != null;
    }

    public void prepareTerrain(int x, int z, Runnable generation) {
        long key = ChunkPos.asLong(x, z);
        if (!preparingTerrain.add(key)) {
            throw new SearchLimitException("recursive_chunk");
        }
        boolean reserve = !budget.isGenerating(key);
        try {
            if (reserve) {
                budget.begin(key, loadedChunks.size());
            }
            generation.run();
        } finally {
            if (reserve) {
                budget.finish(key);
            }
            preparingTerrain.remove(key);
        }
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

    @Override
    public void queueUnload(@Nonnull Chunk chunk) {
    }

    @Override
    public void queueUnloadAll() {
    }

    public int generatedChunks() {
        return budget.generated();
    }

    public int peakChunks() {
        return budget.peak();
    }

    public void clear() {
        loadedChunks.clear();
    }
}
