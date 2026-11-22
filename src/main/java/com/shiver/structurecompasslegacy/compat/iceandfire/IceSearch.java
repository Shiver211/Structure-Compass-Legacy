package com.shiver.structurecompasslegacy.compat.iceandfire;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.compat.biomesoplenty.BiomesOPlentyModule;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.WorldInfo;

final class IceSearch extends ChunkSearchTask {
    private final IceLandmark kind;
    private final IceAndFireModule config;
    private final IcePlacement placement;
    private final boolean terrain;
    private SimulationSession simulation;
    private BiomeProvider lostBiomes;
    private LostWilderness wilderness;
    private IceBiomes biomes;
    private IceStability stability;

    IceSearch(SearchContext context, IceLandmark kind, IceAndFireModule config) {
        super(context);
        this.kind = kind;
        this.config = config;
        placement = IcePlacement.forSearch(context, config);
        terrain = GeneratorFactories.supports(context.source);
        if (placement != null && !terrain
            && context.source.provider.getClass().getName().equals(LostWilderness.PROVIDER)) {
            // The pack uses the unfiltered profile. Do not approximate a filtered/space profile.
            Class<?> type = context.source.getBiomeProvider().getClass();
            try (SimulationScope ignored = SimulationScope.enter()) {
                if (type.getName().equals("biomesoplenty.common.world.BiomeProviderBOP")) {
                    lostBiomes = BiomesOPlentyModule.createBiomes(context.source);
                } else if (type == BiomeProvider.class) {
                    lostBiomes = new BiomeProvider(new WorldInfo(context.source.getWorldInfo()));
                }
                if (lostBiomes != null && needsWilderness()) {
                    wilderness = LostWilderness.match(context.source);
                }
            }
        }
    }

    private boolean horizontalOnly() {
        return kind.roost() || kind == IceLandmark.ICE_CAVE || kind == IceLandmark.SIREN_ISLAND;
    }

