package com.github.sangeeeee.tlm_shogi.engine.search;

import com.github.sangeeeee.tlm_shogi.engine.CancellationToken;
import com.github.sangeeeee.tlm_shogi.engine.SearchLimits;
import com.github.sangeeeee.tlm_shogi.engine.SearchOutcome;
import com.github.sangeeeee.tlm_shogi.engine.SearchResult;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveGenerator;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Single-threaded iterative-deepening negamax/alpha-beta search.
 *
 * <p>This intentionally forms a small, auditable baseline: transposition
 * cutoffs, capture/promotion ordering, quiescence, repetition draws and hard
 * time/node/cancellation checks. More aggressive Sunfish pruning can be ported
 * after this baseline is covered by regression tests.</p>
 */
public final class AlphaBetaSearcher {
    private static final int MAX_PLY = 128;
    private static final int MAX_QUIESCENCE_PLY = 32;

    private final SunfishEvaluator evaluator;
    private final TranspositionTable table;
    private final SearchLimits limits;
    private final CancellationToken cancellationToken;
    private final long[] gameHistory;
    private final long[] pathHashes = new long[MAX_PLY + 1];
    private final Move[][] principalVariation = new Move[MAX_PLY + 1][MAX_PLY + 1];
    private final int[] principalVariationLength = new int[MAX_PLY + 1];

    private long nodes;
    private long startNanos;
    private long deadlineNanos;
    private StopReason stopReason = StopReason.NONE;

