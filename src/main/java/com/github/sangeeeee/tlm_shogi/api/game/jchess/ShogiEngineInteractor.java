package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.engine.CancellationToken;
import com.github.sangeeeee.tlm_shogi.engine.EngineException;
import com.github.sangeeeee.tlm_shogi.engine.EngineState;
import com.github.sangeeeee.tlm_shogi.engine.SearchLimits;
import com.github.sangeeeee.tlm_shogi.engine.SearchRequest;
import com.github.sangeeeee.tlm_shogi.engine.SearchResult;
import com.github.sangeeeee.tlm_shogi.engine.SunfishEngine;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;

/** Owns the client-wide Java shogi engine and runs maid move searches. */
@OnlyIn(Dist.CLIENT)
public final class ShogiEngineInteractor {
    private static final String BUNDLED_RESOURCE_DIRECTORY = "assets/tlm_shogi/sunfish";
    private static final Object ENGINE_LOCK = new Object();
    private static final SearchLimits GAME_LIMITS = SearchLimits.casualPlay();

    private static volatile SunfishEngine sharedEngine;

    private ShogiEngineInteractor() {
    }

    /** Ensures that the shared engine is ready for searches. */
    public static void initialize() throws IOException {
        try {
            initializeSharedEngine();
        } catch (EngineException | RuntimeException exception) {
            throw new IOException("Unable to initialize the Java Sunfish engine", exception);
        }
    }

    /** Loads the engine once for the whole client. Safe to call from a worker thread. */
    private static void initializeSharedEngine() throws EngineException {
        SunfishEngine existing = sharedEngine;
        if (existing != null && existing.state() == EngineState.READY) {
            return;
        }

        synchronized (ENGINE_LOCK) {
            existing = sharedEngine;
            if (existing != null && existing.state() == EngineState.READY) {
                return;
            }

            SunfishResources resources = SunfishResources.fromClasspath(
                    ShogiEngineInteractor.class.getClassLoader(),
                    BUNDLED_RESOURCE_DIRECTORY
            );
            SunfishEngine candidate = new SunfishEngine(resources);
            try {
                candidate.initialize();
                sharedEngine = candidate;
                TouhouLittleMaidShogi.LOGGER.info(
                        "Java Sunfish engine initialized (eval {}, book {} bytes)",
                        candidate.resourceInfo().evalBytes(),
                        candidate.resourceInfo().bookBytes()
                );
            } catch (EngineException | RuntimeException exception) {
                candidate.close();
                throw exception;
            }
        }
    }

    /** Best-effort startup warm-up; a later search retries initialization on failure. */
    public static void warmUp() {
        try {
            initialize();
        } catch (IOException exception) {
            TouhouLittleMaidShogi.LOGGER.error(
                    "Unable to warm up the Java Sunfish engine; it will retry when a game requests a move",
                    exception
            );
        }
    }

    /** Searches the supplied SFEN directly in the current JVM and returns a USI move. */
    public static String search(String sfen) throws IOException, InterruptedException {
        SunfishEngine engine = sharedEngine;
        if (engine == null || engine.state() != EngineState.READY) {
            throw new IllegalStateException("Engine not set up or no longer available");
        }

        SearchResult result;
        try {
            CancellationToken cancellation = () -> Thread.currentThread().isInterrupted();
            result = engine.search(SearchRequest.currentPosition(sfen, GAME_LIMITS), cancellation);
        } catch (EngineException | IllegalArgumentException | IllegalStateException exception) {
            throw new IOException("Java Sunfish search failed", exception);
        }

        TouhouLittleMaidShogi.LOGGER.info(
                "Java Sunfish search: outcome={}, move={}, depth={}, nodes={}, elapsed={} ms",
                result.outcome(),
                result.bestMove().orElse("-"),
                result.depth(),
                result.nodes(),
                result.elapsed().toMillis()
        );

        return switch (result.outcome()) {
            case MOVE -> result.bestMove().orElseThrow();
            case RESIGN -> "resign";
            case WIN -> "win";
            case CANCELLED -> throw new InterruptedException("Java Sunfish search was cancelled");
        };
    }
}
