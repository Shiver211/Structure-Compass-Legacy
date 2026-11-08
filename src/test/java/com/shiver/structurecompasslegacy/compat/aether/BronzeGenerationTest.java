package com.shiver.structurecompasslegacy.compat.aether;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 固定输入的原生反例；坐标属于受控实心地形，不是可供游戏传送的种子样例。 */
class BronzeGenerationTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 40, 0);

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void nativeReturnValueDoesNotProveADungeonWasPlaced() throws Exception {
        BronzeNativeFixture empty = new BronzeNativeFixture();
        empty.terrain.solid = position -> false;
        assertTrue(empty.generate(ORIGIN, 2L, 0L));
        assertFalse(empty.hasBoss(ORIGIN));
        assertTrue(empty.storage("placementStorage").isEmpty());
        assertTrue(empty.storage("replacementStorage").isEmpty());

        BronzeNativeFixture solid = firstDungeon(0L);
        assertEquals(Blocks.BEDROCK, solid.terrain.getBlockState(ORIGIN).getBlock());
        assertEquals(Blocks.AIR, solid.terrain.getBlockState(ORIGIN.add(8, 2, 8)).getBlock());
        for (String storage : new String[] {"placementStorage", "replacementStorage", "tunnelStorage"}) {
            assertTrue(solid.storage(storage).isEmpty(), "已加载区域在原生放置后释放待放置数据");
        }
    }

    @Test
    void unseededNeighbourLayoutChangesWhetherTheSameCandidateSurvives() throws Exception {
        BronzeNativeFixture firstOrder = firstDungeon(0L);
        BronzeNativeFixture secondOrder = firstDungeon(1L);
        assertNotEquals(firstOrder.terrain.blocks, secondOrder.terrain.blocks);

        // 前一个地下城均成功；只改变它的无种子乱序，后一个候选的输入保持相同。
        BlockPos next = new BlockPos(16, 40, -48);
        assertTrue(firstOrder.generate(next, 2L, 0L));
        assertTrue(secondOrder.generate(next, 2L, 0L));
        assertTrue(firstOrder.hasBoss(next));
        assertFalse(secondOrder.hasBoss(next));

        firstOrder.flush();
        secondOrder.flush();
        assertEquals(Blocks.BEDROCK, firstOrder.terrain.getBlockState(next).getBlock());
        assertNotEquals(Blocks.BEDROCK, secondOrder.terrain.getBlockState(next).getBlock());
    }

    @Test
    void loadedChunksChangeWhetherTheSameCandidateSurvives() throws Exception {
        BronzeNativeFixture wide = new BronzeNativeFixture();
        BronzeNativeFixture narrow = new BronzeNativeFixture();
        // 原版 populate 所需的当前、东、南、东南四区块；西侧和北侧尚未加载。
        narrow.terrain.loaded = position -> position.getX() >= 0 && position.getX() < 32
                && position.getZ() >= 0 && position.getZ() < 32;

        assertTrue(wide.generate(ORIGIN, 21L, 0L));
        assertTrue(narrow.generate(ORIGIN, 21L, 0L));
        assertTrue(wide.hasBoss(ORIGIN));
        assertFalse(narrow.hasBoss(ORIGIN));
        assertTrue(narrow.storage("placementStorage").isEmpty());
        assertTrue(narrow.storage("replacementStorage").isEmpty());
    }

    private static BronzeNativeFixture firstDungeon(long shuffleSeed) throws Exception {
        BronzeNativeFixture nativeCode = new BronzeNativeFixture();
        assertTrue(nativeCode.generate(ORIGIN, 2L, shuffleSeed));
        assertTrue(nativeCode.hasBoss(ORIGIN));
        nativeCode.flush();
        return nativeCode;
    }
}
