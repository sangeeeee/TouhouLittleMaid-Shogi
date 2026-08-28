package com.github.sangeeeee.tlm_shogi.engine.tool;

import com.github.sangeeeee.tlm_shogi.engine.CancellationToken;
import com.github.sangeeeee.tlm_shogi.engine.SearchLimits;
import com.github.sangeeeee.tlm_shogi.engine.SearchOutcome;
import com.github.sangeeeee.tlm_shogi.engine.SearchRequest;
import com.github.sangeeeee.tlm_shogi.engine.SearchResult;
import com.github.sangeeeee.tlm_shogi.engine.SunfishEngine;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResourceInfo;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResources;
import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;

import java.time.Duration;

/** Verifies that a packaged mod JAR can initialize and search without external data files. */
public final class BundledEngineSmoke {
    private static final String RESOURCE_DIRECTORY = "assets/tlm_shogi/sunfish";

    private BundledEngineSmoke() {
    }

    public static void main(String[] args) throws Exception {
        SunfishResources resources = SunfishResources.fromClasspath(
                BundledEngineSmoke.class.getClassLoader(),
                RESOURCE_DIRECTORY
        );
        SearchLimits limits = new SearchLimits(Duration.ofSeconds(2), 3, 5_000, 8, 1);

        try (SunfishEngine engine = new SunfishEngine(resources)) {
            engine.initialize();
            SunfishResourceInfo info = engine.resourceInfo();
            SearchResult result = engine.search(
                    SearchRequest.currentPosition("startpos", limits),
                    CancellationToken.none()
            );
            if (result.outcome() != SearchOutcome.MOVE) {
                throw new IllegalStateException("Packaged engine returned " + result.outcome());
            }

            String notation = result.bestMove().orElseThrow();
            Move move = Move.parseSfen(notation).orElseThrow(
                    () -> new IllegalStateException("Packaged engine returned an invalid move: " + notation)
            );
            if (!Position.startPosition().validateMove(move)) {
                throw new IllegalStateException("Packaged engine returned an illegal move: " + notation);
            }

            System.out.printf(
                    "Packaged Java Sunfish OK: eval=%d, book=%d, move=%s, depth=%d, nodes=%d, elapsed=%dms%n",
                    info.evalBytes(),
                    info.bookBytes(),
                    notation,
                    result.depth(),
                    result.nodes(),
                    result.elapsed().toMillis()
            );
        }
    }
}
