package com.shiver.structurecompasslegacy.compat.vanilla;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;

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

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.structure.MapGenStronghold;

/** 按原版环状递增步骤定位要塞，避免在单次调用内计算全部 128 个坐标导致触发 slow_call。 */
final class StrongholdTask implements SearchTask {

    // BiomeProvider.findBiomePosition(radius=112) 的最大偏移约 197 格，保留充足余量做环级提前剪枝。
    private static final double RING_SHIFT_MARGIN = 512.0D;

    private final SearchContext context;
    private final double distance;
    private final int count;
    private final double originFromWorld;
    private final Random random;

    private int spread;
    private double angle;
    private int ring;
    private int indexInRing;
    private int checked;

    private SimulationSession session;
    private List<Biome> allowedBiomes;
    private BlockPos bestPos;
    private long bestDistanceSq = Long.MAX_VALUE;
    private long longestCallNanos;

    static StructureLocator locator() {
        return new StructureLocator() {

            @Override
            public SearchTask createTask(SearchContext context) {
                return new StrongholdTask(context);
            }

            @Override
            public String unavailableReason(WorldServer world) {
                if (!GeneratorFactories.supportsBiomes(world)) {
                    return "structurecompasslegacy.reason.generator_not_adapted";
                }
                if (!isGenerationEnabled(world)) {
                    return "structurecompasslegacy.reason.generation_disabled";
                }
                return "";
            }
        };
    }

    private static boolean isGenerationEnabled(WorldServer world) {
        if (!world.getWorldInfo().isMapFeaturesEnabled()) {
            return false;
        }
        if (world.getWorldType() == WorldType.FLAT && !VanillaModule.hasFlatFeature(world, "stronghold")) {
            return false;
        }
        if (world.getWorldType() == WorldType.CUSTOMIZED) {
            String generatorOptions = world.getWorldInfo().getGeneratorOptions();
            ChunkGeneratorSettings settings = ChunkGeneratorSettings.Factory
                    .jsonToFactory(generatorOptions == null ? "" : generatorOptions)
                    .build();
            if (!settings.useStrongholds) {
                return false;
            }
        }
        return true;
    }

    StrongholdTask(SearchContext context) {
        this.context = context;
        Map<String, String> options = context.source.getWorldType() == WorldType.FLAT
                ? VanillaModule.flatOptions(context.source, "stronghold")
                : Collections.emptyMap();

        double parsedDistance = 32.0D;
        int parsedCount = 128;
        int parsedSpread = 3;
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            if ("distance".equals(key)) {
                parsedDistance = MathHelper.getDouble(entry.getValue(), parsedDistance, 1.0D);
            } else if ("count".equals(key)) {
                parsedCount = MathHelper.getInt(entry.getValue(), parsedCount, 1);
            } else if ("spread".equals(key)) {
                parsedSpread = MathHelper.getInt(entry.getValue(), parsedSpread, 1);
            }
        }

        this.distance = parsedDistance;
        this.count = parsedCount;
        this.spread = parsedSpread;
        this.originFromWorld = Math.hypot(context.origin.getX(), context.origin.getZ());

        this.random = new Random();
        this.random.setSeed(context.seed);
        this.angle = this.random.nextDouble() * Math.PI * 2.0D;
    }

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        if (session == null) {
            long before = System.nanoTime();
            long elapsed;
            try (SimulationScope ignored = SimulationScope.enter()) {
                session = SimulationSession.forBiomes(context.source);
                allowedBiomes = new FastBiomeList(new MapGenStronghold().allowedBiomes);
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                elapsed = System.nanoTime() - before;
                longestCallNanos = Math.max(longestCallNanos, elapsed);
            }
            if (elapsed > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
            if (budget.exhausted()) {
                return SearchStep.running();
            }
        }

        while (checked < count && !budget.exhausted()) {
            if (beyondRemainingRings()) {
                return finishResult();
            }

            long before = System.nanoTime();
            long elapsed;
            BlockPos candidate;
            try (SimulationScope ignored = SimulationScope.enter()) {
                candidate = stepNextStronghold();
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                elapsed = System.nanoTime() - before;
                longestCallNanos = Math.max(longestCallNanos, elapsed);
            }

            if (context.contains(candidate)) {
                long deltaX = (long) candidate.getX() - context.origin.getX();
                long deltaZ = (long) candidate.getZ() - context.origin.getZ();
                long distanceSq = deltaX * deltaX + deltaZ * deltaZ;
                if (bestPos == null || distanceSq < bestDistanceSq) {
                    bestPos = candidate;
                    bestDistanceSq = distanceSq;
                }
            }

            if (elapsed > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }

        if (checked >= count || beyondRemainingRings()) {
            return finishResult();
        }
        return SearchStep.running();
    }

    private BlockPos stepNextStronghold() {
        double radialChunks = 4.0D * distance
                + distance * (double) ring * 6.0D
                + (random.nextDouble() - 0.5D) * distance * 2.5D;
        int chunkX = (int) Math.round(Math.cos(angle) * radialChunks);
        int chunkZ = (int) Math.round(Math.sin(angle) * radialChunks);
        BlockPos biomePos = session.world().getBiomeProvider().findBiomePosition(
                (chunkX << 4) + 8, (chunkZ << 4) + 8, 112, allowedBiomes, random);

        if (biomePos != null) {
            chunkX = biomePos.getX() >> 4;
            chunkZ = biomePos.getZ() >> 4;
        }

        int currentIndex = checked;
        checked++;

        angle += (Math.PI * 2D) / (double) spread;
        indexInRing++;

        if (indexInRing == spread) {
            ring++;
            indexInRing = 0;
            spread += 2 * spread / (ring + 1);
            spread = Math.min(spread, count - currentIndex);
            angle += random.nextDouble() * Math.PI * 2.0D;
        }

        return new BlockPos((chunkX << 4) + 8, 64, (chunkZ << 4) + 8);
    }

    private boolean beyondRemainingRings() {
        double minFromOrigin = (2.75D + 6.0D * ring) * distance * 16.0D - RING_SHIFT_MARGIN;
        double maxRelevantDistance = bestPos != null ? Math.sqrt((double) bestDistanceSq) : context.radius;
        return minFromOrigin - originFromWorld > maxRelevantDistance;
    }

    private SearchStep finishResult() {
        return bestPos != null ? SearchStep.found(new SearchTarget(bestPos)) : SearchStep.exhausted();
    }

    @Override
    public long checkedCandidates() {
        return checked;
    }

    @Override
    public long totalCandidates() {
        return count;
    }

    @Override
    public int generatedChunks() {
        return session == null ? 0 : session.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return session == null ? 0 : session.peakCachedChunks();
    }

    @Override
    public long longestCallNanos() {
        return longestCallNanos;
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
        allowedBiomes = null;
    }

    private static final class FastBiomeList extends AbstractList<Biome> implements RandomAccess {

        private final List<Biome> delegate;
        private final Set<Biome> lookup;

        private FastBiomeList(List<Biome> delegate) {
            this.delegate = delegate;
            this.lookup = new HashSet<>(delegate);
        }

        @Override
        public Biome get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object element) {
            return lookup.contains(element);
        }

        @Override
        public boolean containsAll(Collection<?> elements) {
            return lookup.containsAll(elements);
        }
    }
}
