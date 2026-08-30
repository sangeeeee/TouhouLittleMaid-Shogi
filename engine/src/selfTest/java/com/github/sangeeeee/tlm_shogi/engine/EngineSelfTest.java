package com.github.sangeeeee.tlm_shogi.engine;

import com.github.sangeeeee.tlm_shogi.engine.book.SunfishBook;
import com.github.sangeeeee.tlm_shogi.engine.core.Bitboard;
import com.github.sangeeeee.tlm_shogi.engine.core.Direction;
import com.github.sangeeeee.tlm_shogi.engine.core.Hand;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveGenerator;
import com.github.sangeeeee.tlm_shogi.engine.core.MoveTables;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.RelativeSquare;
import com.github.sangeeeee.tlm_shogi.engine.core.RotatedBitboard;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;
import com.github.sangeeeee.tlm_shogi.engine.core.Zobrist;
import com.github.sangeeeee.tlm_shogi.engine.search.AlphaBetaSearcher;
import com.github.sangeeeee.tlm_shogi.engine.search.SunfishEvaluator;
import com.github.sangeeeee.tlm_shogi.engine.search.SunfishScore;
import com.github.sangeeeee.tlm_shogi.engine.search.TranspositionTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dependency-free tests for running the engine module without Minecraft. */
public final class EngineSelfTest {
    private int checks;

    public static void main(String[] args) throws Exception {
        EngineSelfTest test = new EngineSelfTest();
        test.run();
        System.out.println("Engine self-test passed: " + test.checks + " checks");
    }

    private void run() throws Exception {
        turnAndDirectionValuesMatchSunfish();
        pieceValuesAndNotationMatchSunfish();
        squareGeometryAndNotationMatchSunfish();
        movePackingAndNotationMatchSunfish();
        handEnforcesPhysicalPieceLimits();
        bitboardsMatchSunfishLayoutAndOperations();
        moveTablesMatchSunfishAttacks();
        positionRoundTripsAndUndoesMoves();
        legalMoveGenerationEnforcesShogiRules();
        incrementalCachesAndZobristStayConsistent();
        cancellationIsVisibleThroughToken();
        casualLimitsMatchTheCurrentModConfiguration();
        requestDefensivelyCopiesMoveHistory();
        resultRejectsMoveOutcomeWithoutMove();
        sunfishOpeningBookLoadsAndSelectsByCount();
        sunfishEvaluationLoadsAndIsSymmetric();
        transpositionTableMatchesSunfishSemantics();
        alphaBetaSearchFindsMaterialAndHonorsLimits();
        engineInitializesSearchesAndHonorsCancellation();
        truncatedEvaluationFailsInitialization();
        resourceVersionMismatchFailsInitialization();
    }

    private void turnAndDirectionValuesMatchSunfish() {
        equal(Turn.WHITE, Turn.BLACK.opposite(), "black opposite");
        equal(Turn.BLACK, Turn.WHITE.opposite(), "white opposite");
        equal(Direction.LEFT_UP, Direction.RIGHT_DOWN.reversed(), "reverse direction");
        equal(Direction.RIGHT_UP_KNIGHT, Direction.LEFT_UP_KNIGHT.horizontalSymmetry(), "knight symmetry");
        equal(Direction.NONE, Direction.NONE.reversed(), "none direction");
    }

    private void pieceValuesAndNotationMatchSunfish() {
        equal(0, PieceType.PAWN.raw(), "pawn raw value");
        equal(14, PieceType.DRAGON.raw(), "dragon raw value");
        equal(32, PieceType.EMPTY.raw(), "empty raw value");
        equal(PieceType.PAWN, PieceType.TOKIN.hand(), "tokin hand type");
        equal(PieceType.ROOK, PieceType.DRAGON.hand(), "dragon hand type");
        equal(PieceType.TOKIN, PieceType.PAWN.promote(), "pawn promotion");
        equal(PieceType.BISHOP, PieceType.HORSE.unpromote(), "horse unpromotion");
        check(PieceType.ROOK.isPromotable(), "rook must be promotable");
        check(!PieceType.GOLD.isPromotable(), "gold must not be promotable");
        equal(PieceType.HORSE, PieceType.PRO_SILVER.next(), "piece type iteration gap");

        equal(30, Piece.WHITE_DRAGON.raw(), "white dragon raw value");
        equal(Piece.BLACK_DRAGON, Piece.WHITE_DRAGON.black(), "black conversion");
        equal(Piece.WHITE_DRAGON, Piece.BLACK_DRAGON.enemy(), "enemy conversion");
        equal(Turn.BLACK, Piece.BLACK_PAWN.turn(), "black piece turn");
        equal(Turn.WHITE, Piece.WHITE_PAWN.turn(), "white piece turn");
        equal(Piece.WHITE_PAWN, Piece.BLACK_DRAGON.next(), "piece iteration color gap");

        equal("FU", PieceType.PAWN.toCsa(), "piece type CSA");
        equal("RY", PieceType.DRAGON.toCsa(), "promoted piece type CSA");
        equal(PieceType.DRAGON, PieceType.parseCsa("RY"), "parse piece type CSA");
        equal("+FU", Piece.BLACK_PAWN.toCsa(), "black piece CSA");
        equal("-RY", Piece.WHITE_DRAGON.toCsa(), "white piece CSA");
        equal(Piece.WHITE_DRAGON, Piece.parseCsa("-RY"), "parse piece CSA");
        equal("P", Piece.BLACK_PAWN.toSfen(), "black piece SFEN");
        equal("+r", Piece.WHITE_DRAGON.toSfen(), "white promoted piece SFEN");
        equal(Piece.WHITE_DRAGON, Piece.parseSfen("+r"), "parse piece SFEN");
        equal(Piece.EMPTY, Piece.parseSfen("?"), "invalid piece SFEN");
    }

