package com.shiver.structurecompasslegacy.verification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import com.shiver.structurecompasslegacy.search.SearchWork;

/** 单次主线程调用前后对照，避免把两个 tick 之间的正常世界变化误报为泄漏。 */
final class WorldStateSnapshot implements AutoCloseable {

    private final WorldServer source;
    private final WorldServer registeredWorld;
    private final Set<Chunk> chunks;
    private final NBTTagCompound worldInfo;
    private final byte[] random;
    private final int entities;
    private final SharedStateSnapshot shared;
    private SearchWork work;

    static WorldStateSnapshot capture(WorldServer source, SearchWork work) {
        WorldStateSnapshot snapshot = work.measure("verify.before", () -> new WorldStateSnapshot(source));
        snapshot.work = work;
        return snapshot;
    }

    WorldStateSnapshot(WorldServer source) {
        this.source = source;
        registeredWorld = DimensionManager.getWorld(source.provider.getDimension());
        chunks = new HashSet<>(source.getChunkProvider().getLoadedChunks());
        worldInfo = worldInfoState(source.getWorldInfo());
        random = randomState(source);
        entities = source.loadedEntityList.size();
        shared = new SharedStateSnapshot(source.getMapStorage(), source.getPerWorldStorage());
    }

    @Override
    public void close() {
        if (work == null) {
            verify();
        } else {
            work.run("verify.after", this::verify);
        }
    }

    private void verify() {
        require(DimensionManager.getWorld(source.provider.getDimension()) == registeredWorld, "dimension registration changed");
        require(chunks.equals(new HashSet<>(source.getChunkProvider().getLoadedChunks())), "real chunks changed");
        verifyWorldInfo();
        require(Arrays.equals(random, randomState(source)), "real world random changed");
        require(entities == source.loadedEntityList.size(), "real entity count changed");
        shared.verify();
    }

    private static NBTTagCompound worldInfoState(WorldInfo info) {
        NBTTagCompound snapshot = info.cloneNBTCompound(null).copy();
        // 原版序列化会把 LastPlayed 写成当前系统时间，并非 WorldInfo 状态发生变化。
        snapshot.removeTag("LastPlayed");
        return snapshot;
    }

    private void verifyWorldInfo() {
        NBTTagCompound current = worldInfoState(source.getWorldInfo());
        if (worldInfo.equals(current)) {
            return;
        }
        Set<String> changedKeys = new TreeSet<>(worldInfo.getKeySet());
        changedKeys.addAll(current.getKeySet());
        changedKeys.removeIf(key -> Objects.equals(worldInfo.getTag(key), current.getTag(key)));
        // 只输出字段名，避免在诊断日志中写入世界种子或坐标。
        throw new IllegalStateException("Foundation verification: real WorldInfo changed: "
                + String.join(", ", changedKeys));
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Foundation verification: " + message);
        }
    }

    private static byte[] randomState(WorldServer source) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
                stream.writeObject(source.rand);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect world random without consuming it", exception);
        }
    }
}
