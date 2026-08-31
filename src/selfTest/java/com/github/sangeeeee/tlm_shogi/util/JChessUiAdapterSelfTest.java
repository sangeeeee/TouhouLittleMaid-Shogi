package com.github.sangeeeee.tlm_shogi.util;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.util.List;
import java.util.Optional;

/** Regression coverage for the stateless engine-to-Minecraft UI adapter. */
public final class JChessUiAdapterSelfTest {
    private int checks;

    private JChessUiAdapterSelfTest() {
    }

    public static void main(String[] args) {
        JChessUiAdapterSelfTest test = new JChessUiAdapterSelfTest();
        test.coordinatesModelsAndHandsRemainCompatible();
        test.boardMovesUseCompleteEngineLegality();
        test.dropsUseCompleteEngineLegality();
        test.checkmateAndKinglessTsumeRemainSupported();
        System.out.println("JChess UI adapter self-test passed: " + test.checks + " checks");
    }

    private void coordinatesModelsAndHandsRemainCompatible() {
        Position start = Position.startPosition();
        equal(30, JChessUiAdapter.modelIdAt(start, point(9, 1)), "white lance model id");
        equal(24, JChessUiAdapter.modelIdAt(start, point(5, 1)), "white king model id");
        equal(10, JChessUiAdapter.modelIdAt(start, point(5, 9)), "black king model id");
        equal(13, JChessUiAdapter.modelIdAt(start, point(2, 8)), "black rook model id");
        for (int point = 0; point < 81; point++) {
            equal(point, JChessUiAdapter.pointFromSquare(JChessUiAdapter.squareFromPoint(point)),
                    "point/square round trip " + point);
        }

        Position hands = Position.parse("4k4/9/9/9/9/9/9/9/4K4 b RBG2S3N4L18Pr2p 17");
        List<JChessUiAdapter.HandStack> black = JChessUiAdapter.handStacks(hands, Turn.BLACK);
        equal(7, black.size(), "seven occupied black hand stacks");
        equal(13, black.get(0).modelId(), "rook is first hand model");
        equal(14, black.get(1).modelId(), "bishop is second hand model");
        equal(18, black.get(6).count(), "pawn hand count");
        List<JChessUiAdapter.HandStack> white = JChessUiAdapter.handStacks(hands, Turn.WHITE);
        equal(2, white.size(), "two occupied white hand stacks");
        equal(27, white.get(0).modelId(), "white rook hand model");
        equal(31, white.get(1).modelId(), "white pawn hand model");
        equal(13, JChessUiAdapter.modelIdAt(hands, 81), "black hand point model");
        equal(27, JChessUiAdapter.modelIdAt(hands, 90), "white hand point model");

        Position copy = start.copy();
        JChessUiAdapter.AppliedMove opening = JChessUiAdapter.applyUsiMoveWithPoints(copy, "7g7f")
                .orElseThrow(() -> new AssertionError("copy rejects legal move"));
        checks++;
        equal(point(7, 7), opening.originPoint(), "board move retains its rendered origin");
        equal(point(7, 6), opening.destinationPoint(), "board move retains its rendered destination");
        equal(0, opening.originHandStackCount(), "board origin has no stack height");
        check(!copy.toSfen().equals(start.toSfen()), "engine copy is independent");
    }

    private void boardMovesUseCompleteEngineLegality() {
        Position start = Position.startPosition();
        int from = point(7, 7);
        int to = point(7, 6);
        Optional<Move> opening = JChessUiAdapter.legalMove(start, from, to, false);
        check(opening.isPresent(), "opening pawn move is legal");
        check(JChessUiAdapter.legalMove(start, from, to, true).isEmpty(),
                "opening pawn cannot promote");
        start.makeMoveUnchecked(opening.orElseThrow());
        equal(Turn.WHITE, start.turn(), "move changes side to move");
        equal(2, start.moveNumber(), "move increments move number");

        Position pinned = Position.parse("k3r4/9/9/9/9/9/9/4G4/4K4 b - 1");
        check(JChessUiAdapter.legalMove(pinned, point(5, 8), point(4, 8), false).isEmpty(),
                "pinned piece cannot expose its king");

        Position mandatory = Position.parse("k8/4P4/9/9/9/9/9/9/4K4 b - 1");
        int mandatoryFrom = point(5, 2);
        int mandatoryTo = point(5, 1);
        check(JChessUiAdapter.legalMove(mandatory, mandatoryFrom, mandatoryTo, false).isEmpty(),
                "unpromoted dead pawn is rejected");
        Move promoted = JChessUiAdapter.legalMove(mandatory, mandatoryFrom, mandatoryTo, true)
                .orElseThrow(() -> new AssertionError("mandatory promotion is unavailable"));
        checks++;
        mandatory.makeMoveUnchecked(promoted);
        equal(21, JChessUiAdapter.modelIdAt(mandatory, mandatoryTo), "promoted pawn model id");

        Position optional = Position.parse("k8/9/4P4/9/9/9/9/9/4K4 b - 1");
        check(JChessUiAdapter.legalMove(optional, point(5, 3), point(5, 2), true).isPresent(),
                "optional promotion is available");
        check(JChessUiAdapter.legalMove(optional, point(5, 3), point(5, 2), false).isPresent(),
                "optional non-promotion is available");

        String beforeIllegal = optional.toSfen();
        equal(-1, JChessUiAdapter.applyUsiMove(optional, "5c4c"), "illegal sideways pawn is rejected");
        equal(beforeIllegal, optional.toSfen(), "rejected move does not mutate position");
    }

