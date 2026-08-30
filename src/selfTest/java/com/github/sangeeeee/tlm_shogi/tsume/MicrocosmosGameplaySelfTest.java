package com.github.sangeeeee.tlm_shogi.tsume;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;
import com.github.sangeeeee.tlm_shogi.util.JChessUiAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Replays the fixed record through the same engine-to-UI adapter used by mouse input. */
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

        Position board = Position.parse(initial);
        Position rules = Position.parse(initial);

        for (int index = 0; index < plies; index++) {
            int ply = index + 1;
            String notation = tokens[7 + index];
            Move move = Move.parseSfen(notation)
                    .orElseThrow(() -> new AssertionError("invalid move at ply " + ply));

            if (rules.turn() == Turn.BLACK) {
                applyPlayerMove(board, move, ply);
            } else {
                require(JChessUiAdapter.applyUsiMove(board, notation) >= 0,
                        "board rejected recorded defense at ply " + ply + ": " + notation);
            }
            rules.makeMove(move);

            require(rules.toSfen().equals(board.toSfen()),
                    "board state diverged after ply " + ply + ": " + notation);
        }

        require(plies == MicrocosmosRecord.MAXIMUM_PLY, "unexpected record length");
        require(rules.isMate(), "gameplay replay did not end in mate");
        System.out.println("Microcosmos gameplay path passed: 1525 plies match the in-game board state");
    }

    private static void applyPlayerMove(Position board, Move move, int ply) {
        int to = JChessUiAdapter.pointFromSquare(move.to());
        int from;
        if (move.isDrop()) {
            int handIndex = JChessUiAdapter.handIndex(board, Turn.BLACK, move.droppingPieceType());
            require(handIndex >= 0,
                    "recorded drop piece is absent from the player hand at ply " + ply);
            from = 81 + handIndex;
        } else {
            from = JChessUiAdapter.pointFromSquare(move.from());
        }

        Optional<Move> selected = JChessUiAdapter.legalMove(board, from, to, move.isPromotion());
        require(selected.filter(move::equals).isPresent(),
                "mouse-input path rejected player move at ply " + ply + ": " + move.toSfen());
        board.makeMoveUnchecked(selected.orElseThrow());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
