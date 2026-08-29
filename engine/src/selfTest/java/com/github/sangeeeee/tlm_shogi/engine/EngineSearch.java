package com.github.sangeeeee.tlm_shogi.engine;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Runs one real eval.bin-backed search without loading Minecraft or NeoForge. */
public final class EngineSearch {
    private EngineSearch() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 5) {
            throw new IllegalArgumentException(
                    "Usage: EngineSearch <resource-directory> [depth] [milliseconds] [nodes] [sfen]"
            );
        }

        Path resourceDirectory = Path.of(args[0]);
        int depth = args.length >= 2 ? Integer.parseInt(args[1]) : 5;
        long milliseconds = args.length >= 3 ? Long.parseLong(args[2]) : 3_000L;
        long maximumNodes = args.length >= 4 ? Long.parseLong(args[3]) : 30_000L;
        String sfen = args.length >= 5 ? args[4] : "startpos";
        SearchLimits limits = new SearchLimits(
                Duration.ofMillis(milliseconds),
                depth,
                maximumNodes,
                64,
                1
        );

        try (SunfishEngine engine = new SunfishEngine(SunfishResources.fromDirectory(resourceDirectory))) {
            engine.initialize();
            SearchResult result = engine.search(
                    new SearchRequest(sfen, List.of(), limits, false),
                    CancellationToken.none()
            );
            double seconds = Math.max(0.001, result.elapsed().toNanos() / 1_000_000_000.0);
            long nodesPerSecond = Math.round(result.nodes() / seconds);

            System.out.println("outcome: " + result.outcome());
            System.out.println("bestmove: " + result.bestMove().orElse("none"));
            System.out.println("score: " + result.score());
            System.out.println("depth: " + result.depth());
            System.out.println("nodes: " + result.nodes());
            System.out.println("elapsed: " + result.elapsed().toMillis() + " ms");
            System.out.println("nodes/s: " + nodesPerSecond);
            System.out.println("pv: " + String.join(" ", result.principalVariation()));
        }
    }
}