    private void squareGeometryAndNotationMatchSunfish() {
        Square square76 = Square.of(7, 6);
        equal(23, square76.raw(), "square raw order");
        equal(7, square76.file(), "square file");
        equal(6, square76.rank(), "square rank");
        check(Square.of(4, 3).isPromotable(Turn.BLACK), "black promotion zone");
        check(!Square.of(4, 3).isPromotable(Turn.WHITE), "white promotion zone");
        check(!Square.of(3, 1).isPawnMovable(Turn.BLACK), "black pawn boundary");
        check(!Square.of(1, 8).isKnightMovable(Turn.WHITE), "white knight boundary");

        Square square34 = Square.of(3, 4);
        equal(Square.of(7, 6), square34.pointSymmetry(), "point symmetry");
        equal(Square.of(7, 4), square34.horizontalSymmetry(), "horizontal symmetry");
        equal(Square.of(3, 6), square34.verticalSymmetry(), "vertical symmetry");

        Square square55 = Square.of(5, 5);
        equal(Square.of(5, 4), square55.up(), "square up");
        equal(Square.of(6, 5), square55.left(), "square left");
        equal(Square.of(4, 4), square55.rightUp(), "square right-up");
        equal(Square.of(6, 3), square55.leftUpKnight(), "square knight");
        check(Square.of(9, 1).safetyUp().isInvalid(), "safe up boundary");
        equal(Square.of(8, 2), Square.of(9, 1).safetyRightDown(), "safe diagonal");

        equal(4, square34.distance(Square.of(3, 8)), "orthogonal distance");
        equal(5, square34.distance(Square.of(8, 9)), "diagonal distance");
        equal(Direction.LEFT_UP, Square.of(3, 6).directionTo(Square.of(6, 3)), "diagonal direction");
        equal(Direction.NONE, Square.of(3, 6).directionTo(Square.of(7, 3)), "unaligned direction");
        equal(Direction.DOWN, Square.of(3, 6).directionTo(Square.of(3, 8)), "vertical direction");

        equal("37", Square.of(3, 7).toCsa(), "square CSA");
        equal("3g", Square.of(3, 7).toSfen(), "square SFEN");
        equal(Square.of(3, 7), Square.parseCsa("37").orElseThrow(), "parse square CSA");
        equal(Square.of(3, 7), Square.parseSfen("3g").orElseThrow(), "parse square SFEN");
        check(Square.parseSfen("hoge").isEmpty(), "invalid square SFEN");
        equal(Square.end(), Square.of(1, 9).next(), "square iteration end");

        equal(10, Square.of(6, 2).rotate90().raw(), "90 degree rotation");
        equal(62, Square.of(3, 9).rotate90().raw(), "90 degree rotation edge");
        equal(1, Square.of(8, 2).rotateRight45().raw(), "right 45 rotation");
        equal(49, Square.of(2, 8).rotateRight45().raw(), "right 45 rotation edge");
        equal(1, Square.of(8, 8).rotateLeft45().raw(), "left 45 rotation");
        equal(49, Square.of(2, 2).rotateLeft45().raw(), "left 45 rotation edge");

        equal(144, RelativeSquare.between(Square.of(6, 2), Square.of(6, 2)).raw(), "relative origin");
        equal(73, RelativeSquare.between(Square.of(4, 8), Square.of(8, 5)).raw(), "relative square");
        equal(285, RelativeSquare.between(Square.of(9, 3), Square.of(1, 8)).raw(), "relative square edge");
    }

    private void movePackingAndNotationMatchSunfish() {
        Move move = Move.board(Square.of(5, 7), Square.of(5, 6), false);
        equal("5756", move.toString(), "board move string");
        equal("5g5f", move.toSfen(), "board move SFEN");
        equal("+5756FU", move.toCsa(Turn.BLACK, PieceType.PAWN), "board move CSA");

        Move promotion = Move.board(Square.of(2, 8), Square.of(2, 2), true);
        equal("2h2b+", promotion.toSfen(), "promotion SFEN");
        equal("+2822RY", promotion.toCsa(Turn.BLACK, PieceType.ROOK), "promotion CSA");

        Move drop = Move.drop(PieceType.KNIGHT, Square.of(6, 3));
        equal("63KE", drop.toString(), "drop string");
        equal("N*6c", drop.toSfen(), "drop SFEN");
        equal("-0063KE", drop.toCsa(Turn.WHITE, PieceType.EMPTY), "drop CSA");
        check(drop.isDrop(), "drop flag");
        equal(PieceType.KNIGHT, drop.droppingPieceType(), "drop piece type");

        int packed = promotion.serialize16();
        equal(promotion, Move.deserialize16(packed), "16-bit move round trip");
        promotion.setExtData(55_555);
        equal(55_555, promotion.extData(), "move extension data");
        equal(Move.deserialize16(packed), promotion, "move equality ignores extension");
        equal(0, promotion.excludeExtData().extData(), "exclude extension data");
        check(Move.none().isNone(), "none move");
        expect(IllegalArgumentException.class, () -> Move.drop(PieceType.KING, Square.of(5, 5)));
    }

    private void handEnforcesPhysicalPieceLimits() {
        Hand hand = new Hand();
        equal(1, hand.increment(PieceType.TOKIN), "captured promoted pawn increment");
        equal(1, hand.get(PieceType.PAWN), "promoted hand lookup");
        equal(0, hand.decrement(PieceType.PAWN), "hand decrement");
        hand.set(PieceType.BISHOP, 2);
        equal(2, hand.get(PieceType.HORSE), "promoted hand get");
        expect(IllegalStateException.class, () -> hand.increment(PieceType.BISHOP));
        expect(IllegalArgumentException.class, () -> hand.set(PieceType.ROOK, 3));
        expect(IllegalArgumentException.class, () -> hand.increment(PieceType.KING));

        Hand copy = hand.copy();
        copy.decrement(PieceType.BISHOP);
        equal(2, hand.get(PieceType.BISHOP), "hand copy independence");
        equal(1, copy.get(PieceType.BISHOP), "hand copied count");
    }

