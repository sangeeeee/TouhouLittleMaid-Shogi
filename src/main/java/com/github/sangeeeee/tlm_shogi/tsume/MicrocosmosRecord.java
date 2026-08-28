package com.github.sangeeeee.tlm_shogi.tsume;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The fixed 1,525-ply solution record for Koji Hashimoto's Microcosmos. */
public final class MicrocosmosRecord {
    public static final String INITIAL_SFEN =
            "g1+P1k1+P+P+L/1p3P3/+R+p2pp1pl/1NNsg+p2+R/+b+nL+P1+p3/1P3ssP1/2P1+Ps2N/4+P1P1L/+B5G1g b - 1";
    public static final int MAXIMUM_PLY = 1525;
    public static final String AUTHOR = "橋本孝治";
    public static final String DESCRIPTION_KEY = "board_state.tlm_shogi.tsume.microcosmos";

    private static final String RESOURCE = "/data/tlm_shogi/tsume/microcosmos.usi";
    private static final List<String> MOVES = loadMoves();

    private MicrocosmosRecord() {
    }

    /** Returns whether a move is the recorded move at the zero-based ply. */
    public static boolean isRecordedMove(int zeroBasedPly, String move) {
        return zeroBasedPly >= 0
                && zeroBasedPly < MOVES.size()
                && move != null
                && MOVES.get(zeroBasedPly).equals(move.trim());
    }

    /**
     * Returns the recorded defender response after {@code appliedPly} moves.
     * The attacker moves on odd plies, so a response exists only for an odd,
     * non-final ply.
     */
    public static Optional<String> defenseAfter(int appliedPly) {
        if (appliedPly <= 0 || (appliedPly & 1) == 0 || appliedPly >= MOVES.size()) {
            return Optional.empty();
        }
        return Optional.of(MOVES.get(appliedPly));
    }

    public static int moveCount() {
        return MOVES.size();
    }

    private static List<String> loadMoves() {
        String source;
        try (InputStream input = MicrocosmosRecord.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled Microcosmos record: " + RESOURCE);
            }
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read bundled Microcosmos record", exception);
        }

        String[] tokens = source.split("\\s+");
        int expectedTokens = 7 + MAXIMUM_PLY + 1;
        if (tokens.length != expectedTokens
                || !"position".equals(tokens[0])
                || !"sfen".equals(tokens[1])
                || !"moves".equals(tokens[6])
                || !"mate".equals(tokens[tokens.length - 1])) {
            throw new IllegalStateException("Malformed bundled Microcosmos record");
        }

        String parsedInitial = String.join(" ", tokens[2], tokens[3], tokens[4], tokens[5]);
        if (!Position.parse(INITIAL_SFEN).toSfen().equals(Position.parse(parsedInitial).toSfen())) {
            throw new IllegalStateException("Microcosmos record has an unexpected initial SFEN");
        }

        List<String> moves = new ArrayList<>(MAXIMUM_PLY);
        for (int index = 0; index < MAXIMUM_PLY; index++) {
            String move = tokens[7 + index];
            if (Move.parseSfen(move).isEmpty()) {
                throw new IllegalStateException("Invalid Microcosmos move at ply " + (index + 1) + ": " + move);
            }
            moves.add(move);
        }
        return List.copyOf(moves);
    }
}
