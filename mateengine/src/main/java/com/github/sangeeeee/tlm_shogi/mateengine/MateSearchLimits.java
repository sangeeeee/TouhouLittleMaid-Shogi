package com.github.sangeeeee.tlm_shogi.mateengine;

import java.time.Duration;
import java.util.Objects;

/** Hard limits for one defensive mate search. */
public record MateSearchLimits(Duration moveTime, int maximumPly, long maximumNodes) {
    public MateSearchLimits {
        Objects.requireNonNull(moveTime, "moveTime");
        if (moveTime.isZero() || moveTime.isNegative()) {
            throw new IllegalArgumentException("moveTime must be positive");
        }
        if (maximumPly < 1 || maximumPly > 255) {
            throw new IllegalArgumentException("maximumPly must be between 1 and 255");
        }
        if (maximumNodes < 1) {
            throw new IllegalArgumentException("maximumNodes must be positive");
        }
    }

    public static MateSearchLimits standard() {
        return new MateSearchLimits(Duration.ofSeconds(10), 63, 2_000_000);
    }
}
