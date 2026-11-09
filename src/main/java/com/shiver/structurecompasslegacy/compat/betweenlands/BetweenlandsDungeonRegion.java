package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.layer.GenLayer;

/** 从完整空地的原生候选推导导航点，只查询私人群系图层。 */
final class BetweenlandsDungeonRegion {

    private static final int WIDTH = 512;
    private static final int CENTER = WIDTH / 2;
    // 装饰参考点向外最多 16 格，再加原生四向检查的 45 格。
    private static final int MARGIN = 61;
    private static final int TILE_CHUNKS = 8;
    private static final int TILE_WIDTH = TILE_CHUNKS * 16;
    private static final int[] NEIGHBORS = {-1, 1, -WIDTH, WIDTH};

    private final GenLayer layer;
    private final int biomeId;
    // 仅限本次搜索；图层数组来自复用池，只保留复制出的64个参考点。
    private final Map<Long, BitSet> tiles = cache(256);
    private final Map<Long, Region> regions = cache(4096);

    BetweenlandsDungeonRegion(GenLayer layer, int biomeId) {
        this.layer = layer;
        this.biomeId = biomeId;
    }

    BlockPos find(int chunkX, int chunkZ) {
        int referenceX = chunkX * 16 + 16;
        int referenceZ = chunkZ * 16 + 16;
        Region previous = regions.get(ChunkPos.asLong(chunkX, chunkZ));
        if (previous != null) {
            // 保留旧算法的窗口限制，不能因另一参考点已枚举完整而扩大命中范围。
            return previous.inside(referenceX, referenceZ) ? previous.position : null;
        }
        if (!isClearing(chunkX, chunkZ)) {
            return null;
        }

        int originX = referenceX - CENTER;
        int originZ = referenceZ - CENTER;
        int[] area = layer.getInts(originX, originZ, WIDTH, WIDTH);
        BitSet clearing = new BitSet(WIDTH * WIDTH);
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        int start = CENTER + CENTER * WIDTH;
        clearing.set(start);
        pending.add(start);
        int minX = CENTER, maxX = CENTER, minZ = CENTER, maxZ = CENTER;

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
                if (area[next] == biomeId && !clearing.get(next)) {
                    clearing.set(next);
                    pending.addLast(next);
                }
            }
        }

        // 参考点必须位于 16 格网格；floorDiv 同时保留负坐标的原生规则。
        int firstX = -Math.floorDiv(-(originX + minX), 16) * 16;
        int firstZ = -Math.floorDiv(-(originZ + minZ), 16) * 16;
        BlockPos position = locate(area, clearing, originX, originZ, firstX, firstZ,
                originX + maxX, originZ + maxZ, biomeId);
        Region region = new Region(originX + minX, originX + maxX, originZ + minZ, originZ + maxZ, position);
        // 完整空地的阳性和阴性结果均复用；窗口触边的未完成枚举不会写入缓存。
        for (int x = firstX; x <= originX + maxX; x += 16) {
            for (int z = firstZ; z <= originZ + maxZ; z += 16) {
                if (clearing.get(x - originX + (z - originZ) * WIDTH)) {
                    regions.put(ChunkPos.asLong(Math.floorDiv(x, 16) - 1, Math.floorDiv(z, 16) - 1), region);
                }
            }
        }
        return position;
    }

    private boolean isClearing(int chunkX, int chunkZ) {
        int tileX = Math.floorDiv(chunkX, TILE_CHUNKS);
        int tileZ = Math.floorDiv(chunkZ, TILE_CHUNKS);
        long key = ChunkPos.asLong(tileX, tileZ);
        BitSet points = tiles.get(key);
        if (points == null) {
            int[] area = layer.getInts(tileX * TILE_WIDTH + 16, tileZ * TILE_WIDTH + 16,
                    TILE_WIDTH, TILE_WIDTH);
            points = new BitSet(TILE_CHUNKS * TILE_CHUNKS);
            for (int z = 0; z < TILE_CHUNKS; z++) {
                for (int x = 0; x < TILE_CHUNKS; x++) {
                    if (area[x * 16 + z * 16 * TILE_WIDTH] == biomeId) {
                        points.set(x + z * TILE_CHUNKS);
                    }
                }
            }
            tiles.put(key, points);
        }
        return points.get(Math.floorMod(chunkX, TILE_CHUNKS)
                + Math.floorMod(chunkZ, TILE_CHUNKS) * TILE_CHUNKS);
    }

    private static BlockPos locate(int[] area, BitSet clearing, int originX, int originZ,
            int firstX, int firstZ, int maxX, int maxZ, int biomeId) {
        int candidateMinX = Integer.MAX_VALUE, candidateMaxX = Integer.MIN_VALUE;
        int candidateMinZ = Integer.MAX_VALUE, candidateMaxZ = Integer.MIN_VALUE;
        for (int x = firstX; x <= maxX; x += 16) {
            for (int z = firstZ; z <= maxZ; z += 16) {
                if (!clearing.get(x - originX + (z - originZ) * WIDTH)) {
                    continue;
                }
                for (int dx = 0; dx < 32; dx += 3) {
                    for (int dz = 0; dz < 32; dz += 3) {
                        int candidateX = x - 16 + dx;
                        int candidateZ = z - 16 + dz;
                        int index = candidateX - originX + (candidateZ - originZ) * WIDTH;
                        // 原生只检查四向群系，不额外要求候选中心也是空地。
                        if (area[index - 45] != biomeId || area[index + 45] != biomeId
                                || area[index - 45 * WIDTH] != biomeId || area[index + 45 * WIDTH] != biomeId) {
                            continue;
                        }
                        candidateMinX = Math.min(candidateMinX, candidateX);
                        candidateMaxX = Math.max(candidateMaxX, candidateX);
                        candidateMinZ = Math.min(candidateMinZ, candidateZ);
                        candidateMaxZ = Math.max(candidateMaxZ, candidateZ);
                        // 新候选只会缩小交集；跨度达到32后无需继续枚举。
                        if (candidateMaxX - candidateMinX >= 32 || candidateMaxZ - candidateMinZ >= 32) {
                            return null;
                        }
                    }
                }
            }
        }
        if (candidateMinX == Integer.MAX_VALUE) {
            return null;
        }
        // 候选减 16 为起点；主范围再从 -3 延伸至 +29，即候选的 [-19,+13]。
        // 取所有候选范围的交集中心，Y=125 仅用于区域导航，不代表入口。
        return new BlockPos(Math.floorDiv(candidateMaxX + candidateMinX - 6, 2), 125,
                Math.floorDiv(candidateMaxZ + candidateMinZ - 6, 2));
    }

    private static <T> Map<Long, T> cache(int capacity) {
        return new LinkedHashMap<Long, T>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, T> eldest) {
                return size() > capacity;
            }
        };
    }

    private static final class Region {
        final int minX, maxX, minZ, maxZ;
        final BlockPos position;

        Region(int minX, int maxX, int minZ, int maxZ, BlockPos position) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.position = position;
        }

        boolean inside(int referenceX, int referenceZ) {
            return minX >= referenceX - CENTER + MARGIN && maxX < referenceX + CENTER - MARGIN
                    && minZ >= referenceZ - CENTER + MARGIN && maxZ < referenceZ + CENTER - MARGIN;
        }
    }
}
