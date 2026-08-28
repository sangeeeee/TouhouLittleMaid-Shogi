package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Complete legal shogi move generation, including drops and the pawn-drop-mate rule. */
public final class MoveGenerator {
    private static final List<PieceType> HAND_TYPES = List.of(
            PieceType.PAWN, PieceType.LANCE, PieceType.KNIGHT, PieceType.SILVER,
            PieceType.GOLD, PieceType.BISHOP, PieceType.ROOK
    );

    private MoveGenerator() {
    }

    public static List<Move> generateLegal(Position position) {
        Objects.requireNonNull(position, "position");
        return Collections.unmodifiableList(generateLegalInternal(position, true, false));
    }

    public static List<Move> generateCaptures(Position position) {
        List<Move> result = new ArrayList<>();
        for (Move move : generateLegal(position)) {
            if (position.isCapture(move)) result.add(move);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Move> generateQuiets(Position position) {
        List<Move> result = new ArrayList<>();
        for (Move move : generateLegal(position)) {
            if (!position.isCapture(move)) result.add(move);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Move> generateEvasions(Position position) {
        return position.inCheck() ? generateLegal(position) : List.of();
    }

    public static boolean isLegal(Position position, Move move) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(move, "move");
        if (!isPseudoLegal(position, move)) return false;
        Turn movingSide = position.turn();
        Position next = position.copy();
        next.makeMoveUnchecked(move);
        if (next.inCheck(movingSide)) return false;
        return !isIllegalPawnDropMate(move, next);
    }

    public static boolean isPseudoLegal(Position position, Move move) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(move, "move");
        if (move.isNone() || !move.to().isStrictValid()) return false;
        Turn side = position.turn();

        if (move.isDrop()) {
            if (move.isPromotion()) return false;
            PieceType type = move.droppingPieceType();
            if (type.raw() < PieceType.PAWN.raw() || type.raw() >= PieceType.HAND_END
                    || position.handCount(side, type) == 0 || !position.pieceAt(move.to()).isEmpty()) {
                return false;
            }
            if (!canExistUnpromoted(type, move.to(), side)) return false;
            return !type.equals(PieceType.PAWN) || !position.hasPawnInFile(side, move.to().file());
        }

        if (!move.from().isStrictValid()) return false;
        Piece piece = position.pieceAt(move.from());
        Piece destination = position.pieceAt(move.to());
        if (piece.isEmpty() || piece.turn() != side) return false;
        if (!destination.isEmpty() && (destination.turn() == side || destination.type().equals(PieceType.KING))) {
            return false;
        }
        if (!MoveTables.attacks(piece, move.from(), position.occupied()).contains(move.to())) return false;

        boolean canPromote = piece.isPromotable()
                && (move.from().isPromotable(side) || move.to().isPromotable(side));
        if (move.isPromotion() && !canPromote) return false;
        return move.isPromotion() || canExistUnpromoted(piece.type(), move.to(), side);
    }

    /** Counts leaf nodes and is intended for rule regression tests rather than timed search. */
    public static long perft(Position position, int depth) {
        Objects.requireNonNull(position, "position");
        if (depth < 0) throw new IllegalArgumentException("depth must not be negative");
        if (depth == 0) return 1;
        long nodes = 0;
        for (Move move : generateLegal(position)) {
            Position.Undo undo = position.makeMoveUnchecked(move);
            nodes = Math.addExact(nodes, perft(position, depth - 1));
            position.undoMove(undo);
        }
        return nodes;
    }

    private static List<Move> generateLegalInternal(Position position, boolean enforcePawnDropMate,
                                                    boolean stopAfterFirst) {
        Turn movingSide = position.turn();
        List<Move> legal = new ArrayList<>();
        for (Move move : generatePseudoLegal(position)) {
            Position next = position.copy();
            next.makeMoveUnchecked(move);
            if (next.inCheck(movingSide)) continue;
            if (enforcePawnDropMate && isIllegalPawnDropMate(move, next)) continue;
            legal.add(move);
            if (stopAfterFirst) return legal;
        }
        return legal;
    }

    private static boolean isIllegalPawnDropMate(Move move, Position afterMove) {
        if (!move.isDrop() || !move.droppingPieceType().equals(PieceType.PAWN) || !afterMove.inCheck()) {
            return false;
        }
        return generateLegalInternal(afterMove, true, true).isEmpty();
    }

    private static List<Move> generatePseudoLegal(Position position) {
        List<Move> moves = new ArrayList<>(256);
        Turn side = position.turn();
        Bitboard occupied = position.occupied();
        Bitboard friendly = position.occupied(side);

        for (int raw = 0; raw < Square.COUNT; raw++) {
            Square from = new Square(raw);
            Piece piece = position.pieceAt(from);
            if (piece.isEmpty() || piece.turn() != side) continue;

            Bitboard destinations = MoveTables.attacks(piece, from, occupied).and(friendly.not());
            for (Square to = destinations.pickForward(); to.isStrictValid(); to = destinations.pickForward()) {
                Piece captured = position.pieceAt(to);
                if (!captured.isEmpty() && captured.type().equals(PieceType.KING)) continue;
                addBoardMoves(moves, piece, from, to, side);
            }
        }

        for (PieceType type : HAND_TYPES) {
            if (position.handCount(side, type) == 0) continue;
            for (int raw = 0; raw < Square.COUNT; raw++) {
                Square to = new Square(raw);
                if (!position.pieceAt(to).isEmpty() || !canExistUnpromoted(type, to, side)) continue;
                if (type.equals(PieceType.PAWN) && position.hasPawnInFile(side, to.file())) continue;
                moves.add(Move.drop(type, to));
            }
        }
        return moves;
    }

    private static void addBoardMoves(List<Move> moves, Piece piece, Square from, Square to, Turn side) {
        boolean canPromote = piece.isPromotable() && (from.isPromotable(side) || to.isPromotable(side));
        boolean mandatory = !canExistUnpromoted(piece.type(), to, side);
        if (canPromote) moves.add(Move.board(from, to, true));
        if (!mandatory) moves.add(Move.board(from, to, false));
    }

    private static boolean canExistUnpromoted(PieceType type, Square square, Turn side) {
        return switch (type.raw()) {
            case 0, 1 -> square.isPawnMovable(side);
            case 2 -> square.isKnightMovable(side);
            default -> true;
        };
    }
}