    private void dropsUseCompleteEngineLegality() {
        Position drop = Position.parse("k8/9/9/9/9/9/9/9/4K4 b G 1");
        int center = point(5, 5);
        Move goldDrop = JChessUiAdapter.legalMove(drop, 81, center, false)
                .orElseThrow(() -> new AssertionError("gold drop is unavailable"));
        checks++;
        drop.makeMoveUnchecked(goldDrop);
        equal(11, JChessUiAdapter.modelIdAt(drop, center), "gold appears on board");
        check(JChessUiAdapter.handStacks(drop, Turn.BLACK).isEmpty(), "gold is removed from hand");

        Position stackedDrop = Position.parse("k8/9/9/9/9/9/9/9/4K4 b 2G 1");
        JChessUiAdapter.AppliedMove dropPoints =
                JChessUiAdapter.applyUsiMoveWithPoints(stackedDrop, "G*5e")
                        .orElseThrow(() -> new AssertionError("stacked gold drop is unavailable"));
        checks++;
        equal(81, dropPoints.originPoint(), "drop retains its original hand slot");
        equal(center, dropPoints.destinationPoint(), "drop retains its board destination");
        equal(2, dropPoints.originHandStackCount(), "drop retains its pre-move stack height");
        equal(1, JChessUiAdapter.handStacks(stackedDrop, Turn.BLACK).getFirst().count(),
                "drop leaves one gold in hand");

        Position whiteDrop = Position.parse("4k4/9/9/9/9/9/9/9/4K4 w p 1");
        Move whitePawnDrop = JChessUiAdapter.legalMove(whiteDrop, 90, center, false)
                .orElseThrow(() -> new AssertionError("white pawn drop is unavailable"));
        checks++;
        whiteDrop.makeMoveUnchecked(whitePawnDrop);
        equal(31, JChessUiAdapter.modelIdAt(whiteDrop, center), "white pawn model appears on board");

        Position nifu = Position.parse("4k4/9/9/9/9/9/4P4/9/4K4 b P 1");
        check(JChessUiAdapter.legalMove(nifu, 81, center, false).isEmpty(),
                "nifu pawn drop is rejected");

        Position pawnDropMate = Position.parse(
                "3lkn3/3l5/5G3/9/9/pppp1pppp/PPPP1PPPP/9/4K4 b 3P 1");
        check(JChessUiAdapter.legalMove(pawnDropMate, 81, point(5, 2), false).isEmpty(),
                "pawn-drop mate is rejected");

        Position capture = Position.parse("k8/9/9/9/9/9/4s4/4R4/4K4 b - 1");
        check(JChessUiAdapter.applyUsiMove(capture, "5h5g") >= 0, "capture applies");
        equal(12, JChessUiAdapter.modelIdAt(capture, 81), "captured silver hand model");
    }

    private void checkmateAndKinglessTsumeRemainSupported() {
        Position mateInOne = Position.parse("3lkl3/9/5G3/9/9/9/9/9/4K4 b R 1");
        check(JChessUiAdapter.applyUsiMove(mateInOne, "R*5b") >= 0, "mate-in-one drop applies");
        check(mateInOne.inCheck(), "mate position is check");
        check(mateInOne.isMate(), "mate position has no legal evasion");

        Position kinglessAttacker = Position.parse("4k4/9/4G4/9/9/9/9/9/9 b R 1");
        int from = point(5, 3);
        int to = point(5, 2);
        Move checkingMove = JChessUiAdapter.legalMove(kinglessAttacker, from, to, false)
                .orElseThrow(() -> new AssertionError("kingless tsume move is unavailable"));
        checks++;
        kinglessAttacker.makeMoveUnchecked(checkingMove);
        check(kinglessAttacker.inCheck(), "kingless attacker can give check");
    }

    private static int point(int file, int rank) {
        return (rank - 1) * 9 + (9 - file);
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void equal(Object expected, Object actual, String message) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
