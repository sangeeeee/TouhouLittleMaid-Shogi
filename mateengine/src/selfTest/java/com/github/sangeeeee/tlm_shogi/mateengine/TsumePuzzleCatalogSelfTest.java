package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates the shipped puzzle catalog without loading Minecraft or Gson. */
public final class TsumePuzzleCatalogSelfTest {
    private static final String UNKNOWN_AUTHOR = "board_state.tlm_shogi.tsume.author.unknown";
    private static final Map<Integer, Integer> EXPECTED_COUNTS = Map.of(
            3, 10,
            5, 20,
            7, 50,
            9, 50,
            11, 50
    );
    private static final Pattern RECORD = Pattern.compile(
            "\\{\\s*\"tags\"\\s*:\\s*\\[\\s*\"library\"\\s*]\\s*,\\s*"
                    + "\"display\"\\s*:\\s*\\{\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"author\"\\s*:\\s*\"([^\"]+)\"\\s*}\\s*,\\s*"
                    + "\"data\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"maximum_ply\"\\s*:\\s*(\\d+)\\s*,\\s*"
                    + "\"weight\"\\s*:\\s*(\\d+)\\s*}",
            Pattern.DOTALL);

    private int checks;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("expected path to tsume.json");
        }
        TsumePuzzleCatalogSelfTest test = new TsumePuzzleCatalogSelfTest();
        test.validate(Path.of(args[0]));
        System.out.println("Tsume catalog self-test passed: " + test.checks + " checks");
    }

    private void validate(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = RECORD.matcher(json);
        Map<Integer, Integer> actualCounts = new HashMap<>();
        Set<String> positions = new HashSet<>();
        int records = 0;
        while (matcher.find()) {
            records++;
            String description = matcher.group(1);
            String author = matcher.group(2);
            String sfen = matcher.group(3);
            int maximumPly = Integer.parseInt(matcher.group(4));
            int weight = Integer.parseInt(matcher.group(5));

            equal("board_state.tlm_shogi.tsume.mate" + maximumPly, description,
                    "record " + records + " description");
            equal(UNKNOWN_AUTHOR, author, "record " + records + " author");
            equal(1, weight, "record " + records + " weight");
            check(maximumPly > 0 && (maximumPly & 1) == 1,
                    "record " + records + " has a positive odd move limit");
            actualCounts.merge(maximumPly, 1, Integer::sum);

            Position position = Position.fromSfen(sfen);
            equal(Turn.BLACK, position.turn(), "record " + records + " starts with the player");
            equal(1L, count(position, Piece.WHITE_KING),
                    "record " + records + " has exactly one defender king");
            check(count(position, Piece.BLACK_KING) <= 1,
                    "record " + records + " has at most one attacker king");
            check(!position.inCheck(Turn.BLACK),
                    "record " + records + " does not start with the attacker in check");
            check(position.legalMoves().stream().anyMatch(position::isCheck),
                    "record " + records + " has at least one legal checking move");

            String[] fields = sfen.trim().split("\\s+");
            check(positions.add(String.join(" ", fields[0], fields[1], fields[2])),
                    "record " + records + " is unique");
        }

        String residue = matcher.replaceAll("").replaceAll("[\\s,\\[\\]]", "");
        check(residue.isEmpty(), "catalog contains only recognized records");
        equal(180, records, "catalog record count");
        equal(EXPECTED_COUNTS, actualCounts, "catalog move-limit counts");
    }

    private static long count(Position position, Piece expected) {
        long count = 0;
        for (Piece piece : position.boardCopy()) {
            if (piece.equals(expected)) {
                count++;
            }
        }
        return count;
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
