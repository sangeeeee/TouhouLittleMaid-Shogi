package com.github.sangeeeee.tlm_shogi.engine.core;

/** Side to move. Sunfish encodes black as true and white as false. */
public enum Turn {
    BLACK,
    WHITE;

    public Turn opposite() {
        return this == BLACK ? WHITE : BLACK;
    }
}
