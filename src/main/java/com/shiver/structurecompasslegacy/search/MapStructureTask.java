package com.shiver.structurecompasslegacy.search;

import java.util.function.Function;
import java.util.function.Predicate;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.mixin.MapGenBaseAccessor;
import com.shiver.structurecompasslegacy.mixin.MapGenStructureInvoker;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

/** 只调用新建生成器的候选判定与布局构造，不读取真实结构记录或调用 locate。 */
public final class MapStructureTask extends ChunkSearchTask {

    private final Function<SimulationWorld, MapGenStructure> factory;
    private final Predicate<StructureComponent> componentFilter;
    private final boolean validateLayout;

    private SimulationSession simulation;
    private MapGenStructure generator;

    public static StructureLocator locator(
            Function<SimulationWorld, MapGenStructure> factory,
            Predicate<StructureComponent> componentFilter,
            boolean validateLayout) {
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                return new MapStructureTask(context, factory, componentFilter, validateLayout);
            }

            @Override
            public String unavailableReason(WorldServer world) {
                return GeneratorFactories.supports(world)
                        ? "" : "structurecompasslegacy.reason.generator_not_adapted";
            }
        };
    }

    public MapStructureTask(
            SearchContext context,
            Function<SimulationWorld, MapGenStructure> factory,
            Predicate<StructureComponent> componentFilter,
            boolean validateLayout) {
        super(context);
        this.factory = factory;
        this.componentFilter = componentFilter;
        this.validateLayout = validateLayout;
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
                generator = factory.apply(simulation.world());
                ((MapGenBaseAccessor) generator).compassSetWorld(simulation.world());
            }

            ((MapGenBaseAccessor) generator).compassSetRandom(SeedMath.mapGen(context.seed, chunkX, chunkZ));
            MapGenStructureInvoker access = (MapGenStructureInvoker) generator;
            if (!access.compassCanSpawn(chunkX, chunkZ)) {
                return null;
            }

            if (!validateLayout) {
                return new SearchTarget(new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8));
            }

            simulation.beginCandidate();
            StructureStart start = access.compassCreateStart(chunkX, chunkZ);
            if (start == null || !start.isSizeableStructure()) {
                return null;
            }

            if (componentFilter == null) {
                return target(start.getBoundingBox());
            }

            for (StructureComponent component : start.getComponents()) {
                if (componentFilter.test(component)) {
                    return target(component.getBoundingBox());
                }
            }

            return null;
        }
    }

    private SearchTarget target(StructureBoundingBox bounds) {
        BlockPos center = new BlockPos(
                (bounds.minX + bounds.maxX) / 2,
                (bounds.minY + bounds.maxY) / 2,
                (bounds.minZ + bounds.maxZ) / 2);
        return new SearchTarget(center, bounds);
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
        if (simulation != null) {
            simulation.close();
            simulation = null;
        }
        generator = null;
    }
}
