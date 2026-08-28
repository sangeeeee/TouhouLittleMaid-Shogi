package com.github.sangeeeee.tlm_shogi.engine.search;

import com.github.sangeeeee.tlm_shogi.engine.EngineException;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResources;
import com.github.sangeeeee.tlm_shogi.engine.core.Bitboard;
import com.github.sangeeeee.tlm_shogi.engine.core.Direction;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveTables;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.RotatedBitboard;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

/**
 * Reader and evaluator for Sunfish 4's optimized {@code eval.bin} format.
 *
 * <p>The 47 MiB feature vector is held in one primitive {@code short[]} with
 * no per-weight object overhead. Scores are returned from Black's point of
 * view, just like Sunfish.</p>
 */
public final class SunfishEvaluator {
    public static final int POSITIONAL_SCORE_SCALE = 32;
    public static final int ENTERING_KING_BONUS = 1_000;

    private static final int SQUARES = 81;
    private static final int HAND_INDEX_COUNT = 76;
    private static final int HAND_TYPE_INDEX_COUNT = 38;
    private static final int PIECE_INDEX_COUNT = 18;
    private static final int PIECE_TYPE_INDEX_COUNT = 9;
    private static final int NEIGHBOR_COUNT = 8;
    private static final int OPEN_COUNT = 8;

    private static final int KING_HAND = 0;
    private static final int KING_PIECE = KING_HAND + SQUARES * HAND_INDEX_COUNT;
    private static final int KING_PIECE_NEIGHBOR_X = KING_PIECE
            + SQUARES * SQUARES * PIECE_INDEX_COUNT;
    private static final int KING_PIECE_NEIGHBOR_Y = KING_PIECE_NEIGHBOR_X
            + SQUARES * SQUARES * PIECE_INDEX_COUNT * PIECE_INDEX_COUNT;
    private static final int KING_PIECE_NEIGHBOR_XY = KING_PIECE_NEIGHBOR_Y
            + SQUARES * SQUARES * PIECE_INDEX_COUNT * PIECE_INDEX_COUNT;
    private static final int KING_PIECE_NEIGHBOR_XY2 = KING_PIECE_NEIGHBOR_XY
            + SQUARES * SQUARES * PIECE_INDEX_COUNT * PIECE_INDEX_COUNT;
    private static final int KING_NEIGHBOR_HAND = KING_PIECE_NEIGHBOR_XY2
            + SQUARES * SQUARES * PIECE_INDEX_COUNT * PIECE_INDEX_COUNT;
    private static final int KING_NEIGHBOR_PIECE = KING_NEIGHBOR_HAND
            + SQUARES * NEIGHBOR_COUNT * PIECE_TYPE_INDEX_COUNT * HAND_INDEX_COUNT;
    private static final int KING_KING_HAND = KING_NEIGHBOR_PIECE
            + SQUARES * NEIGHBOR_COUNT * PIECE_TYPE_INDEX_COUNT * SQUARES * PIECE_INDEX_COUNT;
    private static final int KING_KING_PIECE = KING_KING_HAND
            + SQUARES * SQUARES * HAND_TYPE_INDEX_COUNT;
    private static final int KING_OPEN = KING_KING_PIECE
            + SQUARES * SQUARES * SQUARES * PIECE_TYPE_INDEX_COUNT;
    private static final int ONE_KING_OPEN_SIZE = SQUARES * SQUARES * OPEN_COUNT;
    private static final int KING_ALLY_EFFECT_9 = KING_OPEN + 18 * ONE_KING_OPEN_SIZE;
    private static final int KING_ENEMY_EFFECT_9 = KING_ALLY_EFFECT_9 + SQUARES * 10;
    private static final int KING_ALLY_EFFECT_25 = KING_ENEMY_EFFECT_9 + SQUARES * 10;
    private static final int KING_ENEMY_EFFECT_25 = KING_ALLY_EFFECT_25 + SQUARES * 26;
    private static final int KING_EFFECT_9_DIFF = KING_ENEMY_EFFECT_25 + SQUARES * 26;
    private static final int KING_EFFECT_25_DIFF = KING_EFFECT_9_DIFF + SQUARES * 19;

