package com.github.sangeeeee.tlm_shogi.engine;

import com.github.sangeeeee.tlm_shogi.engine.core.Bitboard;
import com.github.sangeeeee.tlm_shogi.engine.core.Direction;
import com.github.sangeeeee.tlm_shogi.engine.core.Hand;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.RelativeSquare;
import com.github.sangeeeee.tlm_shogi.engine.core.RotatedBitboard;
import com.github.sangeeeee.tlm_shogi.engine.core.Square;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

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
        cancellationIsVisibleThroughToken();
        casualLimitsMatchTheCurrentModConfiguration();
        requestDefensivelyCopiesMoveHistory();
        resultRejectsMoveOutcomeWithoutMove();
        engineInitializesAndHonorsCancellation();
        engineReportsMissingSearchPort();
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
        equal(256, limits.transpositionTableMiB(), "hash size");
        equal(1, limits.threads(), "thread count");
    }

    private void requestDefensivelyCopiesMoveHistory() {
        List<String> moves = new ArrayList<>(List.of("7g7f"));
        SearchRequest request = new SearchRequest("startpos", moves, SearchLimits.casualPlay());
        moves.add("3c3d");

        equal(List.of("7g7f"), request.moves(), "request move copy");
        expect(UnsupportedOperationException.class, () -> request.moves().add("3c3d"));
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

    private void engineInitializesAndHonorsCancellation() throws Exception {
        withTemporaryResources(SunfishResources.EXPECTED_EVAL_VERSION, directory -> {
            SunfishEngine engine = new SunfishEngine(SunfishResources.fromDirectory(directory));
            equal(EngineState.NEW, engine.state(), "initial engine state");

            engine.initialize();
            equal(EngineState.READY, engine.state(), "ready engine state");
            equal(SunfishResources.EXPECTED_EVAL_VERSION, engine.resourceInfo().evalVersion(), "eval version");

            CancellationSource cancellation = new CancellationSource();
            cancellation.cancel();
            SearchResult result = engine.search(
                    SearchRequest.currentPosition("startpos", SearchLimits.casualPlay()),
                    cancellation.token()
            );
            equal(SearchOutcome.CANCELLED, result.outcome(), "cancelled outcome");

            engine.close();
            equal(EngineState.CLOSED, engine.state(), "closed engine state");
        });
    }

    private void engineReportsMissingSearchPort() throws Exception {
        withTemporaryResources(SunfishResources.EXPECTED_EVAL_VERSION, directory -> {
            SunfishEngine engine = new SunfishEngine(SunfishResources.fromDirectory(directory));
            engine.initialize();
            EngineException exception = expect(EngineException.class, () -> engine.search(
                    SearchRequest.currentPosition("startpos", SearchLimits.casualPlay()),
                    CancellationToken.none()
            ));
            equal(
                    "Sunfish rule generation and search have not been ported yet",
                    exception.getMessage(),
                    "unimplemented search message"
            );
        });
    }

    private void resourceVersionMismatchFailsInitialization() throws Exception {
        withTemporaryResources("wrong.version", directory -> {
            SunfishEngine engine = new SunfishEngine(SunfishResources.fromDirectory(directory));
            expect(EngineException.class, engine::initialize);
            equal(EngineState.NEW, engine.state(), "failed initialization state");
        });
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
