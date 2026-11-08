package com.shiver.structurecompasslegacy.verification;

import java.nio.file.Files;
import java.nio.file.Path;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;

/** 正常 WorldServer 自然生成并落盘，再用真实 Anvil 加载器重新核对目标。 */
final class ControlledWorldCheck {

    private ControlledWorldCheck() {
    }

    static void verify() throws Exception {
        Path root = Files.createTempDirectory(new java.io.File(".").toPath(), "controlled-world-");
        DataFixer fixer = new DataFixer(1343);
        ControlledServer server = new ControlledServer(root.toFile(), fixer);
        SaveHandler save = new AnvilSaveHandler(root.toFile(), "world", false, fixer);
        WorldInfo info = new WorldInfo(new WorldSettings(0, GameType.SURVIVAL, false, false, WorldType.DEFAULT), "controlled");
        WorldServer world = (WorldServer) new WorldServer(server, save, info, 0, new Profiler()).init();
        int threshold = CompassConfig.slowCallMillis;
        try {
            CompassConfig.slowCallMillis = 10_000;
            BlockPos target = find(world);
            require(target.equals(new BlockPos(-3854, 25, 18)), "controlled simulation target moved");
            require(world.getChunkProvider().getLoadedChunkCount() == 0, "simulation loaded real chunks");
            require(!Files.exists(root.resolve("world/region")), "simulation saved real chunks");

            // 正常区块提供器完成地形与相邻区块 populate，不调用模拟的试生成入口。
            for (int x = -244; x <= -238; x++) {
                for (int z = -2; z <= 4; z++) {
                    world.getChunk(x, z);
                }
            }
            require(world.getBlockState(target).getBlock() == Blocks.MOB_SPAWNER, "real world spawner missing");
            require(world.getTileEntity(target) instanceof TileEntityMobSpawner, "real world tile entity missing");
            world.saveAllChunks(true, null);
            world.flushToDisk();
            save.flush();

            AnvilChunkLoader loader = new AnvilChunkLoader(root.resolve("world").toFile(), fixer);
            Chunk reloaded = loader.loadChunk(world, target.getX() >> 4, target.getZ() >> 4);
            require(reloaded != null && reloaded.isTerrainPopulated(), "saved target chunk missing or unpopulated");
            require(reloaded.getBlockState(target).getBlock() == Blocks.MOB_SPAWNER, "saved spawner missing");
            require(reloaded.getTileEntityMap().get(target) instanceof TileEntityMobSpawner, "saved spawner NBT missing");
            System.out.println("CONTROLLED_WORLD_PASSED: seed=0, spawner=(-3854,25,18), save=" + root.resolve("world"));
        } finally {
            CompassConfig.slowCallMillis = threshold;
            DimensionManager.setWorld(0, null, server);
        }
    }

    private static BlockPos find(WorldServer source) throws Exception {
        SearchContext context = new SearchContext(source, new BlockPos(-242 * 16, 70, 0));
        try (NaturalDungeonTask task = new NaturalDungeonTask(context)) {
            for (int tick = 0; tick < 4000; tick++) {
                SearchStep step = task.advance(new SearchBudget(System.nanoTime(), 2_000_000));
                if (step.state == SearchStep.State.FOUND) {
                    return step.target.position;
                }
                require(step.state == SearchStep.State.RUNNING, "controlled simulation ended: " + step.state);
            }
        }
        throw new AssertionError("Controlled simulation did not finish");
    }
}
