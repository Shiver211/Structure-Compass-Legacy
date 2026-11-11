package com.shiver.structurecompasslegacy.compat.betweenlands;

import com.shiver.structurecompasslegacy.simulation.SimulationSession;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/** 在四种邻区块先装饰的条件下核验候选，至少三种仍产生相同坐标才保留。 */
final class BetweenlandsFortressCheck {

    private static final int WINDOW_SIZE = 4;
    private static final int REQUIRED_MATCHES = 3;
    // 每组依次装饰斜邻区块、X 轴邻区块、Z 轴邻区块，再核验目标区块。
    private static final int[][][] ORDERS = {
            {{-1, -1}, {-1, 0}, {0, -1}, {0, 0}},
            {{-1, 1}, {-1, 0}, {0, 1}, {0, 0}},
            {{1, -1}, {1, 0}, {0, -1}, {0, 0}},
            {{1, 1}, {1, 0}, {0, 1}, {0, 0}}
    };

    private final SimulationSession simulation;
    private final BetweenlandsTerrain terrain;
    private final BetweenlandsPopulation population;
    private final BlockPos expected;
    private final int chunkX;
    private final int chunkZ;
    private final int marsh0;
    private final int marsh1;

    private int direction;
    private int preparedChunks;
    private int neighbor;
    private int matches;
    private boolean started;

    BetweenlandsFortressCheck(SimulationSession simulation, BetweenlandsTerrain terrain,
            BetweenlandsPopulation population, BlockPos expected, int chunkX, int chunkZ, int marsh0, int marsh1) {
        this.simulation = simulation;
        this.terrain = terrain;
        this.population = population;
        this.expected = expected;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.marsh0 = marsh0;
        this.marsh1 = marsh1;
    }

    /** 与候选前置生成共用 tick 预算，每次只生成一个区块或执行一次装饰尝试。 */
    boolean step() {
        if (matches >= REQUIRED_MATCHES || matches + ORDERS.length - direction < REQUIRED_MATCHES) {
            return true;
        }
        if (preparedChunks == 0) {
            simulation.beginCandidate();
            terrain.beginCandidate();
            BetweenlandsStorage.attach(terrain.world);
        }
        if (preparedChunks < WINDOW_SIZE * WINDOW_SIZE) {
            terrain.world.getChunk(chunkX + preparedChunks / WINDOW_SIZE - 1,
                    chunkZ + preparedChunks % WINDOW_SIZE - 1);
            preparedChunks++;
            return false;
        }

        int[] offset = ORDERS[direction][neighbor];
        int x = chunkX + offset[0];
        int z = chunkZ + offset[1];
        if (!started) {
            int biome = Biome.getIdForBiome(
                    terrain.world.getBiome(new BlockPos(x * 16 + 16, 0, z * 16 + 16)));
            // 仅复演已核对的泥沼装饰器；其他群系的影响视为候选不稳定。
            if (biome != marsh0 && biome != marsh1) {
                nextDirection(false);
                return false;
            }
            population.begin(terrain, x, z, neighbor < 3);
            started = true;
            return false;
        }
        boolean done = population.step();
        if (neighbor < 3 && terrain.fortress != null) {
            // 邻区块也可能生成要塞，其本体会改变后续流程；拒绝此条件，不执行本体。
            nextDirection(false);
        } else if (done) {
            if (neighbor == 3) {
                nextDirection(expected.equals(terrain.fortress));
            } else {
                terrain.world.getChunk(x, z).setTerrainPopulated(true);
                neighbor++;
                started = false;
            }
        }
        return false;
    }

    BlockPos result() {
        return matches >= REQUIRED_MATCHES ? expected : null;
    }

    private void nextDirection(boolean matched) {
        if (matched) {
            matches++;
        }
        direction++;
        preparedChunks = 0;
        neighbor = 0;
        started = false;
    }
}
