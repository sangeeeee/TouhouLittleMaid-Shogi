package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Sunfish-compatible movement and attack tables.
 *
 * <p>Step attacks are precomputed. Sliding attacks are traced against the supplied occupancy;
 * the first occupied square is included, as in Sunfish's original lookup tables.</p>
 */
public final class MoveTables {
    private static final int RAW_PIECE_COUNT = Piece.END;
    private static final int[] ONE_STEP_FLAGS = new int[RAW_PIECE_COUNT];
    private static final int[] LONG_STEP_FLAGS = new int[RAW_PIECE_COUNT];
    private static final Bitboard[] BLACK_KNIGHT = new Bitboard[Square.COUNT];
    private static final Bitboard[] WHITE_KNIGHT = new Bitboard[Square.COUNT];
    private static final Bitboard[] BLACK_SILVER = new Bitboard[Square.COUNT];
    private static final Bitboard[] WHITE_SILVER = new Bitboard[Square.COUNT];
    private static final Bitboard[] BLACK_GOLD = new Bitboard[Square.COUNT];
    private static final Bitboard[] WHITE_GOLD = new Bitboard[Square.COUNT];
    private static final Bitboard[] KING = new Bitboard[Square.COUNT];
    private static final Bitboard[] CROSS = new Bitboard[Square.COUNT];
    private static final Bitboard[] X = new Bitboard[Square.COUNT];
    private static final Bitboard[] NEIGHBOR_3X3 = new Bitboard[Square.COUNT];
    private static final Bitboard[] NEIGHBOR_5X5 = new Bitboard[Square.COUNT];

    private static final Direction[] ORTHOGONAL = {
            Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };
    private static final Direction[] DIAGONAL = {
            Direction.LEFT_UP, Direction.LEFT_DOWN, Direction.RIGHT_UP, Direction.RIGHT_DOWN
    };

    static {
        initializeDirectionFlags();
        initializeStepTables();
    }

    private MoveTables() {
    }

    public static boolean isMovableInOneStep(Piece piece, Direction direction) {
        requirePiece(piece);
        return direction != Direction.NONE && (ONE_STEP_FLAGS[piece.raw()] & flag(direction)) != 0;
    }

    public static boolean isMovableInLongStep(Piece piece, Direction direction) {
        requirePiece(piece);
        return direction != Direction.NONE && (LONG_STEP_FLAGS[piece.raw()] & flag(direction)) != 0;
    }

    public static Bitboard blackKnight(Square square) { return table(BLACK_KNIGHT, square); }
    public static Bitboard whiteKnight(Square square) { return table(WHITE_KNIGHT, square); }
    public static Bitboard blackSilver(Square square) { return table(BLACK_SILVER, square); }
    public static Bitboard whiteSilver(Square square) { return table(WHITE_SILVER, square); }
    public static Bitboard blackGold(Square square) { return table(BLACK_GOLD, square); }
    public static Bitboard whiteGold(Square square) { return table(WHITE_GOLD, square); }
    public static Bitboard king(Square square) { return table(KING, square); }
    public static Bitboard cross(Square square) { return table(CROSS, square); }
    public static Bitboard x(Square square) { return table(X, square); }
    public static Bitboard neighbor3x3(Square square) { return table(NEIGHBOR_3X3, square); }
    public static Bitboard neighbor5x5(Square square) { return table(NEIGHBOR_5X5, square); }

    public static Bitboard attacks(Piece piece, Square from, Bitboard occupied) {
        requirePiece(piece);
        requireSquare(from);
        Objects.requireNonNull(occupied, "occupied");
        PieceType type = piece.type();
        Turn turn = piece.turn();
        return switch (type.raw()) {
            case 0 -> single(from, turn == Turn.BLACK ? Direction.UP : Direction.DOWN);
            case 1 -> ray(from, turn == Turn.BLACK ? Direction.UP : Direction.DOWN, occupied::contains);
            case 2 -> turn == Turn.BLACK ? blackKnight(from) : whiteKnight(from);
            case 3 -> turn == Turn.BLACK ? blackSilver(from) : whiteSilver(from);
            case 4, 8, 9, 10, 11 -> turn == Turn.BLACK ? blackGold(from) : whiteGold(from);
            case 5 -> rays(from, occupied::contains, DIAGONAL);
            case 6 -> rays(from, occupied::contains, ORTHOGONAL);
            case 7 -> king(from);
            case 13 -> rays(from, occupied::contains, DIAGONAL).or(cross(from));
            case 14 -> rays(from, occupied::contains, ORTHOGONAL).or(x(from));
            default -> Bitboard.zero();
        };
    }

    public static Bitboard blackLance(Bitboard occupied, Square square) {
        return ray(square, Direction.UP, occupied::contains);
    }

    public static Bitboard whiteLance(Bitboard occupied, Square square) {
        return ray(square, Direction.DOWN, occupied::contains);
    }

