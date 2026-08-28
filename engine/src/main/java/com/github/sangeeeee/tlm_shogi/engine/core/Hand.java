package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Arrays;

/** Counts the seven droppable piece types held by one side. */
public final class Hand {
    private static final int[] MAX_COUNTS = {18, 4, 4, 4, 4, 2, 2};
    private final int[] counts;

    public Hand() {
        counts = new int[PieceType.HAND_END];
    }

    public Hand(Hand source) {
        counts = Arrays.copyOf(source.counts, source.counts.length);
    }

    public int increment(PieceType pieceType) {
        return incrementUnpromoted(pieceType.unpromote());
    }

    public int incrementUnpromoted(PieceType pieceType) {
        int index = indexOf(pieceType);
        if (counts[index] >= MAX_COUNTS[index]) {
            throw new IllegalStateException("too many pieces in hand: " + pieceType);
        }
        return ++counts[index];
    }

    public int decrement(PieceType pieceType) {
        return decrementUnpromoted(pieceType.unpromote());
    }

    public int decrementUnpromoted(PieceType pieceType) {
        int index = indexOf(pieceType);
        if (counts[index] == 0) {
            throw new IllegalStateException("piece is not present in hand: " + pieceType);
        }
        return --counts[index];
    }

    public int get(PieceType pieceType) {
        return counts[indexOf(pieceType.unpromote())];
    }

    public void set(PieceType pieceType, int value) {
        int index = indexOf(pieceType.unpromote());
        if (value < 0 || value > MAX_COUNTS[index]) {
            throw new IllegalArgumentException("invalid hand count for " + pieceType + ": " + value);
        }
        counts[index] = value;
    }

    public Hand copy() {
        return new Hand(this);
    }

    public boolean isEmpty() {
        for (int count : counts) {
            if (count != 0) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Hand hand && Arrays.equals(counts, hand.counts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(counts);
    }

    private static int indexOf(PieceType pieceType) {
        int index = pieceType.raw();
        if (index < PieceType.PAWN.raw() || index >= PieceType.HAND_END) {
            throw new IllegalArgumentException("piece is not a hand type: " + pieceType);
        }
        return index;
    }
}