    private void bitboardsMatchSunfishLayoutAndOperations() {
        Bitboard board = new Bitboard(0x3L, 0xcL).set(new Square(3));
        equal(0xbL, board.first(), "set first bitboard word");
        equal(0xcL, board.second(), "preserve second bitboard word");
        board.set(new Square(46));
        equal(0xeL, board.second(), "set second bitboard word");
        check(board.contains(new Square(47)), "contains second word square");
        equal(9, new Bitboard(0x803L, 0x74cL).count(), "bitboard population count");

        equal(Bitboard.zero().set(Square.of(3, 4)), Bitboard.mask(Square.of(3, 4)), "square mask");
        Bitboard verticalLine = Bitboard.lineMask(Square.of(5, 5), Square.of(5, 2));
        equal(3, verticalLine.count(), "line mask count");
        check(verticalLine.contains(Square.of(5, 4)), "line mask intermediate");
        check(verticalLine.contains(Square.of(5, 2)), "line mask destination");
        check(!verticalLine.contains(Square.of(5, 5)), "line mask excludes source");

        Bitboard andNot = new Bitboard(0x3L, 0xcL).andNot(new Bitboard(0x9L, 0x6L));
        equal(0x8L, andNot.first(), "Sunfish and-not first word");
        equal(0x2L, andNot.second(), "Sunfish and-not second word");
        equal(27, Bitboard.blackPromotable().count(), "black promotion zone count");
        equal(18, Bitboard.whitePromotable2().count(), "white deep promotion zone count");
        check(Bitboard.file1().contains(Square.of(1, 5)), "file mask");
        check(!Bitboard.file1().contains(Square.of(2, 5)), "file mask exclusion");

        Bitboard center = Bitboard.zero().set(Square.of(5, 5));
        equal(Bitboard.zero().set(Square.of(5, 4)), center.up(), "bitboard up");
        equal(Bitboard.zero().set(Square.of(6, 5)), center.left(), "bitboard left");
        equal(Bitboard.zero().set(Square.of(4, 6)), center.rightDown(), "bitboard right-down");
        equal(Bitboard.zero().set(Square.of(6, 3)), center.leftUpKnight(), "bitboard knight");

        Bitboard files = Bitboard.zero().set(Square.of(1, 9)).set(Square.of(5, 9));
        check(files.containsAnyOnFile(1), "check occupied file");
        check(!files.containsAnyOnFile(2), "check empty file");
        equal(Square.of(5, 9), files.pickForward(), "pick forward first word");
        equal(Square.of(1, 9), files.pickForward(), "pick forward second word");
        check(files.pickForward().isInvalid(), "pick forward empty board");

        RotatedBitboard rotated = new RotatedBitboard(0x3L);
        rotated.set(3);
        equal(0xbL, rotated.raw(), "rotated bitboard set");
        equal("000000000000000b", rotated.toString(), "rotated bitboard string");
        equal(new RotatedBitboard(0x1L), rotated.and(new RotatedBitboard(0x5L)), "rotated bitboard and");
    }

    private void moveTablesMatchSunfishAttacks() {
        Square square67 = Square.of(6, 7);
        Bitboard blackSilver = MoveTables.blackSilver(square67);
        equal(5, blackSilver.count(), "black silver attack count");
        check(blackSilver.contains(Square.of(6, 6)), "black silver forward attack");
        check(blackSilver.contains(Square.of(7, 8)), "black silver backward diagonal");
        check(!blackSilver.contains(Square.of(6, 8)), "black silver cannot move straight backward");

        Bitboard occupied = Bitboard.zero().set(Square.of(4, 3)).set(Square.of(4, 8));
        Bitboard blackLance = MoveTables.blackLance(occupied, Square.of(4, 7));
        equal(4, blackLance.count(), "blocked black lance attack count");
        check(blackLance.contains(Square.of(4, 3)), "lance attack includes blocker");
        check(!blackLance.contains(Square.of(4, 2)), "lance attack stops after blocker");
        equal(1, MoveTables.whiteLance(occupied, Square.of(4, 7)).count(), "blocked white lance attack");

        RotatedBitboard horizontalOccupancy = RotatedBitboard.zero();
        horizontalOccupancy.set(Square.of(6, 3).rotate90());
        Bitboard horizontal = MoveTables.hor(horizontalOccupancy, Square.of(1, 3));
        equal(5, horizontal.count(), "horizontal rotated attack count");
        check(horizontal.contains(Square.of(6, 3)), "horizontal attack includes blocker");
        check(!horizontal.contains(Square.of(7, 3)), "horizontal attack stops after blocker");

        RotatedBitboard rightDiagonalOccupancy = RotatedBitboard.zero();
        rightDiagonalOccupancy.set(Square.of(7, 6).rotateRight45());
        Bitboard rightDiagonal = MoveTables.diagR45(rightDiagonalOccupancy, Square.of(4, 3));
        equal(5, rightDiagonal.count(), "right diagonal rotated attack count");
        check(rightDiagonal.contains(Square.of(2, 1)), "right diagonal upper edge");
        check(rightDiagonal.contains(Square.of(7, 6)), "right diagonal includes blocker");
        check(!rightDiagonal.contains(Square.of(8, 7)), "right diagonal stops after blocker");

        RotatedBitboard leftDiagonalOccupancy = RotatedBitboard.zero();
        leftDiagonalOccupancy.set(Square.of(7, 2).rotateLeft45());
        leftDiagonalOccupancy.set(Square.of(2, 7).rotateLeft45());
        Bitboard leftDiagonal = MoveTables.diagL45(leftDiagonalOccupancy, Square.of(3, 6));
        equal(5, leftDiagonal.count(), "left diagonal rotated attack count");
        check(leftDiagonal.contains(Square.of(7, 2)), "left diagonal first blocker");
        check(leftDiagonal.contains(Square.of(2, 7)), "left diagonal second blocker");

        Bitboard horse = MoveTables.attacks(Piece.BLACK_HORSE, Square.of(5, 5), Bitboard.zero());
        equal(20, horse.count(), "center horse attack count");
        check(MoveTables.isMovableInOneStep(Piece.BLACK_HORSE, Direction.UP), "horse king step flag");
        check(MoveTables.isMovableInLongStep(Piece.BLACK_HORSE, Direction.LEFT_UP), "horse bishop ray flag");
        check(!MoveTables.isMovableInLongStep(Piece.BLACK_HORSE, Direction.UP), "horse has no rook ray");
    }

