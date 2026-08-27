package com.github.sangeeeee.tlm_shogi.engine;

/** A lightweight cancellation check for hot search loops. */
@FunctionalInterface
public interface CancellationToken {
    CancellationToken NONE = () -> false;

    boolean isCancellationRequested();

    static CancellationToken none() {
        return NONE;
    }
}
