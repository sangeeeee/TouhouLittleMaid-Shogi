package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Objects;

/** Mutable 64-bit occupancy used by Sunfish's rotated sliding-piece tables. */
public final class RotatedBitboard {
    private long raw;

    public RotatedBitboard(long raw) {
        this.raw = raw;
    }

    public RotatedBitboard(RotatedBitboard source) {
        this(Objects.requireNonNull(source, "source").raw);
    }

    public static RotatedBitboard zero() { return new RotatedBitboard(0); }
    public long raw() { return raw; }
    public int count() { return Long.bitCount(raw); }

    public RotatedBitboard set(RotatedSquare square) {
        return set(square.raw());
    }

    public RotatedBitboard set(int offset) {
        requireOffset(offset);
        raw |= 1L << offset;
        return this;
    }

    public RotatedBitboard unset(RotatedSquare square) {
        return unset(square.raw());
    }

    public RotatedBitboard unset(int offset) {
        requireOffset(offset);
        raw &= ~(1L << offset);
        return this;
    }

    public boolean contains(RotatedSquare square) {
        return contains(square.raw());
    }

    public boolean contains(int offset) {
        requireOffset(offset);
        return (raw & (1L << offset)) != 0;
    }

    public RotatedBitboard or(RotatedBitboard other) { return new RotatedBitboard(raw | other.raw); }
    public RotatedBitboard and(RotatedBitboard other) { return new RotatedBitboard(raw & other.raw); }
    public RotatedBitboard xor(RotatedBitboard other) { return new RotatedBitboard(raw ^ other.raw); }
    public RotatedBitboard not() { return new RotatedBitboard(~raw); }
    public RotatedBitboard orAssign(RotatedBitboard other) { raw |= other.raw; return this; }
    public RotatedBitboard andAssign(RotatedBitboard other) { raw &= other.raw; return this; }
    public RotatedBitboard xorAssign(RotatedBitboard other) { raw ^= other.raw; return this; }

    @Override
    public String toString() { return String.format("%016x", raw); }

    @Override
    public boolean equals(Object other) {
        return other instanceof RotatedBitboard bitboard && raw == bitboard.raw;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(raw);
    }

    private static void requireOffset(int offset) {
        if (offset < 0 || offset >= 64) throw new IllegalArgumentException("invalid rotated offset: " + offset);
    }
}