    public static final int WEIGHT_COUNT = KING_EFFECT_25_DIFF + SQUARES * 51;
    public static final int VERSION_HEADER_BYTES = 1
            + SunfishResources.EXPECTED_EVAL_VERSION.length();
    public static final long EXPECTED_FILE_BYTES = VERSION_HEADER_BYTES + WEIGHT_COUNT * 2L;

    private static final int CACHE_SIZE = 1 << 18;
    private static final int CACHE_MASK = CACHE_SIZE - 1;

    // OptimizedFeatureVector's 18 KingOpen members, in declaration order.
    private static final int B_ROOK_UP = 0;
    private static final int W_ROOK_UP = 1;
    private static final int B_ROOK_DOWN = 2;
    private static final int W_ROOK_DOWN = 3;
    private static final int B_ROOK_LEFT = 4;
    private static final int W_ROOK_LEFT = 5;
    private static final int B_ROOK_RIGHT = 6;
    private static final int W_ROOK_RIGHT = 7;
    private static final int B_BISHOP_LEFT_UP = 8;
    private static final int W_BISHOP_LEFT_UP = 9;
    private static final int B_BISHOP_RIGHT_DOWN = 10;
    private static final int W_BISHOP_RIGHT_DOWN = 11;
    private static final int B_BISHOP_RIGHT_UP = 12;
    private static final int W_BISHOP_RIGHT_UP = 13;
    private static final int B_BISHOP_LEFT_DOWN = 14;
    private static final int W_BISHOP_LEFT_DOWN = 15;
    private static final int B_LANCE = 16;
    private static final int W_LANCE = 17;

    private static final int[] MATERIAL = {
            100, 250, 300, 400, 500, 650, 700, 5_000,
            500, 400, 400, 400, 0, 750, 850
    };

    private static final int[] EVAL_PIECE_INDEX = {
            0, 2, 4, 6, 8, 10, 14, 0, 8, 8, 8, 8, 0, 12, 16, 0,
            1, 3, 5, 7, 9, 11, 15, 0, 9, 9, 9, 9, 0, 13, 17
    };

    private static final int[] EVAL_PIECE_TYPE_INDEX = {
            0, 1, 2, 3, 4, 5, 7, 0, 4, 4, 4, 4, 0, 6, 8
    };

    private static final int[] BLACK_HAND_INDEX = {0, 36, 44, 52, 60, 68, 72};
    private static final int[] WHITE_HAND_INDEX = {18, 40, 48, 56, 64, 70, 74};
    private static final int[] HAND_TYPE_INDEX = {0, 18, 22, 26, 30, 34, 36};

    private final Path source;
    private final short[] weights;
    private final boolean positionalEnabled;
    private final long[] cacheKeys = new long[CACHE_SIZE];
    private final int[] cacheScores = new int[CACHE_SIZE];
    private final byte[] cacheValid = new byte[CACHE_SIZE];

    private SunfishEvaluator(Path source, short[] weights, boolean positionalEnabled) {
        this.source = source;
        this.weights = weights;
        this.positionalEnabled = positionalEnabled;
    }

