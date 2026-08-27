package com.github.sangeeeee.tlm_shogi.engine;

/** Base checked exception for engine initialization and search failures. */
public class EngineException extends Exception {
    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
