package com.shiver.structurecompasslegacy.verification;

import java.io.File;
import java.net.Proxy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;

/** 使用真实服务端组件运行受控世界；不启动网络监听或玩家会话。 */
final class ControlledServer extends MinecraftServer {

    ControlledServer(File directory, DataFixer fixer) {
        super(directory, Proxy.NO_PROXY, fixer, null, null, null, null);
        setPlayerList(new PlayerList(this) {});
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean canStructuresSpawn() {
        return false;
    }

    @Override
    public GameType getGameType() {
        return GameType.SURVIVAL;
    }

    @Override
    public EnumDifficulty getDifficulty() {
        return EnumDifficulty.NORMAL;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int getOpPermissionLevel() {
        return 4;
    }

    @Override
    public boolean shouldBroadcastRconToOps() {
        return false;
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public boolean shouldUseNativeTransport() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return false;
    }

    @Override
    public String shareToLAN(GameType type, boolean allowCheats) {
        return null;
    }

    @Override
    public boolean isCallingFromMinecraftThread() {
        return true;
    }
}
