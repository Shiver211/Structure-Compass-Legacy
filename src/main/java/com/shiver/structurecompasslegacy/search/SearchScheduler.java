package com.shiver.structurecompasslegacy.search;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.StructureCompassLegacy;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.item.CompassTarget;
import com.shiver.structurecompasslegacy.network.CompassNetwork;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** 全服单任务，按提交顺序排队；所有 World 调用均发生在服务端 tick。 */
public final class SearchScheduler {

    private static final Logger LOGGER = LogManager.getLogger("structurecompasslegacy");
    private final StructureRegistry registry = StructureCompassLegacy.STRUCTURES;

    private final Map<UUID, Request> waiting = new LinkedHashMap<>();

    private Request active;
    private long ticks;

    public void submit(EntityPlayerMP player, EnumHand hand, long id, String structureId) {
        StructureDefinition structure;
        try {
            structure = registry.get(structureId);
        } catch (IllegalArgumentException exception) {
            reject(player, id, "unknown_structure");
            return;
        }

        if (structure == null) {
            reject(player, id, "unknown_structure");
            return;
        }
        String reason = structure.unavailableReason(player.getServerWorld());
        if (!reason.isEmpty()) {
            reject(player, id, reason);
            return;
        }

        cancel(player.getUniqueID());
        Request request = new Request(player, hand, id, structure);
        waiting.put(player.getUniqueID(), request);
        sendProgress(request, "queued", "");
    }

    private void reject(EntityPlayerMP player, long requestId, String reason) {
        NBTTagCompound message = new NBTTagCompound();
        message.setString("type", "progress");
        message.setLong("request", requestId);
        message.setString("status", "error");
        message.setString("reason", reason);
        send(player, message);
    }

    public void cancel(UUID player, long requestId) {
        Request queued = waiting.get(player);
        if (queued != null && queued.id == requestId) {
            waiting.remove(player);
            sendProgress(queued, "cancelled", "");
        }
        if (active != null && active.player.getUniqueID().equals(player) && active.id == requestId) {
            finish("cancelled", "");
        }
    }

