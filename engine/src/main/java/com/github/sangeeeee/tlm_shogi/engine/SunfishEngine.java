package com.github.sangeeeee.tlm_shogi.engine;

import com.github.sangeeeee.tlm_shogi.engine.book.SunfishBook;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Piece;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.search.AlphaBetaSearcher;
import com.github.sangeeeee.tlm_shogi.engine.search.SunfishEvaluator;
import com.github.sangeeeee.tlm_shogi.engine.search.TranspositionTable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lifecycle facade for the pure Java Sunfish port.
 *
 * <p>All evaluation and search work runs synchronously in the calling JVM. The
 * Minecraft integration can therefore invoke it from a client worker without
 * starting a native process or placing computation on the server.</p>
 */
public final class SunfishEngine implements ShogiEngine {
    private final SunfishResources resources;
    private final AtomicReference<EngineState> state = new AtomicReference<>(EngineState.NEW);
    private volatile SunfishResourceInfo resourceInfo;
    private SunfishEvaluator evaluator;
    private SunfishBook openingBook;
    private TranspositionTable transpositionTable;
    private final Random bookRandom = new Random();

    public SunfishEngine(SunfishResources resources) {
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    @Override
    public EngineState state() {
        return state.get();
    }

    public SunfishResourceInfo resourceInfo() {
        ensureState(EngineState.READY);
        return resourceInfo;
    }

    @Override
    public synchronized void initialize() throws EngineException {
        EngineState current = state.get();
        if (current == EngineState.READY) {
            return;
        }
        if (current == EngineState.CLOSED) {
            throw new EngineException("A closed engine cannot be initialized again");
        }

        SunfishResourceInfo inspected = resources.inspect();
        SunfishEvaluator loadedEvaluator = SunfishEvaluator.load(resources);
        SunfishBook loadedBook = SunfishBook.load(resources);
        resourceInfo = inspected;
        evaluator = loadedEvaluator;
        openingBook = loadedBook;
        state.set(EngineState.READY);
    }

    @Override
    public synchronized SearchResult search(SearchRequest request, CancellationToken cancellationToken)
            throws EngineException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        ensureState(EngineState.READY);

        if (cancellationToken.isCancellationRequested()) {
            return SearchResult.cancelled(Duration.ZERO, 0);
        }
        if (request.limits().threads() != 1) {
            throw new EngineException("The baseline Java Sunfish search currently supports exactly one thread");
        }

        long startedAt = System.nanoTime();
        PreparedPosition prepared = preparePosition(request, cancellationToken);
        if (prepared == null) return SearchResult.cancelled(Duration.ZERO, 0);

        if (request.useBook()) {
            Optional<Move> bookMove = openingBook.select(prepared.position(), bookRandom);
            if (bookMove.isPresent()) {
                String notation = bookMove.orElseThrow().toSfen();
                return new SearchResult(
                        SearchOutcome.MOVE,
                        Optional.of(notation),
                        0,
                        0,
                        0,
                        Duration.ofNanos(System.nanoTime() - startedAt),
                        List.of(notation)
                );
            }
        }

        try {
            if (transpositionTable == null) {
                transpositionTable = new TranspositionTable(request.limits().transpositionTableMiB());
            } else {
                transpositionTable.resizeMiB(request.limits().transpositionTableMiB());
            }
        } catch (OutOfMemoryError error) {
            transpositionTable = null;
            throw new EngineException("Unable to allocate the requested "
                    + request.limits().transpositionTableMiB() + " MiB transposition table", error);
        }

        return new AlphaBetaSearcher(
                evaluator,
                transpositionTable,
                request.limits(),
                cancellationToken,
                prepared.hashHistory()
        ).search(prepared.position());
    }

    @Override
    public synchronized void close() {
        resourceInfo = null;
        evaluator = null;
        openingBook = null;
        transpositionTable = null;
        state.set(EngineState.CLOSED);
    }

    private static PreparedPosition preparePosition(SearchRequest request, CancellationToken cancellationToken)
            throws EngineException {
        final Position position;
        try {
            position = Position.parse(request.sfen());
        } catch (IllegalArgumentException exception) {
            throw new EngineException("Invalid search SFEN: " + request.sfen(), exception);
        }
        int blackKings = 0;
        int whiteKings = 0;
        for (Piece piece : position.boardCopy()) {
            if (piece.equals(Piece.BLACK_KING)) blackKings++;
            if (piece.equals(Piece.WHITE_KING)) whiteKings++;
        }
        if (blackKings != 1 || whiteKings != 1) {
            throw new EngineException("Search positions must contain exactly one king for each side");
        }

        List<Long> hashes = new ArrayList<>(request.moves().size() + 1);
        hashes.add(position.getHash());
        for (int index = 0; index < request.moves().size(); index++) {
            if (cancellationToken.isCancellationRequested()) return null;
            String notation = request.moves().get(index);
            Optional<Move> parsedMove = Move.parseSfen(notation);
            if (parsedMove.isEmpty()) {
                throw new EngineException("Invalid SFEN move at index " + index + ": " + notation);
            }
            Move move = parsedMove.orElseThrow();
            if (!position.validateMove(move)) {
                throw new EngineException("Illegal move at index " + index + " for position "
                        + position.toSfen() + ": " + notation);
            }
            position.makeMoveUnchecked(move);
            hashes.add(position.getHash());
        }
        return new PreparedPosition(position, List.copyOf(hashes));
    }

    private void ensureState(EngineState expected) {
        EngineState actual = state.get();
        if (actual != expected) {
            throw new IllegalStateException("Engine state is " + actual + "; expected " + expected);
        }
    }

    private record PreparedPosition(Position position, List<Long> hashHistory) {
    }
}
