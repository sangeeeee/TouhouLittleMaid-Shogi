package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Mutable shogi position with Sunfish-compatible board and hand representation. */
public final class Position {
    public static final String START_SFEN =
            "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1";

    private static final List<PieceType> HAND_ORDER = List.of(
            PieceType.ROOK, PieceType.BISHOP, PieceType.GOLD, PieceType.SILVER,
            PieceType.KNIGHT, PieceType.LANCE, PieceType.PAWN
    );

    private final Piece[] board;
    private final Hand blackHand;
    private final Hand whiteHand;
    private Turn turn;
    private int moveNumber;

    /** Creates an empty board with black to move, matching Sunfish's default constructor. */
    public Position() {
        board = new Piece[Square.COUNT];
        Arrays.fill(board, Piece.EMPTY);
        blackHand = new Hand();
        whiteHand = new Hand();
        turn = Turn.BLACK;
        moveNumber = 1;
    }

    public Position(Position source) {
        Objects.requireNonNull(source, "source");
        board = Arrays.copyOf(source.board, source.board.length);
        blackHand = source.blackHand.copy();
        whiteHand = source.whiteHand.copy();
        turn = source.turn;
        moveNumber = source.moveNumber;
    }

    public static Position startPosition() {
        return fromSfen(START_SFEN);
    }

    /** Accepts {@code startpos}, a raw SFEN, or {@code sfen <raw SFEN>}. */
    public static Position parse(String value) {
        if (value == null) throw new IllegalArgumentException("position must not be null");
        String normalized = value.trim();
        if (normalized.equals("startpos")) return startPosition();
        if (normalized.startsWith("sfen ")) normalized = normalized.substring(5).trim();
        return fromSfen(normalized);
    }

