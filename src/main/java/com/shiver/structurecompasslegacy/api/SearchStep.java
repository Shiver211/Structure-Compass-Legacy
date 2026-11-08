package com.shiver.structurecompasslegacy.api;

public final class SearchStep {

    public enum State {
        RUNNING,
        FOUND,
        EXHAUSTED,
        LIMIT,
        ERROR
    }

    public final State state;
    public final SearchTarget target;
    public final String reason;

    private SearchStep(State state, SearchTarget target, String reason) {
        this.state = state;
        this.target = target;
        this.reason = reason;
    }

    public static SearchStep running() {
        return new SearchStep(State.RUNNING, null, "");
    }

    public static SearchStep found(SearchTarget target) {
        return new SearchStep(State.FOUND, target, "");
    }

    public static SearchStep exhausted() {
        return new SearchStep(State.EXHAUSTED, null, "");
    }

    public static SearchStep limit(String reason) {
        return new SearchStep(State.LIMIT, null, reason);
    }

    public static SearchStep error(String reason) {
        return new SearchStep(State.ERROR, null, reason);
    }
}
