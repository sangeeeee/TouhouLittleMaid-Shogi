package com.github.sangeeeee.tlm_shogi.engine;

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
        cancellationIsVisibleThroughToken();
        casualLimitsMatchTheCurrentModConfiguration();
        requestDefensivelyCopiesMoveHistory();
        resultRejectsMoveOutcomeWithoutMove();
        engineInitializesAndHonorsCancellation();
        engineReportsMissingSearchPort();
        resourceVersionMismatchFailsInitialization();
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
