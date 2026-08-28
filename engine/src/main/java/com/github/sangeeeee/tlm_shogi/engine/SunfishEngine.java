package com.github.sangeeeee.tlm_shogi.engine;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lifecycle facade for the pure Java Sunfish port.
 *
 * <p>The resource contract is implemented in this phase. Rule and search code
 * will replace the explicit not-yet-implemented failure in the next phases.</p>
 */
public final class SunfishEngine implements ShogiEngine {
    private final SunfishResources resources;
    private final AtomicReference<EngineState> state = new AtomicReference<>(EngineState.NEW);
    private volatile SunfishResourceInfo resourceInfo;

    public SunfishEngine(SunfishResources resources) {
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    @Override
    public EngineState state() {
        return state.get();
    }

    public SunfishResourceInfo resourceInfo() {
        ensureState(EngineState.READY);
        return resourceInfo;
    }

    @Override
    public synchronized void initialize() throws EngineException {
        EngineState current = state.get();
        if (current == EngineState.READY) {
            return;
        }
        if (current == EngineState.CLOSED) {
            throw new EngineException("A closed engine cannot be initialized again");
        }

        resourceInfo = resources.inspect();
        state.set(EngineState.READY);
    }

    @Override
    public SearchResult search(SearchRequest request, CancellationToken cancellationToken) throws EngineException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        ensureState(EngineState.READY);

        if (cancellationToken.isCancellationRequested()) {
            return SearchResult.cancelled(Duration.ZERO, 0);
        }

        throw new EngineException("Sunfish search has not been ported yet");
    }

    @Override
    public synchronized void close() {
        resourceInfo = null;
        state.set(EngineState.CLOSED);
    }

    private void ensureState(EngineState expected) {
        EngineState actual = state.get();
        if (actual != expected) {
            throw new IllegalStateException("Engine state is " + actual + "; expected " + expected);
        }
    }
}