    private void positionRoundTripsAndUndoesMoves() {
        Position start = Position.startPosition();
        equal(Position.START_SFEN, start.toSfen(), "start position SFEN round trip");
        equal(Piece.WHITE_KING, start.pieceAt(Square.of(5, 1)), "white king placement");
        equal(Piece.BLACK_ROOK, start.pieceAt(Square.of(2, 8)), "black rook placement");
        equal(Square.of(5, 9), start.kingSquare(Turn.BLACK), "black king square");
        check(start.hasPawnInFile(Turn.BLACK, 1), "initial black pawn file");
        check(!start.inCheck(), "initial position is not check");

        String handSfen = "4k4/6+B2/9/9/9/3+p5/9/9/4K4 b P2G15p3n 1";
        Position hands = Position.fromSfen(handSfen);
        equal("4k4/6+B2/9/9/9/3+p5/9/9/4K4 b 2GP3n15p 1", hands.toSfen(),
                "canonical hand and promoted piece SFEN");
        equal(hands, Position.fromSfen(hands.toSfen()), "hand position SFEN round trip");
        equal(2, hands.handCount(Turn.BLACK, PieceType.GOLD), "black hand count");
        equal(15, hands.handCount(Turn.WHITE, PieceType.PAWN), "white hand count");
        equal(Piece.BLACK_HORSE, hands.pieceAt(Square.of(3, 2)), "promoted SFEN piece");

        equal(Move.board(Square.of(7, 7), Square.of(7, 6), false),
                Move.parseSfen("7g7f").orElseThrow(), "parse board move SFEN");
        equal(Move.drop(PieceType.GOLD, Square.of(4, 5)),
                Move.parseSfen("G*4e").orElseThrow(), "parse drop move SFEN");
        check(Move.parseSfen("7g7z").isEmpty(), "reject invalid move SFEN");

        Position before = start.copy();
        Move opening = Move.parseSfen("7g7f").orElseThrow();
        Position.Undo undo = start.makeMove(opening);
        equal(Piece.BLACK_PAWN, start.pieceAt(Square.of(7, 6)), "piece moved on board");
        equal(Piece.EMPTY, start.pieceAt(Square.of(7, 7)), "source cleared");
        equal(Turn.WHITE, start.turn(), "turn changed after move");
        start.undoMove(undo);
        equal(before, start, "position restored by undo");
        equal(before.getHash(), start.getHash(), "position hash restored by undo");

        Position capture = Position.fromSfen("k8/9/9/9/9/9/4s4/4R4/4K4 b - 1");
        Position captureBefore = capture.copy();
        Position.Undo captureUndo = capture.makeMove(Move.parseSfen("5h5g").orElseThrow());
        equal(1, capture.handCount(Turn.BLACK, PieceType.SILVER), "capture adds unpromoted hand piece");
        equal(Piece.BLACK_ROOK, capture.pieceAt(Square.of(5, 7)), "capturing piece destination");
        capture.undoMove(captureUndo);
        equal(captureBefore, capture, "capture restored by undo");

        Position drop = Position.fromSfen("k8/9/9/9/9/9/9/9/4K4 b G 1");
        Position dropBefore = drop.copy();
        Position.Undo dropUndo = drop.makeMove(Move.parseSfen("G*5e").orElseThrow());
        equal(0, drop.handCount(Turn.BLACK, PieceType.GOLD), "drop removes hand piece");
        equal(Piece.BLACK_GOLD, drop.pieceAt(Square.of(5, 5)), "dropped piece placement");
        drop.undoMove(dropUndo);
        equal(dropBefore, drop, "drop restored by undo");
    }