    public static Position fromSfen(String sfen) {
        Objects.requireNonNull(sfen, "sfen");
        String[] fields = sfen.trim().split("\\s+");
        if (fields.length != 4) throw new IllegalArgumentException("SFEN must contain four fields: " + sfen);

        Position position = new Position();
        parseBoard(fields[0], position.board);
        position.turn = switch (fields[1]) {
            case "b" -> Turn.BLACK;
            case "w" -> Turn.WHITE;
            default -> throw new IllegalArgumentException("invalid SFEN turn: " + fields[1]);
        };
        parseHands(fields[2], position.blackHand, position.whiteHand);
        try {
            position.moveNumber = Integer.parseInt(fields[3]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid SFEN move number: " + fields[3], exception);
        }
        if (position.moveNumber < 1) throw new IllegalArgumentException("SFEN move number must be positive");
        return position;
    }

    public Position copy() { return new Position(this); }
    public Turn turn() { return turn; }
    public Turn getTurn() { return turn; }
    public int moveNumber() { return moveNumber; }

    public Piece pieceAt(Square square) {
        return board[requireSquare(square)];
    }

    public Piece getPieceOnBoard(Square square) { return pieceAt(square); }

    public Piece[] boardCopy() { return Arrays.copyOf(board, board.length); }

    public Hand hand(Turn side) {
        return (side == Turn.BLACK ? blackHand : whiteHand).copy();
    }

    public int handCount(Turn side, PieceType pieceType) {
        return handInternal(side).get(pieceType);
    }

    public int getBlackHandPieceCount(PieceType pieceType) { return blackHand.get(pieceType); }
    public int getWhiteHandPieceCount(PieceType pieceType) { return whiteHand.get(pieceType); }
    public Hand getBlackHand() { return blackHand.copy(); }
    public Hand getWhiteHand() { return whiteHand.copy(); }

    public Square kingSquare(Turn side) {
        Piece king = side == Turn.BLACK ? Piece.BLACK_KING : Piece.WHITE_KING;
        for (int raw = 0; raw < board.length; raw++) {
            if (board[raw].equals(king)) return new Square(raw);
        }
        return Square.invalid();
    }

    public Square getBlackKingSquare() { return kingSquare(Turn.BLACK); }
    public Square getWhiteKingSquare() { return kingSquare(Turn.WHITE); }

    public Bitboard occupied() {
        Bitboard result = Bitboard.zero();
        for (int raw = 0; raw < board.length; raw++) {
            if (!board[raw].isEmpty()) result.set(new Square(raw));
        }
        return result;
    }

    public Bitboard occupied(Turn side) {
        Bitboard result = Bitboard.zero();
        for (int raw = 0; raw < board.length; raw++) {
            Piece piece = board[raw];
            if (!piece.isEmpty() && piece.turn() == side) result.set(new Square(raw));
        }
        return result;
    }

    public Bitboard getBOccupiedBitboard() { return occupied(Turn.BLACK); }
    public Bitboard getWOccupiedBitboard() { return occupied(Turn.WHITE); }

    public RotatedBitboard get90RotatedBitboard() { return rotated(Rotation.NINETY); }
    public RotatedBitboard getRight45RotatedBitboard() { return rotated(Rotation.RIGHT_45); }
    public RotatedBitboard getLeft45RotatedBitboard() { return rotated(Rotation.LEFT_45); }

    public boolean hasPawnInFile(Turn side, int file) {
        if (!Square.isValidFile(file)) throw new IllegalArgumentException("invalid file: " + file);
        Piece pawn = side == Turn.BLACK ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
        for (int rank = 1; rank <= 9; rank++) {
            if (pieceAt(Square.of(file, rank)).equals(pawn)) return true;
        }
        return false;
    }

    public boolean hasBlackPawnInFile(int file) { return hasPawnInFile(Turn.BLACK, file); }
    public boolean hasWhitePawnInFile(int file) { return hasPawnInFile(Turn.WHITE, file); }

    public boolean isSquareAttacked(Square target, Turn attacker) {
        requireSquare(target);
        Bitboard occupied = occupied();
        for (int raw = 0; raw < board.length; raw++) {
            Piece piece = board[raw];
            if (!piece.isEmpty() && piece.turn() == attacker
                    && MoveTables.attacks(piece, new Square(raw), occupied).contains(target)) {
                return true;
            }
        }
        return false;
    }

    public boolean inCheck() { return inCheck(turn); }

    public boolean inCheck(Turn side) {
        Square king = kingSquare(side);
        return king.isStrictValid() && isSquareAttacked(king, side.opposite());
    }

    public boolean isCapture(Move move) {
        Objects.requireNonNull(move, "move");
        return !move.isDrop() && !pieceAt(move.to()).isEmpty();
    }

    public boolean isCheck(Move move) {
        Position next = copy();
        next.makeMoveUnchecked(move);
        return next.inCheck(next.turn);
    }

    public boolean validateMove(Move move) {
        return MoveGenerator.isLegal(this, move);
    }

    public Undo makeMove(Move move) {
        if (!validateMove(move)) throw new IllegalArgumentException("illegal move: " + move.toSfen());
        return makeMoveUnchecked(move);
    }

    Undo makeMoveUnchecked(Move move) {
        Objects.requireNonNull(move, "move");
        Turn movingTurn = turn;
        int previousMoveNumber = moveNumber;
        if (move.isDrop()) {
            PieceType type = move.droppingPieceType().hand();
            Square to = move.to();
            if (!pieceAt(to).isEmpty()) throw new IllegalStateException("drop destination is occupied: " + to);
            handInternal(movingTurn).decrementUnpromoted(type);
            board[to.raw()] = movingTurn == Turn.BLACK ? type.black() : type.white();
            turn = turn.opposite();
            moveNumber++;
            return new Undo(move, Piece.EMPTY, Piece.EMPTY, movingTurn, previousMoveNumber);
        }

        Square from = move.from();
        Square to = move.to();
        Piece movingPiece = pieceAt(from);
        Piece captured = pieceAt(to);
        board[from.raw()] = Piece.EMPTY;
        board[to.raw()] = move.isPromotion() ? movingPiece.promote() : movingPiece;
        if (!captured.isEmpty()) handInternal(movingTurn).increment(captured.hand());
        turn = turn.opposite();
        moveNumber++;
        return new Undo(move, movingPiece, captured, movingTurn, previousMoveNumber);
    }

    public void undoMove(Undo undo) {
        Objects.requireNonNull(undo, "undo");
        Move move = undo.move();
        turn = undo.previousTurn();
        moveNumber = undo.previousMoveNumber();
        if (move.isDrop()) {
            board[move.to().raw()] = Piece.EMPTY;
            handInternal(turn).incrementUnpromoted(move.droppingPieceType());
            return;
        }
        board[move.from().raw()] = undo.movedPiece();
        board[move.to().raw()] = undo.capturedPiece();
        if (!undo.capturedPiece().isEmpty()) handInternal(turn).decrement(undo.capturedPiece().hand());
    }

    public void doNullMove() {
        turn = turn.opposite();
        moveNumber++;
    }

    public void undoNullMove() {
        turn = turn.opposite();
        moveNumber = Math.max(1, moveNumber - 1);
    }

    public List<Move> legalMoves() { return MoveGenerator.generateLegal(this); }
    public boolean isMate() { return inCheck() && legalMoves().isEmpty(); }

    public long hash() {
        long hash = 0xcbf29ce484222325L;
        for (int raw = 0; raw < board.length; raw++) {
            hash ^= (long) (board[raw].raw() + 1) * 83 + raw;
            hash *= 0x100000001b3L;
        }
        for (PieceType type : HAND_ORDER) {
            hash ^= ((long) blackHand.get(type) << 32) ^ whiteHand.get(type) ^ type.raw();
            hash *= 0x100000001b3L;
        }
        return turn == Turn.BLACK ? hash ^ 0x9e3779b97f4a7c15L : hash;
    }

    public String toSfen() {
        StringBuilder result = new StringBuilder(100);
        for (int rank = 1; rank <= 9; rank++) {
            if (rank > 1) result.append('/');
            int empty = 0;
            for (int file = 9; file >= 1; file--) {
                Piece piece = pieceAt(Square.of(file, rank));
                if (piece.isEmpty()) {
                    empty++;
                } else {
                    if (empty > 0) result.append(empty);
                    empty = 0;
                    result.append(piece.toSfen());
                }
            }
            if (empty > 0) result.append(empty);
        }
        result.append(turn == Turn.BLACK ? " b " : " w ");
        appendHands(result);
        result.append(' ').append(moveNumber);
        return result.toString();
    }

    public String toStringSfen() { return toSfen(); }

    @Override
    public String toString() { return toSfen(); }

    @Override
    public boolean equals(Object other) {
        return other instanceof Position position
                && Arrays.equals(board, position.board)
                && blackHand.equals(position.blackHand)
                && whiteHand.equals(position.whiteHand)
                && turn == position.turn
                && moveNumber == position.moveNumber;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(board);
        result = 31 * result + blackHand.hashCode();
        result = 31 * result + whiteHand.hashCode();
        result = 31 * result + turn.hashCode();
        return 31 * result + moveNumber;
    }

    private void appendHands(StringBuilder result) {
        if (blackHand.isEmpty() && whiteHand.isEmpty()) {
            result.append('-');
            return;
        }
        appendHand(result, blackHand, true);
        appendHand(result, whiteHand, false);
    }

    private static void appendHand(StringBuilder result, Hand hand, boolean black) {
        for (PieceType type : HAND_ORDER) {
            int count = hand.get(type);
            if (count == 0) continue;
            if (count > 1) result.append(count);
            Piece piece = black ? type.black() : type.white();
            result.append(piece.toSfen());
        }
    }

    private RotatedBitboard rotated(Rotation rotation) {
        RotatedBitboard result = RotatedBitboard.zero();
        for (int raw = 0; raw < board.length; raw++) {
            if (board[raw].isEmpty()) continue;
            Square square = new Square(raw);
            result.set(switch (rotation) {
                case NINETY -> square.rotate90();
                case RIGHT_45 -> square.rotateRight45();
                case LEFT_45 -> square.rotateLeft45();
            });
        }
        return result;
    }

    private Hand handInternal(Turn side) { return side == Turn.BLACK ? blackHand : whiteHand; }

    private static int requireSquare(Square square) {
        Objects.requireNonNull(square, "square");
        if (!square.isStrictValid()) throw new IllegalArgumentException("invalid square: " + square.raw());
        return square.raw();
    }

    private static void parseBoard(String field, Piece[] board) {
        String[] ranks = field.split("/", -1);
        if (ranks.length != 9) throw new IllegalArgumentException("SFEN board must have nine ranks: " + field);
        Arrays.fill(board, Piece.EMPTY);
        for (int rankIndex = 0; rankIndex < ranks.length; rankIndex++) {
            int file = 9;
            String row = ranks[rankIndex];
            for (int index = 0; index < row.length();) {
                char current = row.charAt(index);
                if (current >= '1' && current <= '9') {
                    file -= current - '0';
                    index++;
                    if (file < 0) throw new IllegalArgumentException("too many files in SFEN rank: " + row);
                    continue;
                }
                boolean promoted = current == '+';
                int symbolIndex = promoted ? index + 1 : index;
                if (symbolIndex >= row.length() || file < 1) {
                    throw new IllegalArgumentException("invalid SFEN rank: " + row);
                }
                String symbol = row.substring(index, symbolIndex + 1);
                Piece piece = Piece.parseSfen(symbol);
                if (piece.isEmpty()) throw new IllegalArgumentException("invalid SFEN piece: " + symbol);
                board[Square.of(file, rankIndex + 1).raw()] = piece;
                file--;
                index = symbolIndex + 1;
            }
            if (file != 0) throw new IllegalArgumentException("not enough files in SFEN rank: " + row);
        }
    }

    private static void parseHands(String field, Hand blackHand, Hand whiteHand) {
        if (field.equals("-")) return;
        for (int index = 0; index < field.length();) {
            int count = 0;
            while (index < field.length() && Character.isDigit(field.charAt(index))) {
                count = Math.addExact(Math.multiplyExact(count, 10), field.charAt(index++) - '0');
            }
            if (count == 0) count = 1;
            if (index >= field.length()) throw new IllegalArgumentException("missing SFEN hand piece: " + field);
            String symbol = field.substring(index, index + 1);
            Piece piece = Piece.parseSfen(symbol);
            if (piece.isEmpty() || piece.type().raw() >= PieceType.HAND_END) {
                throw new IllegalArgumentException("invalid SFEN hand piece: " + symbol);
            }
            (piece.isBlack() ? blackHand : whiteHand).set(piece.type(), count);
            index++;
        }
    }

    public record Undo(Move move, Piece movedPiece, Piece capturedPiece,
                       Turn previousTurn, int previousMoveNumber) {
        public Undo {
            Objects.requireNonNull(move, "move");
            Objects.requireNonNull(movedPiece, "movedPiece");
            Objects.requireNonNull(capturedPiece, "capturedPiece");
            Objects.requireNonNull(previousTurn, "previousTurn");
        }
    }

    private enum Rotation { NINETY, RIGHT_45, LEFT_45 }
}
