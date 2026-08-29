package com.github.sangeeeee.tlm_shogi.engine.book;

import com.github.sangeeeee.tlm_shogi.engine.EngineException;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResources;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Reader and weighted move selector for Sunfish 4's {@code book.bin} format.
 *
 * <p>Despite its extension, the upstream file is UTF-8 text. A position line
 * starts with {@code sfen }, followed by one or more {@code move count} lines.
 * Sunfish stores positions with the SFEN move number fixed to {@code 1}; lookup
 * therefore deliberately ignores the Java position's current move number.</p>
 */
public final class SunfishBook {
    private static final int MAX_COUNT = 0xffff;

    private final Map<String, List<BookMove>> movesByPosition;
    private final int moveCount;

    private SunfishBook(Map<String, List<BookMove>> movesByPosition, int moveCount) {
        this.movesByPosition = movesByPosition;
        this.moveCount = moveCount;
    }

    /** Loads and closes the opening-book stream supplied by the engine resources. */
    public static SunfishBook load(SunfishResources resources) throws EngineException {
        Objects.requireNonNull(resources, "resources");
        try (InputStream input = resources.openBook()) {
            return load(input, resources.bookDescription());
        } catch (IOException exception) {
            throw new EngineException("Failed to close Sunfish opening book: "
                    + resources.bookDescription(), exception);
        }
    }

    /** Parses an opening book without closing the caller-owned stream. */
    public static SunfishBook load(InputStream input, String description) throws EngineException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(description, "description");

        Map<String, List<BookMove>> mutable = new HashMap<>();
        String currentKey = null;
        int lineNumber = 0;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.startsWith("sfen ")) {
                    String sfen = line.substring(5).strip();
                    if (sfen.isEmpty()) {
                        throw formatError(description, lineNumber, "position SFEN is empty");
                    }
                    try {
                        currentKey = positionKey(Position.parse(sfen));
                    } catch (IllegalArgumentException exception) {
                        throw formatError(description, lineNumber, "invalid position SFEN", exception);
                    }
                    continue;
                }

                if (currentKey == null) {
                    throw formatError(description, lineNumber, "a position is required before moves");
                }

                String[] columns = line.strip().split("\\s+");
                if (columns.length < 2 || columns[0].isEmpty()) {
                    throw formatError(description, lineNumber, "expected '<move> <count>'");
                }

                Optional<Move> parsedMove = Move.parseSfen(columns[0]);
                if (parsedMove.isEmpty()) {
                    throw formatError(description, lineNumber, "invalid move: " + columns[0]);
                }
                Move move = parsedMove.orElseThrow();
                if (move.isNone()) {
                    throw formatError(description, lineNumber, "opening-book move must not be 'none'");
                }

                int count;
                try {
                    count = Integer.parseInt(columns[1]);
                } catch (NumberFormatException exception) {
                    throw formatError(description, lineNumber, "invalid move count: " + columns[1], exception);
                }
                if (count < 1 || count > MAX_COUNT) {
                    throw formatError(description, lineNumber,
                            "move count must be between 1 and " + MAX_COUNT + ": " + count);
                }

                insert(mutable.computeIfAbsent(currentKey, ignored -> new ArrayList<>()),
                        move, count, description, lineNumber);
            }
        } catch (IOException exception) {
            throw new EngineException("Failed to read Sunfish opening book: " + description, exception);
        }

        Map<String, List<BookMove>> immutable = new HashMap<>(mutable.size());
        int loadedMoveCount = 0;
        for (Map.Entry<String, List<BookMove>> entry : mutable.entrySet()) {
            List<BookMove> moves = List.copyOf(entry.getValue());
            immutable.put(entry.getKey(), moves);
            loadedMoveCount += moves.size();
        }
        return new SunfishBook(Collections.unmodifiableMap(immutable), loadedMoveCount);
    }

    /** Returns the immutable weighted candidates for the supplied position. */
    public List<BookMove> moves(Position position) {
        Objects.requireNonNull(position, "position");
        return movesByPosition.getOrDefault(positionKey(position), List.of());
    }

    /**
     * Selects one legal candidate with probability proportional to its count,
     * matching Sunfish's {@code BookUtil::select} behavior.
     */
    public Optional<Move> select(Position position, RandomGenerator random) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(random, "random");

        List<BookMove> candidates = moves(position);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        List<BookMove> legal = new ArrayList<>(candidates.size());
        long total = 0;
        for (BookMove candidate : candidates) {
            if (position.validateMove(candidate.move())) {
                legal.add(candidate);
                total += candidate.count();
            }
        }
        if (legal.isEmpty()) {
            return Optional.empty();
        }

        long ticket = random.nextLong(total);
        for (int index = 0; index < legal.size() - 1; index++) {
            BookMove candidate = legal.get(index);
            if (ticket < candidate.count()) {
                return Optional.of(candidate.move());
            }
            ticket -= candidate.count();
        }
        return Optional.of(legal.getLast().move());
    }

    public int positionCount() {
        return movesByPosition.size();
    }

    public int moveCount() {
        return moveCount;
    }

    private static String positionKey(Position position) {
        String sfen = position.toSfen();
        int finalSeparator = sfen.lastIndexOf(' ');
        if (finalSeparator < 0) {
            throw new IllegalStateException("Position produced an invalid SFEN: " + sfen);
        }
        return sfen.substring(0, finalSeparator + 1) + '1';
    }

    private static void insert(List<BookMove> moves, Move move, int count,
                               String description, int lineNumber) throws EngineException {
        for (int index = 0; index < moves.size(); index++) {
            BookMove existing = moves.get(index);
            if (!existing.move().equals(move)) {
                continue;
            }
            int merged = existing.count() + count;
            if (merged > MAX_COUNT) {
                throw formatError(description, lineNumber,
                        "combined move count exceeds Sunfish's 16-bit limit: " + merged);
            }
            moves.set(index, new BookMove(move, merged));
            return;
        }
        moves.add(new BookMove(move, count));
    }

    private static EngineException formatError(String description, int lineNumber, String message) {
        return new EngineException("Invalid Sunfish opening book " + description
                + " at line " + lineNumber + ": " + message);
    }

    private static EngineException formatError(String description, int lineNumber,
                                               String message, Throwable cause) {
        return new EngineException("Invalid Sunfish opening book " + description
                + " at line " + lineNumber + ": " + message, cause);
    }

    /** A Sunfish move and its observed-game count. */
    public record BookMove(Move move, int count) {
        public BookMove {
            Objects.requireNonNull(move, "move");
            if (move.isNone() || count < 1 || count > MAX_COUNT) {
                throw new IllegalArgumentException("invalid opening-book entry");
            }
        }
    }
}
