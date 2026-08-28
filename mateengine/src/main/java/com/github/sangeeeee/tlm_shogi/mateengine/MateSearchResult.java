package com.github.sangeeeee.tlm_shogi.mateengine;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of choosing a defense against a continuous-check mate. */
public record MateSearchResult(
        MateSearchOutcome outcome,
        Optional<String> bestMove,
        int matePlies,
        long nodes,
        int reachedPly,
        Duration elapsed,
        List<String> principalVariation
) {
    public MateSearchResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(bestMove, "bestMove");
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(principalVariation, "principalVariation");
        bestMove = bestMove.map(String::strip);
        principalVariation = List.copyOf(principalVariation);
        if (matePlies < -1 || nodes < 0 || reachedPly < 0 || elapsed.isNegative()) {
            throw new IllegalArgumentException("invalid mate-search metrics");
        }
        if (outcome == MateSearchOutcome.FORCED_MATE && matePlies < 0) {
            throw new IllegalArgumentException("FORCED_MATE requires a non-negative mate distance");
        }
        if (outcome != MateSearchOutcome.FORCED_MATE && matePlies != -1) {
            throw new IllegalArgumentException("only FORCED_MATE may carry a mate distance");
        }
    }
}
