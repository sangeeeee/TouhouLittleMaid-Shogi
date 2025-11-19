package com.github.tartaricacid.touhoulittlemaid.api.game.jchess;

import com.github.tartaricacid.touhoulittlemaid.util.JChessUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class PositionUtil {

    // ==================== 棋子映射 ====================
    private static final Map<String, Integer> PIECE_STR_TO_ID = new HashMap<>();
    private static final Map<Integer, String> ID_TO_PIECE_STR = new HashMap<>();

    private static final Map<Integer, Integer> ID_TO_BASE_ID = new HashMap<>();

    static {
        // 黑方（先手）：大写 → models[10-23]
        putBoth('P', 17, 21); // PAWN_B
        putBoth('L', 16, 20);
        putBoth('N', 15, 19);
        putBoth('S', 12, 18);
        putBoth('G', 11, 11);
        putBoth('B', 14, 23);
        putBoth('R', 13, 22);
        putBoth('K', 10, 10);

        // 白方（后手）：小写 → models[24-37]
        putBoth('p', 31, 35);
        putBoth('l', 30, 34);
        putBoth('n', 29, 33);
        putBoth('s', 26, 32);
        putBoth('g', 25, 25);
        putBoth('b', 28, 37);
        putBoth('r', 27, 36);
        putBoth('k', 24, 24);

        ID_TO_BASE_ID.put(10, 10); ID_TO_BASE_ID.put(11, 11); ID_TO_BASE_ID.put(12, 12);
        ID_TO_BASE_ID.put(13, 13); ID_TO_BASE_ID.put(14, 14); ID_TO_BASE_ID.put(15, 15);
        ID_TO_BASE_ID.put(16, 16); ID_TO_BASE_ID.put(17, 17);
        ID_TO_BASE_ID.put(18, 12); ID_TO_BASE_ID.put(19, 15); ID_TO_BASE_ID.put(20, 16);
        ID_TO_BASE_ID.put(21, 17); ID_TO_BASE_ID.put(22, 13); ID_TO_BASE_ID.put(23, 14);

        ID_TO_BASE_ID.put(24, 24); ID_TO_BASE_ID.put(25, 25); ID_TO_BASE_ID.put(26, 26);
        ID_TO_BASE_ID.put(27, 27); ID_TO_BASE_ID.put(28, 28); ID_TO_BASE_ID.put(29, 29);
        ID_TO_BASE_ID.put(30, 30); ID_TO_BASE_ID.put(31, 31);
        ID_TO_BASE_ID.put(32, 26); ID_TO_BASE_ID.put(33, 29); ID_TO_BASE_ID.put(34, 30);
        ID_TO_BASE_ID.put(35, 31); ID_TO_BASE_ID.put(36, 27); ID_TO_BASE_ID.put(37, 28);
    }

    private static void putBoth(char baseChar, int baseId, int promotedId) {
        boolean isBlack = Character.isUpperCase(baseChar);
        char pieceChar = isBlack ? Character.toUpperCase(baseChar) : Character.toLowerCase(baseChar);

        String baseKey = String.valueOf(pieceChar);
        PIECE_STR_TO_ID.put(baseKey, baseId);
        ID_TO_PIECE_STR.put(baseId, baseKey);

        if (promotedId != baseId) {
            String promKey = "+" + pieceChar;
            PIECE_STR_TO_ID.put(promKey, promotedId);
            ID_TO_PIECE_STR.put(promotedId, promKey);
        }
    }

    /**
     * 捕获棋子时，将对方棋子ID转换为当前方的未升变棋子ID
     * @param capturedPieceId 捕获的棋子ID
     * @param capturerIsBlack 捕获者是否先手（黑方）
     * @return 转换后的棋子ID，捕获王时返回 -1
     */
    public static int convertCapturedPiece(int capturedPieceId, boolean capturerIsBlack) {
        if (capturedPieceId == 0) return 0;

        // 检查是否王
        if (capturedPieceId == 10 || capturedPieceId == 24) {
            return -1; // 王不能被捕获
        }

        // 还原到未升变ID
        int baseId = ID_TO_BASE_ID.getOrDefault(capturedPieceId, capturedPieceId);

        // 转换为捕获者阵营的ID
        if (capturerIsBlack) {
            // 黑方：24-37 → 10-23
            return baseId - 14;
        } else {
            // 白方：10-23 → 24-37
            return baseId + 14;
        }
    }

    // ==================== USI sfen → Position ====================
    public static Position fromSfen(String usi) {
        Position pos = new Position();
        String[] parts = usi.trim().split("\\s+");
        if (parts.length < 3) throw new IllegalArgumentException("Invalid USI format");

        String boardStr = parts[0];
        char turn = parts[1].charAt(0);
        String handStr = parts[2];
        int moveNum = parts.length > 3 ? Integer.parseInt(parts[3]) : 1;

        // --- 解析棋盘 ---
        String[] rows = boardStr.split("/");
        if (rows.length != 9) throw new IllegalArgumentException("Board must have 9 rows");

        for (int i = 0; i < 9; i++) {
            String row = rows[i];
            int j = 0, k = 0;
            while (k < row.length() && j < 9) {
                char c = row.charAt(k);

                if (Character.isDigit(c)) {
                    j += c - '0';
                    k++;
                    continue;
                }

                String token;
                if (c == '+') {
                    if (k + 1 >= row.length()) throw new IllegalArgumentException("Invalid +");
                    char next = row.charAt(k + 1);
                    token = "+" + next;
                    k += 2;
                } else {
                    token = String.valueOf(c);
                    k++;
                }

                Integer id = PIECE_STR_TO_ID.get(token);
                if (id == null) throw new IllegalArgumentException("Unknown piece: " + token);

                pos.setPieceAt(j, i, id);
                j++;
            }
            if (j != 9) throw new IllegalArgumentException("Row " + i + " length error");
        }

        pos.setTurn(turn);
        pos.setMoveNumber(moveNum);

        // --- 手牌：关键修复 ---
        if (!"-".equals(handStr)) {
            int idx = 0;
            while (idx < handStr.length()) {
                // 跳过数字（数量在字母前）
                StringBuilder numBuf = new StringBuilder();
                while (idx < handStr.length() && Character.isDigit(handStr.charAt(idx))) {
                    numBuf.append(handStr.charAt(idx));
                    idx++;
                }
                int count = !numBuf.isEmpty() ? Integer.parseInt(numBuf.toString()) : 1;

                if (idx >= handStr.length()) break;

                char c = handStr.charAt(idx);
                if (!Character.isLetter(c)) {
                    throw new IllegalArgumentException("Expected letter after number in hand: " + handStr);
                }

                String token = String.valueOf(c);
                idx++;

                Integer id = PIECE_STR_TO_ID.get(token);
                if (id == null) throw new IllegalArgumentException("Unknown hand piece: " + token);

                // 大写 = 黑方 = 先手
                boolean isBlack = Character.isUpperCase(c);
                pos.addToHand(isBlack, id, count);
            }
        }

        return pos;
    }

    // ==================== Position → USI sfen ====================
    public static String toSfen(Position pos) {
        StringBuilder sb = new StringBuilder();

        // --- 棋盘 ---
        for (int i = 0; i < 9; i++) {
            if (i > 0) sb.append('/');
            int empty = 0;
            for (int j = 0; j < 9; j++) {
                int id = pos.getPieceAt(j, i);
                if (id == 0) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    String token = ID_TO_PIECE_STR.getOrDefault(id, "?");
                    sb.append(token);
                }
            }
            if (empty > 0) sb.append(empty);
        }

        sb.append(' ').append(pos.getTurn());

        // --- 手牌 ---
        List<int[]> black = pos.getBlackHand();  // 黑方
        List<int[]> gote = pos.getWhiteHand();    // 白方

        StringBuilder hand = new StringBuilder();
        appendHand(hand, black);  // 黑方先
        appendHand(hand, gote);

        sb.append(' ').append(hand.isEmpty() ? "-" : hand.toString());
        sb.append(' ').append(pos.getMoveNumber());

        return sb.toString();
    }

    private static void appendHand(StringBuilder sb, List<int[]> hand) {
        for (int[] p : hand) {
            int count = p[0];
            int id = p[1];
            String token = ID_TO_PIECE_STR.getOrDefault(id, "?");
            if (count > 1) sb.append(count);
            sb.append(token);
        }
    }

    /** 根据当前棋子 ID 返回升变后的 ID（无法升变返回 -1） */
    public static int promotePiece(int pieceId) {
        // 黑方
        if (pieceId == 17) return 21; // Pawn → Promoted Pawn
        if (pieceId == 16) return 20; // Lance → Promoted Lance
        if (pieceId == 15) return 19; // Knight → Promoted Knight
        if (pieceId == 12) return 18; // Silver → Promoted Silver
        if (pieceId == 14) return 23; // Bishop → Horse
        if (pieceId == 13) return 22; // Rook  → Dragon
        // 白方（对应 +14）
        if (pieceId == 31) return 35;
        if (pieceId == 30) return 34;
        if (pieceId == 29) return 33;
        if (pieceId == 26) return 32;
        if (pieceId == 28) return 37;
        if (pieceId == 27) return 36;
        // 金、王、已经升变的棋子不能再次升变
        return -1;
    }

    /** 将棋子字母（大写）根据当前回合转成对应阵营的未升变 ID */
    public static int pieceCharToId(char c, boolean isBlack) {
        return switch (c) {
            case 'P' -> isBlack ? 17 : 31; // PAWN
            case 'L' -> isBlack ? 16 : 30; // LANCE
            case 'N' -> isBlack ? 15 : 29; // KNIGHT
            case 'S' -> isBlack ? 12 : 26; // SILVER
            case 'G' -> isBlack ? 11 : 25; // GOLD
            case 'B' -> isBlack ? 14 : 28; // BISHOP
            case 'R' -> isBlack ? 13 : 27; // ROOK
            default -> -1;
        };
    }

    /** USI 坐标（如 "7g"）转 0~80 的棋盘位置 */
    public static int usiToPos(String usi) {
        if (usi.length() != 2) return -1;
        char file = usi.charAt(0);   // '1'~'9'
        char rank = usi.charAt(1);   // 'a'~'i'
        if (file < '1' || file > '9' || rank < 'a' || rank > 'i') return -1;

        int j = 8 - (file - '1');               // 列：0~8（从右到左）
        int i = rank - 'a';               // 行：0~8（从上到下）
        return i * 9 + j;                 // 行优先 → 0~80
    }


    // 在 PositionUtil.java 中添加
    /**
     * 判断棋盘上棋子移动是否合法（不考虑王手）
     */
    public static boolean isValidBoardMove(int pieceId, int fromPos, int toPos, Position pos) {
        if (toPos >= 81) return false;

        int fromI = fromPos / 9, fromJ = fromPos % 9;
        int toI = toPos / 9, toJ = toPos % 9;

        boolean isBlack = JChessUtil.isBlack(pieceId);
        int dir = isBlack ? -1 : 1; // 先手向下，后手向上

        return switch (pieceId) {
            case 10, 24 -> isKingMove(toI - fromI, toJ - fromJ);                    // 王将
            case 11, 25 -> isGoldMove(toI - fromI, toJ - fromJ, dir);               // 金将
            case 12, 26 -> isSilverMove(toI - fromI, toJ - fromJ, dir);             // 银将
            case 13, 27 -> isRookMove(fromI, fromJ, toI, toJ, pos);                 // 飞车
            case 14, 28 -> isBishopMove(fromI, fromJ, toI, toJ, pos);               // 角行
            case 15, 29 -> isKnightMove(toI - fromI, toJ - fromJ, dir);             // 桂马
            case 16, 30 -> isLanceMove(fromI, fromJ, toI, toJ, dir, pos);           // 香车
            case 17, 31 -> isPawnMove(toI - fromI, toJ - fromJ, dir);               // 步兵

            // ========== 升变棋子规则 ==========
            case 18, 32 -> isGoldMove(toI - fromI, toJ - fromJ, dir);               // 成银 = 金规则
            case 19, 33 -> isGoldMove(toI - fromI, toJ - fromJ, dir);             // 成桂 = 桂规则
            case 20, 34 -> isGoldMove(toI - fromI, toJ - fromJ, dir);               // 成香 = 金规则
            case 21, 35 -> isGoldMove(toI - fromI, toJ - fromJ, dir);               // 成步 = 金规则
            case 22, 36 -> isDragonMove(fromI, fromJ, toI, toJ, pos, dir);          // 龙 = 飞车 + 王
            case 23, 37 -> isHorseMove(fromI, fromJ, toI, toJ, pos, dir);
            default ->           // 马 = 角行 + 王
                    false;
        };
    }

    private static boolean isKingMove(int di, int dj) {
        return Math.abs(di) <= 1 && Math.abs(dj) <= 1;
    }
    private static boolean isGoldMove(int di, int dj, int dir) {
        return (Math.abs(di) <= 1 && Math.abs(dj) <= 1) &&
                !(di == -dir && Math.abs(dj) == 1); // 金不能后斜
    }
    private static boolean isSilverMove(int di, int dj, int dir) {
        return (di == dir && Math.abs(dj) <= 1) ||           // 前直斜
                (di == -dir && Math.abs(dj) == 1);           // 后斜
    }

    private static boolean isRookMove(int fromI, int fromJ, int toI, int toJ, Position pos) {
        if (fromI == toI || fromJ == toJ) { // 横或竖直线
            return isPathClear(fromI, fromJ, toI, toJ, pos);
        }
        return false;
    }
    private static boolean isBishopMove(int fromI, int fromJ, int toI, int toJ, Position pos) {
        int di = Integer.signum(toI - fromI);
        int dj = Integer.signum(toJ - fromJ);
        if (Math.abs(toI - fromI) == Math.abs(toJ - fromJ) && di != 0 && dj != 0) {
            return isPathClear(fromI, fromJ, toI, toJ, pos);
        }
        return false;
    }
    private static boolean isKnightMove(int di, int dj, int dir) {
        return di == 2 * dir && Math.abs(dj) == 1; // 桂马两前一侧
    }
    private static boolean isLanceMove(int fromI, int fromJ, int toI, int toJ, int dir, Position pos) {
        // 香车：只能向前直线到底（像 Rook 但只限前进方向）
        int di = toI - fromI;
        if (di != dir * Math.abs(di) || toJ != fromJ) return false; // 必须向前 + 同列

        // 检查路径
        return isPathClear(fromI, fromJ, toI, toJ, pos);
    }

    private static boolean isPawnMove(int di, int dj, int dir) {
        return di == dir && dj == 0; // 步兵一前一步
    }


    private static boolean isDragonMove(int fromI, int fromJ, int toI, int toJ, Position pos, int dir) {
        // 龙 = 飞车（横竖直线） + 王将（周围一格）
        int di = toI - fromI;
        int dj = toJ - fromJ;

        // 王将步法
        if (Math.abs(di) <= 1 && Math.abs(dj) <= 1) return true;

        // 飞车步法（路径检查）
        if (fromI == toI || fromJ == toJ) {
            return isPathClear(fromI, fromJ, toI, toJ, pos);
        }
        return false;
    }

    private static boolean isHorseMove(int fromI, int fromJ, int toI, int toJ, Position pos, int dir) {
        // 马 = 角行（斜线） + 王将（周围一格）
        int di = toI - fromI;
        int dj = toJ - fromJ;

        // 王将步法
        if (Math.abs(di) <= 1 && Math.abs(dj) <= 1) return true;

        // 角行步法（路径检查）
        int stepDi = Integer.signum(di);
        int stepDj = Integer.signum(dj);
        if (Math.abs(di) == Math.abs(dj) && stepDi != 0 && stepDj != 0) {
            return isPathClear(fromI, fromJ, toI, toJ, pos);
        }
        return false;
    }

    // 路径是否畅通（横竖斜）
    private static boolean isPathClear(int fromI, int fromJ, int toI, int toJ, Position pos) {
        int di = Integer.signum(toI - fromI);
        int dj = Integer.signum(toJ - fromJ);

        int i = fromI + di;
        int j = fromJ + dj;
        while (i != toI || j != toJ) {
            if (pos.getPieceAt(j, i) != 0) return false; // 中途有棋子
            i += di;
            j += dj;
        }
        return true;
    }

    /**
     * 判断打入是否合法
     */
    public static boolean isValidDrop(int pieceId, int toPos, Position pos) {
        if (toPos >= 81) return false; // 不能打入驹台

        int toI = toPos / 9;
        int toJ = toPos % 9;

        // 目标必须为空
        if (pos.getPieceAt(toJ, toI) != 0) return false;

        boolean isBlack = JChessUtil.isBlack(pieceId);
        int enemyLastRank = isBlack ? 0 : 8;        // 先手敌阵最后一行 = 1段，后手 = 9段
        int enemySecondLastRank = isBlack ? 1 : 7;  // 倒数第二行 = 2段/8段

        int basePieceId = ID_TO_BASE_ID.getOrDefault(pieceId, pieceId);

        switch (basePieceId) {
            case 16: case 30: // 香车：不能打入敌阵最后一行
                if (toI == enemyLastRank) return false;
                break;

            case 17: case 31: // 步兵：不能打入敌阵最后一行 + 二步违规
                if (toI == enemyLastRank) return false;

                // 二步检查：该列不能已有己方步兵
                for (int i = 0; i < 9; i++) {
                    int existingPawn = pos.getPieceAt(toJ, i);
                    if ((isBlack && existingPawn == 17) || (!isBlack && existingPawn == 31)) return false;
                }

                Position copy = pos.deepCopy();
                copy.setPieceAt(toJ, toI, basePieceId);
                copy.setTurn(isBlack ? 'w' : 'b');
                // 如果对方被将死 → 打步诘违规
                if (copy.isCheck() && copy.isMate()) {
                    return false;
                }
                break;

            case 15: case 29: // 桂马：不能打入敌阵最后两行
                if (toI == enemyLastRank || toI == enemySecondLastRank) return false;
                break;

            // 金、银、角、飞：无特殊限制
            case 11: case 25: case 12: case 26: case 14: case 28: case 13: case 27:
                break;

            default:
                return false; // 其他棋子不能打入（王等）
        }

        return true;
    }

}