package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveGenerator;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.util.Optional;

/**
 * Deprecated source-compatibility helpers for callers of the former rule
 * implementation. No shogi rules are implemented here; every query delegates
 * to the engine core.
 */
@Deprecated
public final class PositionUtil {
    private PositionUtil() {
    }

    public static int convertCapturedPiece(int capturedPieceId, boolean capturerIsBlack) {
        Optional<Piece> captured = JChessEngineAdapter.pieceFromModelId(capturedPieceId);
        if (captured.isEmpty() || captured.orElseThrow().isEmpty()) {
            return capturedPieceId == 0 ? 0 : -1;
        }
        Piece piece = captured.orElseThrow();
        if (piece.type().equals(PieceType.KING)) {
            return -1;
        }
        Piece converted = capturerIsBlack ? piece.hand().black() : piece.hand().white();
        return JChessEngineAdapter.modelId(converted);
    }

    public static Position fromSfen(String usi) {
        Position position = new Position();
        position.applyUSI(usi);
        return position;
    }

    public static String toSfen(Position position) {
        return position.toUSI();
    }

    public static int promotePiece(int pieceId) {
        Optional<Piece> source = JChessEngineAdapter.pieceFromModelId(pieceId);
        if (source.isEmpty() || source.orElseThrow().isEmpty() || !source.orElseThrow().isPromotable()) {
            return -1;
        }
        return JChessEngineAdapter.modelId(source.orElseThrow().promote());
    }

    public static int pieceCharToId(char symbol, boolean isBlack) {
        PieceType type = switch (Character.toUpperCase(symbol)) {
            case 'P' -> PieceType.PAWN;
            case 'L' -> PieceType.LANCE;
            case 'N' -> PieceType.KNIGHT;
            case 'S' -> PieceType.SILVER;
            case 'G' -> PieceType.GOLD;
            case 'B' -> PieceType.BISHOP;
            case 'R' -> PieceType.ROOK;
            default -> null;
        };
        if (type == null) {
            return -1;
        }
        return JChessEngineAdapter.modelId(isBlack ? type.black() : type.white());
    }

    public static int usiToPos(String usi) {
        return Square.parseSfen(usi).map(JChessEngineAdapter::pointFromSquare).orElse(-1);
    }

    /** Retains the old pseudo-legal geometry semantics without duplicating movement tables. */
    public static boolean isValidBoardMove(int pieceId, int fromPos, int toPos, Position position) {
        if (position == null || !JChessEngineAdapter.isBoardPoint(fromPos)
                || !JChessEngineAdapter.isBoardPoint(toPos)
                || position.getPieceByPointNum(fromPos) != pieceId) {
            return false;
        }
        com.github.sangeeeee.tlm_shogi.engine.core.Position engine = position.engineCopy();
        Move normal = Move.board(JChessEngineAdapter.squareFromPoint(fromPos),
                JChessEngineAdapter.squareFromPoint(toPos), false);
        if (MoveGenerator.isPseudoLegal(engine, normal)) {
            return true;
        }
        Move promoted = Move.board(normal.from(), normal.to(), true);
        return MoveGenerator.isPseudoLegal(engine, promoted);
    }

    /** Retains the old helper signature while using complete engine drop legality. */
    public static boolean isValidDrop(int pieceId, int toPos, Position position) {
        if (position == null || !JChessEngineAdapter.isBoardPoint(toPos)) {
            return false;
        }
        Optional<Piece> piece = JChessEngineAdapter.pieceFromModelId(pieceId);
        Optional<PieceType> handType = JChessEngineAdapter.handTypeFromModelId(pieceId);
        if (piece.isEmpty() || handType.isEmpty()) {
            return false;
        }
        Turn side = piece.orElseThrow().isBlack() ? Turn.BLACK : Turn.WHITE;
        com.github.sangeeeee.tlm_shogi.engine.core.Position engine = position.engineCopy();
        if (engine.turn() != side) {
            return false;
        }
        int index = JChessEngineAdapter.handIndex(engine, side, handType.orElseThrow());
        if (index < 0) {
            return false;
        }
        int fromPos = (side == Turn.BLACK ? 81 : 90) + index;
        return position.isLegalMove(fromPos, toPos);
    }
}
