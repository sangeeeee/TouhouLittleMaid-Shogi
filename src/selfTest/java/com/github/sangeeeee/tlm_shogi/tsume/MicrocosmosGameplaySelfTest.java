package com.github.sangeeeee.tlm_shogi.tsume;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.PieceType;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Replays the fixed record through the legacy board class used by mouse input and rendering. */
public final class MicrocosmosGameplaySelfTest {
    private MicrocosmosGameplaySelfTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the microcosmos.usi resource path");
        }
        String[] tokens = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)
                .trim().split("\\s+");
        String initial = String.join(" ", tokens[2], tokens[3], tokens[4], tokens[5]);
        int plies = tokens.length - 8;
        require(MicrocosmosRecord.moveCount() == plies, "runtime record length differs from the file");
        require(MicrocosmosRecord.isRecordedMove(0, tokens[7]), "first player move lookup failed");
        require(MicrocosmosRecord.defenseAfter(1).filter(tokens[8]::equals).isPresent(),
                "first defender response lookup failed");
        require(MicrocosmosRecord.defenseAfter(2).isEmpty(),
                "a defender response was exposed on the player's turn");
        require(MicrocosmosRecord.defenseAfter(MicrocosmosRecord.MAXIMUM_PLY).isEmpty(),
                "a defender response was exposed after the final mate");

        Position board = new Position();
        board.applyUSI(initial);
        com.github.sangeeeee.tlm_shogi.engine.core.Position rules =
                com.github.sangeeeee.tlm_shogi.engine.core.Position.parse(initial);

        for (int index = 0; index < plies; index++) {
            int ply = index + 1;
            String notation = tokens[7 + index];
            Move move = Move.parseSfen(notation)
                    .orElseThrow(() -> new AssertionError("invalid move at ply " + ply));

            if (rules.turn() == Turn.BLACK) {
                applyPlayerMove(board, move, ply);
            } else {
                require(board.makeMove(notation) >= 0,
                        "board rejected recorded defense at ply " + ply + ": " + notation);
            }
            rules.makeMove(move);

            String boardSfen = com.github.sangeeeee.tlm_shogi.engine.core.Position
                    .parse(board.toUSI()).toSfen();
            require(rules.toSfen().equals(boardSfen),
                    "board state diverged after ply " + ply + ": " + notation);
        }

        require(plies == MicrocosmosRecord.MAXIMUM_PLY, "unexpected record length");
        require(rules.isMate(), "gameplay replay did not end in mate");
        System.out.println("Microcosmos gameplay path passed: 1525 plies match the in-game board state");
    }

    private static void applyPlayerMove(Position board, Move move, int ply) {
        int to = boardPoint(move.to().file(), move.to().rank());
        int from;
        if (move.isDrop()) {
            int pieceId = blackPieceId(move.droppingPieceType());
            from = 81 + findHandIndex(board.getBlackHand(), pieceId);
        } else {
            from = boardPoint(move.from().file(), move.from().rank());
        }

        require(board.isLegalMove(from, to),
                "mouse-input rules rejected player move at ply " + ply + ": " + move.toSfen());
        boolean canPromote = board.canPromote(from, to);
        boolean mustPromote = board.mustPromote(from, to);
        require(!mustPromote || move.isPromotion(),
                "record omitted a mandatory promotion at ply " + ply);
        require(!move.isPromotion() || canPromote || mustPromote,
                "record requested an unavailable promotion at ply " + ply);
        require(board.move(from, to, move.isPromotion()) >= 0,
                "board failed to apply player move at ply " + ply + ": " + move.toSfen());
    }

    private static int boardPoint(int file, int rank) {
        return (rank - 1) * 9 + (9 - file);
    }

    private static int findHandIndex(List<int[]> hand, int pieceId) {
        for (int index = 0; index < hand.size(); index++) {
            if (hand.get(index)[0] > 0 && hand.get(index)[1] == pieceId) {
                return index;
            }
        }
        throw new AssertionError("recorded drop piece is absent from the player hand: " + pieceId);
    }

    private static int blackPieceId(PieceType type) {
        return switch (type.raw()) {
            case 0 -> 17;
            case 1 -> 16;
            case 2 -> 15;
            case 3 -> 12;
            case 4 -> 11;
            case 5 -> 14;
            case 6 -> 13;
            default -> throw new AssertionError("piece cannot be dropped: " + type);
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
