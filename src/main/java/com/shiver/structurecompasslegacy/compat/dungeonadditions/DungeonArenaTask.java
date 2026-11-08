package com.shiver.structurecompasslegacy.compat.dungeonadditions;

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
import com.shiver.structurecompasslegacy.simulation.BiomeRuleComparison;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/** 两类竞技场的首个建筑无地形失败分支；后续随机布局不参与起始点定位。 */
final class DungeonArenaTask extends ChunkSearchTask {

    enum Arena {
        NETHER(13259582, 8, false),
        OBSIDILITH(20387994, 4, true);

        final int salt;
        final int offset;
        final boolean whitelist;

        Arena(int salt, int offset, boolean whitelist) {
            this.salt = salt;
            this.offset = offset;
            this.whitelist = whitelist;
        }
    }

    private final Arena arena;
    private final int spacing;
    private final int height;
    private final Set<Biome> listedBiomes;
    private SimulationSession session;

    static StructureLocator locator(Arena arena, IntSupplier spacing, IntSupplier height,
            Supplier<String[]> biomeTypes) {
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                return new DungeonArenaTask(context, arena, spacing.getAsInt(), height.getAsInt(), biomeTypes.get());
            }

            @Override
            public String unavailableReason(WorldServer world) {
                if (spacing.getAsInt() <= 16 || height.getAsInt() < 0 || height.getAsInt() > 255) {
                    return "structurecompasslegacy.limitation.da_arena_parameters";
                }
                return GeneratorFactories.supportsBiomes(world)
                        ? "" : "structurecompasslegacy.reason.generator_not_adapted";
            }
        };
    }

    private DungeonArenaTask(SearchContext context, Arena arena, int spacing, int height, String[] types) {
        super(context);
        this.arena = arena;
        this.spacing = spacing;
        this.height = height;
        listedBiomes = listedBiomes(types);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        if (!candidate(arena, context.seed, chunkX, chunkZ, spacing)) {
            return null;
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (session == null) {
                session = SimulationSession.forBiomes(context.source);
                if (!BiomeRuleComparison.matches(context.source.getBiomeProvider(), session.world().getBiomeProvider())) {
                    throw new UnsupportedOperationException("Dungeon Additions biome rules could not be reproduced");
                }
            }
            Biome biome = session.world().getBiomeProvider().getBiome(new BlockPos(chunkX * 16, 0, chunkZ * 16));
            if (listedBiomes.contains(biome) != arena.whitelist) {
                return null;
            }
            return new SearchTarget(anchor(arena, chunkX, chunkZ, height));
        }
    }

    static boolean candidate(Arena arena, long seed, int chunkX, int chunkZ, int spacing) {
        return SeedMath.grid(seed, chunkX, chunkZ, spacing, 16, arena.salt, true);
    }

    static BlockPos anchor(Arena arena, int chunkX, int chunkZ, int height) {
        return new BlockPos(chunkX * 16 + arena.offset, height, chunkZ * 16 + arena.offset);
    }

    private static Set<Biome> listedBiomes(String[] names) {
        Set<String> wanted = new HashSet<>();
        for (String name : names) {
            wanted.add(name.toUpperCase(Locale.ROOT));
        }
        Set<Biome> result = new HashSet<>();
        // 不调用会惰性创建类型或为群系补登记类型的查询。
        for (BiomeDictionary.Type type : BiomeDictionary.Type.getAll()) {
            if (wanted.contains(type.getName())) {
                result.addAll(BiomeDictionary.getBiomes(type));
            }
        }
        return result;
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