    private void legalMoveGenerationEnforcesShogiRules() {
        Position start = Position.startPosition();
        equal(30, MoveGenerator.generateLegal(start).size(), "start position legal moves");
        equal(900L, MoveGenerator.perft(start.copy(), 2), "start position perft depth 2");
        equal(25_470L, MoveGenerator.perft(start.copy(), 3), "start position perft depth 3");

        Position mandatoryPromotion = Position.fromSfen("k8/4P4/9/9/9/9/9/9/4K4 b - 1");
        Move pawnPromotes = Move.parseSfen("5b5a+").orElseThrow();
        Move deadPawn = Move.parseSfen("5b5a").orElseThrow();
        check(MoveGenerator.isLegal(mandatoryPromotion, pawnPromotes), "last-rank pawn promotion generated");
        check(!MoveGenerator.isLegal(mandatoryPromotion, deadPawn), "unpromoted pawn on last rank rejected");

        Position optionalPromotion = Position.fromSfen("k8/9/4P4/9/9/9/9/9/4K4 b - 1");
        check(MoveGenerator.isLegal(optionalPromotion, Move.parseSfen("5c5b+").orElseThrow()),
                "optional pawn promotion accepted");
        check(MoveGenerator.isLegal(optionalPromotion, Move.parseSfen("5c5b").orElseThrow()),
                "optional non-promotion accepted");

        Position nifu = Position.fromSfen("4k4/9/9/9/9/9/4P4/9/4K4 b P 1");
        List<Move> nifuMoves = MoveGenerator.generateLegal(nifu);
        check(nifuMoves.stream().noneMatch(move -> move.isDrop()
                        && move.droppingPieceType().equals(PieceType.PAWN) && move.to().file() == 5),
                "nifu pawn drops rejected");
        check(nifuMoves.stream().noneMatch(move -> move.isDrop()
                        && move.droppingPieceType().equals(PieceType.PAWN) && move.to().rank() == 1),
                "dead-rank pawn drops rejected");

        Position pinned = Position.fromSfen("k3r4/9/9/9/9/9/9/4G4/4K4 b - 1");
        Move exposeKing = Move.board(Square.of(5, 8), Square.of(4, 8), false);
        check(!MoveGenerator.isLegal(pinned, exposeKing), "pinned move exposing king rejected");
        check(MoveGenerator.generateLegal(pinned).stream().noneMatch(exposeKing::equals),
                "pinned move absent from generated moves");

        Position checked = Position.fromSfen("k8/9/9/9/9/9/9/4r4/4K4 b - 1");
        check(checked.inCheck(), "rook check detected");
        List<Move> evasions = MoveGenerator.generateEvasions(checked);
        check(!evasions.isEmpty(), "check has evasions");
        for (Move move : evasions) {
            Position escaped = checked.copy();
            escaped.makeMove(move);
            check(!escaped.inCheck(Turn.BLACK), "generated evasion resolves check: " + move.toSfen());
        }

        Position pawnDropMate = Position.fromSfen(
                "3lkn3/3l5/5G3/9/9/pppp1pppp/PPPP1PPPP/9/4K4 b 3P 1");
        Move illegalMate = Move.drop(PieceType.PAWN, Square.of(5, 2));
        check(!MoveGenerator.isLegal(pawnDropMate, illegalMate), "pawn-drop mate rejected");
        List<Move> pawnDropMateMoves = MoveGenerator.generateQuiets(pawnDropMate);
        equal(17, pawnDropMateMoves.size(), "Sunfish pawn-drop-mate test move count");
        check(pawnDropMateMoves.stream().noneMatch(illegalMate::equals), "pawn-drop mate absent from moves");

        Position whitePawnDropMate = Position.fromSfen(
                "4k4/9/pppp1pppp/PPPP1PPPP/9/9/5g3/3L5/3LKN3 w 3p 1");
        Move illegalWhiteMate = Move.drop(PieceType.PAWN, Square.of(5, 8));
        check(!MoveGenerator.isLegal(whitePawnDropMate, illegalWhiteMate), "white pawn-drop mate rejected");
        equal(17, MoveGenerator.generateQuiets(whitePawnDropMate).size(),
                "Sunfish white pawn-drop-mate test move count");
    }

    private void incrementalCachesAndZobristStayConsistent() {
        equal(0x17b17c12beec384cL, Zobrist.board(new Square(0), Piece.BLACK_PAWN),
                "Sunfish first board Zobrist constant");
        equal(0x4d04df8fbe3b38bcL, Zobrist.blackHand(PieceType.PAWN),
                "Sunfish black hand Zobrist constant");
        equal(0x63bec768e26d7958L, Zobrist.whiteHand(PieceType.ROOK),
                "Sunfish white hand Zobrist constant");
        equal(1L, Zobrist.turn(Turn.BLACK), "Sunfish black turn hash");
        equal(0L, Zobrist.turn(Turn.WHITE), "Sunfish white turn hash");

        Position position = Position.startPosition();
        String initialSfen = position.toSfen();
        long initialHash = position.getHash();
        check(position.verifyIncrementalState(), "initial incremental state");
        equal(9, position.pieceBitboard(Piece.BLACK_PAWN).count(), "incremental black pawn bitboard");
        equal(20, position.occupied(Turn.BLACK).count(), "incremental black occupancy");
        equal(recomputeBoardHash(position), position.getBoardHash(), "initial board hash recomputation");
        equal(recomputeHandHash(position), position.getHandHash(), "initial hand hash recomputation");

        MoveGenerator.generateLegal(position);
        equal(initialSfen, position.toSfen(), "generation leaves position unchanged");
        equal(initialHash, position.getHash(), "generation restores hash");
        check(position.verifyIncrementalState(), "state after legal generation");

        List<Position.Undo> undos = new ArrayList<>();
        for (int ply = 0; ply < 16; ply++) {
            List<Move> legal = MoveGenerator.generateLegal(position);
            check(!legal.isEmpty(), "deterministic cache test has legal move");
            Move move = legal.get((ply * 7) % legal.size());
            undos.add(position.makeMove(move));
            check(position.verifyIncrementalState(), "incremental state after ply " + (ply + 1));
            equal(recomputeBoardHash(position), position.getBoardHash(), "board hash after ply " + (ply + 1));
            equal(recomputeHandHash(position), position.getHandHash(), "hand hash after ply " + (ply + 1));
        }
        for (int index = undos.size() - 1; index >= 0; index--) {
            position.undoMove(undos.get(index));
            check(position.verifyIncrementalState(), "incremental state after undo " + index);
        }
        equal(initialSfen, position.toSfen(), "multi-move undo restores SFEN");
        equal(initialHash, position.getHash(), "multi-move undo restores Zobrist");

        position.doNullMove();
        equal(initialHash ^ 1L, position.getHash(), "null move toggles only turn hash");
        equal(1, position.moveNumber(), "null move preserves SFEN move number");
        position.undoNullMove();
        equal(initialHash, position.getHash(), "null move undo restores hash");
        check(position.verifyIncrementalState(), "state after null move round trip");
    }

