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
    private final Bitboard[] pieceBitboards;
    private final Bitboard[] occupiedBySide;
    private RotatedBitboard rotated90;
    private RotatedBitboard rotatedRight45;
    private RotatedBitboard rotatedLeft45;
    private Square blackKingSquare;
    private Square whiteKingSquare;
    private Turn turn;
    private int moveNumber;
    private long boardHash;
    private long handHash;

    /** Creates an empty board with black to move, matching Sunfish's default constructor. */
    public Position() {
        board = new Piece[Square.COUNT];
        Arrays.fill(board, Piece.EMPTY);
        blackHand = new Hand();
        whiteHand = new Hand();
        pieceBitboards = new Bitboard[Piece.END];
        occupiedBySide = new Bitboard[] {Bitboard.zero(), Bitboard.zero()};
        initializeEmptyCaches();
        turn = Turn.BLACK;
        moveNumber = 1;
    }

    public Position(Position source) {
        Objects.requireNonNull(source, "source");
        board = Arrays.copyOf(source.board, source.board.length);
        blackHand = source.blackHand.copy();
        whiteHand = source.whiteHand.copy();
        pieceBitboards = copyBitboards(source.pieceBitboards);
        occupiedBySide = copyBitboards(source.occupiedBySide);
        rotated90 = new RotatedBitboard(source.rotated90);
        rotatedRight45 = new RotatedBitboard(source.rotatedRight45);
        rotatedLeft45 = new RotatedBitboard(source.rotatedLeft45);
        blackKingSquare = source.blackKingSquare;
        whiteKingSquare = source.whiteKingSquare;
        turn = source.turn;
        moveNumber = source.moveNumber;
        boardHash = source.boardHash;
        handHash = source.handHash;
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
        position.rebuildCaches();
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
        return side == Turn.BLACK ? blackKingSquare : whiteKingSquare;
    }

    public Square getBlackKingSquare() { return kingSquare(Turn.BLACK); }
    public Square getWhiteKingSquare() { return kingSquare(Turn.WHITE); }

    public Bitboard occupied() {
        return occupiedBySide[0].or(occupiedBySide[1]);
    }

    public Bitboard occupied(Turn side) {
        return occupiedBySide[sideIndex(side)].copy();
    }

    public Bitboard getBOccupiedBitboard() { return occupied(Turn.BLACK); }
    public Bitboard getWOccupiedBitboard() { return occupied(Turn.WHITE); }

    public RotatedBitboard get90RotatedBitboard() { return new RotatedBitboard(rotated90); }
    public RotatedBitboard getRight45RotatedBitboard() { return new RotatedBitboard(rotatedRight45); }
    public RotatedBitboard getLeft45RotatedBitboard() { return new RotatedBitboard(rotatedLeft45); }

    public Bitboard pieceBitboard(Piece piece) {
        Objects.requireNonNull(piece, "piece");
        if (piece.isEmpty() || piece.raw() >= Piece.END) {
            throw new IllegalArgumentException("invalid piece bitboard: " + piece.raw());
        }
        return pieceBitboards[piece.raw()].copy();
    }

    public boolean hasPawnInFile(Turn side, int file) {
        if (!Square.isValidFile(file)) throw new IllegalArgumentException("invalid file: " + file);
        Piece pawn = side == Turn.BLACK ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
        return pieceBitboards[pawn.raw()].containsAnyOnFile(file);
    }

    public boolean hasBlackPawnInFile(int file) { return hasPawnInFile(Turn.BLACK, file); }
    public boolean hasWhitePawnInFile(int file) { return hasPawnInFile(Turn.WHITE, file); }

    public boolean isSquareAttacked(Square target, Turn attacker) {
        requireSquare(target);
        Bitboard occupied = occupied();
        Bitboard sources = occupiedBySide[sideIndex(attacker)].copy();
        for (Square from = sources.pickForward(); from.isStrictValid(); from = sources.pickForward()) {
            Piece piece = board[from.raw()];
            if (MoveTables.attacks(piece, from, occupied).contains(target)) {
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
        Undo undo = makeMoveUnchecked(move);
        try {
            return inCheck(turn);
        } finally {
            undoMove(undo);
        }
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
        long previousBoardHash = boardHash;
        long previousHandHash = handHash;
        if (move.isDrop()) {
            PieceType type = move.droppingPieceType().hand();
            Square to = move.to();
            if (!pieceAt(to).isEmpty()) throw new IllegalStateException("drop destination is occupied: " + to);
            handInternal(movingTurn).decrementUnpromoted(type);
            handHash -= Zobrist.hand(movingTurn, type);
            putPiece(to, movingTurn == Turn.BLACK ? type.black() : type.white());
            turn = turn.opposite();
            moveNumber++;
            return new Undo(move, Piece.EMPTY, Piece.EMPTY, movingTurn, previousMoveNumber,
                    previousBoardHash, previousHandHash);
        }

        Square from = move.from();
        Square to = move.to();
        Piece movingPiece = takePiece(from);
        Piece captured = pieceAt(to);
        if (!captured.isEmpty()) {
            takePiece(to);
            handInternal(movingTurn).increment(captured.hand());
            handHash += Zobrist.hand(movingTurn, captured.hand());
        }
        putPiece(to, move.isPromotion() ? movingPiece.promote() : movingPiece);
        turn = turn.opposite();
        moveNumber++;
        return new Undo(move, movingPiece, captured, movingTurn, previousMoveNumber,
                previousBoardHash, previousHandHash);
    }

    public void undoMove(Undo undo) {
        Objects.requireNonNull(undo, "undo");
        Move move = undo.move();
        turn = undo.previousTurn();
        moveNumber = undo.previousMoveNumber();
        if (move.isDrop()) {
            takePiece(move.to());
            handInternal(turn).incrementUnpromoted(move.droppingPieceType());
            boardHash = undo.previousBoardHash();
            handHash = undo.previousHandHash();
            return;
        }
        takePiece(move.to());
        putPiece(move.from(), undo.movedPiece());
        if (!undo.capturedPiece().isEmpty()) {
            putPiece(move.to(), undo.capturedPiece());
            handInternal(turn).decrement(undo.capturedPiece().hand());
        }
        boardHash = undo.previousBoardHash();
        handHash = undo.previousHandHash();
    }

    public void doNullMove() {
        turn = turn.opposite();
    }

    public void undoNullMove() {
        turn = turn.opposite();
    }

    public List<Move> legalMoves() { return MoveGenerator.generateLegal(this); }
    public boolean isMate() { return inCheck() && legalMoves().isEmpty(); }

    public long hash() { return getHash(); }
    public long getHash() { return boardHash ^ handHash ^ getTurnHash(); }
    public long getBoardHash() { return boardHash; }
    public long getHandHash() { return handHash; }
    public long getTurnHash() { return Zobrist.turn(turn); }

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

    /** Rebuilds every cache independently and compares it with the incremental state. */
    public boolean verifyIncrementalState() {
        Position rebuilt = new Position(this);
        rebuilt.rebuildCaches();
        return Arrays.equals(pieceBitboards, rebuilt.pieceBitboards)
                && Arrays.equals(occupiedBySide, rebuilt.occupiedBySide)
                && rotated90.equals(rebuilt.rotated90)
                && rotatedRight45.equals(rebuilt.rotatedRight45)
                && rotatedLeft45.equals(rebuilt.rotatedLeft45)
                && blackKingSquare.equals(rebuilt.blackKingSquare)
                && whiteKingSquare.equals(rebuilt.whiteKingSquare)
                && boardHash == rebuilt.boardHash
                && handHash == rebuilt.handHash;
    }

    private Hand handInternal(Turn side) { return side == Turn.BLACK ? blackHand : whiteHand; }

    private void initializeEmptyCaches() {
        for (int raw = 0; raw < pieceBitboards.length; raw++) pieceBitboards[raw] = Bitboard.zero();
        occupiedBySide[0] = Bitboard.zero();
        occupiedBySide[1] = Bitboard.zero();
        rotated90 = RotatedBitboard.zero();
        rotatedRight45 = RotatedBitboard.zero();
        rotatedLeft45 = RotatedBitboard.zero();
        blackKingSquare = Square.invalid();
        whiteKingSquare = Square.invalid();
        boardHash = 0;
        handHash = 0;
    }

    private void rebuildCaches() {
        initializeEmptyCaches();
        for (int raw = 0; raw < board.length; raw++) {
            Piece piece = board[raw];
            if (!piece.isEmpty()) addToCaches(new Square(raw), piece);
        }
        for (PieceType type : HAND_ORDER) {
            handHash += Zobrist.blackHand(type) * blackHand.get(type);
            handHash += Zobrist.whiteHand(type) * whiteHand.get(type);
        }
    }

    private void putPiece(Square square, Piece piece) {
        int raw = requireSquare(square);
        Objects.requireNonNull(piece, "piece");
        if (piece.isEmpty() || piece.raw() >= Piece.END) throw new IllegalArgumentException("invalid piece");
        if (!board[raw].isEmpty()) throw new IllegalStateException("square is already occupied: " + square);
        board[raw] = piece;
        addToCaches(square, piece);
    }

    private Piece takePiece(Square square) {
        int raw = requireSquare(square);
        Piece piece = board[raw];
        if (piece.isEmpty()) throw new IllegalStateException("square is empty: " + square);
        removeFromCaches(square, piece);
        board[raw] = Piece.EMPTY;
        return piece;
    }

    private void addToCaches(Square square, Piece piece) {
        pieceBitboards[piece.raw()].set(square);
        occupiedBySide[sideIndex(piece.turn())].set(square);
        setRotated(square);
        if (piece.equals(Piece.BLACK_KING)) blackKingSquare = square;
        if (piece.equals(Piece.WHITE_KING)) whiteKingSquare = square;
        boardHash ^= Zobrist.board(square, piece);
    }

    private void removeFromCaches(Square square, Piece piece) {
        pieceBitboards[piece.raw()].unset(square);
        occupiedBySide[sideIndex(piece.turn())].unset(square);
        unsetRotated(square);
        if (piece.equals(Piece.BLACK_KING)) blackKingSquare = Square.invalid();
        if (piece.equals(Piece.WHITE_KING)) whiteKingSquare = Square.invalid();
        boardHash ^= Zobrist.board(square, piece);
    }

    private void setRotated(Square square) {
        RotatedSquare r90 = square.rotate90();
        RotatedSquare right45 = square.rotateRight45();
        RotatedSquare left45 = square.rotateLeft45();
        if (r90.raw() != 0) rotated90.set(r90);
        if (right45.raw() != 0) rotatedRight45.set(right45);
        if (left45.raw() != 0) rotatedLeft45.set(left45);
    }

    private void unsetRotated(Square square) {
        RotatedSquare r90 = square.rotate90();
        RotatedSquare right45 = square.rotateRight45();
        RotatedSquare left45 = square.rotateLeft45();
        if (r90.raw() != 0) rotated90.unset(r90);
        if (right45.raw() != 0) rotatedRight45.unset(right45);
        if (left45.raw() != 0) rotatedLeft45.unset(left45);
    }

    private static Bitboard[] copyBitboards(Bitboard[] source) {
        Bitboard[] result = new Bitboard[source.length];
        for (int index = 0; index < source.length; index++) result[index] = source[index].copy();
        return result;
    }

    private static int sideIndex(Turn side) { return side == Turn.BLACK ? 0 : 1; }

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
                       Turn previousTurn, int previousMoveNumber,
                       long previousBoardHash, long previousHandHash) {
        public Undo {
            Objects.requireNonNull(move, "move");
            Objects.requireNonNull(movedPiece, "movedPiece");
            Objects.requireNonNull(capturedPiece, "capturedPiece");
            Objects.requireNonNull(previousTurn, "previousTurn");
        }
    }
}
