package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private static final Map<Integer, Integer> EXPECTED_MASTERPIECE_COUNTS = Map.of(
            7, 2,
            9, 3,
            11, 2,
            13, 1,
            15, 2
    );
    private static final List<String> MASTERPIECE_AUTHORS = List.of(
            "波崎黒生",
            "加藤徹",
            "加藤徹",
            "小湊奈美子",
            "ドうえもん",
            "岩田俊二",
            "三枝・三木・岸本",
            "菅野哲郎",
            "吉田京平",
            "駒場和男"
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
        if (args.length != 2) {
            throw new IllegalArgumentException("expected paths to tsume.json and tsume_masterpieces.json");
        }
        TsumePuzzleCatalogSelfTest test = new TsumePuzzleCatalogSelfTest();
        Set<String> positions = new HashSet<>();
        test.validate(Path.of(args[0]), "ordinary",
                "board_state.tlm_shogi.tsume", null,
                180, EXPECTED_COUNTS, positions, true);
        test.validate(Path.of(args[1]), "masterpiece",
                "board_state.tlm_shogi.tsume.masterpiece", MASTERPIECE_AUTHORS,
                10, EXPECTED_MASTERPIECE_COUNTS, positions, false);
        test.equal(190, positions.size(), "combined catalog positions are unique");
        System.out.println("Tsume catalog self-test passed: " + test.checks + " checks");
    }

    private void validate(Path path, String catalogName, String descriptionPrefix,
                          List<String> expectedAuthors, int expectedTotal,
                          Map<Integer, Integer> expectedCounts, Set<String> positions,
                          boolean requireAttackerSafeAtStart) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = RECORD.matcher(json);
        Map<Integer, Integer> actualCounts = new HashMap<>();
        int records = 0;
        while (matcher.find()) {
            records++;
            String description = matcher.group(1);
            String author = matcher.group(2);
            String sfen = matcher.group(3);
            int maximumPly = Integer.parseInt(matcher.group(4));
            int weight = Integer.parseInt(matcher.group(5));

            equal(descriptionPrefix, description,
                    catalogName + " record " + records + " description");
            String expectedAuthor = expectedAuthors == null ? UNKNOWN_AUTHOR : expectedAuthors.get(records - 1);
            equal(expectedAuthor, author, catalogName + " record " + records + " author");
            equal(1, weight, catalogName + " record " + records + " weight");
            check(maximumPly > 0 && (maximumPly & 1) == 1,
                    catalogName + " record " + records + " has a positive odd move limit");
            actualCounts.merge(maximumPly, 1, Integer::sum);

            Position position = Position.fromSfen(sfen);
            equal(Turn.BLACK, position.turn(), catalogName + " record " + records + " starts with the player");
            equal(1L, count(position, Piece.WHITE_KING),
                    catalogName + " record " + records + " has exactly one defender king");
            check(count(position, Piece.BLACK_KING) <= 1,
                    catalogName + " record " + records + " has at most one attacker king");
            if (requireAttackerSafeAtStart) {
                check(!position.inCheck(Turn.BLACK),
                        catalogName + " record " + records + " does not start with the attacker in check");
            }
            check(position.legalMoves().stream().anyMatch(position::isCheck),
                    catalogName + " record " + records + " has at least one legal checking move");

            String[] fields = sfen.trim().split("\\s+");
            check(positions.add(String.join(" ", fields[0], fields[1], fields[2])),
                    catalogName + " record " + records + " is unique across both catalogs");
        }

        String residue = matcher.replaceAll("").replaceAll("[\\s,\\[\\]]", "");
        check(residue.isEmpty(), catalogName + " catalog contains only recognized records");
        equal(expectedTotal, records, catalogName + " catalog record count");
        equal(expectedCounts, actualCounts, catalogName + " catalog move-limit counts");
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
