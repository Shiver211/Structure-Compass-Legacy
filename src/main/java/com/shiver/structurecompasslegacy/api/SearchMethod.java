package com.shiver.structurecompasslegacy.api;

import java.util.Locale;

public enum SearchMethod {

    PREDICTION,
    SIMULATION,
    UNSUPPORTED;

    public String translationKey() {
        return "structurecompasslegacy.method." + name().toLowerCase(Locale.ROOT);
    }
}
