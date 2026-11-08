package com.shiver.structurecompasslegacy.search;

public final class SearchLimitException extends RuntimeException {

    public SearchLimitException(String reason) {
        super(reason);
    }
}
