package com.github.sangeeeee.tlm_shogi.engine;

/** Direct in-process engine API; implementations must not start external processes. */
public interface ShogiEngine extends AutoCloseable {
    EngineState state();

    void initialize() throws EngineException;

    SearchResult search(SearchRequest request, CancellationToken cancellationToken) throws EngineException;

    @Override
    void close();
}
