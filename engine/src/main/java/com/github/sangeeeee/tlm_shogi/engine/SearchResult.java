package com.github.sangeeeee.tlm_shogi.engine;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result returned by the direct Java engine API.
 *
 * <p>{@code score} is relative to the side to move in the fully replayed root
 * position. Positive values favor that side; mate scores approach +/-16000.</p>
 */
public record SearchResult(
        SearchOutcome outcome,
        Optional<String> bestMove,
        int score,
        int depth,
        long nodes,
        Duration elapsed,
        List<String> principalVariation
) {
    public SearchResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(bestMove, "bestMove");
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(principalVariation, "principalVariation");

        bestMove = bestMove.map(String::strip);
        principalVariation = List.copyOf(principalVariation);

        if (outcome == SearchOutcome.MOVE && bestMove.filter(move -> !move.isEmpty()).isEmpty()) {
            throw new IllegalArgumentException("MOVE results require a best move");
        }
        if (outcome != SearchOutcome.MOVE && bestMove.isPresent()) {
            throw new IllegalArgumentException("Only MOVE results may carry a best move");
        }
        if (depth < 0 || nodes < 0 || elapsed.isNegative()) {
            throw new IllegalArgumentException("search metrics must not be negative");
        }
    }

    public static SearchResult cancelled(Duration elapsed, long nodes) {
        return new SearchResult(
                SearchOutcome.CANCELLED,
                Optional.empty(),
                0,
                0,
                nodes,
                elapsed,
                List.of()
        );
    }
}
