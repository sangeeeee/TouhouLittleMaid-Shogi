package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converts between the board UI's historical point/model identifiers and the
 * Java engine's Sunfish-compatible core types.
 */
final class JChessEngineAdapter {
    private static final List<PieceType> HAND_ORDER = List.of(
            PieceType.ROOK, PieceType.BISHOP, PieceType.GOLD, PieceType.SILVER,
            PieceType.KNIGHT, PieceType.LANCE, PieceType.PAWN
    );

    private JChessEngineAdapter() {
    }

    static Square squareFromPoint(int point) {
        if (!isBoardPoint(point)) {
            throw new IllegalArgumentException("board point is outside 0..80: " + point);
        }
        int rank = point / 9 + 1;
        int file = 9 - point % 9;
        return Square.of(file, rank);
    }

    static int pointFromSquare(Square square) {
        if (square == null || !square.isStrictValid()) {
            return -1;
        }
        return (square.rank() - 1) * 9 + (9 - square.file());
    }

    static boolean isBoardPoint(int point) {
        return point >= 0 && point < Square.COUNT;
    }

    static int modelId(Piece piece) {
        if (piece == null || piece.isEmpty()) {
            return 0;
        }
        return switch (piece.raw()) {
            case 7 -> 10;   // black king
            case 4 -> 11;   // black gold
            case 3 -> 12;   // black silver
            case 6 -> 13;   // black rook
            case 5 -> 14;   // black bishop
            case 2 -> 15;   // black knight
            case 1 -> 16;   // black lance
            case 0 -> 17;   // black pawn
            case 11 -> 18;  // black promoted silver
            case 10 -> 19;  // black promoted knight
            case 9 -> 20;   // black promoted lance
            case 8 -> 21;   // black tokin
            case 14 -> 22;  // black dragon
            case 13 -> 23;  // black horse
            case 23 -> 24;  // white king
            case 20 -> 25;  // white gold
            case 19 -> 26;  // white silver
            case 22 -> 27;  // white rook
            case 21 -> 28;  // white bishop
            case 18 -> 29;  // white knight
            case 17 -> 30;  // white lance
            case 16 -> 31;  // white pawn
            case 27 -> 32;  // white promoted silver
            case 26 -> 33;  // white promoted knight
            case 25 -> 34;  // white promoted lance
            case 24 -> 35;  // white tokin
            case 30 -> 36;  // white dragon
            case 29 -> 37;  // white horse
            default -> throw new IllegalArgumentException("unsupported engine piece: " + piece.raw());
        };
    }

    static Optional<Piece> pieceFromModelId(int modelId) {
        Piece piece = switch (modelId) {
            case 0 -> Piece.EMPTY;
            case 10 -> Piece.BLACK_KING;
            case 11 -> Piece.BLACK_GOLD;
            case 12 -> Piece.BLACK_SILVER;
            case 13 -> Piece.BLACK_ROOK;
            case 14 -> Piece.BLACK_BISHOP;
            case 15 -> Piece.BLACK_KNIGHT;
            case 16 -> Piece.BLACK_LANCE;
            case 17 -> Piece.BLACK_PAWN;
            case 18 -> Piece.BLACK_PRO_SILVER;
            case 19 -> Piece.BLACK_PRO_KNIGHT;
            case 20 -> Piece.BLACK_PRO_LANCE;
            case 21 -> Piece.BLACK_TOKIN;
            case 22 -> Piece.BLACK_DRAGON;
            case 23 -> Piece.BLACK_HORSE;
            case 24 -> Piece.WHITE_KING;
            case 25 -> Piece.WHITE_GOLD;
            case 26 -> Piece.WHITE_SILVER;
            case 27 -> Piece.WHITE_ROOK;
            case 28 -> Piece.WHITE_BISHOP;
            case 29 -> Piece.WHITE_KNIGHT;
            case 30 -> Piece.WHITE_LANCE;
            case 31 -> Piece.WHITE_PAWN;
            case 32 -> Piece.WHITE_PRO_SILVER;
            case 33 -> Piece.WHITE_PRO_KNIGHT;
            case 34 -> Piece.WHITE_PRO_LANCE;
            case 35 -> Piece.WHITE_TOKIN;
            case 36 -> Piece.WHITE_DRAGON;
            case 37 -> Piece.WHITE_HORSE;
            default -> null;
        };
        return Optional.ofNullable(piece);
    }

    static Optional<PieceType> handTypeFromModelId(int modelId) {
        return pieceFromModelId(modelId)
                .filter(piece -> !piece.isEmpty() && !piece.type().equals(PieceType.KING))
                .map(piece -> piece.type().hand())
                .filter(type -> type.raw() >= PieceType.PAWN.raw() && type.raw() < PieceType.HAND_END);
    }

    static List<int[]> handEntries(com.github.sangeeeee.tlm_shogi.engine.core.Position position, Turn side) {
        List<int[]> entries = new ArrayList<>(PieceType.HAND_END);
        for (PieceType type : HAND_ORDER) {
            int count = position.handCount(side, type);
            if (count > 0) {
                Piece piece = side == Turn.BLACK ? type.black() : type.white();
                entries.add(new int[]{count, modelId(piece)});
            }
        }
        return entries;
    }

    static Optional<Move> moveFromPoints(com.github.sangeeeee.tlm_shogi.engine.core.Position position,
                                         int fromPoint, int toPoint, boolean promote) {
        if (!isBoardPoint(toPoint)) {
            return Optional.empty();
        }
        Square to = squareFromPoint(toPoint);
        if (isBoardPoint(fromPoint)) {
            return Optional.of(Move.board(squareFromPoint(fromPoint), to, promote));
        }
        if (promote) {
            return Optional.empty();
        }

        Turn handSide;
        int handIndex;
        if (fromPoint >= 81 && fromPoint <= 89) {
            handSide = Turn.BLACK;
            handIndex = fromPoint - 81;
        } else if (fromPoint >= 90 && fromPoint <= 98) {
            handSide = Turn.WHITE;
            handIndex = fromPoint - 90;
        } else {
            return Optional.empty();
        }
        if (position.turn() != handSide) {
            return Optional.empty();
        }

        List<PieceType> occupiedTypes = occupiedHandTypes(position, handSide);
        if (handIndex < 0 || handIndex >= occupiedTypes.size()) {
            return Optional.empty();
        }
        return Optional.of(Move.drop(occupiedTypes.get(handIndex), to));
    }

    static int handIndex(com.github.sangeeeee.tlm_shogi.engine.core.Position position,
                         Turn side, PieceType wanted) {
        List<PieceType> occupiedTypes = occupiedHandTypes(position, side);
        PieceType handType = wanted.hand();
        for (int index = 0; index < occupiedTypes.size(); index++) {
            if (occupiedTypes.get(index).equals(handType)) {
                return index;
            }
        }
        return -1;
    }

    private static List<PieceType> occupiedHandTypes(
            com.github.sangeeeee.tlm_shogi.engine.core.Position position, Turn side) {
        List<PieceType> result = new ArrayList<>(PieceType.HAND_END);
        for (PieceType type : HAND_ORDER) {
            if (position.handCount(side, type) > 0) {
                result.add(type);
            }
        }
        return result;
    }
}
