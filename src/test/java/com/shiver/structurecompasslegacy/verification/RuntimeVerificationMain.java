package com.shiver.structurecompasslegacy.verification;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import com.shiver.structurecompasslegacy.compat.netherapi.NetherInfoRuntimeCheck;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IFMLSidedHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.LoaderState.ModState;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.util.datafix.DataFixer;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 宿主服务使用测试替身，模拟世界、区块、Mixin 和生成入口执行真实实现。 */
public final class RuntimeVerificationMain {

    private RuntimeVerificationMain() {
    }

    public static void main(String[] arguments) throws Exception {
        Bootstrap.register();
        set(Loader.class, Loader.instance(), "namedMods", Collections.emptyMap());
        WorldServer source = sourceWorld();
        Map<String, String> filesBefore = sourceFiles();
        SearchContext context = new SearchContext(source, new BlockPos(-242 * 16, 70, 0));
        int threshold = CompassConfig.slowCallMillis;
        try {
            // 本任务核查功能与隔离，不把测试机冷启动/JIT 的墙钟时间当作游戏性能结论。
            CompassConfig.slowCallMillis = 10_000;
            NetherInfoRuntimeCheck.verify(source);
            verifyBiomeOnlySession(source);
            RuntimeIdIsolationCheck.verify(source);
            require(run(new FoundationTask(context, false)).state == SearchStep.State.EXHAUSTED, "state task failed");
            require(run(new IsolationGuardTask(context)).state == SearchStep.State.EXHAUSTED, "write guards failed");
            verifyExit(context, FoundationTask.Exit.HOLD);
            verifyExit(context, FoundationTask.Exit.ERROR);
            verifyExit(context, FoundationTask.Exit.LIMIT);
            SearchStep natural = run(new NaturalDungeonTask(context));
            require(natural.state == SearchStep.State.FOUND, "natural task did not find the fixed fixture: " + natural.state);
            require(natural.target.position.equals(new BlockPos(-3854, 25, 18)), "natural fixture moved");
            verifyRecurrentRestriction(source);
            require(filesBefore.equals(sourceFiles()), "runtime checks changed source save files");
            System.out.println("FOUNDATION_RUNTIME_PASSED: id_isolation, state, guards, close, exception, limit, natural, recurrent_restriction");
        } finally {
            CompassConfig.slowCallMillis = threshold;
            DimensionManager.setWorld(0, null, source.getMinecraftServer());
        }
        ControlledWorldCheck.verify();
        // Anvil 文件线程常驻；存盘与卸载完成后结束独立测试 JVM。
        System.exit(0);
    }

    private static void verifyBiomeOnlySession(WorldServer source) {
        try (SimulationSession session = SimulationSession.forBiomes(source)) {
            require(session.world().getBiomeProvider() != source.getBiomeProvider(), "biome provider was shared");
            require(session.world().getBiomeProvider().getBiome(BlockPos.ORIGIN) != null, "biome query failed");
            require(session.generatedChunks() == 0, "biome query generated chunks");
            try {
                session.world().getChunk(0, 0);
                throw new AssertionError("biome-only query allowed terrain generation");
            } catch (UnsupportedOperationException expected) {
                require(expected.getMessage().contains("Biome-only"), "unexpected biome-only rejection");
            }
        }
        System.out.println("BIOME_ONLY_PASSED: private_provider, no_chunks, terrain_rejected");
    }

    private static void verifyRecurrentRestriction(WorldServer source) throws Exception {
        Loader loader = Loader.instance();
        Field controllerField = Loader.class.getDeclaredField("modController");
        controllerField.setAccessible(true);
        Object previousController = controllerField.get(loader);
        ModContainer recurrent = mock(ModContainer.class);
        LoadController controller = mock(LoadController.class);
        when(controller.getModState(recurrent)).thenReturn(ModState.AVAILABLE);
        try {
            set(Loader.class, loader, "namedMods", Collections.singletonMap("reccomplex", recurrent));
            controllerField.set(loader, controller);
            require(NaturalDungeonTask.unavailableReason(source).endsWith("natural_recurrent"),
                    "RC population prerequisites were treated as vanilla");
            try {
                new NaturalDungeonTask(new SearchContext(source, BlockPos.ORIGIN));
                throw new AssertionError("Unadapted RC environment was accepted");
            } catch (UnsupportedOperationException expected) {
                require(expected.getMessage().endsWith("natural_recurrent"), "wrong RC restriction reason");
            }
        } finally {
            set(Loader.class, loader, "namedMods", Collections.emptyMap());
            controllerField.set(loader, previousController);
        }
    }