    public static Bitboard ver(Bitboard occupied, Square square) {
        return rays(square, occupied::contains, Direction.UP, Direction.DOWN);
    }

    public static Bitboard hor(RotatedBitboard occupied, Square square) {
        Objects.requireNonNull(occupied, "occupied");
        return rays(square, candidate -> occupied.contains(candidate.rotate90()), Direction.LEFT, Direction.RIGHT);
    }

    public static Bitboard diagR45(RotatedBitboard occupied, Square square) {
        Objects.requireNonNull(occupied, "occupied");
        return rays(square, candidate -> occupied.contains(candidate.rotateRight45()),
                Direction.RIGHT_UP, Direction.LEFT_DOWN);
    }

    public static Bitboard diagL45(RotatedBitboard occupied, Square square) {
        Objects.requireNonNull(occupied, "occupied");
        return rays(square, candidate -> occupied.contains(candidate.rotateLeft45()),
                Direction.LEFT_UP, Direction.RIGHT_DOWN);
    }

    public static Bitboard up(Bitboard occupied, Square square) { return blackLance(occupied, square); }
    public static Bitboard down(Bitboard occupied, Square square) { return whiteLance(occupied, square); }
    public static Bitboard left(RotatedBitboard occupied, Square square) {
        return ray(square, Direction.LEFT, candidate -> occupied.contains(candidate.rotate90()));
    }
    public static Bitboard right(RotatedBitboard occupied, Square square) {
        return ray(square, Direction.RIGHT, candidate -> occupied.contains(candidate.rotate90()));
    }
    public static Bitboard rightUp45(RotatedBitboard occupied, Square square) {
        return ray(square, Direction.RIGHT_UP, candidate -> occupied.contains(candidate.rotateRight45()));
    }
    public static Bitboard leftDown45(RotatedBitboard occupied, Square square) {
        return ray(square, Direction.LEFT_DOWN, candidate -> occupied.contains(candidate.rotateRight45()));
    }
    public static Bitboard leftUp45(RotatedBitboard occupied, Square square) {
        return ray(square, Direction.LEFT_UP, candidate -> occupied.contains(candidate.rotateLeft45()));
    }
    public static Bitboard rightDown45(RotatedBitboard occupied, Square square) {
        return ray(square, Direction.RIGHT_DOWN, candidate -> occupied.contains(candidate.rotateLeft45()));
    }

