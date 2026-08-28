package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Objects;

/** Sunfish's 81-bit board split into 45-bit and 36-bit words. */
public final class Bitboard {
    public static final int FIRST_WIDTH = 45;
    public static final int SECOND_WIDTH = 36;
    private static final long FIRST_MASK = (1L << FIRST_WIDTH) - 1;
    private static final long SECOND_MASK = (1L << SECOND_WIDTH) - 1;

    private static final Bitboard[] SQUARE_MASKS = new Bitboard[Square.COUNT];
    private static final Bitboard[][] LINE_MASKS = new Bitboard[Square.COUNT][Square.COUNT];

    static {
        for (int raw = 0; raw < Square.COUNT; raw++) {
            SQUARE_MASKS[raw] = zero().set(new Square(raw));
        }
        for (int fromRaw = 0; fromRaw < Square.COUNT; fromRaw++) {
            Square from = new Square(fromRaw);
            for (int toRaw = 0; toRaw < Square.COUNT; toRaw++) {
                Square to = new Square(toRaw);
                Bitboard line = zero();
                Direction direction = from.directionTo(to);
                if (direction != Direction.NONE) {
                    for (Square square = from.move(direction); ; square = square.move(direction)) {
                        line.set(square);
                        if (square.equals(to)) {
                            break;
                        }
                    }
                }
                LINE_MASKS[fromRaw][toRaw] = line;
            }
        }
    }

    private long first;
    private long second;

    public Bitboard(long first, long second) {
        this.first = first;
        this.second = second;
    }

    public Bitboard(Bitboard source) {
        this(source.first, source.second);
    }

    public static Bitboard zero() { return new Bitboard(0, 0); }
    public static Bitboard full() { return new Bitboard(FIRST_MASK, SECOND_MASK); }
    public Bitboard copy() { return new Bitboard(this); }
    public long first() { return first; }
    public long second() { return second; }

    public Bitboard set(Square square) {
        int offset = requireSquare(square);
        if (offset < FIRST_WIDTH) first |= 1L << offset;
        else second |= 1L << (offset - FIRST_WIDTH);
        return this;
    }

    public Bitboard unset(Square square) {
        int offset = requireSquare(square);
        if (offset < FIRST_WIDTH) first &= ~(1L << offset);
        else second &= ~(1L << (offset - FIRST_WIDTH));
        return this;
    }

    public boolean contains(Square square) {
        int offset = requireSquare(square);
        return offset < FIRST_WIDTH
                ? (first & (1L << offset)) != 0
                : (second & (1L << (offset - FIRST_WIDTH))) != 0;
    }

    public boolean containsAnyOnFile(int file) {
        if (!Square.isValidFile(file)) throw new IllegalArgumentException("invalid file: " + file);
        int offset = (9 - file) * 9;
        return offset < FIRST_WIDTH
                ? ((first >>> offset) & 0x1ffL) != 0
                : ((second >>> (offset - FIRST_WIDTH)) & 0x1ffL) != 0;
    }

    public int count() { return Long.bitCount(first) + Long.bitCount(second); }
    public boolean isEmpty() { return first == 0 && second == 0; }
    public boolean intersects(Bitboard other) { return (first & other.first) != 0 || (second & other.second) != 0; }

    public Square findForward() {
        if (first != 0) return new Square(Long.numberOfTrailingZeros(first));
        if (second != 0) return new Square(FIRST_WIDTH + Long.numberOfTrailingZeros(second));
        return Square.invalid();
    }

    public Square pickForward() {
        Square square = findForward();
        if (square.isStrictValid()) unset(square);
        return square;
    }

    public Bitboard or(Bitboard other) { return new Bitboard(first | other.first, second | other.second); }
    public Bitboard and(Bitboard other) { return new Bitboard(first & other.first, second & other.second); }
    public Bitboard xor(Bitboard other) { return new Bitboard(first ^ other.first, second ^ other.second); }
    public Bitboard not() { return new Bitboard((~first) & FIRST_MASK, (~second) & SECOND_MASK); }

    /** Matches Sunfish: returns {@code (~this) & other}. */
    public Bitboard andNot(Bitboard other) {
        return new Bitboard((~first) & other.first, (~second) & other.second);
    }

    public Bitboard orAssign(Bitboard other) { first |= other.first; second |= other.second; return this; }
    public Bitboard andAssign(Bitboard other) { first &= other.first; second &= other.second; return this; }
    public Bitboard xorAssign(Bitboard other) { first ^= other.first; second ^= other.second; return this; }

    public Bitboard up() { return shiftEachRight(1).and(rank1to8()); }
    public Bitboard down() { return shiftEachLeft(1).and(rank2to9()); }
    public Bitboard left() { return shiftRight(9); }
    public Bitboard right() { return shiftLeft(9); }
    public Bitboard leftUp() { return shiftRight(10).and(rank1to8()); }
    public Bitboard leftDown() { return shiftRight(8).and(rank2to9()); }
    public Bitboard rightUp() { return shiftLeft(8).and(rank1to8()); }
    public Bitboard rightDown() { return shiftLeft(10).and(rank2to9()); }
    public Bitboard leftUpKnight() { return shiftRight(11).and(rank1to8()); }
    public Bitboard leftDownKnight() { return shiftRight(7).and(rank2to9()); }
    public Bitboard rightUpKnight() { return shiftLeft(7).and(rank1to8()); }
    public Bitboard rightDownKnight() { return shiftLeft(11).and(rank2to9()); }

