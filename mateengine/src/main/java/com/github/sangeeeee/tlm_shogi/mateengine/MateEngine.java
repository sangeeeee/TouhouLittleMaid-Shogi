package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.CancellationToken;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveGenerator;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Defensive tsume-shogi solver.
 *
 * <p>The side to move is the defender controlled by this engine and must have
 * exactly one king. The attacker may omit its king, as is customary in mating
 * problems. Defender nodes maximize the distance to mate and attacker nodes
 * minimize it while considering only legal checking moves.</p>
 */
public final class MateEngine {
    private final Map<CacheKey, NodeResult> table = new HashMap<>();
    private MateSearchLimits limits;
    private CancellationToken cancellationToken;
    private Turn defender;
    private long nodes;
    private long startNanos;
    private long deadlineNanos;
    private int reachedPly;

    public MateSearchResult search(String sfen) {
        return search(sfen, MateSearchLimits.standard(), CancellationToken.none());
    }

    public MateSearchResult search(String sfen, MateSearchLimits limits) {
        return search(sfen, limits, CancellationToken.none());
    }

    public synchronized MateSearchResult search(String sfen, MateSearchLimits limits,
                                                CancellationToken cancellationToken) {
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(cancellationToken, "cancellationToken");

        Position position;
        try {
            position = Position.parse(sfen);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid mate-engine SFEN: " + sfen, exception);
        }
        validateRoot(position);

        this.limits = limits;
        this.cancellationToken = cancellationToken;
        this.defender = position.turn();
        this.nodes = 0;
        this.reachedPly = 0;
        this.table.clear();
        this.startNanos = System.nanoTime();
        this.deadlineNanos = saturatedAdd(startNanos, saturatedToNanos(limits.moveTime()));

        List<Move> fallbackMoves = orderedLegalMoves(position);
        Optional<String> fallback = fallbackMoves.stream().findFirst().map(Move::toSfen);
        NodeResult completedUnknown = NodeResult.unknown(
                fallbackMoves.stream().findFirst().map(List::of).orElseGet(List::of));
        try {
            for (int depth = 1; depth <= limits.maximumPly(); depth++) {
                NodeResult result = solve(position, depth, 0, new HashSet<>());
                if (result.outcome != InternalOutcome.UNKNOWN) {
                    return toPublicResult(result);
                }
                completedUnknown = result;
            }
            return toPublicResult(completedUnknown);
        } catch (SearchStopped stopped) {
            List<String> pv = completedUnknown.pv.stream().map(Move::toSfen).toList();
            return new MateSearchResult(
                    stopped.cancelled ? MateSearchOutcome.CANCELLED : MateSearchOutcome.UNKNOWN,
                    pv.stream().findFirst().or(() -> fallback),
                    -1,
                    nodes,
                    reachedPly,
                    elapsed(),
                    pv.isEmpty() ? fallback.map(List::of).orElseGet(List::of) : pv
            );
        }
    }

    private NodeResult solve(Position position, int remainingPly, int ply, Set<Long> path) {
        visitNode(ply);
        long hash = position.getHash();
        if (!path.add(hash)) {
            // A checking cycle is not a finite forced mate, so it favors the defender.
            return NodeResult.escape(List.of(), false);
        }

        CacheKey key = new CacheKey(hash, remainingPly);
        NodeResult cached = table.get(key);
        if (cached != null) {
            path.remove(hash);
            return cached;
        }

        NodeResult result;
        try {
            result = position.turn() == defender
                    ? solveDefender(position, remainingPly, ply, path)
                    : solveAttacker(position, remainingPly, ply, path);
        } finally {
            path.remove(hash);
        }

        if (result.cacheable && result.outcome != InternalOutcome.UNKNOWN) {
            table.put(key, result);
        }
        return result;
    }