    private boolean needsWilderness() {
        return !horizontalOnly() || kind == IceLandmark.SIREN_ISLAND;
    }

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        while (!budget.exhausted()) {
            if (stability == null) {
                SearchStep step = super.advance(budget);
                if (step.state != SearchStep.State.FOUND || stability == null) {
                    return step;
                }
            }
            try (SimulationScope ignored = SimulationScope.enter()) {
                while (stability.hasNext() && !budget.exhausted()) {
                    long before = System.nanoTime();
                    boolean clear;
                    long elapsed;
                    try {
                        clear = stability.inspectNext(this::futureBiome);
                    } finally {
                        elapsed = System.nanoTime() - before;
                        recordCallNanos(elapsed);
                    }
                    if (elapsed > CompassConfig.slowCallMillis * 1_000_000L) {
                        return SearchStep.limit("slow_call");
                    }
                    if (!clear) {
                        stability = null;
                        break;
                    }
                }
                if (stability != null && !stability.hasNext()) {
                    IceStability checked = stability;
                    stability = null;
                    // 核验可能跨 tick：返回前重查真实生成状态和生成器的实时 last。
                    int x = checked.center.getX() >> 4, z = checked.center.getZ() >> 4;
                    if (!context.source.isChunkGeneratedAt(x, z)
                        && checked.prediction.equals(placement.predict(kind, checked.biome, checked.center))) {
                        return SearchStep.found(checked.target);
                    }
                }
            }
        }
        return SearchStep.running();
    }

    private Biome futureBiome(int x, int z) {
        Chunk loaded = context.source.getChunkProvider().getLoadedChunk(x, z);
        if (loaded != null) {
            return loaded.isTerrainPopulated() ? null
                : loaded.getBiome(new BlockPos(x * 16 + 8, 0, z * 16 + 8), context.source.getBiomeProvider());
        }
        return biomes.at(x, z);
    }

    private SearchTarget predicted(Biome biome, BlockPos center, BlockPos candidate, BlockPos position) {
        SearchTarget target = new SearchTarget(position);
        if (kind == IceLandmark.SIREN_ISLAND || kind.material != null && !kind.roost()) {
            stability = new IceStability(placement, kind, biome, center, candidate, target);
        }
        return target;
    }

    @Override
    protected SearchTarget inspect(int cx, int cz) {
        BlockPos center = new BlockPos(cx * 16 + 8, 0, cz * 16 + 8);
        Chunk loaded = context.source.getChunkProvider().getLoadedChunk(cx, cz);
        if (loaded != null && loaded.isTerrainPopulated()) {
            BlockPos candidate = null;
            if (placement != null && (kind == IceLandmark.SIREN_ISLAND || kind.material != null && !kind.roost())) {
                candidate = placement.predict(kind, loaded.getBiome(center, context.source.getBiomeProvider()), center);
            }
            BlockPos found = IceEvidence.find(context.source, loaded, kind, config.last(kind.last), candidate);
            return found == null ? null : new SearchTarget(found);
        }
        if (placement == null || loaded != null || !context.contains(center)
            || !terrain && (lostBiomes == null || needsWilderness() && wilderness == null)
            || context.source.getWorldType() == WorldType.FLAT && !config.flag("spawnStructuresOnSuperflat")) {
            return null;
        }
        if (!placement.mayPredict(kind, cx, cz)) {
            return null;
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (terrain && simulation == null) {
                simulation = new SimulationSession(context.source);
            }
            if (biomes == null) {
                biomes = new IceBiomes(terrain ? simulation.world().getBiomeProvider() : lostBiomes);
            }
            Biome biome = biomes.at(cx, cz);
            IcePlacement.MausoleumRoll tomb = kind == IceLandmark.MAUSOLEUM ? new IcePlacement.MausoleumRoll() : null;
            BlockPos candidate = placement.predict(kind, biome, center, tomb);
            if (candidate == null || context.source.isChunkGeneratedAt(cx, cz)) {
                return null;
            }
            if (wilderness != null && !wilderness.wilderness(cx, cz)) {
                // 城市、桥路和铁路可能抬高海洋表面，不能沿用塞壬岛的海平面导航高度。
                return null;
            }
            if (!terrain && horizontalOnly()) {
                // The three roosts and siren island need surface Y only for display; the ice den
                // does not have the sky test used by the other two underground variants.
                return predicted(biome, center, candidate,
                    kind == IceLandmark.ICE_CAVE ? candidate : candidate.up(context.source.getSeaLevel()));
            }
            if (simulation == null) {
                simulation = new SimulationSession(context.source,
                    w -> new LostTerrain(w, wilderness, context.source::isChunkGeneratedAt));
            }
            simulation.beginCandidate();
            SimulationWorld world = simulation.world();
            world.getChunk(cx, cz);
            BlockPos surface = world.getHeight(candidate);
            if (kind.material != null && !kind.roost()) {
                if (kind != IceLandmark.ICE_CAVE && world.canBlockSeeSky(candidate)) {
                    return null;
                }
                return predicted(biome, center, candidate, candidate);
            }
            if (kind.roost() || kind == IceLandmark.MAUSOLEUM) {
                while (surface.getY() > 1) {
                    IBlockState state = world.getBlockState(surface);
                    if (state.isOpaqueCube() && !(state.getBlock() instanceof BlockLog)
                        && !(state.getBlock() instanceof BlockLiquid)
                        && (kind != IceLandmark.MAUSOLEUM || !(state.getBlock() instanceof BlockLeaves))) {
                        break;
                    }
                    surface = surface.down();
                }
            }
            if (tomb != null) {
                if (!TombFootprint.clear(world, surface, tomb.facing)) {
                    return null;
                }
                // A guaranteed snow village takes precedence at the unshifted surface.
                if (config.flag("generateSnowVillages") && config.integer("generateSnowVillageChance") == 0) {
                    BlockPos last = config.last("lastSnowVillage");
                    int spacing = config.integer("worldGenDistance");
                    if (last == null || last.distanceSq(world.getHeight(center)) >= (double) (spacing * spacing)) {
                        return null;
                    }
                }
            }
            return predicted(biome, center, candidate, surface);
        } catch (LostTerrain.UnknownChunk ignored) {
            return null;
        }
    }

    private static final class TombFootprint {
        private static final ResourceLocation TEMPLATE = new ResourceLocation("iceandfire", "dread_mausoleum_forge");
        private static final ModMethod DREAD = ModMethod.bind(ModConstructor.type(
            "com.github.alexthe666.iceandfire.world.gen.WorldGenMausoleum"), "isPartOfRuin", IBlockState.class);

        static boolean clear(SimulationWorld world, BlockPos ground, EnumFacing face) {
            BlockPos size =
                world.getSaveHandler().getStructureTemplateManager().getTemplate(world.getMinecraftServer(), TEMPLATE)
                    .getSize();
            return IceFootprint.clearTomb(ground, face, size, p -> (boolean) DREAD.call(null, world.getBlockState(p)));
        }
    }

    @Override
    public int generatedChunks() {
        return simulation == null ? 0 : simulation.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return simulation == null ? 0 : simulation.peakCachedChunks();
    }

    @Override
    public void close() {
        stability = null;
        if (biomes != null) {
            biomes.clear();
        }
        if (simulation != null) {
            simulation.close();
        }
    }
}
