package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.time.Duration;
import java.util.List;

/** Dependency-free regression tests; no Minecraft launch or evaluation data is required. */
public final class MateEngineSelfTest {
    private static final MateSearchLimits SMALL_LIMITS =
            new MateSearchLimits(Duration.ofSeconds(2), 15, 200_000);
    private static final MateSearchLimits THREE_PLY_LIMITS =
            new MateSearchLimits(Duration.ofSeconds(3), 2, 500_000);
    private static final List<String> CURATED_THREE_PLY_PUZZLES = List.of(
            "lns+R4l/1p1p5/p1pkppB1p/6p2/1R7/6P1P/P1PPnPS2/2+b1G1g2/L3K1sNL b 2GS3Pnp 51",
            "lnsG5/4g4/prpp1p1pp/1p4p2/4+B3k/2P1P4/P+b1PSP1LP/4K2SL/2G2G1r1 b SP3nl3p 71",
            "l5+R1l/4kS3/p4pnpp/2Pppb3/6p1P/P2s5/NP2+nPPR1/2+bS2GK1/L6NL b 3GSP4p 93",
            "lR5nl/5k1b1/2gp3p1/2s1p1P2/p4N2p/P3PpR2/1PPP1P2P/2G1K2s1/LN6L b GSN2Pbgs2p 83",
            "l1+R5l/2pS5/p2pp+P1pp/2k3p2/2N4P1/PP2R1P1P/2+pPP1N2/2GSG1bs1/LN1K4L b 2GSNPbp 73",
            "lnsg4l/1r1b5/p1pp1+N1+R1/4p3p/9/P3SSk2/NpPPPPg1P/2GK5/L1S4NL b 2Pbg4p 91",
            "l3k2G1/1+B4gPl/n2+Nppsp1/pP2R2bp/9/Pps1P1N1P/2GG1P3/3S5/LNK5L b R6Ps 97",
            "lnkgp1+R1l/1rs4+P1/p1ppG2p1/4N3p/3S5/P7P/2+lPP4/2G1KP3/L1S4+b1 b BN2Pgsn4p 83",
            "3g2S1l/3s2k2/3ppplpp/2p3R2/rP7/1LP1P2P1/N2P1P2P/2GSG4/3KN2NL b BG4Pbsnp 89",
            "l1G1k2nl/2Rs2+R2/pp2bp2p/4p1p2/1n1p1N2P/4P1P2/PPG1SP3/2p1G4/LN1K1s2L b 3Pbgsp 85"
    );

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
        checkingMoveAvailabilityIsDetected();
        cancellationIsReported();
        curatedThreePlyPuzzlesAreValid();
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

    private void checkingMoveAvailabilityIsDetected() {
        check(TsumeRules.hasLegalCheckingMove(
                        "4k4/9/9/9/9/9/9/9/4R4 b - 1"),
                "attacker has a legal checking rook move");
        check(!TsumeRules.hasLegalCheckingMove(
                        "4k4/9/9/9/9/9/9/9/9 b - 1"),
                "attacker with no pieces has no legal checking move");
    }

    private void cancellationIsReported() {
        MateSearchResult result = new MateEngine().search(
                "4R4/4k4/9/9/9/9/9/9/9 w - 1",
                SMALL_LIMITS,
                () -> true);
        equal(MateSearchOutcome.CANCELLED, result.outcome(), "cancellation outcome");
    }

    private void curatedThreePlyPuzzlesAreValid() {
        for (int puzzleIndex = 0; puzzleIndex < CURATED_THREE_PLY_PUZZLES.size(); puzzleIndex++) {
            Position position = Position.fromSfen(CURATED_THREE_PLY_PUZZLES.get(puzzleIndex));
            equal(Turn.BLACK, position.turn(), "curated puzzle " + (puzzleIndex + 1) + " starts with player");

            boolean foundExactThreePlySolution = false;
            boolean foundImmediateMate = false;
            for (Move firstMove : position.legalMoves()) {
                if (!position.isCheck(firstMove)) {
                    continue;
                }
                Position.Undo undo = position.makeMoveUnchecked(firstMove);
                try {
                    MateSearchResult result = new MateEngine().search(position.toSfen(), THREE_PLY_LIMITS);
                    if (result.outcome() == MateSearchOutcome.FORCED_MATE && result.matePlies() == 0) {
                        foundImmediateMate = true;
                    }
                    if (result.outcome() == MateSearchOutcome.FORCED_MATE && result.matePlies() == 2) {
                        foundExactThreePlySolution = true;
                    }
                } finally {
                    position.undoMove(undo);
                }
            }
            check(foundExactThreePlySolution,
                    "curated puzzle " + (puzzleIndex + 1) + " must have an exact three-ply solution");
            check(!foundImmediateMate,
                    "curated puzzle " + (puzzleIndex + 1) + " must not have a one-ply solution");
        }
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