    private static long recomputeBoardHash(Position position) {
        long hash = 0;
        Piece[] board = position.boardCopy();
        for (int raw = 0; raw < board.length; raw++) {
            if (!board[raw].isEmpty()) hash ^= Zobrist.board(new Square(raw), board[raw]);
        }
        return hash;
    }

    private static long recomputeHandHash(Position position) {
        long hash = 0;
        for (PieceType type : List.of(PieceType.PAWN, PieceType.LANCE, PieceType.KNIGHT,
                PieceType.SILVER, PieceType.GOLD, PieceType.BISHOP, PieceType.ROOK)) {
            hash += Zobrist.blackHand(type) * position.handCount(Turn.BLACK, type);
            hash += Zobrist.whiteHand(type) * position.handCount(Turn.WHITE, type);
        }
        return hash;
    }

    private void cancellationIsVisibleThroughToken() {
        CancellationSource source = new CancellationSource();
        check(!source.token().isCancellationRequested(), "new token must not be cancelled");
        check(source.cancel(), "first cancellation must change state");
        check(source.token().isCancellationRequested(), "token must observe cancellation");
        check(!source.cancel(), "second cancellation must not change state");
    }

    private void casualLimitsMatchTheCurrentModConfiguration() {
        SearchLimits limits = SearchLimits.casualPlay();
        equal(Duration.ofSeconds(3), limits.moveTime(), "move time");
        equal(8, limits.maximumDepth(), "maximum depth");
        equal(30_000L, limits.maximumNodes(), "maximum nodes");
        equal(64, limits.transpositionTableMiB(), "hash size");
        equal(1, limits.threads(), "thread count");
    }

    private void requestDefensivelyCopiesMoveHistory() {
        List<String> moves = new ArrayList<>(List.of("7g7f"));
        SearchRequest request = new SearchRequest("startpos", moves, SearchLimits.casualPlay());
        moves.add("3c3d");

        equal(List.of("7g7f"), request.moves(), "request move copy");
        expect(UnsupportedOperationException.class, () -> request.moves().add("3c3d"));
        check(request.useBook(), "opening book is enabled by default");
    }

    private void resultRejectsMoveOutcomeWithoutMove() {
        expect(IllegalArgumentException.class, () -> new SearchResult(
                SearchOutcome.MOVE,
                Optional.empty(),
                0,
                1,
                1,
                Duration.ZERO,
                List.of()
        ));
    }

    private void sunfishOpeningBookLoadsAndSelectsByCount() throws Exception {
        Position afterPawn = Position.startPosition();
        afterPawn.makeMove(Move.parseSfen("7g7f").orElseThrow());
        String afterPawnBookSfen = afterPawn.toSfen().replace(" w - 2", " w - 1");
        String content = """
                sfen %s
                7g7f 3
                2g2f 1
                7g7f 2
                sfen %s
                3c3d 4
                """.formatted(Position.START_SFEN, afterPawnBookSfen);

        SunfishBook synthetic = SunfishBook.load(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "synthetic book"
        );
        equal(2, synthetic.positionCount(), "synthetic book position count");
        equal(3, synthetic.moveCount(), "synthetic book distinct move count");

        List<SunfishBook.BookMove> startMoves = synthetic.moves(Position.startPosition());
        equal(2, startMoves.size(), "synthetic start candidates");
        equal("7g7f", startMoves.get(0).move().toSfen(), "duplicate move keeps first position");
        equal(5, startMoves.get(0).count(), "duplicate move counts are merged");
        equal(1, startMoves.get(1).count(), "second move count");
        equal("7g7f", synthetic.select(Position.startPosition(), () -> 0L)
                .orElseThrow().toSfen(), "zero ticket selects first weighted move");
        expect(UnsupportedOperationException.class,
                () -> startMoves.add(new SunfishBook.BookMove(
                        Move.parseSfen("5g5f").orElseThrow(), 1)));

        Position differentMoveNumber = Position.fromSfen(Position.START_SFEN.replace(" - 1", " - 42"));
        equal(startMoves, synthetic.moves(differentMoveNumber), "book lookup ignores SFEN move number");
        equal("3c3d", synthetic.select(afterPawn, () -> 0L).orElseThrow().toSfen(),
                "history position uses normalized book key");
        check(synthetic.moves(Position.fromSfen("k8/9/9/9/9/9/9/9/4K4 b - 1")).isEmpty(),
                "unknown position misses book");
        expect(EngineException.class, () -> SunfishBook.load(
                new ByteArrayInputStream("7g7f 1\n".getBytes(StandardCharsets.UTF_8)),
                "moves before position"
        ));

        SunfishBook bundled = SunfishBook.load(repositoryResources());
        equal(32_480, bundled.positionCount(), "bundled book position count");
        equal(52_968, bundled.moveCount(), "bundled book move count");
        List<SunfishBook.BookMove> bundledStart = bundled.moves(Position.startPosition());
        equal(29, bundledStart.size(), "bundled start-position candidates");
        equal("2g2f", bundledStart.get(0).move().toSfen(), "most frequent start move");
        equal(27_937, bundledStart.get(0).count(), "most frequent start move count");
    }

