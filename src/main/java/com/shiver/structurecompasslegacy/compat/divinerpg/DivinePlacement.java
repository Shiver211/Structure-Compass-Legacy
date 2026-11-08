package com.shiver.structurecompasslegacy.compat.divinerpg;

import java.util.Random;

import com.shiver.structurecompasslegacy.api.SearchTarget;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/** 纯种子计算；不创建世界、生成器、模板或结构位置记录。 */
final class DivinePlacement {

    private final long seed;
    private final long seedX;
    private final long seedZ;

    DivinePlacement(long seed) {
        this.seed = seed;
        Random random = new Random(seed);
        seedX = random.nextLong() / 2L * 2L + 1L;
        seedZ = random.nextLong() / 2L * 2L + 1L;
    }

    boolean canSpawn(DivineStructure structure, int chunkX, int chunkZ) {
        // WorldGenUtils 的盐先与 Z 项相加，再与 X 项及世界种子异或。
        long candidateSeed = (chunkX * seedX) ^ (structure.folder.hashCode() + chunkZ * seedZ) ^ seed;
        return new Random(candidateSeed).nextInt(structure.rarity) == 0;
    }

    SearchTarget layout(DivineStructure structure, int chunkX, int chunkZ) {
        // 布局重新播种，不接着消费概率判定的随机源。
        Random random = new Random((chunkX * seedX + chunkZ * seedZ) ^ seed);
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        int height = structure.altitude;
        if (structure.layer == 0) {
            height += random.nextInt(40);
        }

        BlockPos origin = new BlockPos(chunkX * 16, height, chunkZ * 16);
        BlockPos extent = new BlockPos(structure.size.getX(), structure.size.getY() - 1,
                structure.size.getZ()).rotate(rotation);
        // StructureComponentTemplate 的水平最大边界包含 size 本身，而不是 size - 1。
        StructureBoundingBox bounds = new StructureBoundingBox(origin, origin.add(extent));
        BlockPos anchor = new BlockPos(7, 2, Math.min(7, structure.size.getZ() / 2));
        return new SearchTarget(origin.add(anchor.rotate(rotation)), bounds);
    }

    boolean overlapsNeighbour(DivineStructure structure, int chunkX, int chunkZ, StructureBoundingBox bounds) {
        // populate 的写入窗口从区块 +8 开始；仅包围盒不相交仍可能争用同一窗口。
        int minX = Math.floorDiv(bounds.minX - 8, 16) * 16 + 8;
        int minZ = Math.floorDiv(bounds.minZ - 8, 16) * 16 + 8;
        int maxX = Math.floorDiv(bounds.maxX - 8, 16) * 16 + 23;
        int maxZ = Math.floorDiv(bounds.maxZ - 8, 16) * 16 + 23;

        for (DivineStructure neighbour : DivineStructure.values()) {
            if (neighbour.layer != structure.layer) {
                continue;
            }

            int reach = Math.max(neighbour.size.getX(), neighbour.size.getZ());
            int firstX = Math.floorDiv(minX - reach, 16);
            int lastX = Math.floorDiv(maxX + reach, 16);
            int firstZ = Math.floorDiv(minZ - reach, 16);
            int lastZ = Math.floorDiv(maxZ + reach, 16);

            for (int x = firstX; x <= lastX; x++) {
                for (int z = firstZ; z <= lastZ; z++) {
                    if (neighbour == structure && x == chunkX && z == chunkZ) {
                        continue;
                    }
                    if (canSpawn(neighbour, x, z)
                            && layout(neighbour, x, z).bounds.intersectsWith(minX, minZ, maxX, maxZ)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