    /** Opens and validates an optimized Sunfish feature vector. */
    public static SunfishEvaluator load(Path path) throws EngineException {
        Objects.requireNonNull(path, "path");
        Path normalized = path.toAbsolutePath().normalize();
        try {
            long size = Files.size(normalized);
            if (size != EXPECTED_FILE_BYTES) {
                throw new EngineException("Invalid eval.bin size: " + size
                        + " bytes; expected " + EXPECTED_FILE_BYTES);
            }

            int headerBytes;
            try (InputStream input = Files.newInputStream(normalized)) {
                int length = input.read();
                if (length < 1 || length > 31) {
                    throw new EngineException("eval.bin contains an invalid version length: " + length);
                }
                byte[] versionBytes = input.readNBytes(length);
                if (versionBytes.length != length) {
                    throw new EngineException("eval.bin ended inside its version header");
                }
                String version = new String(versionBytes, StandardCharsets.US_ASCII);
                if (!SunfishResources.EXPECTED_EVAL_VERSION.equals(version)) {
                    throw new EngineException("Unsupported eval.bin version: " + version
                            + "; expected " + SunfishResources.EXPECTED_EVAL_VERSION);
                }
                headerBytes = 1 + length;
            }

            try (FileChannel channel = FileChannel.open(normalized, StandardOpenOption.READ)) {
                short[] loadedWeights = new short[WEIGHT_COUNT];
                ByteBuffer buffer = ByteBuffer.allocate(1 << 20).order(ByteOrder.LITTLE_ENDIAN);
                channel.position(headerBytes);
                int weightIndex = 0;
                while (channel.read(buffer) >= 0) {
                    buffer.flip();
                    while (buffer.remaining() >= Short.BYTES) {
                        loadedWeights[weightIndex++] = buffer.getShort();
                    }
                    buffer.compact();
                    if (weightIndex == WEIGHT_COUNT) break;
                }
                if (weightIndex != WEIGHT_COUNT || buffer.position() != 0) {
                    throw new EngineException("eval.bin contained " + weightIndex
                            + " complete weights; expected " + WEIGHT_COUNT);
                }
                return new SunfishEvaluator(normalized, loadedWeights, true);
            }
        } catch (IOException exception) {
            throw new EngineException("Failed to load Sunfish evaluation data: " + normalized, exception);
        }
    }

    /** A material-only evaluator useful for isolated search tests. */
    public static SunfishEvaluator materialOnly() {
        return new SunfishEvaluator(null, null, false);
    }

    public Path source() {
        return source;
    }

    public int weightCount() {
        return positionalEnabled ? weights.length : 0;
    }

    /** Returns the complete score from Black's point of view. */
    public int evaluate(Position position) {
        Objects.requireNonNull(position, "position");
        int index = (int) position.getHash() & CACHE_MASK;
        if (cacheValid[index] != 0 && cacheKeys[index] == position.getHash()) {
            return cacheScores[index];
        }

        int score = (short) (calculateMaterialScore(position) + calculatePositionalScore(position));
        cacheKeys[index] = position.getHash();
        cacheScores[index] = score;
        cacheValid[index] = 1;
        return (short) score;
    }

    public int calculateMaterialScore(Position position) {
        Objects.requireNonNull(position, "position");
        int score = 0;
        for (int raw = PieceType.PAWN.raw(); raw < PieceType.HAND_END; raw++) {
            PieceType type = new PieceType(raw);
            score += MATERIAL[raw] * position.handCount(Turn.BLACK, type);
            score -= MATERIAL[raw] * position.handCount(Turn.WHITE, type);
        }

        for (int raw = 0; raw < Square.COUNT; raw++) {
            Piece piece = position.pieceAt(new Square(raw));
            if (piece.isEmpty() || piece.type().equals(PieceType.KING)) continue;
            int value = materialValue(piece.type());
            score += piece.isBlack() ? value : -value;
        }

        Square blackKing = position.kingSquare(Turn.BLACK);
        Square whiteKing = position.kingSquare(Turn.WHITE);
        if (blackKing.isStrictValid() && blackKing.rank() <= 3) score += ENTERING_KING_BONUS;
        if (whiteKing.isStrictValid() && whiteKing.rank() >= 7) score -= ENTERING_KING_BONUS;
        return score;
    }