    private void sunfishEvaluationLoadsAndIsSymmetric() throws Exception {
        Path evalPath = repositoryResources().evalFile();
        SunfishEvaluator evaluator = SunfishEvaluator.load(evalPath);
        equal(SunfishEvaluator.WEIGHT_COUNT, evaluator.weightCount(), "optimized eval weight count");
        equal(SunfishEvaluator.EXPECTED_FILE_BYTES, Files.size(evalPath), "optimized eval byte size");

        Position start = Position.startPosition();
        equal(0, evaluator.calculateMaterialScore(start), "start position material symmetry");
        equal(0, evaluator.evaluate(start), "start position total evaluation symmetry");

        Position blackAdvance = Position.startPosition();
        blackAdvance.makeMove(Move.parseSfen("7g7f").orElseThrow());
        Position mirroredWhiteAdvance = Position.fromSfen(
                Position.START_SFEN.replace(" b - 1", " w - 1")
        );
        mirroredWhiteAdvance.makeMove(Move.parseSfen("3c3d").orElseThrow());
        int blackAdvanceScore = evaluator.evaluate(blackAdvance);
        int mirroredScore = evaluator.evaluate(mirroredWhiteAdvance);
        equal(blackAdvanceScore, -mirroredScore, "point-symmetric eval sign");
        check(blackAdvanceScore != 0, "trained eval reacts to an opening pawn move");
    }

    private void transpositionTableMatchesSunfishSemantics() {
        TranspositionTable table = new TranspositionTable(1);
        equal(16_384, table.bucketCount(), "one MiB TT bucket count");

        long hash = 0x1234_5678_9abc_def0L;
        Move move = Move.parseSfen("7g7f").orElseThrow();
        equal(TranspositionTable.StoreStatus.REPLACE,
                table.store(hash, -123, 456, 77, 5, 3, move), "first TT store");
        TranspositionTable.Entry entry = table.probe(hash, 8).orElseThrow();
        equal(77, entry.score(), "TT exact score");
        equal(5, entry.depth(), "TT depth");
        equal(TranspositionTable.EXACT, entry.scoreType(), "TT exact bound");
        equal(move, entry.move(), "TT move");

        equal(TranspositionTable.StoreStatus.REJECT,
                table.store(hash, -123, 456, 77, 3, 3, move), "shallower TT rejection");
        table.store(hash + 1, -123, 456, SunfishScore.INFINITY - 7, 5, 3, move);
        equal(SunfishScore.INFINITY - 8, table.probe(hash + 1, 4).orElseThrow().score(),
                "positive mate-distance normalization");
        table.store(hash + 2, -123, 456, -SunfishScore.INFINITY + 5, 5, 4, move);
        equal(-SunfishScore.INFINITY + 6, table.probe(hash + 2, 5).orElseThrow().score(),
                "negative mate-distance normalization");
        check(table.usageRate() > 0.0f, "TT usage diagnostic");

        table.clear();
        long collisionStride = table.bucketCount();
        long first = 0x55L;
        long shallowest = first + collisionStride;
        long deepest = first + collisionStride * 2L;
        long replacement = first + collisionStride * 3L;
        table.store(first, -10, 10, 0, 5, 0, move);
        table.store(shallowest, -10, 10, 0, 2, 0, move);
        table.store(deepest, -10, 10, 0, 7, 0, move);
        table.store(replacement, -10, 10, 0, 4, 0, move);
        check(table.probe(first, 0).isPresent(), "TT collision preserves first deeper slot");
        check(table.probe(shallowest, 0).isEmpty(), "TT collision replaces shallowest slot");
        check(table.probe(deepest, 0).isPresent(), "TT collision preserves deepest slot");
        check(table.probe(replacement, 0).isPresent(), "TT collision stores replacement");
    }

    private void alphaBetaSearchFindsMaterialAndHonorsLimits() {
        Position position = Position.fromSfen("k8/9/9/9/4r4/4R4/9/9/4K4 b - 1");
        String before = position.toSfen();
        SearchLimits limits = new SearchLimits(Duration.ofSeconds(1), 2, 10_000, 1, 1);
        AlphaBetaSearcher searcher = new AlphaBetaSearcher(
                SunfishEvaluator.materialOnly(),
                new TranspositionTable(1),
                limits,
                CancellationToken.none(),
                List.of(position.getHash())
        );
        SearchResult result = searcher.search(position);
        equal(SearchOutcome.MOVE, result.outcome(), "standalone alpha-beta outcome");
        equal("5f5e", result.bestMove().orElseThrow(), "standalone alpha-beta capture");
        check(result.depth() >= 1, "standalone alpha-beta completed depth");
        check(result.nodes() <= limits.maximumNodes(), "standalone alpha-beta node cap");
        equal(before, position.toSfen(), "search restores root position");

        Position mateInOne = Position.fromSfen("3lkl3/9/5G3/9/9/9/9/9/4K4 b R 1");
        SearchResult mate = new AlphaBetaSearcher(
                SunfishEvaluator.materialOnly(),
                new TranspositionTable(1),
                limits,
                CancellationToken.none(),
                List.of(mateInOne.getHash())
        ).search(mateInOne);
        equal("R*5b", mate.bestMove().orElseThrow(), "mate-in-one move");
        equal(SunfishScore.INFINITY - 1, mate.score(), "mate-in-one distance score");

        SearchLimits oneNode = new SearchLimits(Duration.ofSeconds(1), 8, 1, 1, 1);
        SearchResult limited = new AlphaBetaSearcher(
                SunfishEvaluator.materialOnly(),
                new TranspositionTable(1),
                oneNode,
                CancellationToken.none(),
                List.of(position.getHash())
        ).search(position);
        equal(SearchOutcome.MOVE, limited.outcome(), "node-limited fallback move");
        equal(1L, limited.nodes(), "hard node limit");
        equal(0, limited.depth(), "incomplete iteration is not reported as complete");

        Position cancellationPosition = Position.startPosition();
        String cancellationBefore = cancellationPosition.toSfen();
        int[] cancellationChecks = {0};
        CancellationToken delayedCancellation = () -> ++cancellationChecks[0] > 100;
        SearchResult cancelled = new AlphaBetaSearcher(
                SunfishEvaluator.materialOnly(),
                new TranspositionTable(1),
                new SearchLimits(Duration.ofSeconds(5), 8, 100_000, 1, 1),
                delayedCancellation,
                List.of(cancellationPosition.getHash())
        ).search(cancellationPosition);
        equal(SearchOutcome.CANCELLED, cancelled.outcome(), "mid-search cancellation outcome");
        equal(cancellationBefore, cancellationPosition.toSfen(), "cancellation restores root position");
    }