    private static Map<String, String> sourceFiles() throws Exception {
        Path directory = new File("source").toPath();
        Map<String, String> hashes = new HashMap<>();
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path file : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
                hashes.put(directory.relativize(file).toString(), Base64.getEncoder().encodeToString(digest));
            }
        }
        return hashes;
    }

    private static SearchStep run(SearchTask task) throws Exception {
        try (SearchTask ignored = task) {
            for (int tick = 0; tick < 4000; tick++) {
                SearchStep step = task.advance(budget());
                if (step.state != SearchStep.State.RUNNING) {
                    return step;
                }
            }
            throw new AssertionError("Task did not finish");
        }
    }

    private static void verifyExit(SearchContext context, FoundationTask.Exit exit) throws Exception {
        FoundationTask task = new FoundationTask(context, false, exit);
        try {
            for (int tick = 0; tick < 3; tick++) {
                require(task.advance(budget()).state == SearchStep.State.RUNNING, "resource preparation failed");
            }
            if (exit == FoundationTask.Exit.ERROR) {
                try {
                    task.advance(budget());
                    throw new AssertionError("Expected injected failure");
                } catch (FoundationTask.VerificationFailure expected) {
                    // finally 中的真实会话关闭必须完成全部清理断言。
                    require(expected.getSuppressed().length == 0, "injected failure also failed isolation checks");
                }
            } else if (exit == FoundationTask.Exit.LIMIT) {
                try {
                    task.advance(budget());
                    throw new AssertionError("Expected budget rejection");
                } catch (com.shiver.structurecompasslegacy.search.SearchLimitException expected) {
                    require(expected.getMessage().equals("generated_chunks"), "wrong budget limit");
                }
            } else {
                require(task.holdingResources(), "hold task is not ready for cancellation");
            }
        } finally {
            task.close();
            task.close();
        }
    }

    private static SearchBudget budget() {
        return new SearchBudget(System.nanoTime(), 2_000_000);
    }

    private static WorldServer sourceWorld() throws Exception {
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getViewDistance()).thenReturn(8);
        when(server.isCallingFromMinecraftThread()).thenReturn(true);
        when(server.getMaxWorldSize()).thenReturn(30_000_000);
        server.worldTickTimes = new Hashtable<>();
        IFMLSidedHandler host = mock(IFMLSidedHandler.class);
        when(host.getSide()).thenReturn(Side.SERVER);
        when(host.getServer()).thenReturn(server);
        when(host.getDataFixer()).thenReturn(new CompoundDataFixer(new DataFixer(1343)));
        set(FMLCommonHandler.class, FMLCommonHandler.instance(), "sidedDelegate", host);

        WorldServer source = mock(WorldServer.class, CALLS_REAL_METHODS);
        WorldInfo info = new WorldInfo(new WorldSettings(0, GameType.SURVIVAL, false, false, WorldType.DEFAULT), "fixture");
        set(World.class, source, "worldInfo", info);
        set(World.class, source, "rand", new Random(0));
        set(World.class, source, "loadedEntityList", new ArrayList<>());
        set(World.class, source, "mapStorage", new MapStorage(null));
        set(World.class, source, "perWorldStorage", new MapStorage(null));
        set(World.class, source, "profiler", new Profiler());
        set(World.class, source, "saveHandler", new SaveHandler(new File("."), "source", false, null));
        set(WorldServer.class, source, "server", server);
        WorldProviderSurface provider = new WorldProviderSurface();
        set(World.class, source, "provider", provider);
        provider.setWorld(source);
        ChunkGeneratorOverworld generator = new ChunkGeneratorOverworld(source, 0, false, "");
        set(World.class, source, "chunkProvider", new ChunkProviderServer(source, null, generator));
        DimensionManager.setWorld(0, source, server);
        return source;
    }

    private static void set(Class<?> type, Object owner, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }
}
