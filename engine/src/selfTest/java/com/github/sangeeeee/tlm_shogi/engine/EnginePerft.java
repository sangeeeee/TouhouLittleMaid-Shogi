package com.github.sangeeeee.tlm_shogi.engine;

import com.github.sangeeeee.tlm_shogi.engine.core.MoveGenerator;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;

import java.util.Map;

/** Standalone correctness and throughput probe for the Java rule layer. */
public final class EnginePerft {
    private static final Map<Integer, Long> START_POSITION_RESULTS = Map.of(
            0, 1L,
            1, 30L,
            2, 900L,
            3, 25_470L,
            4, 719_731L,
            5, 19_861_490L
    );

    private EnginePerft() {
    }

    public static void main(String[] arguments) {
        int depth = arguments.length == 0 ? 4 : Integer.parseInt(arguments[0]);
        Position position = Position.startPosition();
        long started = System.nanoTime();
        long nodes = MoveGenerator.perft(position, depth);
        long elapsed = System.nanoTime() - started;
        Long expected = START_POSITION_RESULTS.get(depth);
        if (expected != null && nodes != expected) {
            throw new AssertionError("perft depth " + depth + ": expected=" + expected + ", actual=" + nodes);
        }
        if (!position.verifyIncrementalState() || !position.toSfen().equals(Position.START_SFEN)) {
            throw new AssertionError("perft did not restore the initial position");
        }
        double seconds = elapsed / 1_000_000_000.0;
        long nodesPerSecond = seconds == 0 ? nodes : Math.round(nodes / seconds);
        System.out.printf("Java Sunfish perft depth %d: %,d nodes in %.3f s (%,d nodes/s)%n",
                depth, nodes, seconds, nodesPerSecond);
    }
}