    private void engineInitializesSearchesAndHonorsCancellation() throws Exception {
        SunfishEngine engine = new SunfishEngine(repositoryResources());
        equal(EngineState.NEW, engine.state(), "initial engine state");
        engine.initialize();
        equal(EngineState.READY, engine.state(), "ready engine state");
        equal(SunfishResources.EXPECTED_EVAL_VERSION, engine.resourceInfo().evalVersion(), "eval version");

        CancellationSource cancellation = new CancellationSource();
        cancellation.cancel();
        SearchResult cancelled = engine.search(
                SearchRequest.currentPosition("startpos", SearchLimits.casualPlay()),
                cancellation.token()
        );
        equal(SearchOutcome.CANCELLED, cancelled.outcome(), "cancelled outcome");

        SearchLimits limits = new SearchLimits(Duration.ofSeconds(2), 2, 5_000, 1, 1);
        SearchResult bookResult = engine.search(
                SearchRequest.currentPosition("startpos", limits),
                CancellationToken.none()
        );
        equal(SearchOutcome.MOVE, bookResult.outcome(), "opening-book outcome");
        equal(0, bookResult.depth(), "opening-book move bypasses alpha-beta depth");
        equal(0L, bookResult.nodes(), "opening-book move bypasses alpha-beta nodes");
        Move selectedBookMove = Move.parseSfen(bookResult.bestMove().orElseThrow()).orElseThrow();
        check(Position.startPosition().validateMove(selectedBookMove), "opening book returns a legal move");
        equal(List.of(selectedBookMove.toSfen()), bookResult.principalVariation(),
                "opening-book principal variation");

        SearchResult result = engine.search(
                new SearchRequest("startpos", List.of("7g7f"), limits, false),
                CancellationToken.none()
        );
        equal(SearchOutcome.MOVE, result.outcome(), "engine search outcome");
        Position afterHistory = Position.startPosition();
        afterHistory.makeMove(Move.parseSfen("7g7f").orElseThrow());
        Move bestMove = Move.parseSfen(result.bestMove().orElseThrow()).orElseThrow();
        check(afterHistory.validateMove(bestMove), "engine returns a legal move after replaying history");
        check(!result.principalVariation().isEmpty(), "engine returns a principal variation");
        check(result.nodes() <= limits.maximumNodes(), "engine search node cap");

        expect(EngineException.class, () -> engine.search(
                new SearchRequest("startpos", List.of("7g7z"), limits),
                CancellationToken.none()
        ));
        expect(EngineException.class, () -> engine.search(
                SearchRequest.currentPosition("9/9/9/9/9/9/9/9/4K4 b - 1", limits),
                CancellationToken.none()
        ));
        engine.close();
        equal(EngineState.CLOSED, engine.state(), "closed engine state");
    }

    private void truncatedEvaluationFailsInitialization() throws Exception {
        withTemporaryResources(SunfishResources.EXPECTED_EVAL_VERSION, directory -> {
            SunfishEngine engine = new SunfishEngine(SunfishResources.fromDirectory(directory));
            EngineException exception = expect(EngineException.class, engine::initialize);
            check(exception.getMessage().startsWith("Invalid eval.bin size:"),
                    "truncated eval size message");
            equal(EngineState.NEW, engine.state(), "truncated eval leaves engine new");
        });
    }

    private void resourceVersionMismatchFailsInitialization() throws Exception {
        withTemporaryResources("wrong.version", directory -> {
            SunfishEngine engine = new SunfishEngine(SunfishResources.fromDirectory(directory));
            expect(EngineException.class, engine::initialize);
            equal(EngineState.NEW, engine.state(), "failed initialization state");
        });
    }

    private static SunfishResources repositoryResources() {
        Path direct = Path.of("tem");
        if (Files.isRegularFile(direct.resolve("eval.bin"))) {
            return SunfishResources.fromDirectory(direct);
        }
        Path parent = Path.of("..", "tem");
        return SunfishResources.fromDirectory(parent);
    }

    private void withTemporaryResources(String version, ThrowingConsumer<Path> test) throws Exception {
        Path directory = Files.createTempDirectory("tlm-shogi-engine-test-");
        Path eval = directory.resolve("eval.bin");
        Path book = directory.resolve("book.bin");
        try {
            writeEvalHeader(eval, version);
            Files.writeString(book, "sfen startpos\n", StandardCharsets.UTF_8);
            test.accept(directory);
        } finally {
            Files.deleteIfExists(book);
            Files.deleteIfExists(eval);
            Files.deleteIfExists(directory);
        }
    }

    private static void writeEvalHeader(Path path, String version) throws IOException {
        byte[] versionBytes = version.getBytes(StandardCharsets.US_ASCII);
        byte[] content = new byte[versionBytes.length + 1];
        content[0] = (byte) versionBytes.length;
        System.arraycopy(versionBytes, 0, content, 1, versionBytes.length);
        Files.write(path, content);
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

    private <T extends Throwable> T expect(Class<T> type, ThrowingRunnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable throwable) {
            if (type.isInstance(throwable)) {
                return type.cast(throwable);
            }
            throw new AssertionError("Expected " + type.getName() + " but got " + throwable, throwable);
        }
        throw new AssertionError("Expected " + type.getName() + " but no exception was thrown");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
