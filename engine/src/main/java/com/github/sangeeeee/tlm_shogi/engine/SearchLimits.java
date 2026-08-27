package com.github.sangeeeee.tlm_shogi.engine;

import java.time.Duration;
import java.util.Objects;

/** Hard limits applied to one engine search. */
public record SearchLimits(
        Duration moveTime,
        int maximumDepth,
        long maximumNodes,
        int transpositionTableMiB,
        int threads
) {
    public SearchLimits {
        Objects.requireNonNull(moveTime, "moveTime");
        if (moveTime.isZero() || moveTime.isNegative()) {
            throw new IllegalArgumentException("moveTime must be positive");
        }
        if (maximumDepth < 1) {
            throw new IllegalArgumentException("maximumDepth must be at least 1");
        }
        if (maximumNodes < 1) {
            throw new IllegalArgumentException("maximumNodes must be at least 1");
        }
        if (transpositionTableMiB < 1) {
            throw new IllegalArgumentException("transpositionTableMiB must be at least 1");
        }
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
    }

    /** Matches the limits currently used by the mod's casual maid game. */
    public static SearchLimits casualPlay() {
        return new SearchLimits(Duration.ofSeconds(3), 8, 30_000, 256, 1);
    }
}
