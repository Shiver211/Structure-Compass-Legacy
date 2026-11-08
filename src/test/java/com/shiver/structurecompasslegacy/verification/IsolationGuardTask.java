package com.shiver.structurecompasslegacy.verification;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.search.SearchWork;
import com.shiver.structurecompasslegacy.simulation.MemorySaveHandler;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import net.minecraft.world.storage.WorldInfo;

/** 探测调用均为幂等操作或只读入口，即使保护未加载，也不修改玩家地形。 */
final class IsolationGuardTask implements SearchTask {

    private final SearchContext context;
    private final SearchWork work = new SearchWork();
    private int checked;

    IsolationGuardTask(SearchContext context) {
        this.context = context;
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        if (budget.exhausted()) {
            return SearchStep.running();
        }
        long time = context.source.getWorldTime();
        try (WorldStateSnapshot snapshot = WorldStateSnapshot.capture(context.source, work);
                SimulationScope ignored = SimulationScope.enter()) {
            work.run("guard." + checked, () -> probe(time));
            checked++;
        }
        if (longestCallNanos() > CompassConfig.slowCallMillis * 1_000_000L) {
            return SearchStep.limit("slow_call");
        }
        return checked == 5 ? SearchStep.exhausted() : SearchStep.running();
    }

    private void probe(long time) {
        switch (checked) {
            case 0:
                requireBlocked(() -> context.source.setWorldTime(time));
                break;
            case 1:
                requireBlocked(() -> context.source.getChunkProvider().provideChunk(
                        context.origin.getX() >> 4, context.origin.getZ() >> 4));
                break;
            case 2:
                requireBlocked(() -> context.source.getSaveHandler().getWorldDirectory());
                break;
            case 3:
                requireBlocked(() -> context.source.addScheduledTask(() -> {}));
                break;
            case 4:
                requireBlocked(() -> new MemorySaveHandler().saveWorldInfo(new WorldInfo(context.source.getWorldInfo())));
                break;
        }
    }

    private void requireBlocked(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new IllegalStateException("Simulation write barrier did not reject probe " + checked);
    }

    @Override
    public long checkedCandidates() {
        return checked;
    }

    @Override
    public long totalCandidates() {
        return 5;
    }

    @Override
    public long longestCallNanos() {
        return work.longestNanos();
    }

    @Override
    public String longestOperation() {
        return work.longestOperation();
    }

    @Override
    public void close() {
    }
}
