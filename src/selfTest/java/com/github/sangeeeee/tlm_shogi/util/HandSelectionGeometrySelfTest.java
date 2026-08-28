package com.github.sangeeeee.tlm_shogi.util;

/** Verifies that each hand-piece footprint is selectable and every visual gap is not. */
public final class HandSelectionGeometrySelfTest {
    private static final double DELTA = 0.00001;

    private HandSelectionGeometrySelfTest() {
    }

    public static void main(String[] args) {
        require(JChessUtil.HAND_PIECE_WIDTH < JChessUtil.HAND_SLOT_WIDTH,
                "piece width must leave a horizontal gap");
        require(JChessUtil.HAND_PIECE_DEPTH < JChessUtil.HAND_SLOT_DEPTH,
                "piece depth must leave a row gap");

        for (int index = 0; index < JChessUtil.HAND_COLUMNS * JChessUtil.HAND_ROWS; index++) {
            int column = index % JChessUtil.HAND_COLUMNS;
            int row = index / JChessUtil.HAND_COLUMNS;
            double minX = JChessUtil.HAND_PIECE_MIN_X + column * JChessUtil.HAND_SLOT_WIDTH;
            double maxX = minX + JChessUtil.HAND_PIECE_WIDTH;
            double minZ = JChessUtil.HAND_PIECE_MIN_Z + row * JChessUtil.HAND_SLOT_DEPTH;
            double maxZ = minZ + JChessUtil.HAND_PIECE_DEPTH;

            equal(index, JChessUtil.getPlayerHandSlot((minX + maxX) / 2, (minZ + maxZ) / 2),
                    "piece center " + index);
            equal(index, JChessUtil.getPlayerHandSlot(minX, minZ),
                    "piece minimum corner " + index);
            equal(index, JChessUtil.getPlayerHandSlot(maxX, maxZ),
                    "piece maximum corner " + index);

            if (column + 1 < JChessUtil.HAND_COLUMNS) {
                double nextMinX = minX + JChessUtil.HAND_SLOT_WIDTH;
                equal(-1, JChessUtil.getPlayerHandSlot((maxX + nextMinX) / 2, (minZ + maxZ) / 2),
                        "horizontal gap after piece " + index);
            }
            if (row + 1 < JChessUtil.HAND_ROWS) {
                double nextMinZ = minZ + JChessUtil.HAND_SLOT_DEPTH;
                equal(-1, JChessUtil.getPlayerHandSlot((minX + maxX) / 2, (maxZ + nextMinZ) / 2),
                        "row gap after piece " + index);
            }
        }

        double firstCenterZ = JChessUtil.HAND_PIECE_MIN_Z + JChessUtil.HAND_PIECE_DEPTH / 2;
        equal(-1, JChessUtil.getPlayerHandSlot(JChessUtil.HAND_PIECE_MIN_X - DELTA, firstCenterZ),
                "space immediately left of the first piece");
        equal(-1, JChessUtil.getPlayerHandSlot(0.5200, firstCenterZ),
                "former number area");
        System.out.println("Hand selection geometry passed: tight footprints and unselectable gaps");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void equal(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
