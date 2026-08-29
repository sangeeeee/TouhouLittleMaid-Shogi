package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import java.util.List;

/** Regression coverage for the Minecraft-facing engine compatibility shell. */
public final class JChessPositionCompatibilitySelfTest {
    private int checks;

    private JChessPositionCompatibilitySelfTest() {
    }

    public static void main(String[] args) {
        JChessPositionCompatibilitySelfTest test = new JChessPositionCompatibilitySelfTest();
        test.sfenAndModelIdsRemainCompatible();
        test.boardMovesUseCompleteEngineLegality();
        test.dropsAndHandsUseCompleteEngineLegality();
        test.checkmateAndKinglessTsumeRemainSupported();
        test.repetitionIgnoresTheMoveNumber();
        System.out.println("JChess compatibility self-test passed: " + test.checks + " checks");
    }

    private void sfenAndModelIdsRemainCompatible() {
        Position start = position("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1");
        equal("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1",
                start.toUSI(), "start SFEN round trip");
        equal(30, start.getPieceByPointNum(point(9, 1)), "white lance model id");
        equal(24, start.getPieceByPointNum(point(5, 1)), "white king model id");
        equal(10, start.getPieceByPointNum(point(5, 9)), "black king model id");
        equal(13, start.getPieceByPointNum(point(2, 8)), "black rook model id");

        Position hands = position("4k4/9/9/9/9/9/9/9/4K4 b RBG2S3N4L18Pr2p 17");
        List<int[]> black = hands.getBlackHand();
        equal(7, black.size(), "seven occupied black hand stacks");
        equal(13, black.get(0)[1], "rook is first hand model");
        equal(14, black.get(1)[1], "bishop is second hand model");
        equal(18, black.get(6)[0], "pawn hand count");
        List<int[]> white = hands.getWhiteHand();
        equal(2, white.size(), "two occupied white hand stacks");
        equal(27, white.get(0)[1], "white rook hand model");
        equal(31, white.get(1)[1], "white pawn hand model");

        Position copy = start.deepCopy();
        check(copy.makeMove("7g7f") >= 0, "deep copy accepts legal move");
        check(!copy.toUSI().equals(start.toUSI()), "deep copy is independent");
    }

    private void boardMovesUseCompleteEngineLegality() {
        Position start = position("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1");
        int from = point(7, 7);
        int to = point(7, 6);
        check(start.isLegalMove(from, to), "opening pawn move is legal");
        check(!start.canPromote(from, to), "opening pawn cannot promote");
        equal(to, start.move(from, to, false), "UI move returns destination point");
        equal('w', start.getTurn(), "move changes side to move");
        equal(2, start.getMoveNumber(), "move increments move number");

        Position pinned = position("k3r4/9/9/9/9/9/9/4G4/4K4 b - 1");
        check(!pinned.isLegalMove(point(5, 8), point(4, 8)),
                "pinned piece cannot expose its king");

        Position mandatory = position("k8/4P4/9/9/9/9/9/9/4K4 b - 1");
        int mandatoryFrom = point(5, 2);
        int mandatoryTo = point(5, 1);
        check(mandatory.isLegalMove(mandatoryFrom, mandatoryTo), "mandatory promotion move is selectable");
        check(mandatory.canPromote(mandatoryFrom, mandatoryTo), "mandatory promotion can promote");
        check(mandatory.mustPromote(mandatoryFrom, mandatoryTo), "last-rank pawn must promote");
        equal(-1, mandatory.move(mandatoryFrom, mandatoryTo, false), "unpromoted dead pawn is rejected");
        equal(mandatoryTo, mandatory.move(mandatoryFrom, mandatoryTo, true), "mandatory promotion applies");
        equal(21, mandatory.getPieceByPointNum(mandatoryTo), "promoted pawn model id");

        Position optional = position("k8/9/4P4/9/9/9/9/9/4K4 b - 1");
        check(optional.canPromote(point(5, 3), point(5, 2)), "optional promotion is available");
        check(!optional.mustPromote(point(5, 3), point(5, 2)), "optional promotion is not forced");

        String beforeIllegal = optional.toUSI();
        equal(-1, optional.makeMove("5c4c"), "illegal sideways pawn is rejected");
        equal(beforeIllegal, optional.toUSI(), "rejected move does not mutate position");
    }

    private void dropsAndHandsUseCompleteEngineLegality() {
        Position drop = position("k8/9/9/9/9/9/9/9/4K4 b G 1");
        equal(11, drop.getPieceByPointNum(81), "black hand slot exposes gold model");
        int center = point(5, 5);
        check(drop.isLegalMove(81, center), "gold drop is legal");
        equal(center, drop.move(81, center, false), "gold drop applies");
        equal(11, drop.getPieceByPointNum(center), "gold appears on board");
        check(drop.getBlackHand().isEmpty(), "gold is removed from hand");

        Position whiteDrop = position("4k4/9/9/9/9/9/9/9/4K4 w p 1");
        equal(31, whiteDrop.getPieceByPointNum(90), "white hand slot exposes pawn model");
        check(whiteDrop.isLegalMove(90, center), "white hand slots use the same engine path");
        equal(center, whiteDrop.move(90, center, false), "white pawn drop applies");
        equal(31, whiteDrop.getPieceByPointNum(center), "white pawn model appears on board");

        Position nifu = position("4k4/9/9/9/9/9/4P4/9/4K4 b P 1");
        check(!nifu.isLegalMove(81, center), "nifu pawn drop is rejected");

        Position pawnDropMate = position("3lkn3/3l5/5G3/9/9/pppp1pppp/PPPP1PPPP/9/4K4 b 3P 1");
        check(!pawnDropMate.isLegalMove(81, point(5, 2)), "pawn-drop mate is rejected");

        Position capture = position("k8/9/9/9/9/9/4s4/4R4/4K4 b - 1");
        check(capture.makeMove("5h5g") >= 0, "capture applies");
        equal(12, capture.getPieceByPointNum(81), "captured silver uses black hand model");
    }

    private void checkmateAndKinglessTsumeRemainSupported() {
        Position mateInOne = position("3lkl3/9/5G3/9/9/9/9/9/4K4 b R 1");
        check(mateInOne.makeMove("R*5b") >= 0, "mate-in-one drop applies");
        check(mateInOne.isCheck(), "mate position is check");
        check(mateInOne.isMate(), "mate position has no legal evasion");

        Position kinglessAttacker = position("4k4/9/4G4/9/9/9/9/9/9 b R 1");
        int from = point(5, 3);
        int to = point(5, 2);
        check(kinglessAttacker.isLegalMove(from, to), "tsume attacker may omit its king");
        equal(to, kinglessAttacker.move(from, to, false), "kingless tsume move applies");
        check(kinglessAttacker.isCheck(), "kingless attacker can give check");
    }

    private void repetitionIgnoresTheMoveNumber() {
        Position position = position("4k4/9/9/9/9/9/9/9/4K4 b - 13");
        List<String> history = List.of(
                "4k4/9/9/9/9/9/9/9/4K4 b - 1",
                "4k4/9/9/9/9/9/9/9/4K4 b - 5",
                "4k4/9/9/9/9/9/9/9/4K4 b - 9",
                "4k4/9/9/9/9/9/9/9/4K4 b - 13"
        );
        check(position.isRepetition(history), "fourfold repetition ignores SFEN move number");
        check(!position.isRepetition(history.subList(0, 3)), "three occurrences are not repetition");
    }

    private static Position position(String sfen) {
        Position result = new Position();
        result.applyUSI(sfen);
        return result;
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