    private NodeResult solveDefender(Position position, int remainingPly,
                                     int ply, Set<Long> path) {
        if (!position.inCheck(defender)) {
            return NodeResult.escape(List.of(), true);
        }

        List<Move> evasions = orderedLegalMoves(position);
        if (evasions.isEmpty()) {
            return NodeResult.mate(0, List.of(), true);
        }
        if (remainingPly == 0) {
            return NodeResult.unknown(List.of());
        }

        NodeResult longestMate = null;
        NodeResult preferredUnknown = null;
        boolean allMateCacheable = true;
        for (Move move : evasions) {
            Position.Undo undo = position.makeMoveUnchecked(move);
            NodeResult child;
            try {
                child = solve(position, remainingPly - 1, ply + 1, path);
            } finally {
                position.undoMove(undo);
            }
            NodeResult candidate = child.prepend(move);
            if (child.outcome == InternalOutcome.ESCAPE) {
                return NodeResult.escape(candidate.pv, child.cacheable);
            }
            if (child.outcome == InternalOutcome.UNKNOWN) {
                if (preferredUnknown == null || candidate.pv.size() > preferredUnknown.pv.size()) {
                    preferredUnknown = candidate;
                }
                continue;
            }
            allMateCacheable &= child.cacheable;
            if (longestMate == null || candidate.matePlies > longestMate.matePlies) {
                longestMate = candidate;
            }
        }

        if (preferredUnknown != null) {
            return NodeResult.unknown(preferredUnknown.pv);
        }
        return NodeResult.mate(longestMate.matePlies, longestMate.pv, allMateCacheable);
    }

    private NodeResult solveAttacker(Position position, int remainingPly,
                                     int ply, Set<Long> path) {
        List<Move> checks = orderedCheckingMoves(position);
        if (checks.isEmpty()) {
            return NodeResult.escape(List.of(), true);
        }
        if (remainingPly == 0) {
            return NodeResult.unknown(List.of());
        }

        NodeResult shortestMate = null;
        NodeResult preferredUnknown = null;
        boolean allEscapeCacheable = true;
        for (Move move : checks) {
            if (shortestMate != null && shortestMate.matePlies == 1) {
                break;
            }
            int childPly = remainingPly - 1;
            if (shortestMate != null) {
                // A different checking move only matters if it mates sooner.
                // Searching through D-2 child plies is sufficient to beat a
                // current D-ply line and avoids expanding irrelevant longer lines.
                childPly = Math.min(childPly, Math.max(0, shortestMate.matePlies - 2));
            }
            Position.Undo undo = position.makeMoveUnchecked(move);
            NodeResult child;
            try {
                child = solve(position, childPly, ply + 1, path);
            } finally {
                position.undoMove(undo);
            }
            NodeResult candidate = child.prepend(move);
            if (child.outcome == InternalOutcome.MATE) {
                if (shortestMate == null || candidate.matePlies < shortestMate.matePlies) {
                    shortestMate = candidate;
                    preferredUnknown = null;
                }
                continue;
            }
            if (child.outcome == InternalOutcome.UNKNOWN) {
                if (shortestMate == null
                        && (preferredUnknown == null || candidate.pv.size() < preferredUnknown.pv.size())) {
                    preferredUnknown = candidate;
                }
            } else {
                allEscapeCacheable &= child.cacheable;
            }
        }

        if (shortestMate != null) {
            return NodeResult.mate(shortestMate.matePlies, shortestMate.pv, shortestMate.cacheable);
        }
        if (preferredUnknown != null) {
            return NodeResult.unknown(preferredUnknown.pv);
        }
        return NodeResult.escape(List.of(), allEscapeCacheable);
    }

    private List<Move> orderedCheckingMoves(Position position) {
        List<Move> checks = new ArrayList<>();
        for (Move move : MoveGenerator.generateLegal(position)) {
            if (position.isCheck(move)) {
                checks.add(move);
            }
        }
        checks.sort(moveComparator(position));
        return checks;
    }

    private List<Move> orderedLegalMoves(Position position) {
        List<Move> moves = new ArrayList<>(MoveGenerator.generateLegal(position));
        moves.sort(moveComparator(position));
        return moves;
    }