    public void cancel(UUID player) {
        Request queued = waiting.remove(player);
        if (queued != null) {
            sendProgress(queued, "cancelled", "");
        }

        if (active != null && active.player.getUniqueID().equals(player)) {
            finish("cancelled", "");
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long tickStarted = System.nanoTime();
        ticks++;
        if (active == null) {
            startNext();
        }
        if (active == null) {
            return;
        }
        if (active.contextChanged()) {
            finish("cancelled", "context_changed");
            return;
        }
        if (System.nanoTime() - active.started >= CompassConfig.timeoutSeconds * 1_000_000_000L) {
            finish("limit", "timeout");
            return;
        }

        advance(new SearchBudget(tickStarted, (long) (CompassConfig.tickBudgetMillis * 1_000_000L)));
    }

    private void startNext() {
        Iterator<Request> iterator = waiting.values().iterator();
        if (!iterator.hasNext()) {
            return;
        }

        active = iterator.next();
        iterator.remove();
        active.started = System.nanoTime();
        if (active.contextChanged()) {
            finish("cancelled", "context_changed");
            return;
        }

        try {
            String reason = active.structure.unavailableReason(active.world);
            if (!reason.isEmpty()) {
                finish("error", reason);
                return;
            }
            active.context = new SearchContext(active.world, active.player.getPosition());
            try {
                active.task = active.structure.locator.createTask(active.context);
            } finally {
                active.workNanos = System.nanoTime() - active.started;
                active.initializationNanos = active.workNanos;
            }
            if (active.workNanos > CompassConfig.slowCallMillis * 1_000_000L) {
                finish("limit", "slow_call");
                return;
            }
            sendProgress(active, "running", "");
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private void advance(SearchBudget budget) {
        if (budget.exhausted()) {
            return;
        }
        long started = System.nanoTime();
        try {
            SearchStep step;
            try {
                step = active.task.advance(budget);
            } finally {
                // 异常和资源超限的调用也计入主线程开销。
                active.workNanos += System.nanoTime() - started;
            }

            if (step.state == SearchStep.State.FOUND) {
                CompassTarget.write(active.stack, active.structure, step.target, active.context.dimension);
                active.player.inventory.markDirty();
                active.target = step.target.position.toLong();
                finish("found", "");
            } else if (step.state == SearchStep.State.EXHAUSTED) {
                finish("exhausted", "");
            } else if (step.state == SearchStep.State.LIMIT) {
                finish("limit", step.reason);
            } else if (ticks % 5 == 0) {
                sendProgress(active, "running", "");
            }
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private void fail(Exception exception) {
        if (exception instanceof SearchLimitException && exception.getSuppressed().length == 0) {
            finish("limit", exception.getMessage());
            return;
        }
        LOGGER.warn("Structure search failed: {}", active.structure.id, exception);
        String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        if (exception.getSuppressed().length > 0) {
            reason += " [suppressed=" + exception.getSuppressed().length + "]";
        }
        finish("error", reason);
    }

    private void finish(String status, String reason) {
        Request finished = active;
        active = null;
        try {
            logPerformance(finished, status, reason);
            sendProgress(finished, status, reason);
        } finally {
            if (finished.task != null) {
                closeTask(finished.task);
            }
        }
    }

    private void logPerformance(Request request, String status, String reason) {
        if (request.task == null) {
            return;
        }

        // 只记录性能指标，不保存种子、玩家位置或结构坐标。
        LOGGER.info(
                "Structure search {}: status={}, reason={}, elapsedMs={}, workMs={}, longestMs={}, "
                        + "checked={}, generatedChunks={}, peakCachedChunks={}, longestOperation={}",
                request.structure.id, status, reason,
                (System.nanoTime() - request.started) / 1_000_000L,
                request.workNanos / 1_000_000L, Math.max(request.initializationNanos, request.task.longestCallNanos()) / 1_000_000L,
                request.task.checkedCandidates(), request.task.generatedChunks(), request.task.peakCachedChunks(),
                request.initializationNanos > request.task.longestCallNanos()
                        ? "task.initialize" : "task");
    }

    private void sendProgress(Request request, String status, String reason) {
        if (request.player.connection == null || !request.player.connection.netManager.isChannelOpen()) {
            return;
        }

        NBTTagCompound message = new NBTTagCompound();
        message.setString("type", "progress");
        message.setLong("request", request.id);
        message.setString("status", status);
        message.setString("reason", reason);
        message.setLong("target", request.target);
        message.setLong("elapsed", request.started == 0 ? 0 : (System.nanoTime() - request.started) / 1_000_000L);
        if (request.task != null) {
            message.setLong("checked", request.task.checkedCandidates());
            message.setLong("total", request.task.totalCandidates());
        }

        send(request.player, message);
    }

    private void send(EntityPlayerMP player, NBTTagCompound message) {
        try {
            CompassNetwork.send(player, message);
        } catch (RuntimeException exception) {
            LOGGER.warn("Cannot send structure search progress", exception);
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        cancel(event.player.getUniqueID());
    }

    private void closeTask(SearchTask task) {
        try {
            task.close();
        } catch (RuntimeException exception) {
            LOGGER.warn("Cannot fully release a structure search", exception);
        }
    }

    public void clear() {
        waiting.clear();
        if (active != null) {
            if (active.task != null) {
                closeTask(active.task);
            }
            active = null;
        }
    }

    private static final class Request {

        private final EntityPlayerMP player;
        private final WorldServer world;
        private final EnumHand hand;
        private final ItemStack stack;
        private final long id;
        private final StructureDefinition structure;
        private SearchContext context;

        private SearchTask task;
        private long started;
        private long workNanos;
        private long initializationNanos;
        private long target;

        private Request(EntityPlayerMP player, EnumHand hand, long id, StructureDefinition structure) {
            this.player = player;
            this.world = player.getServerWorld();
            this.hand = hand;
            this.stack = player.getHeldItem(hand);
            this.id = id;
            this.structure = structure;
        }

        private boolean contextChanged() {
            return player.isDead || player.getServerWorld() != world || player.getHeldItem(hand) != stack
                    || player.connection == null || !player.connection.netManager.isChannelOpen();
        }
    }
}