    public int calculatePositionalScore(Position position) {
        Objects.requireNonNull(position, "position");
        if (!positionalEnabled) return 0;

        Square blackKingSquare = position.kingSquare(Turn.BLACK);
        Square whiteKingSquare = position.kingSquare(Turn.WHITE);
        if (!blackKingSquare.isStrictValid() || !whiteKingSquare.isStrictValid()) {
            throw new IllegalArgumentException("Sunfish evaluation requires both kings on the board");
        }

        FeatureMeta meta = createMeta(position, blackKingSquare, whiteKingSquare);
        int sum = evaluateHands(position, meta);
        Bitboard blackEffects = Bitboard.zero();
        Bitboard whiteEffects = Bitboard.zero();
        Bitboard occupied = position.occupied();
        RotatedBitboard occupied90 = position.get90RotatedBitboard();
        RotatedBitboard occupiedRight45 = position.getRight45RotatedBitboard();
        RotatedBitboard occupiedLeft45 = position.getLeft45RotatedBitboard();

        for (int raw = 0; raw < Square.COUNT; raw++) {
            Square square = new Square(raw);
            Piece piece = position.pieceAt(square);
            if (piece.isEmpty() || piece.type().equals(PieceType.KING)) continue;

            sum += evaluatePiece(meta, piece, square);
            if (piece.isBlack()) {
                blackEffects.orAssign(MoveTables.attacks(piece, square, occupied));
            } else {
                whiteEffects.orAssign(MoveTables.attacks(piece, square, occupied));
            }

            PieceType type = piece.type();
            if (type.equals(PieceType.BISHOP) || type.equals(PieceType.HORSE)) {
                sum += evaluateBishopOpen(meta, piece, square, occupiedRight45, occupiedLeft45);
            }
            if (type.equals(PieceType.ROOK) || type.equals(PieceType.DRAGON)) {
                sum += evaluateRookOpen(meta, piece, square, occupied, occupied90);
            }
            if (type.equals(PieceType.LANCE)) {
                sum += evaluateLanceOpen(meta, piece, square, occupied);
            }
        }

        sum += evaluateNeighborPairs(position, meta, Direction.RIGHT, KING_PIECE_NEIGHBOR_X);
        sum += evaluateNeighborPairs(position, meta, Direction.UP, KING_PIECE_NEIGHBOR_Y);
        sum += evaluateNeighborPairs(position, meta, Direction.RIGHT_UP, KING_PIECE_NEIGHBOR_XY);
        sum += evaluateNeighborPairs(position, meta, Direction.LEFT_UP, KING_PIECE_NEIGHBOR_XY2);
        sum += evaluateKingEffects(meta, blackKingSquare, whiteKingSquare, blackEffects, whiteEffects);

        return (short) (sum / POSITIONAL_SCORE_SCALE);
    }

    public void clearCache() {
        Arrays.fill(cacheValid, (byte) 0);
    }

    static int materialValue(PieceType type) {
        int raw = type.raw();
        if (raw < 0 || raw >= MATERIAL.length) return 0;
        return MATERIAL[raw];
    }

    static int exchangeValue(Piece piece) {
        PieceType type = piece.type();
        return materialValue(type) + materialValue(type.unpromote());
    }

    static int promotionGain(Piece piece) {
        if (!piece.isPromotable()) return 0;
        return materialValue(piece.type().promote()) - materialValue(piece.type());
    }

    private FeatureMeta createMeta(Position position, Square blackKing, Square whiteKing) {
        FeatureMeta meta = new FeatureMeta(blackKing.raw(), whiteKing.pointSymmetry().raw());
        Bitboard blackNeighborhood = MoveTables.king(blackKing);
        Bitboard whiteNeighborhood = MoveTables.king(whiteKing);
        for (int raw = 0; raw < Square.COUNT; raw++) {
            Square square = new Square(raw);
            Piece piece = position.pieceAt(square);
            if (piece.isEmpty() || piece.type().equals(PieceType.KING)) continue;
            if (piece.isBlack() && blackNeighborhood.contains(square)) {
                meta.blackNeighbors[meta.blackNeighborCount++] = neighborData(
                        neighborIndex(blackKing, square),
                        evalPieceTypeIndex(piece.type())
                );
            }
            if (piece.isWhite() && whiteNeighborhood.contains(square)) {
                meta.whiteNeighbors[meta.whiteNeighborCount++] = neighborData(
                        reverseNeighborIndex(whiteKing, square),
                        evalPieceTypeIndex(piece.type())
                );
            }
        }
        return meta;
    }

