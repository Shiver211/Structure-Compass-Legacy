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
import com.shiver.structurecompasslegacy.item.CompassTarget;
import com.shiver.structurecompasslegacy.network.CompassNetwork;

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

    private final Map<UUID, Request> waiting = new LinkedHashMap<>();

    private Request active;
    private long ticks;

    public void submit(EntityPlayerMP player, EnumHand hand, long id, String structureId) {
        StructureDefinition structure;
        try {
            structure = StructureCompassLegacy.STRUCTURES.get(structureId);
        } catch (IllegalArgumentException exception) {
            return;
        }

        if (structure == null || !structure.supports(player.getServerWorld())) {
            return;
        }

        cancel(player.getUniqueID());
        Request request = new Request(player, hand, id, structure);
        waiting.put(player.getUniqueID(), request);
        sendProgress(request, "queued", "");
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

        ticks++;
        if (active == null) {
            startNext();
        }
        if (active == null) {
            return;
        }
        if (active.isValid()) {
            finish("cancelled", "context_changed");
            return;
        }
        if (System.nanoTime() - active.started >= CompassConfig.timeoutSeconds * 1_000_000_000L) {
            finish("limit", "timeout");
            return;
        }

        advance();
    }

    private void startNext() {
        Iterator<Request> iterator = waiting.values().iterator();
        if (!iterator.hasNext()) {
            return;
        }

        active = iterator.next();
        iterator.remove();
        active.started = System.nanoTime();
        if (active.isValid()) {
            finish("cancelled", "context_changed");
            return;
        }

        try {
            active.task = active.structure.locator.createTask(active.context);
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private void advance() {
        long started = System.nanoTime();
        try {
            SearchBudget budget = new SearchBudget(started, (long) (CompassConfig.tickBudgetMillis * 1_000_000L));
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
            } else if (step.state == SearchStep.State.ERROR) {
                finish("error", step.reason);
            } else if (ticks % 5 == 0) {
                sendProgress(active, "running", "");
            }
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private void fail(Exception exception) {
        StructureCompassLegacy.logger.warn("Structure search failed: {}", active.structure.id, exception);
        finish("error", exception.getClass().getSimpleName() + ": " + exception.getMessage());
    }

    private void finish(String status, String reason) {
        Request finished = active;
        active = null;
        try {
            logPerformance(finished, status, reason);
            sendProgress(finished, status, reason);
        } finally {
            if (finished.task != null) {
                finished.task.close();
            }
        }
    }

    private void logPerformance(Request request, String status, String reason) {
        if (request.task == null) {
            return;
        }

        // 只记录性能指标，不保存种子、玩家位置或结构坐标。
        StructureCompassLegacy.logger.info(
                "Structure search {}: status={}, reason={}, elapsedMs={}, workMs={}, longestMs={}, "
                        + "checked={}, generatedChunks={}, peakCachedChunks={}",
                request.structure.id, status, reason,
                (System.nanoTime() - request.started) / 1_000_000L,
                request.workNanos / 1_000_000L, request.task.longestCallNanos() / 1_000_000L,
                request.task.checkedCandidates(), request.task.generatedChunks(), request.task.peakCachedChunks());
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
        message.setLong("work", request.workNanos / 1_000_000L);

        if (request.task != null) {
            message.setLong("checked", request.task.checkedCandidates());
            message.setLong("total", request.task.totalCandidates());
            message.setInteger("chunks", request.task.generatedChunks());
            message.setInteger("peakChunks", request.task.peakCachedChunks());
            message.setLong("longest", request.task.longestCallNanos() / 1_000_000L);
        }

        CompassNetwork.send(request.player, message);
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        cancel(event.player.getUniqueID());
    }

    public void clear() {
        waiting.clear();
        if (active != null) {
            if (active.task != null) {
                active.task.close();
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
        private final SearchContext context;

        private SearchTask task;
        private long started;
        private long workNanos;
        private long target;

        private Request(EntityPlayerMP player, EnumHand hand, long id, StructureDefinition structure) {
            this.player = player;
            this.world = player.getServerWorld();
            this.hand = hand;
            this.stack = player.getHeldItem(hand);
            this.id = id;
            this.structure = structure;
            this.context = new SearchContext(world, player.getPosition());
        }

        private boolean isValid() {
            return player.isDead || player.getServerWorld() != world || player.getHeldItem(hand) != stack;
        }
    }
}
