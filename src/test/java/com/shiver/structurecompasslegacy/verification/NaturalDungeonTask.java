package com.shiver.structurecompasslegacy.verification;

import java.util.List;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.SimulationSearchTask;
import com.shiver.structurecompasslegacy.search.SearchWork;
import com.shiver.structurecompasslegacy.simulation.SimulationPlan;
import com.shiver.structurecompasslegacy.simulation.SimulationStage.Result;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;

/** 原版地牢的有限自然复演验证，不声明整包坐标可靠性，也不写物品或结构记录。 */
final class NaturalDungeonTask extends SimulationSearchTask {

    private final ChunkGeneratorSettings settings;
    private final SearchWork work = new SearchWork();
    private List<MapGenStructure> structures;
    private StrongholdRing strongholds;
    private int biomeRejections;
    private int structureRejections;
    private int populatedChunks;
    private int dungeonAttempts;

    NaturalDungeonTask(SearchContext context) {
        super(context);
        String reason = unavailableReason(context.source);
        if (!reason.isEmpty()) {
            throw new UnsupportedOperationException(reason);
        }
        settings = ChunkGeneratorSettings.Factory.jsonToFactory(
                context.source.getWorldInfo().getGeneratorOptions()).build();
        if (!settings.useDungeons || settings.dungeonChance <= 0) {
            throw new UnsupportedOperationException("Natural dungeon generation is disabled in this world");
        }
    }

    static String unavailableReason(WorldServer world) {
        // RC 的 PopulateChunkEvent.Pre 使用原版 Random 并补全跨区块结构。
        // 仅调用 FactorMatch 不能复现这些前置步骤，不能把相同生成器类当成相同生成环境。
        if (Loader.isModLoaded("reccomplex")) {
            return "structurecompasslegacy.verify.natural_recurrent";
        }
        boolean vanilla = world.provider.getClass() == WorldProviderSurface.class
                && world.getBiomeProvider().getClass() == BiomeProvider.class
                && world.getChunkProvider().chunkGenerator.getClass() == ChunkGeneratorOverworld.class;
        return vanilla ? "" : "structurecompasslegacy.verify.natural_environment";
    }

    @Override
    protected boolean isCandidate(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    protected SimulationPlan createPlan(SimulationWorld world, int chunkX, int chunkZ) {
        if (structures == null) {
            structures = work.measure("structure.initialize", () -> DungeonTerrain.createStructureChecks(world, settings));
            if (world.getWorldInfo().isMapFeaturesEnabled() && settings.useStrongholds) {
                strongholds = work.measure("stronghold.initialize", () -> new StrongholdRing(context.seed));
            }
        }
        Candidate candidate = new Candidate(world, chunkX, chunkZ);
        return new SimulationPlan(candidate.terrain::advance, candidate::prepare, candidate::place,
                world::beginPlacement, candidate::collect, () -> {
                    if (candidate.terrain.rejection().equals("shared_biome_state")) {
                        biomeRejections++;
                    } else if (candidate.terrain.rejection().equals("large_structure")) {
                        structureRejections++;
                    }
                });
    }

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        SearchStep step;
        try (WorldStateSnapshot snapshot = WorldStateSnapshot.capture(context.source, work)) {
            step = super.advance(budget);
        }
        if (step.state != SearchStep.State.FOUND && longestCallNanos() > CompassConfig.slowCallMillis * 1_000_000L) {
            return SearchStep.limit("slow_call");
        }
        return step;
    }

    @Override
    public long longestCallNanos() {
        return Math.max(super.longestCallNanos(), work.longestNanos());
    }

    @Override
    public String longestOperation() {
        return work.longestNanos() >= super.longestCallNanos() ? work.longestOperation() : super.longestOperation();
    }

    @Override
    public void close() {
        try (WorldStateSnapshot snapshot = WorldStateSnapshot.capture(context.source, work)) {
            work.run("session.close", super::close);
        } finally {
            structures = null;
            strongholds = null;
            LogManager.getLogger("structurecompasslegacy").info(
                    "Natural dungeon check: biomeRejected={}, structureRejected={}, populated={}, dungeonAttempts={}",
                    biomeRejections, structureRejections, populatedChunks, dungeonAttempts);
            LogManager.getLogger("structurecompasslegacy").info("Natural check operations: {}", work.summary());
        }
    }

    private final class Candidate {

        private final SimulationWorld world;
        private final DungeonTerrain terrain;
        private final DungeonPopulation population;
        private int lakePhase;
        private BlockPos spawner;

        private Candidate(SimulationWorld world, int chunkX, int chunkZ) {
            this.world = world;
            terrain = new DungeonTerrain(world, structures, strongholds, work, chunkX, chunkZ);
            population = new DungeonPopulation(world, settings, chunkX, chunkZ);
        }

        private Result prepare(SearchBudget budget) {
            while (lakePhase < 2 && !budget.exhausted()) {
                if (lakePhase == 0) {
                    work.run("population.water", population::generateWaterLake);
                } else {
                    work.run("population.lava", population::generateLavaLake);
                }
                lakePhase++;
            }
            if (lakePhase < 2) {
                return Result.RUNNING;
            }
            populatedChunks++;
            return Result.COMPLETE;
        }

        private Result place(SearchBudget budget) {
            while (population.hasNextDungeon() && !budget.exhausted()) {
                // 只收集这次成功地牢的范围，不混入湖泊或前一次失败写入。
                world.beginPlacement();
                spawner = work.measure("population.dungeon", population::generateNextDungeon);
                dungeonAttempts++;
                if (spawner != null) {
                    return Result.COMPLETE;
                }
            }
            return population.hasNextDungeon() ? Result.RUNNING : Result.REJECTED;
        }

        private SearchTarget collect() {
            SearchTarget written = world.finishPlacement();
            require(written != null && written.bounds.isVecInside(spawner), "dungeon placement bounds missing");
            require(world.getBlockState(spawner).getBlock() == Blocks.MOB_SPAWNER, "dungeon spawner missing");
            require(world.getTileEntity(spawner) instanceof TileEntityMobSpawner, "dungeon tile entity missing");
            return new SearchTarget(spawner, written.bounds);
        }
    }
}