    private int evaluateHands(Position position, FeatureMeta meta) {
        int sum = 0;
        for (int raw = 0; raw < PieceType.HAND_END; raw++) {
            PieceType type = new PieceType(raw);
            sum += evaluateHand(meta, Turn.BLACK, position.handCount(Turn.BLACK, type),
                    HAND_TYPE_INDEX[raw], BLACK_HAND_INDEX[raw], WHITE_HAND_INDEX[raw]);
            sum += evaluateHand(meta, Turn.WHITE, position.handCount(Turn.WHITE, type),
                    HAND_TYPE_INDEX[raw], WHITE_HAND_INDEX[raw], BLACK_HAND_INDEX[raw]);
        }
        return sum;
    }

    private int evaluateHand(FeatureMeta meta, Turn turn, int count,
                             int typeIndex, int blackIndex, int whiteIndex) {
        if (count == 0) return 0;
        int bi = blackIndex + count - 1;
        int wi = whiteIndex + count - 1;
        int sum = kingHand(meta.blackKing, bi) - kingHand(meta.whiteKing, wi);
        for (int index = 0; index < meta.blackNeighborCount; index++) {
            int data = meta.blackNeighbors[index];
            sum += kingNeighborHand(meta.blackKing, neighborNumber(data), neighborType(data), bi);
        }
        for (int index = 0; index < meta.whiteNeighborCount; index++) {
            int data = meta.whiteNeighbors[index];
            sum -= kingNeighborHand(meta.whiteKing, neighborNumber(data), neighborType(data), wi);
        }
        if (turn == Turn.BLACK) {
            sum += kingKingHand(meta.blackKing, meta.whiteKing, typeIndex + count - 1);
        } else {
            sum -= kingKingHand(meta.whiteKing, meta.blackKing, typeIndex + count - 1);
        }
        return sum;
    }

    private int evaluatePiece(FeatureMeta meta, Piece piece, Square square) {
        int blackSquare = square.raw();
        int whiteSquare = square.pointSymmetry().raw();
        int blackIndex = evalPieceIndex(piece);
        int whiteIndex = evalPieceIndex(piece.enemy());
        int typeIndex = evalPieceTypeIndex(piece.type());
        int sum = kingPiece(meta.blackKing, blackSquare, blackIndex)
                - kingPiece(meta.whiteKing, whiteSquare, whiteIndex);
        for (int index = 0; index < meta.blackNeighborCount; index++) {
            int data = meta.blackNeighbors[index];
            sum += kingNeighborPiece(meta.blackKing, neighborNumber(data), neighborType(data),
                    blackSquare, blackIndex);
        }
        for (int index = 0; index < meta.whiteNeighborCount; index++) {
            int data = meta.whiteNeighbors[index];
            sum -= kingNeighborPiece(meta.whiteKing, neighborNumber(data), neighborType(data),
                    whiteSquare, whiteIndex);
        }
        if (piece.isBlack()) {
            sum += kingKingPiece(meta.blackKing, meta.whiteKing, blackSquare, typeIndex);
        } else {
            sum -= kingKingPiece(meta.whiteKing, meta.blackKing, whiteSquare, typeIndex);
        }
        return sum;
    }

