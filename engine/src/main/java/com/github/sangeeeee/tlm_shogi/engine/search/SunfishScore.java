package com.github.sangeeeee.tlm_shogi.engine.search;

/** Score-domain constants used by Sunfish 4. */
public final class SunfishScore {
    public static final int ZERO = 0;
    public static final int INFINITY = 16_000;
    public static final int MATE = 15_000;

    private SunfishScore() {
    }
}
