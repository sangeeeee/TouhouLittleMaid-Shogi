package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Objects;

/** Sunfish's packed 16-bit move with an additional 16-bit search-data field. */
public final class Move {
    private static final int TO_MASK = 0x0000007f;
    private static final int FROM_MASK = 0x00003f80;
    private static final int PROMOTE = 0x00004000;
    private static final int DROP = 0x00008000;
    private static final int EXT_MASK = 0xffff0000;
    private static final int NONE = 0x0000ffff;
    private static final int FROM_OFFSET = 7;
    private static final int EXT_OFFSET = 16;

    private int raw;

    private Move(int raw) {
        this.raw = raw;
    }

    public static Move board(Square from, Square to, boolean promote) {
        requireSquare(from, "from");
        requireSquare(to, "to");
        return new Move(to.raw() | (from.raw() << FROM_OFFSET) | (promote ? PROMOTE : 0));
    }

    public static Move drop(PieceType pieceType, Square to) {
        Objects.requireNonNull(pieceType, "pieceType");
        requireSquare(to, "to");
        PieceType hand = pieceType.hand();
        if (hand.raw() < PieceType.PAWN.raw() || hand.raw() >= PieceType.HAND_END) {
            throw new IllegalArgumentException("piece cannot be dropped: " + pieceType);
        }
        return new Move(to.raw() | (hand.raw() << FROM_OFFSET) | DROP);
    }

    public static Move none() {
        return new Move(NONE);
    }

    public static Move deserialize(int raw) {
        return new Move(raw);
    }

    public static Move deserialize16(int raw16) {
        return new Move(raw16 & 0xffff);
    }

    public boolean isNone() { return raw == NONE; }
    public Square from() { return new Square((raw & FROM_MASK) >>> FROM_OFFSET); }
    public Square to() { return new Square(raw & TO_MASK); }
    public boolean isPromotion() { return (raw & PROMOTE) != 0; }
    public PieceType droppingPieceType() { return new PieceType((raw & FROM_MASK) >>> FROM_OFFSET); }
    public boolean isDrop() { return (raw & DROP) != 0; }

    public Move setExtData(int data) {
        if (data < 0 || data > 0xffff) {
            throw new IllegalArgumentException("extension data must fit in 16 bits: " + data);
        }
        raw = (raw & ~EXT_MASK) | (data << EXT_OFFSET);
        return this;
    }

    public int extData() { return raw >>> EXT_OFFSET; }
    public Move excludeExtData() { return new Move(raw & ~EXT_MASK); }
    public int serialize() { return raw; }
    public int serialize16() { return raw & 0xffff; }

    public String toCsa(Turn turn, PieceType movingPieceType) {
        if (isNone()) {
            return "none";
        }
        PieceType resultType = isDrop() ? droppingPieceType() : Objects.requireNonNull(movingPieceType, "movingPieceType");
        if (isPromotion()) {
            resultType = resultType.promote();
        }
        return (turn == Turn.BLACK ? "+" : "-")
                + (isDrop() ? "00" : from().toCsa())
                + to().toCsa()
                + resultType.toCsa();
    }

    public String toSfen() {
        if (isNone()) {
            return "none";
        }
        if (isDrop()) {
            return droppingPieceType().black().toSfen() + "*" + to().toSfen();
        }
        return from().toSfen() + to().toSfen() + (isPromotion() ? "+" : "");
    }

    @Override
    public String toString() {
        if (isNone()) {
            return "none";
        }
        return (isDrop() ? to().toCsa() + droppingPieceType().toCsa() : from().toCsa() + to().toCsa())
                + (isPromotion() ? "+" : "");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Move move && ((raw ^ move.raw) & ~EXT_MASK) == 0;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(serialize16());
    }

    private static void requireSquare(Square square, String name) {
        Objects.requireNonNull(square, name);
        if (!square.isStrictValid()) {
            throw new IllegalArgumentException(name + " square is outside the board: " + square.raw());
        }
    }
}
