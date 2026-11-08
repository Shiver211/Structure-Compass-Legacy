package com.shiver.structurecompasslegacy.api;

public final class SearchRoute {

    public final SearchMethod method;
    public final StructureLocator locator;
    public final boolean experimental;
    public final String limitation;

    public SearchRoute(SearchMethod method, StructureLocator locator, String limitation) {
        this(method, locator, method == SearchMethod.SIMULATION, limitation);
    }

    public SearchRoute(SearchMethod method, StructureLocator locator, boolean experimental, String limitation) {
        if (method == SearchMethod.UNSUPPORTED || locator == null) {
            throw new IllegalArgumentException("A search route needs a locator and a supported method");
        }
        this.method = method;
        this.locator = locator;
        this.experimental = experimental;
        this.limitation = limitation;
    }

    public String translationKey() {
        return experimental ? "structurecompasslegacy.method.experimental_simulation" : method.translationKey();
    }
}
