package com.github.sangeeeee.tlm_shogi.util;

import com.github.sangeeeee.tlm_shogi.block.properties.ShogiPart;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;
import com.github.sangeeeee.tlm_shogi.engine.core.Turn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class JChessUtil {
    /**
     * Player-hand layout in the board's player-oriented coordinate system.
     * The slot pitch preserves the rendered 3x3 layout, while the smaller
     * piece bounds match the SELECTED model at its 0.85 render scale, excluding
     * the former number area and the gaps between stacks.
     */
    public static final double HAND_SLOT_WIDTH = 0.1167;
    public static final double HAND_SLOT_DEPTH = 0.1096;
    public static final double HAND_PIECE_MIN_X = 0.539153125;
    public static final double HAND_PIECE_MIN_Z = 0.156425;
    public static final double HAND_PIECE_WIDTH = 0.09509375;
    public static final double HAND_PIECE_DEPTH = 0.099078125;
    public static final int HAND_COLUMNS = 3;
    public static final int HAND_ROWS = 3;

    /** The board/hand-stand surface and rendered shogi-piece stack geometry, in blocks. */
    public static final double BOARD_SURFACE_Y = 10.0 / 16.0;
    public static final double HAND_PIECE_HEIGHT = 0.3 * 0.85 / 16.0;
    public static final double HAND_STACK_GAP = 0.0010;
    public static final double HAND_STACK_LAYER_HEIGHT = HAND_PIECE_HEIGHT + HAND_STACK_GAP;
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

        int handIndex = getPlayerHandPosition(clickPos, position);
        return handIndex < 0 ? -1 : 81 + handIndex;
    }

    /** Returns the occupied player-hand stack hit by this point, or {@code -1}. */
    public static int getPlayerHandPosition(Vec3 clickPos, Position position) {
        int handIndex = getPlayerHandSlot(clickPos.x, clickPos.z);
        List<JChessUiAdapter.HandStack> hand = JChessUiAdapter.handStacks(position, Turn.BLACK);
        if (handIndex < 0 || handIndex >= hand.size()) {
            return -1;
        }

        int count = hand.get(handIndex).count();
        if (count < 1
                || clickPos.y + CLICK_EPSILON < BOARD_SURFACE_Y
                || clickPos.y - CLICK_EPSILON > handStackTopY(count)) {
            return -1;
        }
        return handIndex;
    }

    /** Returns a row-major player-hand slot, or {@code -1} outside the 3x3 stand grid. */
    public static int getPlayerHandSlot(double x, double z) {
        // There are only nine stacks. Testing their exact rectangles directly
        // avoids floating-point boundary errors from pitch-based division.
        for (int row = 0; row < HAND_ROWS; row++) {
            double pieceMinZ = HAND_PIECE_MIN_Z + row * HAND_SLOT_DEPTH;
            double pieceMaxZ = pieceMinZ + HAND_PIECE_DEPTH;
            if (z < pieceMinZ || z > pieceMaxZ) {
                continue;
            }
            for (int column = 0; column < HAND_COLUMNS; column++) {
                double pieceMinX = HAND_PIECE_MIN_X + column * HAND_SLOT_WIDTH;
                double pieceMaxX = pieceMinX + HAND_PIECE_WIDTH;
                if (x >= pieceMinX && x <= pieceMaxX) {
                    return column + row * HAND_COLUMNS;
                }
            }
        }
        return -1;
    }

    public static double handStackTopY(int count) {
        if (count < 1) {
            return BOARD_SURFACE_Y;
        }
        return BOARD_SURFACE_Y + HAND_PIECE_HEIGHT + (count - 1) * HAND_STACK_LAYER_HEIGHT;
    }

    /** Converts a hit on any of the three board blocks into player-oriented board coordinates. */
    public static Vec3 toPlayerOrientedClick(Vec3 hitLocation, BlockPos blockPos,
                                              ShogiPart part, Direction facing) {
        return hitLocation
                .subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                .add(part.getPosX() - 0.5, 0, part.getPosY() - 0.5)
                .yRot(facing.toYRot() * Mth.DEG_TO_RAD);
    }

}
