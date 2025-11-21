package com.github.sangeeeee.tlm_shogi.util;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import net.minecraft.world.phys.Vec3;

public final class JChessUtil {
    public static final String INIT = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1";
//    public static final String INIT = "4k4/1R7/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b B2G2S2N2L9P 1";

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

    public static int getClickPosition(Vec3 clickPos) {
        double x = (clickPos.x + 0.4744) / 0.1055;
        double z = (clickPos.z + 0.4744) / 0.1055;
        if (x < 0 || z < 0) {
            return -1;
        }
        int xFloor = (int) Math.floor(x);
        int zFloor = (int) Math.floor(z);
        if (xFloor < 9 && zFloor < 9) {
            return xFloor + zFloor * 9;
        } else {
            double plate_grid_x = 0.1167;
            double plate_grid_z = 0.1096;
            x = (clickPos.x  - 0.5120) / plate_grid_x;
            z = (clickPos.z - 0.1570) / plate_grid_z;
            if (x < 0 || z < 0) {
                return -1;
            }
            xFloor = (int) Math.floor(x);
            zFloor = (int) Math.floor(z);
            return 81 + xFloor + zFloor * 3;
        }
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