    private int evaluateBishopOpen(FeatureMeta meta, Piece piece, Square square,
                                   RotatedBitboard right45, RotatedBitboard left45) {
        int sum = 0;
        if (piece.isBlack()) {
            sum += openPair(meta, square, B_BISHOP_RIGHT_UP, W_BISHOP_RIGHT_UP,
                    MoveTables.rightUp45(right45, square));
            sum += openPair(meta, square, B_BISHOP_LEFT_DOWN, W_BISHOP_LEFT_DOWN,
                    MoveTables.leftDown45(right45, square));
            sum += openPair(meta, square, B_BISHOP_LEFT_UP, W_BISHOP_LEFT_UP,
                    MoveTables.leftUp45(left45, square));
            sum += openPair(meta, square, B_BISHOP_RIGHT_DOWN, W_BISHOP_RIGHT_DOWN,
                    MoveTables.rightDown45(left45, square));
        } else {
            sum += openPair(meta, square, W_BISHOP_RIGHT_UP, B_BISHOP_RIGHT_UP,
                    MoveTables.leftDown45(right45, square));
            sum += openPair(meta, square, W_BISHOP_LEFT_DOWN, B_BISHOP_LEFT_DOWN,
                    MoveTables.rightUp45(right45, square));
            sum += openPair(meta, square, W_BISHOP_LEFT_UP, B_BISHOP_LEFT_UP,
                    MoveTables.rightDown45(left45, square));
            sum += openPair(meta, square, W_BISHOP_RIGHT_DOWN, B_BISHOP_RIGHT_DOWN,
                    MoveTables.leftUp45(left45, square));
        }
        return sum;
    }

    private int evaluateRookOpen(FeatureMeta meta, Piece piece, Square square,
                                 Bitboard occupied, RotatedBitboard occupied90) {
        int sum = 0;
        if (piece.isBlack()) {
            sum += openPair(meta, square, B_ROOK_UP, W_ROOK_UP, MoveTables.up(occupied, square));
            sum += openPair(meta, square, B_ROOK_DOWN, W_ROOK_DOWN, MoveTables.down(occupied, square));
            sum += openPair(meta, square, B_ROOK_LEFT, W_ROOK_LEFT, MoveTables.left(occupied90, square));
            sum += openPair(meta, square, B_ROOK_RIGHT, W_ROOK_RIGHT, MoveTables.right(occupied90, square));
        } else {
            sum += openPair(meta, square, W_ROOK_UP, B_ROOK_UP, MoveTables.down(occupied, square));
            sum += openPair(meta, square, W_ROOK_DOWN, B_ROOK_DOWN, MoveTables.up(occupied, square));
            sum += openPair(meta, square, W_ROOK_LEFT, B_ROOK_LEFT, MoveTables.right(occupied90, square));
            sum += openPair(meta, square, W_ROOK_RIGHT, B_ROOK_RIGHT, MoveTables.left(occupied90, square));
        }
        return sum;
    }

    private int evaluateLanceOpen(FeatureMeta meta, Piece piece, Square square, Bitboard occupied) {
        if (piece.isBlack()) {
            return openPair(meta, square, B_LANCE, W_LANCE,
                    MoveTables.blackLance(occupied, square));
        }
        return openPair(meta, square, W_LANCE, B_LANCE,
                MoveTables.whiteLance(occupied, square));
    }

    private int openPair(FeatureMeta meta, Square square, int blackArray, int whiteArray, Bitboard ray) {
        int count = Math.max(0, ray.count() - 1);
        if (count >= OPEN_COUNT) {
            throw new IllegalStateException("Sunfish open-line index is outside eval.bin: " + count);
        }
        return kingOpen(blackArray, meta.blackKing, square.raw(), count)
                - kingOpen(whiteArray, meta.whiteKing, square.pointSymmetry().raw(), count);
    }

