package com.shiver.structurecompasslegacy.compat.abyssalcraft;

import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;

import net.minecraft.util.math.BlockPos;

final class JzaharTempleTask implements SearchTask {

    private final SearchContext context;

    JzaharTempleTask(SearchContext context) {
        this.context = context;
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        // Well.addComponentParts 的入口是 (4, 54, 7)，模板随后向下寻找地面。
        BlockPos entrance = new BlockPos(4, 54, 7);
        return context.contains(entrance)
                ? SearchStep.found(new SearchTarget(entrance)) : SearchStep.exhausted();
    }

    @Override
    public long checkedCandidates() {
        return 1;
    }

    @Override
    public long totalCandidates() {
        return 1;
    }

    @Override
    public void close() {
    }
}
