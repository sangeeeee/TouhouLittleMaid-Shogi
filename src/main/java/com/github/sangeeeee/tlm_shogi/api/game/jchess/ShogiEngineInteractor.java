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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/** Compatibility facade for the old USI-process call site, backed by Java. */
@OnlyIn(Dist.CLIENT)
public final class ShogiEngineInteractor {
    private static final String BUNDLED_RESOURCE_DIRECTORY = "assets/tlm_shogi/sunfish";
    private static final Object ENGINE_LOCK = new Object();
    private static final SearchLimits DEFAULT_LIMITS = SearchLimits.casualPlay();

    private static volatile SunfishEngine sharedEngine;
    private SearchLimits limits = DEFAULT_LIMITS;

    /** Loads the engine once for the whole client. Safe to call from a worker thread. */
    public static void initializeSharedEngine() throws EngineException {
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
            initializeSharedEngine();
        } catch (EngineException | RuntimeException exception) {
            TouhouLittleMaidShogi.LOGGER.error(
                    "Unable to warm up the Java Sunfish engine; it will retry when a game requests a move",
                    exception
            );
        }
    }

    /** Parses the legacy option JSON and ensures the shared Java engine is ready. */
    public String setup(String jsonParams) throws IOException {
        limits = parseLimits(jsonParams);
        try {
            initializeSharedEngine();
        } catch (EngineException | RuntimeException exception) {
            throw new IOException("Unable to initialize the Java Sunfish engine", exception);
        }
        return "setup ok";
    }

    /** Searches the supplied SFEN directly in the current JVM and returns a USI move. */
    public String interact(String sfen, String moves) throws IOException, InterruptedException {
        SunfishEngine engine = sharedEngine;
        if (engine == null || engine.state() != EngineState.READY) {
            throw new IllegalStateException("Engine not set up or no longer available");
        }

        List<String> moveList = splitMoves(moves);
        SearchResult result;
        try {
            CancellationToken cancellation = () -> Thread.currentThread().isInterrupted();
            result = engine.search(new SearchRequest(sfen, moveList, limits), cancellation);
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

    /** Kept for call-site compatibility; the shared Java engine is intentionally reused. */
    public String stop() {
        return "stop ok";
    }

    private static SearchLimits parseLimits(String jsonParams) {
        if (jsonParams == null || jsonParams.isBlank()) {
            return DEFAULT_LIMITS;
        }

        try {
            JsonObject options = new Gson().fromJson(jsonParams, JsonObject.class);
            if (options == null) {
                return DEFAULT_LIMITS;
            }
            int hashMiB = intOption(options, "USI_Hash", DEFAULT_LIMITS.transpositionTableMiB());
            long nodes = longOption(options, "NodesLimit", DEFAULT_LIMITS.maximumNodes());
            int depth = intOption(options, "DepthLimit", DEFAULT_LIMITS.maximumDepth());
            long moveTimeMillis = longOption(options, "MoveTime", DEFAULT_LIMITS.moveTime().toMillis());
            return new SearchLimits(Duration.ofMillis(moveTimeMillis), depth, nodes, hashMiB, 1);
        } catch (JsonSyntaxException | NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid engine option JSON", exception);
        }
    }

    private static int intOption(JsonObject options, String name, int fallback) {
        return options.has(name) ? options.get(name).getAsInt() : fallback;
    }

    private static long longOption(JsonObject options, String name, long fallback) {
        return options.has(name) ? options.get(name).getAsLong() : fallback;
    }

    private static List<String> splitMoves(String moves) {
        if (moves == null || moves.isBlank()) {
            return List.of();
        }
        return Arrays.stream(moves.strip().split("\\s+"))
                .filter(move -> !move.isBlank())
                .toList();
    }
}
