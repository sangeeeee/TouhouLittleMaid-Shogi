package com.github.sangeeeee.tlm_shogi.engine;

import java.util.List;
import java.util.Objects;

/** A USI-compatible position description and its search limits. */
public record SearchRequest(String sfen, List<String> moves, SearchLimits limits) {
    public SearchRequest {
        Objects.requireNonNull(sfen, "sfen");
        Objects.requireNonNull(moves, "moves");
        Objects.requireNonNull(limits, "limits");

        sfen = sfen.strip();
        if (sfen.isEmpty()) {
            throw new IllegalArgumentException("sfen must not be blank");
        }

        moves = List.copyOf(moves);
        if (moves.stream().anyMatch(move -> move == null || move.isBlank())) {
            throw new IllegalArgumentException("moves must not contain null or blank values");
        }
    }

    public static SearchRequest currentPosition(String sfen, SearchLimits limits) {
        return new SearchRequest(sfen, List.of(), limits);
    }
}
