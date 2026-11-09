package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.ArrayDeque;
import java.util.BitSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.layer.GenLayer;

/** 从完整空地的原生候选推导导航点，只查询私人群系图层。 */
final class BetweenlandsDungeonRegion {

    private static final int WIDTH = 512;
    private static final int CENTER = WIDTH / 2;
    // 装饰参考点向外最多 16 格，再加原生四向检查的 45 格。
    private static final int MARGIN = 61;

    private BetweenlandsDungeonRegion() {
    }

    static BlockPos find(GenLayer layer, int biomeId, int chunkX, int chunkZ) {
        int referenceX = chunkX * 16 + 16;
        int referenceZ = chunkZ * 16 + 16;
        if (layer.getInts(referenceX, referenceZ, 1, 1)[0] != biomeId) {
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
            for (int next : new int[] {index - 1, index + 1, index - WIDTH, index + WIDTH}) {
                if (area[next] == biomeId && !clearing.get(next)) {
                    clearing.set(next);
                    pending.addLast(next);
                }
            }
        }

        // 参考点必须位于 16 格网格；floorDiv 同时保留负坐标的原生规则。
        int firstX = -Math.floorDiv(-(originX + minX), 16) * 16;
        int firstZ = -Math.floorDiv(-(originZ + minZ), 16) * 16;
        int candidateMinX = Integer.MAX_VALUE, candidateMaxX = Integer.MIN_VALUE;
        int candidateMinZ = Integer.MAX_VALUE, candidateMaxZ = Integer.MIN_VALUE;
        for (int x = firstX; x <= originX + maxX; x += 16) {
            for (int z = firstZ; z <= originZ + maxZ; z += 16) {
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
                    }
                }
            }
        }
        if (candidateMinX == Integer.MAX_VALUE || candidateMaxX - candidateMinX >= 32
                || candidateMaxZ - candidateMinZ >= 32) {
            return null;
        }
        // 候选减 16 为起点；主范围再从 -3 延伸至 +29，即候选的 [-19,+13]。
        // 取所有候选范围的交集中心，Y=125 仅用于区域导航，不代表入口。
        return new BlockPos(Math.floorDiv(candidateMaxX + candidateMinX - 6, 2), 125,
                Math.floorDiv(candidateMaxZ + candidateMinZ - 6, 2));
    }
}