    private static Comparator<Move> moveComparator(Position position) {
        return Comparator.comparingInt((Move move) -> moveOrderScore(position, move)).reversed()
                .thenComparing(Move::toSfen);
    }

    private static int moveOrderScore(Position position, Move move) {
        int score = 0;
        if (position.isCapture(move)) score += 4;
        if (move.isPromotion()) score += 2;
        if (move.isDrop()) score += 1;
        return score;
    }

    private MateSearchResult toPublicResult(NodeResult result) {
        MateSearchOutcome outcome = switch (result.outcome) {
            case ESCAPE -> MateSearchOutcome.ESCAPE;
            case MATE -> MateSearchOutcome.FORCED_MATE;
            case UNKNOWN -> MateSearchOutcome.UNKNOWN;
        };
        List<String> pv = result.pv.stream().map(Move::toSfen).toList();
        return new MateSearchResult(
                outcome,
                pv.stream().findFirst(),
                result.outcome == InternalOutcome.MATE ? result.matePlies : -1,
                nodes,
                reachedPly,
                elapsed(),
                pv
        );
    }

    private void validateRoot(Position position) {
        Turn side = position.turn();
        int ownKings = countKings(position, side);
        int opponentKings = countKings(position, side.opposite());
        if (ownKings != 1) {
            throw new IllegalArgumentException("The side to move must have exactly one king");
        }
        if (opponentKings > 1) {
            throw new IllegalArgumentException("The attacking side may have zero or one king, not " + opponentKings);
        }
        if (!position.inCheck(side)) {
            throw new IllegalArgumentException("The side to move must currently be in check");
        }
    }

    private static int countKings(Position position, Turn side) {
        Piece king = side == Turn.BLACK ? Piece.BLACK_KING : Piece.WHITE_KING;
        int count = 0;
        for (Piece piece : position.boardCopy()) {
            if (piece.equals(king)) count++;
        }
        return count;
    }

    private void visitNode(int ply) {
        reachedPly = Math.max(reachedPly, ply);
        if (cancellationToken.isCancellationRequested()) {
            throw new SearchStopped(true);
        }
        if (nodes >= limits.maximumNodes() || System.nanoTime() >= deadlineNanos) {
            throw new SearchStopped(false);
        }
        nodes++;
    }

    private Duration elapsed() {
        return Duration.ofNanos(Math.max(0, System.nanoTime() - startNanos));
    }

    private static long saturatedToNanos(Duration duration) {
        try {
            return duration.toNanos();
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static long saturatedAdd(long first, long second) {
        if (second > 0 && first > Long.MAX_VALUE - second) return Long.MAX_VALUE;
        return first + second;
    }

    private enum InternalOutcome { ESCAPE, MATE, UNKNOWN }

    private record CacheKey(long hash, int remainingPly) {
    }

    private record NodeResult(InternalOutcome outcome, int matePlies,
                              List<Move> pv, boolean cacheable) {
        private NodeResult {
            pv = List.copyOf(pv);
        }

        static NodeResult escape(List<Move> pv, boolean cacheable) {
            return new NodeResult(InternalOutcome.ESCAPE, -1, pv, cacheable);
        }

        static NodeResult mate(int matePlies, List<Move> pv, boolean cacheable) {
            return new NodeResult(InternalOutcome.MATE, matePlies, pv, cacheable);
        }

        static NodeResult unknown(List<Move> pv) {
            return new NodeResult(InternalOutcome.UNKNOWN, -1, pv, false);
        }

        NodeResult prepend(Move move) {
            List<Move> combined = new ArrayList<>(pv.size() + 1);
            combined.add(move);
            combined.addAll(pv);
            return new NodeResult(outcome,
                    outcome == InternalOutcome.MATE ? matePlies + 1 : -1,
                    combined,
                    cacheable);
        }
    }

    private static final class SearchStopped extends RuntimeException {
        private final boolean cancelled;

        private SearchStopped(boolean cancelled) {
            super(null, null, false, false);
            this.cancelled = cancelled;
        }
    }
}
