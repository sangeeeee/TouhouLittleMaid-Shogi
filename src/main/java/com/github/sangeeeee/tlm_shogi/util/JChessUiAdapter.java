package com.github.sangeeeee.tlm_shogi.util;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Stateless conversions between the engine position and Minecraft's board UI. */
public final class JChessUiAdapter {
    private static final List<PieceType> HAND_ORDER = List.of(
            PieceType.ROOK, PieceType.BISHOP, PieceType.GOLD, PieceType.SILVER,
            PieceType.KNIGHT, PieceType.LANCE, PieceType.PAWN
    );

    private JChessUiAdapter() {
    }

    public static boolean isBoardPoint(int point) {
        return point >= 0 && point < Square.COUNT;
    }

    public static Square squareFromPoint(int point) {
        if (!isBoardPoint(point)) {
            throw new IllegalArgumentException("board point is outside 0..80: " + point);
        }
        return Square.of(9 - point % 9, point / 9 + 1);
    }

    public static Square squareFromGrid(int column, int row) {
        if (column < 0 || column >= 9 || row < 0 || row >= 9) {
            throw new IllegalArgumentException("board grid is outside 9x9: " + column + "," + row);
        }
        return squareFromPoint(row * 9 + column);
    }

    public static int pointFromSquare(Square square) {
        if (square == null || !square.isStrictValid()) {
            return -1;
        }
        return (square.rank() - 1) * 9 + (9 - square.file());
    }

    public static int modelIdAt(Position position, int point) {
        return modelId(pieceAtPoint(position, point));
    }

    public static Piece pieceAtPoint(Position position, int point) {
        if (position == null) {
            return Piece.EMPTY;
        }
        if (isBoardPoint(point)) {
            return position.pieceAt(squareFromPoint(point));
        }

        Turn side;
        int index;
        if (point >= 81 && point <= 89) {
            side = Turn.BLACK;
            index = point - 81;
        } else if (point >= 90 && point <= 98) {
            side = Turn.WHITE;
            index = point - 90;
        } else {
            return Piece.EMPTY;
        }
        List<HandStack> stacks = handStacks(position, side);
        if (index < 0 || index >= stacks.size()) {
            return Piece.EMPTY;
        }
        PieceType type = stacks.get(index).type();
        return side == Turn.BLACK ? type.black() : type.white();
    }

    public static int modelId(Piece piece) {
        if (piece == null || piece.isEmpty()) {
            return 0;
        }
        return switch (piece.raw()) {
            case 7 -> 10;
            case 4 -> 11;
            case 3 -> 12;
            case 6 -> 13;
            case 5 -> 14;
            case 2 -> 15;
            case 1 -> 16;
            case 0 -> 17;
            case 11 -> 18;
            case 10 -> 19;
            case 9 -> 20;
            case 8 -> 21;
            case 14 -> 22;
            case 13 -> 23;
            case 23 -> 24;
            case 20 -> 25;
            case 19 -> 26;
            case 22 -> 27;
            case 21 -> 28;
            case 18 -> 29;
            case 17 -> 30;
            case 16 -> 31;
            case 27 -> 32;
            case 26 -> 33;
            case 25 -> 34;
            case 24 -> 35;
            case 30 -> 36;
            case 29 -> 37;
            default -> throw new IllegalArgumentException("unsupported engine piece: " + piece.raw());
        };
    }

    public static List<HandStack> handStacks(Position position, Turn side) {
        List<HandStack> stacks = new ArrayList<>(PieceType.HAND_END);
        for (PieceType type : HAND_ORDER) {
            int count = position.handCount(side, type);
            if (count > 0) {
                Piece piece = side == Turn.BLACK ? type.black() : type.white();
                stacks.add(new HandStack(type, count, modelId(piece)));
            }
        }
        return List.copyOf(stacks);
    }

    public static int handIndex(Position position, Turn side, PieceType wanted) {
        List<HandStack> stacks = handStacks(position, side);
        PieceType handType = wanted.hand();
        for (int index = 0; index < stacks.size(); index++) {
            if (stacks.get(index).type().equals(handType)) {
                return index;
            }
        }
        return -1;
    }

    /** Returns the rendered source location before a legal move mutates the position. */
    public static MoveOrigin moveOrigin(Position position, Move move) {
        if (position == null || move == null || move.isNone()) {
            throw new IllegalArgumentException("position and move must describe a real move");
        }
        if (!move.isDrop()) {
            return new MoveOrigin(pointFromSquare(move.from()), 0);
        }

        Turn side = position.turn();
        List<HandStack> stacks = handStacks(position, side);
        int index = handIndex(position, side, move.droppingPieceType());
        if (index < 0) {
            throw new IllegalArgumentException("dropped piece is not present in the moving side's hand");
        }
        int point = (side == Turn.BLACK ? 81 : 90) + index;
        return new MoveOrigin(point, stacks.get(index).count());
    }

    public static Optional<Move> moveFromPoints(Position position, int fromPoint,
                                                int toPoint, boolean promote) {
        if (position == null || !isBoardPoint(toPoint)) {
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
        List<HandStack> stacks = handStacks(position, handSide);
        if (handIndex < 0 || handIndex >= stacks.size()) {
            return Optional.empty();
        }
        return Optional.of(Move.drop(stacks.get(handIndex).type(), to));
    }

    public static Optional<Move> legalMove(Position position, int fromPoint,
                                           int toPoint, boolean promote) {
        try {
            return moveFromPoints(position, fromPoint, toPoint, promote)
                    .filter(position::validateMove);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /** Applies one legal USI move and returns its UI destination point, or {@code -1}. */
    public static int applyUsiMove(Position position, String moveText) {
        return applyUsiMoveWithPoints(position, moveText)
                .map(AppliedMove::destinationPoint)
                .orElse(-1);
    }

    /** Applies one legal USI move while retaining its rendered source and destination. */
    public static Optional<AppliedMove> applyUsiMoveWithPoints(Position position, String moveText) {
        if (position == null || moveText == null) {
            return Optional.empty();
        }
        try {
            Optional<Move> parsed = Move.parseSfen(moveText.trim())
                    .filter(move -> !move.isNone())
                    .filter(position::validateMove);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            Move move = parsed.orElseThrow();
            MoveOrigin origin = moveOrigin(position, move);
            position.makeMoveUnchecked(move);
            return Optional.of(new AppliedMove(
                    origin.point(), pointFromSquare(move.to()), origin.handStackCount()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public record MoveOrigin(int point, int handStackCount) {
        public MoveOrigin {
            if (point < 0 || point > 98 || handStackCount < 0) {
                throw new IllegalArgumentException("invalid rendered move origin");
            }
        }
    }

    public record AppliedMove(int originPoint, int destinationPoint, int originHandStackCount) {
        public AppliedMove {
            if (originPoint < 0 || originPoint > 98
                    || !isBoardPoint(destinationPoint) || originHandStackCount < 0) {
                throw new IllegalArgumentException("invalid rendered move points");
            }
        }
    }

    public record HandStack(PieceType type, int count, int modelId) {
        public HandStack {
            if (type == null || type.raw() < PieceType.PAWN.raw() || type.raw() >= PieceType.HAND_END) {
                throw new IllegalArgumentException("invalid hand piece type: " + type);
            }
            if (count < 1) {
                throw new IllegalArgumentException("hand stack must contain a piece");
            }
        }
    }
}