    private int evaluateNeighborPairs(Position position, FeatureMeta meta,
                                      Direction partnerDirection, int arrayOffset) {
        int sum = 0;
        for (int raw = 0; raw < Square.COUNT; raw++) {
            Square firstSquare = new Square(raw);
            Piece first = position.pieceAt(firstSquare);
            if (first.isEmpty() || first.type().equals(PieceType.KING)) continue;
            Square secondSquare = firstSquare.safetyMove(partnerDirection);
            if (!secondSquare.isStrictValid()) continue;
            Piece second = position.pieceAt(secondSquare);
            if (second.isEmpty() || second.type().equals(PieceType.KING)) continue;

            int blackFirst = evalPieceIndex(first);
            int blackSecond = evalPieceIndex(second);
            int whiteFirst = evalPieceIndex(second.enemy());
            int whiteSecond = evalPieceIndex(first.enemy());
            sum += kingPieceNeighbor(arrayOffset, meta.blackKing, firstSquare.raw(), blackFirst, blackSecond);
            sum -= kingPieceNeighbor(arrayOffset, meta.whiteKing,
                    secondSquare.pointSymmetry().raw(), whiteFirst, whiteSecond);
        }
        return sum;
    }

    private int evaluateKingEffects(FeatureMeta meta, Square blackKing, Square whiteKing,
                                    Bitboard blackEffects, Bitboard whiteEffects) {
        int sum = 0;
        Bitboard blackNine = MoveTables.neighbor3x3(blackKing);
        int blackAlly9 = blackEffects.and(blackNine).count();
        int blackEnemy9 = whiteEffects.and(blackNine).count();
        sum += effect(KING_ALLY_EFFECT_9, meta.blackKing, blackAlly9);
        sum += effect(KING_ENEMY_EFFECT_9, meta.blackKing, blackEnemy9);
        sum += effect(KING_EFFECT_9_DIFF, meta.blackKing, 9 + blackAlly9 - blackEnemy9);

        Bitboard blackTwentyFive = MoveTables.neighbor5x5(blackKing);
        int blackAlly25 = blackEffects.and(blackTwentyFive).count();
        int blackEnemy25 = whiteEffects.and(blackTwentyFive).count();
        sum += effect(KING_ALLY_EFFECT_25, meta.blackKing, blackAlly25);
        sum += effect(KING_ENEMY_EFFECT_25, meta.blackKing, blackEnemy25);
        sum += effect(KING_EFFECT_25_DIFF, meta.blackKing, 25 + blackAlly25 - blackEnemy25);

        Bitboard whiteNine = MoveTables.neighbor3x3(whiteKing);
        int whiteBlack9 = blackEffects.and(whiteNine).count();
        int whiteWhite9 = whiteEffects.and(whiteNine).count();
        sum -= effect(KING_ALLY_EFFECT_9, meta.whiteKing, whiteWhite9);
        sum -= effect(KING_ENEMY_EFFECT_9, meta.whiteKing, whiteBlack9);
        sum -= effect(KING_EFFECT_9_DIFF, meta.whiteKing, 9 + whiteWhite9 - whiteBlack9);

        Bitboard whiteTwentyFive = MoveTables.neighbor5x5(whiteKing);
        int whiteBlack25 = blackEffects.and(whiteTwentyFive).count();
        int whiteWhite25 = whiteEffects.and(whiteTwentyFive).count();
        sum -= effect(KING_ALLY_EFFECT_25, meta.whiteKing, whiteWhite25);
        sum -= effect(KING_ENEMY_EFFECT_25, meta.whiteKing, whiteBlack25);
        sum -= effect(KING_EFFECT_25_DIFF, meta.whiteKing, 25 + whiteWhite25 - whiteBlack25);
        return sum;
    }

    private int kingHand(int king, int hand) {
        return weight(KING_HAND + king * HAND_INDEX_COUNT + hand);
    }

    private int kingPiece(int king, int square, int piece) {
        return weight(KING_PIECE + (king * SQUARES + square) * PIECE_INDEX_COUNT + piece);
    }

    private int kingPieceNeighbor(int offset, int king, int square, int first, int second) {
        return weight(offset + ((king * SQUARES + square) * PIECE_INDEX_COUNT + first)
                * PIECE_INDEX_COUNT + second);
    }

    private int kingNeighborHand(int king, int neighbor, int type, int hand) {
        return weight(KING_NEIGHBOR_HAND
                + (((king * NEIGHBOR_COUNT + neighbor) * PIECE_TYPE_INDEX_COUNT + type)
                * HAND_INDEX_COUNT + hand));
    }

