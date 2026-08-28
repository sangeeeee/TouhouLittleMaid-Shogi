package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Validates the complete fixed record without loading Minecraft. */
public final class MicrocosmosRecordSelfTest {
    private static final String EXPECTED_INITIAL =
            "g1+P1k1+P+P+L/1p3P3/+R+p2pp1pl/1NNsg+p2+R/+b+nL+P1+p3/1P3ssP1/2P1+Ps2N/4+P1P1L/+B5G1g b - 1";
    private static final int EXPECTED_PLIES = 1525;

    private MicrocosmosRecordSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the microcosmos.usi resource path");
        }

        String[] tokens = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)
                .trim().split("\\s+");
        require(tokens.length == 7 + EXPECTED_PLIES + 1, "unexpected token count");
        require("position".equals(tokens[0]), "missing position command");
        require("sfen".equals(tokens[1]), "missing sfen marker");
        require("moves".equals(tokens[6]), "missing moves marker");
        require("mate".equals(tokens[tokens.length - 1]), "missing terminal mate marker");

        String initial = String.join(" ", tokens[2], tokens[3], tokens[4], tokens[5]);
        require(Position.parse(initial).toSfen().equals(Position.parse(EXPECTED_INITIAL).toSfen()),
                "unexpected initial SFEN");

        Position position = Position.parse(initial);
        for (int index = 0; index < EXPECTED_PLIES; index++) {
            int ply = index + 1;
            Move move = Move.parseSfen(tokens[7 + index])
                    .orElseThrow(() -> new AssertionError("invalid USI move at ply " + ply));
            require(position.validateMove(move), "illegal move at ply " + ply + ": " + move.toSfen());
            position.makeMoveUnchecked(move);
            require(position.verifyIncrementalState(), "incremental state mismatch at ply " + ply);

            if ((ply & 1) == 1) {
                require(position.inCheck(), "attacker move is not check at ply " + ply);
                if (ply < EXPECTED_PLIES) {
                    require(!position.legalMoves().isEmpty(), "premature mate at ply " + ply);
                }
            } else {
                require(!position.inCheck(), "defender remains in check at ply " + ply);
            }
        }

        require(position.isMate(), "record does not end in mate");
        System.out.println("Microcosmos record passed: 1525 legal plies ending in mate");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