    private static void initializeDirectionFlags() {
        setOne(Piece.BLACK_PAWN, Direction.UP);
        setOne(Piece.BLACK_LANCE, Direction.UP);
        setOne(Piece.BLACK_KNIGHT, Direction.LEFT_UP_KNIGHT, Direction.RIGHT_UP_KNIGHT);
        setOne(Piece.BLACK_SILVER, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                Direction.LEFT_DOWN, Direction.RIGHT_DOWN);
        setOne(Piece.BLACK_GOLD, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                Direction.LEFT, Direction.RIGHT, Direction.DOWN);
        setOne(Piece.BLACK_BISHOP, DIAGONAL);
        setOne(Piece.BLACK_ROOK, ORTHOGONAL);
        setOne(Piece.BLACK_KING, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                Direction.LEFT, Direction.RIGHT, Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
        copyOne(Piece.BLACK_GOLD, Piece.BLACK_TOKIN, Piece.BLACK_PRO_LANCE,
                Piece.BLACK_PRO_KNIGHT, Piece.BLACK_PRO_SILVER);
        copyOne(Piece.BLACK_KING, Piece.BLACK_HORSE, Piece.BLACK_DRAGON);

        setOne(Piece.WHITE_PAWN, Direction.DOWN);
        setOne(Piece.WHITE_LANCE, Direction.DOWN);
        setOne(Piece.WHITE_KNIGHT, Direction.LEFT_DOWN_KNIGHT, Direction.RIGHT_DOWN_KNIGHT);
        setOne(Piece.WHITE_SILVER, Direction.LEFT_UP, Direction.RIGHT_UP,
                Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
        setOne(Piece.WHITE_GOLD, Direction.UP, Direction.LEFT, Direction.RIGHT,
                Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
        setOne(Piece.WHITE_BISHOP, DIAGONAL);
        setOne(Piece.WHITE_ROOK, ORTHOGONAL);
        setOne(Piece.WHITE_KING, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                Direction.LEFT, Direction.RIGHT, Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
        copyOne(Piece.WHITE_GOLD, Piece.WHITE_TOKIN, Piece.WHITE_PRO_LANCE,
                Piece.WHITE_PRO_KNIGHT, Piece.WHITE_PRO_SILVER);
        copyOne(Piece.WHITE_KING, Piece.WHITE_HORSE, Piece.WHITE_DRAGON);

        copyLong(Piece.BLACK_LANCE, Piece.BLACK_LANCE);
        copyLong(Piece.BLACK_BISHOP, Piece.BLACK_BISHOP, Piece.BLACK_HORSE);
        copyLong(Piece.BLACK_ROOK, Piece.BLACK_ROOK, Piece.BLACK_DRAGON);
        copyLong(Piece.WHITE_LANCE, Piece.WHITE_LANCE);
        copyLong(Piece.WHITE_BISHOP, Piece.WHITE_BISHOP, Piece.WHITE_HORSE);
        copyLong(Piece.WHITE_ROOK, Piece.WHITE_ROOK, Piece.WHITE_DRAGON);
    }

    private static void initializeStepTables() {
        for (int raw = 0; raw < Square.COUNT; raw++) {
            Square square = new Square(raw);
            BLACK_KNIGHT[raw] = steps(square, Direction.LEFT_UP_KNIGHT, Direction.RIGHT_UP_KNIGHT);
            WHITE_KNIGHT[raw] = steps(square, Direction.LEFT_DOWN_KNIGHT, Direction.RIGHT_DOWN_KNIGHT);
            BLACK_SILVER[raw] = steps(square, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                    Direction.LEFT_DOWN, Direction.RIGHT_DOWN);
            WHITE_SILVER[raw] = steps(square, Direction.LEFT_UP, Direction.RIGHT_UP,
                    Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
            BLACK_GOLD[raw] = steps(square, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                    Direction.LEFT, Direction.RIGHT, Direction.DOWN);
            WHITE_GOLD[raw] = steps(square, Direction.UP, Direction.LEFT, Direction.RIGHT,
                    Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
            KING[raw] = steps(square, Direction.LEFT_UP, Direction.UP, Direction.RIGHT_UP,
                    Direction.LEFT, Direction.RIGHT, Direction.LEFT_DOWN, Direction.DOWN, Direction.RIGHT_DOWN);
            CROSS[raw] = steps(square, ORTHOGONAL);
            X[raw] = steps(square, DIAGONAL);
            NEIGHBOR_3X3[raw] = KING[raw].copy().set(square);
            Bitboard five = Bitboard.zero();
            for (int fileDelta = -2; fileDelta <= 2; fileDelta++) {
                for (int rankDelta = -2; rankDelta <= 2; rankDelta++) {
                    int file = square.file() + fileDelta;
                    int rank = square.rank() + rankDelta;
                    if (Square.isValidFile(file) && Square.isValidRank(rank)) {
                        five.set(Square.of(file, rank));
                    }
                }
            }
            NEIGHBOR_5X5[raw] = five;
        }
    }

    private static Bitboard steps(Square from, Direction... directions) {
        Bitboard result = Bitboard.zero();
        for (Direction direction : directions) {
            Square to = from.safetyMove(direction);
            if (to.isStrictValid()) result.set(to);
        }
        return result;
    }

    private static Bitboard single(Square from, Direction direction) {
        return steps(from, direction);
    }

    private static Bitboard rays(Square from, Predicate<Square> occupied, Direction... directions) {
        Bitboard result = Bitboard.zero();
        for (Direction direction : directions) result.orAssign(ray(from, direction, occupied));
        return result;
    }

    private static Bitboard ray(Square from, Direction direction, Predicate<Square> occupied) {
        requireSquare(from);
        Bitboard result = Bitboard.zero();
        for (Square square = from.safetyMove(direction); square.isStrictValid(); square = square.safetyMove(direction)) {
            result.set(square);
            if (occupied.test(square)) break;
        }
        return result;
    }

    private static Bitboard table(Bitboard[] table, Square square) {
        requireSquare(square);
        return table[square.raw()].copy();
    }

    private static void setOne(Piece piece, Direction... directions) {
        int flags = 0;
        for (Direction direction : directions) flags |= flag(direction);
        ONE_STEP_FLAGS[piece.raw()] = flags;
    }

    private static void copyOne(Piece source, Piece... targets) {
        for (Piece target : targets) ONE_STEP_FLAGS[target.raw()] = ONE_STEP_FLAGS[source.raw()];
    }

    private static void copyLong(Piece source, Piece... targets) {
        for (Piece target : targets) LONG_STEP_FLAGS[target.raw()] = ONE_STEP_FLAGS[source.raw()];
    }

    private static int flag(Direction direction) { return 1 << direction.ordinal(); }

    private static void requirePiece(Piece piece) {
        Objects.requireNonNull(piece, "piece");
        if (piece.isEmpty() || piece.raw() < 0 || piece.raw() >= RAW_PIECE_COUNT) {
            throw new IllegalArgumentException("invalid moving piece: " + piece.raw());
        }
    }

    private static void requireSquare(Square square) {
        Objects.requireNonNull(square, "square");
        if (!square.isStrictValid()) throw new IllegalArgumentException("invalid square: " + square.raw());
    }
}
