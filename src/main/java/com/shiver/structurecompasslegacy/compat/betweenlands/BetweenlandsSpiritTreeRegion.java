package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.layer.GenLayer;

/**
 * 复现 generateSubBiomeStructure 的步长 3、32×32 候选和四向 36 格群系检查。
 * 原版随后检查地面、打乱候选并检查树干空间，因此这里只能预测区域。
 */
final class BetweenlandsSpiritTreeRegion {

    private static final int WIDTH = 512;
    private static final int CENTER = WIDTH / 2;
    private static final int CHECK_RANGE = 36;
    private static final int MARGIN = 16 + CHECK_RANGE;
    private static final int TILE_CHUNKS = 8;
    private static final int TILE_WIDTH = TILE_CHUNKS * 16;
    private static final int CANDIDATE_WINDOW_SIZE = 32;
    private static final int CANDIDATE_STEP = 3;
    private static final int MAX_CANDIDATE_SPAN = 64;
    private static final int NAVIGATION_Y = 125;
    private static final int[] NEIGHBORS = {-1, 1, -WIDTH, WIDTH};

    private final GenLayer biomeLayer;
    private final int clearingBiomeId;

    // 缓存仅用于本次搜索；图层数组来自复用池，每个分片只保留 64 个参考点的判定。
    private final Map<Long, BitSet> clearingTiles = boundedCache(256);
    private final Map<Long, Region> cachedRegions = boundedCache(4096);

    BetweenlandsSpiritTreeRegion(GenLayer biomeLayer, int clearingBiomeId) {
        this.biomeLayer = biomeLayer;
        this.clearingBiomeId = clearingBiomeId;
    }

    Region find(int chunkX, int chunkZ) {
        int referenceX = chunkX * 16 + 16;
        int referenceZ = chunkZ * 16 + 16;
        Region previous = cachedRegions.get(ChunkPos.asLong(chunkX, chunkZ));
        if (previous != null) {
            // 缓存命中仍需满足当前参考点的完整枚举窗口。
            return previous.fitsWindow(referenceX, referenceZ) ? previous : null;
        }

        if (!isClearing(chunkX, chunkZ)) {
            return null;
        }

        int originX = referenceX - CENTER;
        int originZ = referenceZ - CENTER;
        int[] biomeIds = biomeLayer.getInts(originX, originZ, WIDTH, WIDTH);
        BitSet clearing = new BitSet(WIDTH * WIDTH);
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        int start = CENTER + CENTER * WIDTH;
        clearing.set(start);
        pending.add(start);

        int minX = CENTER;
        int maxX = CENTER;
        int minZ = CENTER;
        int maxZ = CENTER;

        while (!pending.isEmpty()) {
            int index = pending.removeFirst();
            int x = index % WIDTH;
            int z = index / WIDTH;

            // 无法完整枚举的空地跳过，避免漏掉影响导航点的装饰区块。
            if (x < MARGIN || z < MARGIN || x >= WIDTH - MARGIN || z >= WIDTH - MARGIN) {
                return null;
            }

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);

            for (int offset : NEIGHBORS) {
                int next = index + offset;
                if (biomeIds[next] == clearingBiomeId && !clearing.get(next)) {
                    clearing.set(next);
                    pending.addLast(next);
                }
            }
        }

        // 参考点必须位于 16 格网格；floorDiv 同时保留负坐标的原生规则。
        int firstX = -Math.floorDiv(-(originX + minX), 16) * 16;
        int firstZ = -Math.floorDiv(-(originZ + minZ), 16) * 16;
        List<CandidateArea> candidateAreas = collectCandidates(biomeIds, clearing, originX, originZ, firstX, firstZ,
                originX + maxX, originZ + maxZ);
        Region region = new Region(originX + minX, originX + maxX, originZ + minZ, originZ + maxZ, candidateAreas);

        // 完整空地的阳性和阴性结果均复用；窗口触边的未完成枚举不会写入缓存。
        for (int x = firstX; x <= originX + maxX; x += 16) {
            for (int z = firstZ; z <= originZ + maxZ; z += 16) {
                if (clearing.get(x - originX + (z - originZ) * WIDTH)) {
                    cachedRegions.put(ChunkPos.asLong(Math.floorDiv(x, 16) - 1, Math.floorDiv(z, 16) - 1), region);
                }
            }
        }

