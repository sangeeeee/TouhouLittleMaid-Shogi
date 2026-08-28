package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.time.Duration;

/** Dependency-free regression tests; no Minecraft launch or evaluation data is required. */
public final class MateEngineSelfTest {
    private static final MateSearchLimits SMALL_LIMITS =
            new MateSearchLimits(Duration.ofSeconds(2), 15, 200_000);

    private int checks;

    public static void main(String[] args) {
        MateEngineSelfTest test = new MateEngineSelfTest();
        test.run();
        System.out.println("Mate-engine self-test passed: " + test.checks + " checks");
    }

    private void run() {
        missingAttackerKingCanEscape();
        eitherColorCanBeTheDefender();
        attackerKingMayBePresent();
        alreadyMatedPositionNeedsNoMove();
        longestForcedDefenseIsSelected();
        shallowSearchReportsUnknown();
        missingDefenderKingIsRejected();
        duplicateAttackerKingsAreRejected();
        nonCheckPositionIsRejected();
        cancellationIsReported();
    }

    private void eitherColorCanBeTheDefender() {
        String sfen = "9/9/9/9/9/9/9/4K4/4r4 b - 1";
        MateSearchResult result = new MateEngine().search(sfen, SMALL_LIMITS);
        equal(MateSearchOutcome.ESCAPE, result.outcome(), "black defender with missing white king");
        check(result.bestMove().isPresent(), "black defender receives a move");
    }

    private void attackerKingMayBePresent() {
        String sfen = "4R4/4k4/9/9/9/9/9/9/K8 w - 1";
        MateSearchResult result = new MateEngine().search(sfen, SMALL_LIMITS);
        equal(MateSearchOutcome.ESCAPE, result.outcome(), "attacker may retain one king");
    }

    private void missingAttackerKingCanEscape() {
        String sfen = "4R4/4k4/9/9/9/9/9/9/9 w - 1";
        MateSearchResult result = new MateEngine().search(sfen, SMALL_LIMITS);
        equal(MateSearchOutcome.ESCAPE, result.outcome(), "capturing an unprotected checker escapes");
        String move = result.bestMove().orElseThrow();
        Position position = Position.fromSfen(sfen);
        Move parsed = Move.parseSfen(move).orElseThrow();
        check(position.validateMove(parsed), "returned escape must be legal");
        position.makeMove(parsed);
        check(!position.inCheck(), "returned escape must answer the check");
    }

    private void alreadyMatedPositionNeedsNoMove() {
        String sfen = "4k4/4G4/4R4/9/9/9/9/9/9 w - 1";
        MateSearchResult result = new MateEngine().search(sfen, SMALL_LIMITS);
        equal(MateSearchOutcome.FORCED_MATE, result.outcome(), "terminal mate outcome");
        equal(0, result.matePlies(), "terminal mate distance");
        check(result.bestMove().isEmpty(), "mated position has no defense");
    }

    private void longestForcedDefenseIsSelected() {
        // White has two legal evasions. 2b1c is mated in two plies, whereas
        // 2b2a survives four; the engine must maximize that finite distance.
        String sfen = "4B2R1/3G3k1/7S1/6L2/9/9/9/9/9 w - 1";
        MateSearchResult result = new MateEngine().search(sfen, SMALL_LIMITS);
        equal(MateSearchOutcome.FORCED_MATE, result.outcome(), "forced-mate fixture");
        equal("2b2a", result.bestMove().orElseThrow(), "longest defense");
        equal(4, result.matePlies(), "longest mate distance");
        equal(4, result.principalVariation().size(), "complete longest line");
        assertMatingPrincipalVariation(sfen, result);
    }

    private void shallowSearchReportsUnknown() {
        String sfen = "4B2R1/3G3k1/7S1/6L2/9/9/9/9/9 w - 1";
        MateSearchLimits shallow = new MateSearchLimits(Duration.ofSeconds(1), 1, 10_000);
        MateSearchResult result = new MateEngine().search(sfen, shallow);
        equal(MateSearchOutcome.UNKNOWN, result.outcome(), "depth-limited result must not claim proof");
        check(result.bestMove().isPresent(), "unknown result keeps a legal fallback");
    }

    private void missingDefenderKingIsRejected() {
        expect(IllegalArgumentException.class, () -> new MateEngine().search(
                "9/9/4r4/9/9/9/9/9/9 b - 1", SMALL_LIMITS));
    }

    private void duplicateAttackerKingsAreRejected() {
        expect(IllegalArgumentException.class, () -> new MateEngine().search(
                "4R4/4k4/9/9/9/9/9/9/K7K w - 1", SMALL_LIMITS));
    }

    private void nonCheckPositionIsRejected() {
        expect(IllegalArgumentException.class, () -> new MateEngine().search(
                "4k4/9/9/9/9/9/9/9/9 w - 1", SMALL_LIMITS));
    }

    private void cancellationIsReported() {
        MateSearchResult result = new MateEngine().search(
                "4R4/4k4/9/9/9/9/9/9/9 w - 1",
                SMALL_LIMITS,
                () -> true);
        equal(MateSearchOutcome.CANCELLED, result.outcome(), "cancellation outcome");
    }

    private void assertMatingPrincipalVariation(String sfen, MateSearchResult result) {
        Position position = Position.fromSfen(sfen);
        Turn defender = position.turn();
        for (int index = 0; index < result.principalVariation().size(); index++) {
            Move move = Move.parseSfen(result.principalVariation().get(index)).orElseThrow();
            check(position.validateMove(move), "PV move " + index + " must be legal");
            position.makeMove(move);
            if ((index & 1) == 1) {
                check(position.inCheck(defender), "every attacker PV move must give check");
            }
        }
        check(position.inCheck(defender), "PV must end with the defender in check");
        check(position.legalMoves().isEmpty(), "PV must end in checkmate");
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void equal(Object expected, Object actual, String message) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable throwable) {
            if (type.isInstance(throwable)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + throwable, throwable);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
