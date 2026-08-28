package com.github.sangeeeee.tlm_shogi.util;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class JChessUtil {
    public static final String INIT = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1";
//    public static final String INIT = "4k4/1R7/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b B2G2S2N2L9P 1";

    /** Player-hand slot geometry in the board's player-oriented coordinate system. */
    public static final double HAND_MIN_X = 0.5120;
    public static final double HAND_MIN_Z = 0.1570;
    public static final double HAND_SLOT_WIDTH = 0.1167;
    public static final double HAND_SLOT_DEPTH = 0.1096;
    public static final int HAND_COLUMNS = 3;
    public static final int HAND_ROWS = 3;

    /** The board/hand-stand surface and one rendered shogi-piece thickness, in blocks. */
    public static final double BOARD_SURFACE_Y = 10.0 / 16.0;
    public static final double HAND_STACK_LAYER_HEIGHT = 0.0160;
    private static final double CLICK_EPSILON = 1.0e-4;

    public static boolean isClickResetArea(Vec3 clickPos) {
        double x = clickPos.x;
        double y = clickPos.y;
        double z = clickPos.z;
        if (y == 0.625) {
            return false;
        }
        if (x == -0.5) {
            return -0.125 < z && z < 0.5;
        } else if (x == 0.5) {
            return -0.5 < z && z < 0.125;
        }
        return false;
    }

    public static int getClickPosition(Vec3 clickPos, Position position) {
        double x = (clickPos.x + 0.4744) / 0.1055;
        double z = (clickPos.z + 0.4744) / 0.1055;
        int xFloor = (int) Math.floor(x);
        int zFloor = (int) Math.floor(z);
        if (x >= 0 && z >= 0 && xFloor < 9 && zFloor < 9) {
            return xFloor + zFloor * 9;
        }

        int handIndex = getPlayerHandSlot(clickPos.x, clickPos.z);
        List<int[]> hand = position.getBlackHand();
        if (handIndex < 0 || handIndex >= hand.size()) {
            return -1;
        }

        int count = hand.get(handIndex)[0];
        if (count < 1
                || clickPos.y + CLICK_EPSILON < BOARD_SURFACE_Y
                || clickPos.y - CLICK_EPSILON > handStackTopY(count)) {
            return -1;
        }
        return 81 + handIndex;
    }

    /** Returns a row-major player-hand slot, or {@code -1} outside the 3x3 stand grid. */
    public static int getPlayerHandSlot(double x, double z) {
        double slotX = (x - HAND_MIN_X) / HAND_SLOT_WIDTH;
        double slotZ = (z - HAND_MIN_Z) / HAND_SLOT_DEPTH;
        if (slotX < 0 || slotZ < 0) {
            return -1;
        }
        int column = (int) Math.floor(slotX);
        int row = (int) Math.floor(slotZ);
        if (column >= HAND_COLUMNS || row >= HAND_ROWS) {
            return -1;
        }
        return column + row * HAND_COLUMNS;
    }

    public static double handStackTopY(int count) {
        return BOARD_SURFACE_Y + Math.max(0, count) * HAND_STACK_LAYER_HEIGHT;
    }

    public static boolean isWhite(int piecesIndex) {
        return 24 <= piecesIndex && piecesIndex <= 37;
    }

    public static boolean isBlack(int piecesIndex) {
        return 10 <= piecesIndex && piecesIndex <= 23;
    }

    public static boolean isPlayer(Position position) {
        return position.isPlayer();
    }

    public static boolean isMaid(Position position) {
        return !position.isPlayer();
    }
}