        return region;
    }

    private boolean isClearing(int chunkX, int chunkZ) {
        int tileX = Math.floorDiv(chunkX, TILE_CHUNKS);
        int tileZ = Math.floorDiv(chunkZ, TILE_CHUNKS);
        long key = ChunkPos.asLong(tileX, tileZ);
        BitSet points = clearingTiles.get(key);

        if (points == null) {
            int[] biomeIds = biomeLayer.getInts(tileX * TILE_WIDTH + 16, tileZ * TILE_WIDTH + 16,
                    TILE_WIDTH, TILE_WIDTH);
            points = new BitSet(TILE_CHUNKS * TILE_CHUNKS);
            for (int z = 0; z < TILE_CHUNKS; z++) {
                for (int x = 0; x < TILE_CHUNKS; x++) {
                    if (biomeIds[x * 16 + z * 16 * TILE_WIDTH] == clearingBiomeId) {
                        points.set(x + z * TILE_CHUNKS);
                    }
                }
            }
            clearingTiles.put(key, points);
        }

        return points.get(Math.floorMod(chunkX, TILE_CHUNKS)
                + Math.floorMod(chunkZ, TILE_CHUNKS) * TILE_CHUNKS);
    }

    private List<CandidateArea> collectCandidates(int[] biomeIds, BitSet clearing, int originX, int originZ,
            int firstX, int firstZ, int maxX, int maxZ) {
        List<CandidateArea> candidateAreas = new ArrayList<>();
        for (int x = firstX; x <= maxX; x += 16) {
            for (int z = firstZ; z <= maxZ; z += 16) {
                if (!clearing.get(x - originX + (z - originZ) * WIDTH)) {
                    continue;
                }

                CandidateArea candidate = new CandidateArea(Math.floorDiv(x, 16) - 1, Math.floorDiv(z, 16) - 1);
                for (int dx = 0; dx < CANDIDATE_WINDOW_SIZE; dx += CANDIDATE_STEP) {
                    for (int dz = 0; dz < CANDIDATE_WINDOW_SIZE; dz += CANDIDATE_STEP) {
                        int candidateX = x - 16 + dx;
                        int candidateZ = z - 16 + dz;
                        int index = candidateX - originX + (candidateZ - originZ) * WIDTH;

                        // 原生只检查四向群系，不额外要求候选中心也是空地。
                        if (biomeIds[index - CHECK_RANGE] != clearingBiomeId
                                || biomeIds[index + CHECK_RANGE] != clearingBiomeId
                                || biomeIds[index - CHECK_RANGE * WIDTH] != clearingBiomeId
                                || biomeIds[index + CHECK_RANGE * WIDTH] != clearingBiomeId) {
                            continue;
                        }

                        candidate.minX = Math.min(candidate.minX, candidateX);
                        candidate.maxX = Math.max(candidate.maxX, candidateX);
                        candidate.minZ = Math.min(candidate.minZ, candidateZ);
                        candidate.maxZ = Math.max(candidate.maxZ, candidateZ);
                    }
                }

                if (candidate.minX != Integer.MAX_VALUE) {
                    candidateAreas.add(candidate);
                }
            }
        }

        return candidateAreas;
    }

    private static <T> Map<Long, T> boundedCache(int capacity) {
        return new LinkedHashMap<Long, T>(capacity, 0.75F, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, T> eldest) {
                return size() > capacity;
            }
        };
    }

    static final class Region {

        final int minX;
        final int maxX;
        final int minZ;
        final int maxZ;
        final List<CandidateArea> candidateAreas;

        Region(int minX, int maxX, int minZ, int maxZ, List<CandidateArea> candidateAreas) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.candidateAreas = candidateAreas;
        }

        boolean fitsWindow(int referenceX, int referenceZ) {
            return minX >= referenceX - CENTER + MARGIN
                    && maxX < referenceX + CENTER - MARGIN
                    && minZ >= referenceZ - CENTER + MARGIN
                    && maxZ < referenceZ + CENTER - MARGIN;
        }

        boolean hasCandidateInChunk(int chunkX, int chunkZ) {
            for (CandidateArea candidate : candidateAreas) {
                if (candidate.chunk.x == chunkX && candidate.chunk.z == chunkZ) {
                    return true;
                }
            }

            return false;
        }

        boolean containsCandidatePosition(BlockPos position) {
            for (CandidateArea candidate : candidateAreas) {
                if (position.getX() >= candidate.minX
                        && position.getX() <= candidate.maxX
                        && position.getZ() >= candidate.minZ
                        && position.getZ() <= candidate.maxZ) {
                    return true;
                }
            }

            return false;
        }

        BlockPos navigationPoint(Predicate<ChunkPos> includeChunk) {
            int candidateMinX = Integer.MAX_VALUE;
            int candidateMaxX = Integer.MIN_VALUE;
            int candidateMinZ = Integer.MAX_VALUE;
            int candidateMaxZ = Integer.MIN_VALUE;

            for (CandidateArea candidate : candidateAreas) {
                // 只缓存群系几何；已加载区块的状态每次重新读取，不能缓存成永久阴性。
                if (!includeChunk.test(candidate.chunk)) {
                    continue;
                }

                candidateMinX = Math.min(candidateMinX, candidate.minX);
                candidateMaxX = Math.max(candidateMaxX, candidate.maxX);
                candidateMinZ = Math.min(candidateMinZ, candidate.minZ);
                candidateMaxZ = Math.max(candidateMaxZ, candidate.maxZ);

                if (candidateMaxX - candidateMinX >= MAX_CANDIDATE_SPAN
                        || candidateMaxZ - candidateMinZ >= MAX_CANDIDATE_SPAN) {
                    return null;
                }
            }

            // 所有候选允许范围的交集中心；Y 仅为导航参考高度。
            return candidateMinX == Integer.MAX_VALUE
                    ? null
                    : new BlockPos(Math.floorDiv(candidateMaxX + candidateMinX, 2), NAVIGATION_Y,
                            Math.floorDiv(candidateMaxZ + candidateMinZ, 2));
        }
    }

    private static final class CandidateArea {

        final ChunkPos chunk;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        CandidateArea(int chunkX, int chunkZ) {
            chunk = new ChunkPos(chunkX, chunkZ);
        }
    }
}
