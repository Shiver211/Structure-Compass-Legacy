package com.shiver.structurecompasslegacy.simulation;

import java.util.function.BooleanSupplier;

import com.shiver.structurecompasslegacy.api.SearchBudget;

@FunctionalInterface
public interface SimulationStage {

    enum Result {
        RUNNING,
        COMPLETE,
        REJECTED
    }

    Result advance(SearchBudget budget) throws Exception;

    static SimulationStage once(BooleanSupplier action) {
        return budget -> action.getAsBoolean() ? Result.COMPLETE : Result.REJECTED;
    }

    static SimulationStage skip() {
        return budget -> Result.COMPLETE;
    }
}
