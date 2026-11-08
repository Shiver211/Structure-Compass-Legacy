package com.shiver.structurecompasslegacy.search;

/** 逐圈枚举区块，常量内存；负坐标与正坐标采用同一顺序。 */
public final class ChunkSpiral {

    private final int centerX;
    private final int centerZ;
    private final long total;

    private int x;
    private int z;
    private int dx;
    private int dz = -1;
    private long visited;

    public ChunkSpiral(int centerX, int centerZ, int radius) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.total = (2L * radius + 1) * (2L * radius + 1);
    }

    public boolean hasNext() {
        return visited < total;
    }

    public int x() {
        return centerX + x;
    }

    public int z() {
        return centerZ + z;
    }

    public long visited() {
        return visited;
    }

    public long total() {
        return total;
    }

    public void next() {
        if (!hasNext()) {
            throw new IllegalStateException("Spiral exhausted");
        }

        if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
            int previousDx = dx;
            dx = -dz;
            dz = previousDx;
        }

        x += dx;
        z += dz;
        visited++;
    }
}
