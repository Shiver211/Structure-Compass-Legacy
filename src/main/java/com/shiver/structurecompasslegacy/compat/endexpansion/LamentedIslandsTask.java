package com.shiver.structurecompasslegacy.compat.endexpansion;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;
import com.shiver.structurecompasslegacy.search.SeedMath;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/** 只预测必定先加入布局的起始岛；不执行模组的全局随机布局或模板计数。 */
final class LamentedIslandsTask extends ChunkSearchTask {

    private final int spacing;
    private final Set<Biome> excludedBiomes;
    private SimulationSession session;

    static StructureLocator locator(IntSupplier spacing, Supplier<String[]> excludedTypes) {
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                return new LamentedIslandsTask(context, spacing.getAsInt(), excludedTypes.get());
            }

            @Override
            public String unavailableReason(WorldServer world) {
                if (spacing.getAsInt() <= 16) {
                    return "structurecompasslegacy.reason.generation_disabled";
                }
                return GeneratorFactories.supports(world)
                        ? "" : "structurecompasslegacy.reason.generator_not_adapted";
            }
        };
    }

    private LamentedIslandsTask(SearchContext context, int spacing, String[] excludedTypes) {
        super(context);
        this.spacing = spacing;
        this.excludedBiomes = excludedBiomes(excludedTypes);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        if (!outsideCentralArea(chunkX, chunkZ) || !gridCandidate(context.seed, chunkX, chunkZ, spacing)) {
            return null;
        }

        try (SimulationScope ignored = SimulationScope.enter()) {
            if (session == null) {
                session = new SimulationSession(context.source);
            }
            // 原判定在区块西北角取群系，使用独立图层，不准备地形或读取真实区块。
            BlockPos corner = new BlockPos(chunkX << 4, 0, chunkZ << 4);
            Biome biome = session.world().getBiomeProvider().getBiome(corner);
            return excludedBiomes.contains(biome) ? null : new SearchTarget(anchor(chunkX, chunkZ));
        }
    }

    static boolean outsideCentralArea(int chunkX, int chunkZ) {
        return Math.abs(chunkX * 16 + 8) > 600 || Math.abs(chunkZ * 16 + 8) > 600;
    }

    static boolean gridCandidate(long seed, int chunkX, int chunkZ, int spacing) {
        // 当前分支在负坐标 floor 修正之后检查绝对值，不能改成原区块坐标的绝对值。
        int adjustedX = chunkX < 0 ? chunkX - spacing + 1 : chunkX;
        int adjustedZ = chunkZ < 0 ? chunkZ - spacing + 1 : chunkZ;
        return Math.abs(adjustedX) > 35 && Math.abs(adjustedZ) > 35
                && SeedMath.grid(seed, chunkX, chunkZ, spacing, 16, 10388824, true);
    }

    static BlockPos anchor(int chunkX, int chunkZ) {
        return new BlockPos(chunkX * 16 + 8, 90, chunkZ * 16 + 8);
    }

    private static Set<Biome> excludedBiomes(String[] names) {
        Set<String> excluded = new HashSet<>();
        for (String name : names) {
            excluded.add(name.toUpperCase(Locale.ROOT));
        }
        Set<Biome> biomes = new HashSet<>();
        // getTypes 会惰性修改全局群系表；读取已有类型的集合即可完成相同的排除条件。
        for (BiomeDictionary.Type type : BiomeDictionary.Type.getAll()) {
            if (excluded.contains(type.getName())) {
                biomes.addAll(BiomeDictionary.getBiomes(type));
            }
        }
        return biomes;
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
