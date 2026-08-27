package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Optional;

/** A board square using Sunfish's file-major raw order: 91 is zero and 19 is eighty. */
public record Square(int raw) {
    public static final int INVALID_RAW = -1;
    public static final int COUNT = 81;
    public static final int FILE_MAX = 9;
    public static final int RANK_MAX = 9;

    private static final int[] ROTATE_90 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 8, 15, 22, 29, 36, 43, 50, 57,
            2, 9, 16, 23, 30, 37, 44, 51, 58,
            3, 10, 17, 24, 31, 38, 45, 52, 59,
            4, 11, 18, 25, 32, 39, 46, 53, 60,
            5, 12, 19, 26, 33, 40, 47, 54, 61,
            6, 13, 20, 27, 34, 41, 48, 55, 62,
            7, 14, 21, 28, 35, 42, 49, 56, 63,
            0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] ROTATE_RIGHT_45 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 2, 4, 7, 11, 16, 22, 0,
            0, 3, 5, 8, 12, 17, 23, 29, 0,
            0, 6, 9, 13, 18, 24, 30, 35, 0,
            0, 10, 14, 19, 25, 31, 36, 40, 0,
            0, 15, 20, 26, 32, 37, 41, 44, 0,
            0, 21, 27, 33, 38, 42, 45, 47, 0,
            0, 28, 34, 39, 43, 46, 48, 49, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] ROTATE_LEFT_45 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 22, 16, 11, 7, 4, 2, 1, 0,
            0, 29, 23, 17, 12, 8, 5, 3, 0,
            0, 35, 30, 24, 18, 13, 9, 6, 0,
            0, 40, 36, 31, 25, 19, 14, 10, 0,
            0, 44, 41, 37, 32, 26, 20, 15, 0,
            0, 47, 45, 42, 38, 33, 27, 21, 0,
            0, 49, 48, 46, 43, 39, 34, 28, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    public static Square of(int file, int rank) {
        if (!isValidFile(file) || !isValidRank(rank)) {
            throw new IllegalArgumentException("invalid square: " + file + rank);
        }
        return new Square((9 - file) * RANK_MAX + rank - 1);
    }

    public static Square invalid() {
        return new Square(INVALID_RAW);
    }

    public static Square begin() {
        return new Square(0);
    }

    public static Square end() {
        return new Square(COUNT);
    }

    public int file() {
        return 9 - raw / RANK_MAX;
    }

    public int rank() {
        return raw % RANK_MAX + 1;
    }

    public boolean isValid() {
        return raw != INVALID_RAW;
    }

    public boolean isInvalid() {
        return !isValid();
    }

    public boolean isStrictValid() {
        return raw >= 0 && raw < COUNT;
    }

    public static boolean isValidFile(int file) {
        return file >= 1 && file <= FILE_MAX;
    }

    public static boolean isValidRank(int rank) {
        return rank >= 1 && rank <= RANK_MAX;
    }

    public boolean isPromotable(Turn turn) {
        return turn == Turn.BLACK ? rank() <= 3 : rank() >= 7;
    }

    public boolean isPawnMovable(Turn turn) {
        return turn == Turn.BLACK ? rank() != 1 : rank() != 9;
    }

    public boolean isLanceMovable(Turn turn) {
        return isPawnMovable(turn);
    }

    public boolean isKnightMovable(Turn turn) {
        return turn == Turn.BLACK ? rank() >= 3 : rank() <= 7;
    }

    public Square pointSymmetry() {
        return new Square(COUNT - 1 - raw);
    }

    public Square next() {
        return new Square(raw + 1);
    }

    public Square horizontalSymmetry() {
        return of(FILE_MAX + 1 - file(), rank());
    }

    public Square verticalSymmetry() {
        return of(file(), RANK_MAX + 1 - rank());
    }

    public Square up() { return up(1); }
    public Square up(int distance) { return new Square(raw - distance); }
    public Square down() { return down(1); }
    public Square down(int distance) { return new Square(raw + distance); }
    public Square left() { return left(1); }
    public Square left(int distance) { return new Square(raw - distance * RANK_MAX); }
    public Square right() { return right(1); }
    public Square right(int distance) { return new Square(raw + distance * RANK_MAX); }
    public Square leftUp() { return leftUp(1); }
    public Square leftUp(int distance) { return left(distance).up(distance); }
    public Square leftDown() { return leftDown(1); }
    public Square leftDown(int distance) { return left(distance).down(distance); }
    public Square rightUp() { return rightUp(1); }
    public Square rightUp(int distance) { return right(distance).up(distance); }
    public Square rightDown() { return rightDown(1); }
    public Square rightDown(int distance) { return right(distance).down(distance); }
    public Square leftUpKnight() { return left().up(2); }
    public Square leftDownKnight() { return left().down(2); }
    public Square rightUpKnight() { return right().up(2); }
    public Square rightDownKnight() { return right().down(2); }

    public Square safetyUp() { return safetyUp(1); }
    public Square safetyUp(int distance) {
        return isValid() && rank() - distance >= 1 ? up(distance) : invalid();
    }
    public Square safetyDown() { return safetyDown(1); }
    public Square safetyDown(int distance) {
        return isValid() && rank() + distance <= 9 ? down(distance) : invalid();
    }
    public Square safetyLeft() { return safetyLeft(1); }
    public Square safetyLeft(int distance) {
        return isValid() && file() + distance <= 9 ? left(distance) : invalid();
    }
    public Square safetyRight() { return safetyRight(1); }
    public Square safetyRight(int distance) {
        return isValid() && file() - distance >= 1 ? right(distance) : invalid();
    }
    public Square safetyLeftUp() { return safetyLeftUp(1); }
    public Square safetyLeftUp(int distance) { return safetyLeft(distance).safetyUp(distance); }
    public Square safetyLeftDown() { return safetyLeftDown(1); }
    public Square safetyLeftDown(int distance) { return safetyLeft(distance).safetyDown(distance); }
    public Square safetyRightUp() { return safetyRightUp(1); }
    public Square safetyRightUp(int distance) { return safetyRight(distance).safetyUp(distance); }
    public Square safetyRightDown() { return safetyRightDown(1); }
    public Square safetyRightDown(int distance) { return safetyRight(distance).safetyDown(distance); }
    public Square safetyLeftUpKnight() { return safetyLeft().safetyUp(2); }
    public Square safetyLeftDownKnight() { return safetyLeft().safetyDown(2); }
    public Square safetyRightUpKnight() { return safetyRight().safetyUp(2); }
    public Square safetyRightDownKnight() { return safetyRight().safetyDown(2); }

    public int distance(Square to) {
        int fileDistance = Math.abs(to.file() - file());
        int rankDistance = Math.abs(to.rank() - rank());
        if (fileDistance == 0 || rankDistance == 0 || fileDistance == rankDistance) {
            return Math.max(fileDistance, rankDistance);
        }
        if ((fileDistance == 1 && rankDistance == 2) || (fileDistance == 2 && rankDistance == 1)) {
            return 1;
        }
        return 0;
    }

    public Direction directionTo(Square to) {
        int df = to.file() - file();
        int dr = to.rank() - rank();
        if (df == 0) {
            return dr < 0 ? Direction.UP : dr > 0 ? Direction.DOWN : Direction.NONE;
        }
        if (dr == 0) {
            return df > 0 ? Direction.LEFT : Direction.RIGHT;
        }
        if (Math.abs(df) == Math.abs(dr)) {
            if (df > 0) return dr < 0 ? Direction.LEFT_UP : Direction.LEFT_DOWN;
            return dr < 0 ? Direction.RIGHT_UP : Direction.RIGHT_DOWN;
        }
        if (Math.abs(df) == 1 && Math.abs(dr) == 2) {
            if (df > 0) return dr < 0 ? Direction.LEFT_UP_KNIGHT : Direction.LEFT_DOWN_KNIGHT;
            return dr < 0 ? Direction.RIGHT_UP_KNIGHT : Direction.RIGHT_DOWN_KNIGHT;
        }
        return Direction.NONE;
    }

    public Square move(Direction direction) {
        return switch (direction) {
            case UP -> up();
            case DOWN -> down();
            case LEFT -> left();
            case RIGHT -> right();
            case LEFT_UP -> leftUp();
            case LEFT_DOWN -> leftDown();
            case RIGHT_UP -> rightUp();
            case RIGHT_DOWN -> rightDown();
            case LEFT_UP_KNIGHT -> leftUpKnight();
            case LEFT_DOWN_KNIGHT -> leftDownKnight();
            case RIGHT_UP_KNIGHT -> rightUpKnight();
            case RIGHT_DOWN_KNIGHT -> rightDownKnight();
            case NONE -> invalid();
        };
    }

    public Square safetyMove(Direction direction) {
        return switch (direction) {
            case UP -> safetyUp();
            case DOWN -> safetyDown();
            case LEFT -> safetyLeft();
            case RIGHT -> safetyRight();
            case LEFT_UP -> safetyLeftUp();
            case LEFT_DOWN -> safetyLeftDown();
            case RIGHT_UP -> safetyRightUp();
            case RIGHT_DOWN -> safetyRightDown();
            case LEFT_UP_KNIGHT -> safetyLeftUpKnight();
            case LEFT_DOWN_KNIGHT -> safetyLeftDownKnight();
            case RIGHT_UP_KNIGHT -> safetyRightUpKnight();
            case RIGHT_DOWN_KNIGHT -> safetyRightDownKnight();
            case NONE -> invalid();
        };
    }

    public RotatedSquare rotate90() {
        requireStrictValid();
        return new RotatedSquare(ROTATE_90[raw]);
    }

    public RotatedSquare rotateRight45() {
        requireStrictValid();
        return new RotatedSquare(ROTATE_RIGHT_45[raw]);
    }

    public RotatedSquare rotateLeft45() {
        requireStrictValid();
        return new RotatedSquare(ROTATE_LEFT_45[raw]);
    }

    public String toCsa() {
        requireStrictValid();
        return Integer.toString(file()) + rank();
    }

    public String toSfen() {
        requireStrictValid();
        return Integer.toString(file()) + (char) ('a' + rank() - 1);
    }

    public static Optional<Square> parseCsa(String value) {
        if (value != null && value.length() >= 2 && value.charAt(0) >= '1' && value.charAt(0) <= '9'
                && value.charAt(1) >= '1' && value.charAt(1) <= '9') {
            return Optional.of(of(value.charAt(0) - '0', value.charAt(1) - '0'));
        }
        return Optional.empty();
    }

    public static Optional<Square> parseSfen(String value) {
        if (value != null && value.length() >= 2 && value.charAt(0) >= '1' && value.charAt(0) <= '9'
                && value.charAt(1) >= 'a' && value.charAt(1) <= 'i') {
            return Optional.of(of(value.charAt(0) - '0', value.charAt(1) - 'a' + 1));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return isStrictValid() ? toCsa() : "invalid(" + raw + ")";
    }

    private void requireStrictValid() {
        if (!isStrictValid()) {
            throw new IllegalStateException("square is outside the board: " + raw);
        }
    }
}