    public Bitboard shiftEachLeft(int amount) {
        requireShift(amount);
        return new Bitboard(first << amount, second << amount);
    }

    public Bitboard shiftEachRight(int amount) {
        requireShift(amount);
        return new Bitboard(first >>> amount, second >>> amount);
    }

    public Bitboard shiftLeft(int amount) {
        requireCrossShift(amount);
        return new Bitboard(
                (first << amount) & FIRST_MASK,
                ((second << amount) | (first >>> (FIRST_WIDTH - amount))) & SECOND_MASK
        );
    }

    public Bitboard shiftRight(int amount) {
        requireCrossShift(amount);
        return new Bitboard(
                ((first >>> amount) | (second << (FIRST_WIDTH - amount))) & FIRST_MASK,
                (second >>> amount) & SECOND_MASK
        );
    }

    public static Bitboard mask(Square square) {
        return new Bitboard(SQUARE_MASKS[requireSquare(square)]);
    }

    public static Bitboard lineMask(Square from, Square to) {
        return new Bitboard(LINE_MASKS[requireSquare(from)][requireSquare(to)]);
    }

    public static Bitboard blackPromotable() { return new Bitboard(0x00000070381c0e07L, 0x00000000381c0e07L); }
    public static Bitboard whitePromotable() { return new Bitboard(0x00001c0e070381c0L, 0x0000000e070381c0L); }
    public static Bitboard blackPromotable2() { return new Bitboard(0x00000030180c0603L, 0x00000000180c0603L); }
    public static Bitboard whitePromotable2() { return new Bitboard(0x0000180c06030180L, 0x0000000c06030180L); }
    public static Bitboard blackNotPromotable() { return new Bitboard(0x00001f8fc7e3f1f8L, 0x0000000fc7e3f1f8L); }
    public static Bitboard whiteNotPromotable() { return new Bitboard(0x000003f1f8fc7e3fL, 0x00000001f8fc7e3fL); }
    public static Bitboard rank1() { return new Bitboard(0x0000001008040201L, 0x0000000008040201L); }
    public static Bitboard rank2() { return new Bitboard(0x0000002010080402L, 0x0000000010080402L); }
    public static Bitboard rank3to9() { return new Bitboard(0x00001fcfe7f3f9fcL, 0x0000000fe7f3f9fcL); }
    public static Bitboard rank2to9() { return new Bitboard(0x00001feff7fbfdfeL, 0x0000000ff7fbfdfeL); }
    public static Bitboard rank9() { return new Bitboard(0x0000100804020100L, 0x0000000804020100L); }
    public static Bitboard rank8() { return new Bitboard(0x0000080402010080L, 0x0000000402010080L); }
    public static Bitboard rank1to7() { return new Bitboard(0x000007f3f9fcfe7fL, 0x00000003f9fcfe7fL); }
    public static Bitboard rank1to8() { return new Bitboard(0x00000ff7fbfdfeffL, 0x00000007fbfdfeffL); }
    public static Bitboard file1() { return new Bitboard(0, 0x0000000ff8000000L); }
    public static Bitboard file2() { return new Bitboard(0, 0x0000000007fc0000L); }
    public static Bitboard file3() { return new Bitboard(0, 0x000000000003fe00L); }
    public static Bitboard file4() { return new Bitboard(0, 0x00000000000001ffL); }
    public static Bitboard file5() { return new Bitboard(0x00001ff000000000L, 0); }
    public static Bitboard file6() { return new Bitboard(0x0000000ff8000000L, 0); }
    public static Bitboard file7() { return new Bitboard(0x0000000007fc0000L, 0); }
    public static Bitboard file8() { return new Bitboard(0x000000000003fe00L, 0); }
    public static Bitboard file9() { return new Bitboard(0x00000000000001ffL, 0); }

    public String toBoardString() {
        StringBuilder builder = new StringBuilder(90);
        for (int rank = 1; rank <= 9; rank++) {
            for (int file = 9; file >= 1; file--) {
                builder.append(contains(Square.of(file, rank)) ? '1' : '0');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public String toString() { return toBoardString(); }

    @Override
    public boolean equals(Object other) {
        return other instanceof Bitboard board && first == board.first && second == board.second;
    }

    @Override
    public int hashCode() { return Objects.hash(first, second); }

    private static int requireSquare(Square square) {
        Objects.requireNonNull(square, "square");
        if (!square.isStrictValid()) throw new IllegalArgumentException("square is outside the board: " + square.raw());
        return square.raw();
    }

    private static void requireShift(int amount) {
        if (amount < 0 || amount >= 64) throw new IllegalArgumentException("invalid shift: " + amount);
    }

    private static void requireCrossShift(int amount) {
        if (amount < 1 || amount >= FIRST_WIDTH) throw new IllegalArgumentException("invalid cross-word shift: " + amount);
    }
}
