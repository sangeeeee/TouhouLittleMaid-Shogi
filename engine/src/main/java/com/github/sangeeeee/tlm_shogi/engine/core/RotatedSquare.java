package com.github.sangeeeee.tlm_shogi.engine.core;

public record RotatedSquare(int raw) {
    public static final int INVALID_RAW = -1;

    public static RotatedSquare invalid() {
        return new RotatedSquare(INVALID_RAW);
    }

    public boolean isValid() {
        return raw != INVALID_RAW;
    }
}
