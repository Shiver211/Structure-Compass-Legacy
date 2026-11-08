package com.shiver.structurecompasslegacy.api;

import com.shiver.structurecompasslegacy.CompassConfig;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public final class SearchContext {

    // 真实世界仅用于读取参数和创建独立模拟环境；定位器不得调用其生成流程。
    public final WorldServer source;
    public final BlockPos origin;
    public final long seed;
    public final int dimension;
    public final int radius;

    public SearchContext(WorldServer source, BlockPos origin) {
        this.source = source;
        this.origin = origin.toImmutable();
        this.seed = source.getSeed();
        this.dimension = source.provider.getDimension();
        this.radius = CompassConfig.radius;
    }

    public boolean contains(BlockPos position) {
        long deltaX = (long) position.getX() - origin.getX();
        long deltaZ = (long) position.getZ() - origin.getZ();
        return deltaX * deltaX + deltaZ * deltaZ <= (long) radius * radius;
    }
}
