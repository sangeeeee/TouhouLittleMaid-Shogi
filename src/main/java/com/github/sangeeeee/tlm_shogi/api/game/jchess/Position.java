package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveTables;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.util.List;
import java.util.Optional;

/**
 * Compatibility view used by the Minecraft board UI.
 *
 * <p>The authoritative position and every rule decision live in the pure-Java
 * engine. This class only translates the UI's historical board points and
 * model identifiers so existing renderers, packets and block-entity storage do
 * not need to change at once.</p>
 */
public final class Position {
    private com.github.sangeeeee.tlm_shogi.engine.core.Position delegate;

    /** Creates the same empty, black-to-move position as the former board class. */
    public Position() {
        this(new com.github.sangeeeee.tlm_shogi.engine.core.Position());
    }

    private Position(com.github.sangeeeee.tlm_shogi.engine.core.Position delegate) {
        this.delegate = delegate;
    }

    public int getPieceAt(int column, int row) {
        if (row < 0 || row >= 9 || column < 0 || column >= 9) {
            return 0;
        }
        int point = row * 9 + column;
        return JChessEngineAdapter.modelId(delegate.pieceAt(JChessEngineAdapter.squareFromPoint(point)));
    }

    public int getPieceByPointNum(int point) {
        if (point < 0) {
            return 0;
        }
        if (JChessEngineAdapter.isBoardPoint(point)) {
            return JChessEngineAdapter.modelId(delegate.pieceAt(JChessEngineAdapter.squareFromPoint(point)));
        }
        if (point >= 81 && point <= 89) {
            return handPieceAt(getBlackHand(), point - 81);
        }
        if (point >= 90 && point <= 98) {
            return handPieceAt(getWhiteHand(), point - 90);
        }
        return 0;
    }

    /** Retained for source compatibility; move construction requires a position and promotion choice. */
    @Deprecated
    public static String getMove(int preClick, int nowClick) {
        return "1a1b";
    }

    public List<int[]> getBlackHand() {
        return JChessEngineAdapter.handEntries(delegate, Turn.BLACK);
    }

    public List<int[]> getWhiteHand() {
        return JChessEngineAdapter.handEntries(delegate, Turn.WHITE);
    }

    public char getTurn() {
        return delegate.turn() == Turn.BLACK ? 'b' : 'w';
    }

    public boolean isPlayer() {
        return delegate.turn() == Turn.BLACK;
    }

    public int getMoveNumber() {
        return delegate.moveNumber();
    }

    public void applyUSI(String usi) {
        delegate = com.github.sangeeeee.tlm_shogi.engine.core.Position.parse(usi);
    }

    public String toUSI() {
        return delegate.toSfen();
    }

    /** Applies one fully legal UI move and returns its destination point, or {@code -1}. */
    public int move(int fromPos, int toPos, boolean promote) {
        Optional<Move> candidate = JChessEngineAdapter.moveFromPoints(delegate, fromPos, toPos, promote);
        if (candidate.isEmpty()) {
            return -1;
        }
        try {
            delegate.makeMove(candidate.orElseThrow());
            return toPos;
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    /** Parses and applies one fully legal USI/SFEN move, returning its destination point or {@code -1}. */
    public int makeMove(String usiMove) {
        if (usiMove == null) {
            return -1;
        }
        Optional<Move> candidate = Move.parseSfen(usiMove.trim());
        if (candidate.isEmpty() || candidate.orElseThrow().isNone()) {
            return -1;
        }
        try {
            Move move = candidate.orElseThrow();
            delegate.makeMove(move);
            return JChessEngineAdapter.pointFromSquare(move.to());
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    /** Returns whether the promoted version of this board move is fully legal. */
    public boolean canPromote(int fromPos, int toPos) {
        return isLegalVariant(fromPos, toPos, true);
    }

    /** Returns whether promotion is the only fully legal version of this board move. */
    public boolean mustPromote(int fromPos, int toPos) {
        return isLegalVariant(fromPos, toPos, true) && !isLegalVariant(fromPos, toPos, false);
    }

    /**
     * Returns whether at least one legal promotion choice exists for a board
     * move, or whether the selected hand piece can legally be dropped there.
     */
    public boolean isLegalMove(int fromPos, int toPos) {
        if (isLegalVariant(fromPos, toPos, false)) {
            return true;
        }
        return JChessEngineAdapter.isBoardPoint(fromPos) && isLegalVariant(fromPos, toPos, true);
    }

    /** Returns whether the side to move is in check. */
    public boolean isCheck() {
        return delegate.inCheck();
    }

    /**
     * Returns whether the requested king is attacked. Missing kings remain
     * invalid for ordinary-game compatibility; tsume callers explicitly allow
     * the attacker's omitted king.
     */
    public boolean isKingUnderAttack(boolean checkBlackKing) {
        Turn side = checkBlackKing ? Turn.BLACK : Turn.WHITE;
        Square king = delegate.kingSquare(side);
        return !king.isStrictValid() || delegate.inCheck(side);
    }

    /** Tests the supplied piece's attack geometry against this position's occupancy. */
    public static boolean canPieceAttack(int pieceId, int fromPos, int toPos, Position position) {
        if (position == null || !JChessEngineAdapter.isBoardPoint(fromPos)
                || !JChessEngineAdapter.isBoardPoint(toPos)) {
            return false;
        }
        Optional<Piece> piece = JChessEngineAdapter.pieceFromModelId(pieceId);
        if (piece.isEmpty() || piece.orElseThrow().isEmpty()) {
            return false;
        }
        Square from = JChessEngineAdapter.squareFromPoint(fromPos);
        Square to = JChessEngineAdapter.squareFromPoint(toPos);
        return MoveTables.attacks(piece.orElseThrow(), from, position.delegate.occupied()).contains(to);
    }

    public boolean isMate() {
        return delegate.isMate();
    }

    /** Counts four occurrences of the same board, hands and side to move, ignoring the SFEN move number. */
    public boolean isRepetition(List<String> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }
        long currentHash = delegate.getHash();
        int occurrences = 0;
        for (String past : history) {
            try {
                if (com.github.sangeeeee.tlm_shogi.engine.core.Position.parse(past).getHash() == currentHash
                        && ++occurrences >= 4) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Old or corrupt history entries must not break the board entity.
            }
        }
        return false;
    }

    public Position deepCopy() {
        return new Position(delegate.copy());
    }

    com.github.sangeeeee.tlm_shogi.engine.core.Position engineCopy() {
        return delegate.copy();
    }

    private boolean isLegalVariant(int fromPos, int toPos, boolean promote) {
        Optional<Move> candidate = JChessEngineAdapter.moveFromPoints(delegate, fromPos, toPos, promote);
        if (candidate.isEmpty()) {
            return false;
        }
        try {
            return delegate.validateMove(candidate.orElseThrow());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static int handPieceAt(List<int[]> hand, int index) {
        if (index < 0 || index >= hand.size()) {
            return 0;
        }
        return hand.get(index)[1];
    }
}