    public AlphaBetaSearcher(SunfishEvaluator evaluator,
                             TranspositionTable table,
                             SearchLimits limits,
                             CancellationToken cancellationToken,
                             List<Long> gameHistory) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.table = Objects.requireNonNull(table, "table");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
        Objects.requireNonNull(gameHistory, "gameHistory");
        this.gameHistory = new long[gameHistory.size()];
        for (int index = 0; index < gameHistory.size(); index++) {
            this.gameHistory[index] = Objects.requireNonNull(gameHistory.get(index), "history hash");
        }
    }

    public SearchResult search(Position position) {
        Objects.requireNonNull(position, "position");
        startNanos = System.nanoTime();
        deadlineNanos = saturatedAdd(startNanos, saturatedToNanos(limits.moveTime()));

        if (isCancellationRequested()) {
            return SearchResult.cancelled(Duration.ZERO, 0);
        }

        List<Move> rootMoves = new ArrayList<>(MoveGenerator.generateLegal(position));
        if (rootMoves.isEmpty()) {
            return new SearchResult(
                    SearchOutcome.RESIGN,
                    Optional.empty(),
                    position.inCheck() ? -SunfishScore.INFINITY : 0,
                    0,
                    nodes,
                    elapsed(),
                    List.of()
            );
        }

        Move fallback = rootMoves.get(0);
        Move completedBest = null;
        int completedScore = evaluateForSideToMove(position);
        int completedDepth = 0;
        List<String> completedPv = List.of(fallback.toSfen());
        Move previousBest = null;

        int maximumDepth = Math.min(limits.maximumDepth(), MAX_PLY - 1);
        for (int depth = 1; depth <= maximumDepth; depth++) {
            try {
                IterationResult iteration = searchRoot(position, rootMoves, depth, previousBest);
                completedBest = iteration.bestMove();
                previousBest = completedBest;
                completedScore = iteration.score();
                completedDepth = depth;
                completedPv = iteration.principalVariation();
                if (Math.abs(completedScore) >= SunfishScore.MATE) break;
            } catch (SearchStopped ignored) {
                break;
            }
        }

        if (stopReason == StopReason.CANCELLED) {
            return SearchResult.cancelled(elapsed(), nodes);
        }

        Move best = completedBest == null ? fallback : completedBest;
        return new SearchResult(
                SearchOutcome.MOVE,
                Optional.of(best.toSfen()),
                completedScore,
                completedDepth,
                nodes,
                elapsed(),
                completedPv
        );
    }

    public long nodes() {
        return nodes;
    }

    private IterationResult searchRoot(Position position, List<Move> rootMoves,
                                       int depth, Move previousBest) {
        visitNode();
        principalVariationLength[0] = 0;
        long hash = position.getHash();
        int ttSlot = table.find(hash);
        Move ttMove = ttSlot >= 0 ? table.moveAt(ttSlot) : Move.none();
        Move preferred = previousBest != null ? previousBest : ttMove;
        List<Move> ordered = orderMoves(position, rootMoves, preferred);

        int originalAlpha = -SunfishScore.INFINITY;
        int alpha = originalAlpha;
        int beta = SunfishScore.INFINITY;
        int bestScore = -SunfishScore.INFINITY;
        Move bestMove = ordered.get(0);

        for (Move move : ordered) {
            Position.Undo undo = position.makeMoveUnchecked(move);
            int score;
            try {
                score = -negamax(position, depth - 1, -beta, -alpha, 1);
            } finally {
                position.undoMove(undo);
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                updatePrincipalVariation(0, move);
            }
            if (score > alpha) alpha = score;
        }

        table.store(hash, originalAlpha, beta, bestScore, depth, 0, bestMove);
        return new IterationResult(bestMove, bestScore, extractPrincipalVariation(0));
    }

    private int negamax(Position position, int depth, int alpha, int beta, int ply) {
        if (depth <= 0) return quiescence(position, alpha, beta, ply, 0);
        visitNode();
        principalVariationLength[ply] = ply;

        if (ply >= MAX_PLY) return evaluateForSideToMove(position);
        long hash = position.getHash();
        pathHashes[ply] = hash;
        if (isFourfoldRepetition(hash, ply)) return 0;

        int originalAlpha = alpha;
        int originalBeta = beta;
        int ttSlot = table.find(hash);
        Move ttMove = Move.none();
        if (ttSlot >= 0) {
            ttMove = table.moveAt(ttSlot);
            if (table.depthAt(ttSlot) >= depth) {
                int ttScore = table.scoreAt(ttSlot, ply);
                int scoreType = table.scoreTypeAt(ttSlot);
                if (scoreType == TranspositionTable.EXACT) return ttScore;
                if (scoreType == TranspositionTable.LOWER && ttScore >= beta) return ttScore;
                if (scoreType == TranspositionTable.UPPER && ttScore <= alpha) return ttScore;
                if (scoreType == TranspositionTable.LOWER) alpha = Math.max(alpha, ttScore);
                if (scoreType == TranspositionTable.UPPER) beta = Math.min(beta, ttScore);
                if (alpha >= beta) return ttScore;
            }
        }

        List<Move> legalMoves = MoveGenerator.generateLegal(position);
        if (legalMoves.isEmpty()) {
            return position.inCheck() ? -SunfishScore.INFINITY + ply : 0;
        }

        List<Move> ordered = orderMoves(position, legalMoves, ttMove);
        int bestScore = -SunfishScore.INFINITY;
        Move bestMove = Move.none();
        for (Move move : ordered) {
            Position.Undo undo = position.makeMoveUnchecked(move);
            int score;
            try {
                score = -negamax(position, depth - 1, -beta, -alpha, ply + 1);
            } finally {
                position.undoMove(undo);
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                updatePrincipalVariation(ply, move);
            }
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }

        table.store(hash, originalAlpha, originalBeta, bestScore, depth, ply, bestMove);
        return bestScore;
    }

    private int quiescence(Position position, int alpha, int beta, int ply, int qPly) {
        visitNode();
        principalVariationLength[ply] = ply;
        if (ply >= MAX_PLY) return evaluateForSideToMove(position);

        long hash = position.getHash();
        pathHashes[ply] = hash;
        if (isFourfoldRepetition(hash, ply)) return 0;

        boolean inCheck = position.inCheck();
        int bestScore;
        if (inCheck) {
            bestScore = -SunfishScore.INFINITY;
        } else {
            bestScore = evaluateForSideToMove(position);
            if (bestScore >= beta) return bestScore;
            if (bestScore > alpha) alpha = bestScore;
            if (qPly >= MAX_QUIESCENCE_PLY) return bestScore;
        }

        List<Move> tacticalMoves = new ArrayList<>();
        for (Move move : MoveGenerator.generateLegal(position)) {
            if (inCheck || position.isCapture(move) || move.isPromotion()) tacticalMoves.add(move);
        }
        if (tacticalMoves.isEmpty()) {
            return inCheck ? -SunfishScore.INFINITY + ply : bestScore;
        }

        for (Move move : orderMoves(position, tacticalMoves, Move.none())) {
            Position.Undo undo = position.makeMoveUnchecked(move);
            int score;
            try {
                score = -quiescence(position, -beta, -alpha, ply + 1, qPly + 1);
            } finally {
                position.undoMove(undo);
            }
            if (score > bestScore) {
                bestScore = score;
                updatePrincipalVariation(ply, move);
            }
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }
        return bestScore;
    }

    private List<Move> orderMoves(Position position, List<Move> moves, Move preferred) {
        List<Move> ordered = new ArrayList<>(moves);
        ordered.sort(Comparator.comparingInt((Move move) -> moveOrderScore(position, move, preferred)).reversed());
        return ordered;
    }

    private static int moveOrderScore(Position position, Move move, Move preferred) {
        if (preferred != null && !preferred.isNone() && move.equals(preferred)) return 2_000_000;
        int score = 0;
        if (!move.isDrop()) {
            Piece moving = position.pieceAt(move.from());
            Piece captured = position.pieceAt(move.to());
            if (!captured.isEmpty()) {
                score += 1_000_000 + SunfishEvaluator.exchangeValue(captured) * 32
                        - SunfishEvaluator.materialValue(moving.type());
            }
            if (move.isPromotion()) score += 100_000 + SunfishEvaluator.promotionGain(moving) * 16;
        }
        return score;
    }

    private int evaluateForSideToMove(Position position) {
        int score = evaluator.evaluate(position);
        score = Math.max(-SunfishScore.MATE + 1, Math.min(SunfishScore.MATE - 1, score));
        return position.turn() == Turn.BLACK ? score : -score;
    }

    private void updatePrincipalVariation(int ply, Move move) {
        principalVariation[ply][ply] = move;
        int childLength = principalVariationLength[ply + 1];
        for (int index = ply + 1; index < childLength; index++) {
            principalVariation[ply][index] = principalVariation[ply + 1][index];
        }
        principalVariationLength[ply] = Math.max(ply + 1, childLength);
    }

    private List<String> extractPrincipalVariation(int ply) {
        List<String> result = new ArrayList<>();
        for (int index = ply; index < principalVariationLength[ply]; index++) {
            Move move = principalVariation[ply][index];
            if (move == null || move.isNone()) break;
            result.add(move.toSfen());
        }
        return List.copyOf(result);
    }

    private boolean isFourfoldRepetition(long hash, int ply) {
        int occurrences = 0;
        for (long historicalHash : gameHistory) {
            if (historicalHash == hash && ++occurrences >= 4) return true;
        }
        // gameHistory already contains the root position, so the search path starts at ply one.
        for (int index = 1; index <= ply; index++) {
            if (pathHashes[index] == hash && ++occurrences >= 4) return true;
        }
        return false;
    }

    private void visitNode() {
        if (isCancellationRequested()) {
            stopReason = StopReason.CANCELLED;
            throw SearchStopped.INSTANCE;
        }
        if (nodes >= limits.maximumNodes() || deadlineReached()) {
            stopReason = StopReason.LIMIT;
            throw SearchStopped.INSTANCE;
        }
        nodes++;
    }

    private boolean isCancellationRequested() {
        return cancellationToken.isCancellationRequested() || Thread.currentThread().isInterrupted();
    }

    private boolean deadlineReached() {
        return System.nanoTime() - deadlineNanos >= 0;
    }

    private Duration elapsed() {
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - startNanos));
    }

    private static long saturatedToNanos(Duration duration) {
        try {
            return duration.toNanos();
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static long saturatedAdd(long first, long second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private enum StopReason {
        NONE,
        LIMIT,
        CANCELLED
    }

    private record IterationResult(Move bestMove, int score, List<String> principalVariation) {
    }

    private static final class SearchStopped extends RuntimeException {
        private static final SearchStopped INSTANCE = new SearchStopped();

        private SearchStopped() {
            super(null, null, false, false);
        }
    }
}
