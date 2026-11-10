package com.shiver.structurecompasslegacy.search;

import java.util.Comparator;
import java.util.PriorityQueue;

/** 按区块中心到起点的水平距离合并各行；内存只随搜索半径线性增长。 */
public final class ChunkDistanceOrder {

    private final int originX;
    private final int originZ;
    private final int minX;
    private final int maxX;
    private final PriorityQueue<Row> rows = new PriorityQueue<>(Comparator
            .comparingLong((Row row) -> row.distance)
            .thenComparingInt(row -> row.x)
            .thenComparingInt(row -> row.z));
    private final long total;
    private long visited;

    public ChunkDistanceOrder(int originX, int originZ, int radius) {
        this.originX = originX;
        this.originZ = originZ;
        minX = -Math.floorDiv(-(originX - radius - 8), 16);
        maxX = Math.floorDiv(originX + radius - 8, 16);
        int minZ = -Math.floorDiv(-(originZ - radius - 8), 16);
        int maxZ = Math.floorDiv(originZ + radius - 8, 16);
        int startX = Math.max(minX, Math.min(maxX, (originX - 1) >> 4));
        for (int z = minZ; z <= maxZ; z++) {
            rows.add(new Row(startX, z));
        }
        total = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public boolean hasNext() { return !rows.isEmpty(); }
    public int x() { return rows.element().x; }
    public int z() { return rows.element().z; }
    public long visited() { return visited; }
    public long total() { return total; }

    public void next() {
        Row row = rows.remove();
        visited++;
        if (row.left < minX && row.right > maxX) {
            return;
        }
        if (row.right > maxX || row.left >= minX
                && distance(row.left, row.z) <= distance(row.right, row.z)) {
            row.x = row.left--;
        } else {
            row.x = row.right++;
        }
        row.distance = distance(row.x, row.z);
        rows.add(row);
    }

    private long distance(int x, int z) {
        long dx = (long) x * 16 + 8 - originX;
        long dz = (long) z * 16 + 8 - originZ;
        return dx * dx + dz * dz;
    }

    private final class Row {
        private int x;
        private final int z;
        private int left;
        private int right;
        private long distance;

        private Row(int x, int z) {
            this.x = x;
            this.z = z;
            left = x - 1;
            right = x + 1;
            distance = distance(x, z);
        }
    }
}
