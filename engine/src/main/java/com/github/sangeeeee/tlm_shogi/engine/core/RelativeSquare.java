package com.github.sangeeeee.tlm_shogi.engine.core;

public record RelativeSquare(int raw) {
    public static final int WIDTH = 17;
    public static final int HEIGHT = 17;
    public static final int COUNT = WIDTH * HEIGHT;

    public static RelativeSquare between(Square from, Square to) {
        return new RelativeSquare(
                (from.file() - to.file() + 8) * HEIGHT + (to.rank() - from.rank() + 8)
        );
    }

    public int file() { return raw / HEIGHT; }
    public int rank() { return raw % HEIGHT; }
    public RelativeSquare horizontalSymmetry() {
        return new RelativeSquare((HEIGHT - 1 - raw / HEIGHT) * HEIGHT + raw % HEIGHT);
    }
    public RelativeSquare up() { return new RelativeSquare(raw - 1); }
    public RelativeSquare down() { return new RelativeSquare(raw + 1); }
    public RelativeSquare left() { return new RelativeSquare(raw - HEIGHT); }
    public RelativeSquare right() { return new RelativeSquare(raw + HEIGHT); }

    @Override
    public String toString() {
        return "[" + (file() - 8) + "," + (rank() - 8) + "]";
    }
}
