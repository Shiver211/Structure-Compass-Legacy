package com.shiver.structurecompasslegacy.simulation;

import java.io.File;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;

public final class MemorySaveHandler implements ISaveHandler {

    private final ResourceTemplateManager templates = new ResourceTemplateManager();
    final boolean terrainEnabled;

    public MemorySaveHandler() {
        this(true);
    }

    MemorySaveHandler(boolean terrainEnabled) {
        this.terrainEnabled = terrainEnabled;
    }

    @Override
    public WorldInfo loadWorldInfo() {
        return null;
    }

    @Override
    public void checkSessionLock() {
    }

    @Override
    @MethodsReturnNonnullByDefault
    public IChunkLoader getChunkLoader(@Nonnull WorldProvider provider) {
        throw new UnsupportedOperationException("Simulation chunks live in memory");
    }

    @Override
    public void saveWorldInfoWithPlayer(@Nonnull WorldInfo info, @Nonnull NBTTagCompound player) {
        throw new UnsupportedOperationException("Simulation cannot save world data");
    }

    @Override
    public void saveWorldInfo(@Nonnull WorldInfo info) {
        throw new UnsupportedOperationException("Simulation cannot save world data");
    }

    @Override
    @MethodsReturnNonnullByDefault
    public IPlayerFileData getPlayerNBTManager() {
        throw new UnsupportedOperationException("Simulation has no player files");
    }

    @Override
    public void flush() {
    }

    @Override
    @MethodsReturnNonnullByDefault
    public File getWorldDirectory() {
        throw new UnsupportedOperationException("Simulation cannot access a world directory");
    }

    @Override
    @MethodsReturnNonnullByDefault
    public File getMapFileFromName(@Nonnull String name) {
        throw new UnsupportedOperationException("Simulation cannot access map files");
    }

    @Override
    @MethodsReturnNonnullByDefault
    public TemplateManager getStructureTemplateManager() {
        return templates;
    }
}
