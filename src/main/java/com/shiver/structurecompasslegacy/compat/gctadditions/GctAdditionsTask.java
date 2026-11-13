package com.shiver.structurecompasslegacy.compat.gctadditions;

import java.util.Comparator;
import java.util.PriorityQueue;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.search.SearchLimitException;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

final class GctAdditionsTask implements SearchTask {

    private final SearchContext context;
    private final GctAdditionsStructure structure;
    private final Biome requiredBiome;
    private final RegionOrder regions;
    private final long radiusSq;

    private SimulationSession simulation;
    private SearchTarget best;
    private long bestDistSq = Long.MAX_VALUE;
    private int generated;
    private int peakCached;
    private long longestCallNanos;

    static StructureLocator locator(GctAdditionsStructure structure, Biome requiredBiome) {
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                return new GctAdditionsTask(context, structure, requiredBiome);
            }

            @Override
            public String unavailableReason(WorldServer world) {
                return GeneratorFactories.supports(world)
                        ? "" : "structurecompasslegacy.reason.generator_not_adapted";
            }
        };
    }

    private GctAdditionsTask(SearchContext context, GctAdditionsStructure structure, Biome requiredBiome) {
        this.context = context;
        this.structure = structure;
        this.requiredBiome = requiredBiome;
        this.radiusSq = (long) context.radius * context.radius;
        this.regions = new RegionOrder(context.origin.getX(), context.origin.getZ(), context.radius, structure);
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        while (regions.hasNext() && !budget.exhausted()) {
            if (best != null && regions.minNextDistanceSq() >= bestDistSq) {
                return SearchStep.found(best);
            }

            long before = System.nanoTime();
            long elapsed;
            try {
                inspectNextRegion();
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                elapsed = System.nanoTime() - before;
                longestCallNanos = Math.max(longestCallNanos, elapsed);
            }

            if (elapsed > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }

        if (!regions.hasNext()) {
            return best != null ? SearchStep.found(best) : SearchStep.exhausted();
        }
        return SearchStep.running();
    }

    private void inspectNextRegion() {
        int rx = regions.rx();
        int rz = regions.rz();
        regions.next();

        GctAdditionsStructure.Candidate candidate = structure.candidate(context.seed, rx, rz);
        long dx = (long) candidate.targetX - context.origin.getX();
        long dz = (long) candidate.targetZ - context.origin.getZ();
        long distSq = dx * dx + dz * dz;
        if (distSq > radiusSq || distSq >= bestDistSq) {
            return;
        }

        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
            }

            if (requiredBiome != null) {
                Biome biome = simulation.world().getBiomeProvider()
                        .getBiome(new BlockPos(candidate.originX, 0, candidate.originZ));
                if (biome != requiredBiome) {
                    return;
                }
            }

            simulation.world().beginCandidate();
            Chunk rawChunk = simulation.world().getChunk(candidate.originX >> 4, candidate.originZ >> 4);
            int surfaceY = findChunkSurfaceY(rawChunk, candidate.originX, candidate.originZ);
            simulation.world().clearCandidate();
            if (surfaceY < 0) {
                return;
            }

            int originY = surfaceY + structure.yOffset;
            if (originY < 0 || originY + structure.maxRelY > 255) {
                return;
            }

            bestDistSq = distSq;
            BlockPos targetPos = new BlockPos(
                    candidate.targetX,
                    originY + structure.targetOffsetY,
                    candidate.targetZ);
            StructureBoundingBox bounds = new StructureBoundingBox(
                    candidate.originX,
                    originY,
                    candidate.originZ,
                    candidate.originX + structure.sizeX - 1,
                    originY + structure.maxRelY,
                    candidate.originZ + structure.sizeZ - 1);
            best = new SearchTarget(targetPos, bounds);
        }
    }

    private static int findChunkSurfaceY(Chunk chunk, int worldX, int worldZ) {
        int localX = worldX & 15;
        int localZ = worldZ & 15;
        int topY = chunk.getHeightValue(localX, localZ);
        if (topY <= 0) {
            return -1;
        }
        int y = Math.min(topY, 255);
        while (y > 0 && !isSolidState(chunk.getBlockState(localX, y, localZ))) {
            y--;
        }
        return isSolidState(chunk.getBlockState(localX, y, localZ)) ? y - 1 : -1;
    }

    private static boolean isSolidState(IBlockState state) {
        return state.getBlock() != Blocks.AIR && state.getMaterial().blocksMovement();
    }

    @Override
    public long checkedCandidates() {
        return regions.visited();
    }

    @Override
    public long totalCandidates() {
        return regions.total();
    }

    @Override
    public int generatedChunks() {
        return generated + (simulation == null ? 0 : simulation.generatedChunks());
    }

    @Override
    public int peakCachedChunks() {
        return Math.max(peakCached, simulation == null ? 0 : simulation.peakCachedChunks());
    }

    @Override
    public long longestCallNanos() {
        return longestCallNanos;
    }

    @Override
    public void close() {
        if (simulation != null) {
            generated += simulation.generatedChunks();
            peakCached = Math.max(peakCached, simulation.peakCachedChunks());
            simulation.close();
            simulation = null;
        }
    }

    private static final class RegionOrder {

        private final int originX;
        private final int originZ;
        private final GctAdditionsStructure structure;
        private final int minRx;
        private final int maxRx;
        private final PriorityQueue<Row> rows = new PriorityQueue<>(Comparator
                .comparingLong((Row row) -> row.distance)
                .thenComparingInt(row -> row.rx)
                .thenComparingInt(row -> row.rz));
        private final long total;
        private long visited;

        private RegionOrder(int originX, int originZ, int radius, GctAdditionsStructure structure) {
            this.originX = originX;
            this.originZ = originZ;
            this.structure = structure;

            int regionBlocks = structure.spacing * 16;
            int bound = structure.spacing - structure.separation;
            int spanX = (bound - 1) * 16 + structure.originRangeX;
            int spanZ = (bound - 1) * 16 + structure.originRangeZ;

            minRx = -Math.floorDiv(-(originX - radius - spanX - structure.targetOffsetX), regionBlocks);
            maxRx = Math.floorDiv(originX + radius - 1 - structure.targetOffsetX, regionBlocks);
            int minRz = -Math.floorDiv(-(originZ - radius - spanZ - structure.targetOffsetZ), regionBlocks);
            int maxRz = Math.floorDiv(originZ + radius - 1 - structure.targetOffsetZ, regionBlocks);

            if (minRx <= maxRx && minRz <= maxRz) {
                int startRx = Math.max(minRx, Math.min(maxRx, Math.floorDiv(originX, regionBlocks)));
                for (int rz = minRz; rz <= maxRz; rz++) {
                    rows.add(new Row(startRx, rz));
                }
                total = (long) (maxRx - minRx + 1) * (maxRz - minRz + 1);
            } else {
                total = 0;
            }
        }

        private boolean hasNext() {
            return !rows.isEmpty();
        }

        private long minNextDistanceSq() {
            return rows.element().distance;
        }

        private int rx() {
            return rows.element().rx;
        }

        private int rz() {
            return rows.element().rz;
        }

        private long visited() {
            return visited;
        }

        private long total() {
            return total;
        }

        private void next() {
            Row row = rows.remove();
            visited++;
            if (row.left < minRx && row.right > maxRx) {
                return;
            }
            if (row.right > maxRx || row.left >= minRx
                    && minDistSq(row.left, row.rz) <= minDistSq(row.right, row.rz)) {
                row.rx = row.left--;
            } else {
                row.rx = row.right++;
            }
            row.distance = minDistSq(row.rx, row.rz);
            rows.add(row);
        }

        private long minDistSq(int rx, int rz) {
            long dx = clampDelta(originX, structure.minTargetX(rx), structure.maxTargetX(rx));
            long dz = clampDelta(originZ, structure.minTargetZ(rz), structure.maxTargetZ(rz));
            return dx * dx + dz * dz;
        }

        private static long clampDelta(int value, int min, int max) {
            if (value < min) {
                return (long) min - value;
            }
            if (value > max) {
                return (long) value - max;
            }
            return 0;
        }

        private final class Row {

            private int rx;
            private final int rz;
            private int left;
            private int right;
            private long distance;

            private Row(int rx, int rz) {
                this.rx = rx;
                this.rz = rz;
                this.left = rx - 1;
                this.right = rx + 1;
                this.distance = minDistSq(rx, rz);
            }
        }
    }
}