    private int kingNeighborPiece(int king, int neighbor, int type, int square, int piece) {
        return weight(KING_NEIGHBOR_PIECE
                + ((((king * NEIGHBOR_COUNT + neighbor) * PIECE_TYPE_INDEX_COUNT + type)
                * SQUARES + square) * PIECE_INDEX_COUNT + piece));
    }

    private int kingKingHand(int firstKing, int secondKing, int hand) {
        return weight(KING_KING_HAND
                + (firstKing * SQUARES + secondKing) * HAND_TYPE_INDEX_COUNT + hand);
    }

    private int kingKingPiece(int firstKing, int secondKing, int square, int type) {
        return weight(KING_KING_PIECE
                + ((firstKing * SQUARES + secondKing) * SQUARES + square)
                * PIECE_TYPE_INDEX_COUNT + type);
    }

    private int kingOpen(int array, int king, int square, int count) {
        return weight(KING_OPEN + array * ONE_KING_OPEN_SIZE
                + (king * SQUARES + square) * OPEN_COUNT + count);
    }

    private int effect(int offset, int king, int count) {
        int width;
        if (offset == KING_ALLY_EFFECT_9 || offset == KING_ENEMY_EFFECT_9) width = 10;
        else if (offset == KING_ALLY_EFFECT_25 || offset == KING_ENEMY_EFFECT_25) width = 26;
        else if (offset == KING_EFFECT_9_DIFF) width = 19;
        else width = 51;
        return weight(offset + king * width + count);
    }

    private int weight(int index) {
        if (index < 0 || index >= WEIGHT_COUNT) {
            throw new IllegalStateException("eval.bin weight index is outside the vector: " + index);
        }
        return weights[index];
    }

    private static int evalPieceIndex(Piece piece) {
        int raw = piece.raw();
        if (raw < 0 || raw >= EVAL_PIECE_INDEX.length || piece.isEmpty()
                || piece.type().equals(PieceType.KING)) {
            throw new IllegalArgumentException("piece has no Sunfish evaluation index: " + piece);
        }
        return EVAL_PIECE_INDEX[raw];
    }

    private static int evalPieceTypeIndex(PieceType type) {
        int raw = type.raw();
        if (raw < 0 || raw >= EVAL_PIECE_TYPE_INDEX.length || type.equals(PieceType.KING)) {
            throw new IllegalArgumentException("piece type has no Sunfish evaluation index: " + type);
        }
        return EVAL_PIECE_TYPE_INDEX[raw];
    }

    private static int neighborIndex(Square king, Square square) {
        return neighborIndexFromDifference(square.raw() - king.raw());
    }

    private static int reverseNeighborIndex(Square king, Square square) {
        return neighborIndexFromDifference(king.raw() - square.raw());
    }

    private static int neighborIndexFromDifference(int difference) {
        return switch (difference) {
            case -10 -> 0;
            case -9 -> 1;
            case -8 -> 2;
            case -1 -> 3;
            case 1 -> 4;
            case 8 -> 5;
            case 9 -> 6;
            case 10 -> 7;
            default -> throw new IllegalArgumentException("squares are not king neighbors: " + difference);
        };
    }

    private static int neighborData(int number, int type) {
        return number | type << 8;
    }

    private static int neighborNumber(int data) {
        return data & 0xff;
    }

    private static int neighborType(int data) {
        return data >>> 8;
    }

    private static final class FeatureMeta {
        private final int blackKing;
        private final int whiteKing;
        private final int[] blackNeighbors = new int[NEIGHBOR_COUNT];
        private final int[] whiteNeighbors = new int[NEIGHBOR_COUNT];
        private int blackNeighborCount;
        private int whiteNeighborCount;

        private FeatureMeta(int blackKing, int whiteKing) {
            this.blackKing = blackKing;
            this.whiteKing = whiteKing;
        }
    }
}
