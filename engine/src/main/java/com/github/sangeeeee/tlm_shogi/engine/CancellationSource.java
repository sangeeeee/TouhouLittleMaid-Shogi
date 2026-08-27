package com.github.sangeeeee.tlm_shogi.engine;

import java.util.concurrent.atomic.AtomicBoolean;

/** Owns a cancellation flag that can be shared with one engine search. */
public final class CancellationSource {
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final CancellationToken token = cancelled::get;

    public CancellationToken token() {
        return token;
    }

    public boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
