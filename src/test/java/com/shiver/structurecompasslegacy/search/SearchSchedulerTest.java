package com.shiver.structurecompasslegacy.search;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchMethod;
import com.shiver.structurecompasslegacy.api.SearchRoute;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.item.CompassTarget;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchSchedulerTest {

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void fifoUsesPositionAtStartAndDoesNotCreateQueuedTasks() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP first = fixture.player();
        EntityPlayerMP second = fixture.player();
        SearchTask firstTask = runningTask();
        SearchTask secondTask = runningTask();
        AtomicReference<SearchContext> context = new AtomicReference<>();
        fixture.add("first", ignored -> firstTask);
        fixture.add("second", started -> {
            context.set(started);
            return secondTask;
        });
        fixture.submit(first, 1, "first");
        fixture.submit(second, 2, "second");
        fixture.tick();
        verify(secondTask, never()).advance(any());
        assertEquals(null, context.get());
        when(second.getPosition()).thenReturn(new BlockPos(-80, 70, 112));
        fixture.scheduler.cancel(first.getUniqueID());
        fixture.tick();
        assertEquals(new BlockPos(-80, 70, 112), context.get().origin);
        verify(firstTask).close();
        fixture.scheduler.clear();
        verify(secondTask).close();
    }

    @Test
    void replacementClosesOldTaskAndStaleCancelCannotCancelNewRequest() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP player = fixture.player();
        SearchTask oldTask = runningTask();
        SearchTask newTask = runningTask();
        fixture.add("old", ignored -> oldTask);
        fixture.add("new", ignored -> newTask);
        fixture.submit(player, 10, "old");
        fixture.tick();
        fixture.submit(player, 11, "new");
        fixture.scheduler.cancel(player.getUniqueID(), 10);
        fixture.tick();
        verify(oldTask).close();
        verify(newTask).advance(any());
        fixture.scheduler.cancel(player.getUniqueID(), 11);
        verify(newTask).close();
    }

    @Test
    void timeoutExcludesQueueTime() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP first = fixture.player();
        EntityPlayerMP second = fixture.player();
        SearchTask firstTask = runningTask();
        SearchTask secondTask = runningTask();
        fixture.add("first", ignored -> firstTask);
        fixture.add("second", ignored -> secondTask);
        fixture.submit(first, 1, "first");
        fixture.submit(second, 2, "second");
        fixture.tick();
        fixture.clock.addAndGet(CompassConfig.timeoutSeconds * 1_000_000_000L);
        fixture.tick();
        assertEquals("limit", fixture.last().getString("status"));
        fixture.tick();
        verify(secondTask).advance(any());
        fixture.scheduler.clear();
    }

    @Test
    void disconnectAndDimensionChangeReleaseTasks() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP player = fixture.player();
        SearchTask task = runningTask();
        fixture.add("test", ignored -> task);
        fixture.submit(player, 1, "test");
        fixture.tick();
        when(player.connection.netManager.isChannelOpen()).thenReturn(false);
        fixture.tick();
        verify(task).close();

        SearchTask next = runningTask();
        fixture.add("next", ignored -> next);
        when(player.connection.netManager.isChannelOpen()).thenReturn(true);
        fixture.submit(player, 2, "next");
        fixture.tick();
        when(player.getServerWorld()).thenReturn(mock(WorldServer.class));
        fixture.tick();
        verify(next).close();
        assertEquals("context_changed", fixture.last().getString("reason"));
    }

    @Test
    void errorsAndLimitsCloseTasksWithoutBlockingTheQueue() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP first = fixture.player();
        EntityPlayerMP second = fixture.player();
        SearchTask broken = runningTask();
        when(broken.advance(any())).thenThrow(new IllegalStateException("fixture failure"));
        SearchTask limited = runningTask();
        when(limited.advance(any())).thenReturn(SearchStep.limit("generated_chunks"));
        fixture.add("broken", ignored -> broken);
        fixture.add("limited", ignored -> limited);
        fixture.submit(first, 1, "broken");
        fixture.submit(second, 2, "limited");
        fixture.tick();
        verify(broken).close();
        fixture.tick();
        verify(limited).close();
        assertEquals("generated_chunks", fixture.last().getString("reason"));
    }

    @Test
    void exhaustedPredictionDoesNotStartSimulationAndTargetKeepsSelectedMethod() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP player = fixture.player();
        SearchTask prediction = runningTask();
        when(prediction.advance(any())).thenReturn(SearchStep.exhausted());
        SearchTask simulation = runningTask();
        fixture.registry.register(new StructureDefinition("test:routes", "Test", "Routes", "路线", world -> true,
                new SearchRoute(SearchMethod.PREDICTION, ignored -> prediction, ""),
                new SearchRoute(SearchMethod.SIMULATION, ignored -> simulation, ""),
                "", () -> true));
        fixture.submit(player, 1, "routes");
        fixture.tick();
        verify(simulation, never()).advance(any());
        assertEquals("exhausted", fixture.last().getString("status"));

        fixture.registry.register(new StructureDefinition("test:simulation", "Test", "Simulation", "模拟",
                SearchMethod.SIMULATION, world -> true, ignored -> simulation, ""));
        when(simulation.advance(any())).thenReturn(SearchStep.found(new SearchTarget(new BlockPos(20, 70, 20))));
        fixture.submit(player, 2, "simulation");
        fixture.tick();
        assertEquals("structurecompasslegacy.method.experimental_simulation",
                CompassTarget.read(player.getHeldItem(EnumHand.MAIN_HAND)).method);
        assertEquals(fixture.last().getString("method"), CompassTarget.read(player.getHeldItem(EnumHand.MAIN_HAND)).method);
        verify(simulation).close();
    }

    @Test
    void initializationUsesTickBudgetAndSlowInitializationClosesTheTask() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP player = fixture.player();
        SearchTask normal = runningTask();
        fixture.add("normal", ignored -> {
            fixture.clock.addAndGet((long) (CompassConfig.tickBudgetMillis * 1_000_000L));
            return normal;
        });
        fixture.submit(player, 1, "normal");
        fixture.tick();
        verify(normal, never()).advance(any());
        fixture.tick();
        verify(normal).advance(any());

        SearchTask slow = runningTask();
        fixture.add("slow", ignored -> {
            fixture.clock.addAndGet((CompassConfig.slowCallMillis + 1L) * 1_000_000L);
            return slow;
        });
        fixture.submit(player, 2, "slow");
        fixture.tick();
        verify(normal).close();
        verify(slow, never()).advance(any());
        verify(slow).close();
        assertEquals("slow_call", fixture.last().getString("reason"));
        assertEquals(CompassConfig.slowCallMillis + 1L, fixture.last().getLong("longest"));
    }

    @Test
    void cancellingQueuedRequestsAndStoppingNeverCreateTheirTasks() throws Exception {
        Fixture fixture = new Fixture();
        EntityPlayerMP first = fixture.player();
        EntityPlayerMP second = fixture.player();
        SearchTask active = runningTask();
        StructureLocator waiting = mock(StructureLocator.class);
        when(waiting.unavailableReason(any())).thenReturn("");
        fixture.add("active", ignored -> active);
        fixture.add("waiting", waiting);
        fixture.submit(first, 1, "active");
        fixture.submit(second, 2, "waiting");
        fixture.tick();
        fixture.scheduler.cancel(second.getUniqueID(), 2);
        fixture.submit(second, 3, "waiting");
        fixture.scheduler.clear();
        fixture.tick();
        verify(active).close();
        verify(waiting, never()).createTask(any());
    }

    @Test
    void cleanupFailureStillAllowsNextRequest() throws Exception {
        Fixture fixture = new Fixture();
        SearchTask broken = runningTask();
        when(broken.advance(any())).thenReturn(SearchStep.exhausted());
        doThrow(new IllegalStateException("cleanup failure")).when(broken).close();
        SearchTask next = runningTask();
        fixture.add("broken", ignored -> broken);
        fixture.add("next", ignored -> next);
        fixture.submit(fixture.player(), 1, "broken");
        fixture.submit(fixture.player(), 2, "next");
        fixture.tick();
        fixture.tick();
        verify(next).advance(any());
        fixture.scheduler.clear();
        verify(next).close();
    }

    @Test
    void transportFailureCannotBreakCompletionOrCleanup() throws Exception {
        Fixture fixture = new Fixture();
        SearchScheduler scheduler = new SearchScheduler(fixture.registry, (player, message) -> {
            throw new IllegalStateException("connection closed");
        }, fixture.clock::get);
        SearchTask task = runningTask();
        when(task.advance(any())).thenReturn(SearchStep.exhausted());
        fixture.add("test", ignored -> task);
        scheduler.submit(fixture.player(), EnumHand.MAIN_HAND, 1, "test:test");
        scheduler.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        verify(task).close();
    }

    @Test
    void limitWithIsolationFailureMustReportError() throws Exception {
        Fixture fixture = new Fixture();
        SearchTask task = runningTask();
        SearchLimitException limit = new SearchLimitException("generated_chunks");
        limit.addSuppressed(new IllegalStateException("real world changed"));
        when(task.advance(any())).thenThrow(limit);
        fixture.add("limited", ignored -> task);
        fixture.submit(fixture.player(), 1, "limited");
        fixture.tick();
        assertEquals("error", fixture.last().getString("status"));
        assertTrue(fixture.last().getString("reason").endsWith("[suppressed=1]"));
        verify(task).close();
    }

    private SearchTask runningTask() throws Exception {
        SearchTask task = mock(SearchTask.class);
        when(task.advance(any())).thenReturn(SearchStep.running());
        return task;
    }

    private static final class Fixture {

        private final StructureRegistry registry = new StructureRegistry();
        private final List<NBTTagCompound> messages = new ArrayList<>();
        private final AtomicLong clock = new AtomicLong(1);
        private final SearchScheduler scheduler = new SearchScheduler(registry,
                (player, message) -> messages.add(message), clock::get);

        private EntityPlayerMP player() throws Exception {
            WorldServer world = mock(WorldServer.class);
            setField(World.class, world, "provider", new WorldProviderSurface());
            EntityPlayerMP player = mock(EntityPlayerMP.class);
            NetworkManager network = mock(NetworkManager.class);
            when(network.isChannelOpen()).thenReturn(true);
            NetHandlerPlayServer connection = mock(NetHandlerPlayServer.class);
            setField(NetHandlerPlayServer.class, connection, "netManager", network);
            player.connection = connection;
            setField(net.minecraft.entity.player.EntityPlayer.class, player, "inventory", new InventoryPlayer(player));
            when(player.getUniqueID()).thenReturn(UUID.randomUUID());
            when(player.getServerWorld()).thenReturn(world);
            when(player.getPosition()).thenReturn(BlockPos.ORIGIN);
            when(player.getHeldItem(EnumHand.MAIN_HAND)).thenReturn(new ItemStack(new Item()));
            return player;
        }

        private void add(String id, StructureLocator locator) {
            registry.register(new StructureDefinition("test:" + id, "Test", id, id, SearchMethod.PREDICTION,
                    world -> true, locator, ""));
        }

        private void submit(EntityPlayerMP player, long requestId, String id) {
            scheduler.submit(player, EnumHand.MAIN_HAND, requestId, "test:" + id);
        }

        private void tick() {
            scheduler.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        }

        private NBTTagCompound last() {
            return messages.get(messages.size() - 1);
        }

        private void setField(Class<?> owner, Object instance, String name, Object value) throws Exception {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
        }
    }
}
