package com.github.sangeeeee.tlm_shogi.mateengine;

import java.time.Duration;

/** Command-line entry point. Prints only the chosen USI move to standard output. */
public final class MateEngineMain {
    private MateEngineMain() {
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 4) {
            throw new IllegalArgumentException(
                    "Usage: MateEngineMain <sfen> [maximumPly] [maximumNodes] [moveTimeMillis]");
        }

        MateSearchLimits defaults = MateSearchLimits.standard();
        int maximumPly = args.length >= 2 ? Integer.parseInt(args[1]) : defaults.maximumPly();
        long maximumNodes = args.length >= 3 ? Long.parseLong(args[2]) : defaults.maximumNodes();
        long moveTimeMillis = args.length >= 4
                ? Long.parseLong(args[3])
                : defaults.moveTime().toMillis();

        MateSearchResult result = new MateEngine().search(
                args[0],
                new MateSearchLimits(Duration.ofMillis(moveTimeMillis), maximumPly, maximumNodes)
        );

        System.out.println(result.bestMove().orElse("resign"));
        System.err.printf(
                "outcome=%s matePlies=%d nodes=%d reachedPly=%d elapsed=%dms pv=%s%n",
                result.outcome(), result.matePlies(), result.nodes(), result.reachedPly(),
                result.elapsed().toMillis(), String.join(" ", result.principalVariation()));
    }
}
