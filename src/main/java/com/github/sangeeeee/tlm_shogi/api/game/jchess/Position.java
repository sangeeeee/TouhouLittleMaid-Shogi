package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import com.github.sangeeeee.tlm_shogi.util.JChessUtil;

import java.util.ArrayList;
import java.util.List;

import static com.github.sangeeeee.tlm_shogi.api.game.jchess.PositionUtil.isValidBoardMove;

@SuppressWarnings("all")
public class Position {
    public int[][] board;                    // 9×9 棋盘
    public List<int[]> blackHand;            // 先手（黑方）驹台: {count, pieceId}
    public List<int[]> whiteHand;             // 后手（白方）驹台: {count, pieceId}
    private char turn;                        // 'b' 或 'w'
    private int moveNumber;                   // 回合数，将棋从 1 开始，任意方下棋都+1

    public Position() {
        this.board = new int[9][9];
        this.blackHand = new ArrayList<>();
        this.whiteHand = new ArrayList<>();
        this.turn = 'b';
        this.moveNumber = 1;
    }

    // getters and setters
    public int getPieceAt(int j, int i) {
        if (i < 0 || i >= 9 || j < 0 || j >= 9) return 0;
        return board[i][j];
    }

    public void setPieceAt(int j, int i, int pieceId) {
        if (i >= 0 && i < 9 && j >= 0 && j < 9) {
            board[i][j] = pieceId;
        }
    }

    public int getPieceByPointNum(int pos) {
        if (pos < 0) return 0;
        if (pos < 81) {
            int i = pos / 9;
            int j = pos % 9;
            return board[i][j];
        } else if (pos < 90) {
            int i = pos - 81;
            if (i >= 0 && i < blackHand.size()) {
                return blackHand.get(i)[1];
            } else {
                return 0;
            }
        } else if (pos <= 98) {
            int i = pos - 90;
            if (i >= 0 && i < whiteHand.size()) {
                return whiteHand.get(i)[1];
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static String getMove(int preClick, int nowClick) {
        return "1a1b";
    }

    public List<int[]> getBlackHand() { return new ArrayList<>(blackHand); }
    public List<int[]> getWhiteHand() { return new ArrayList<>(whiteHand); }
    public char getTurn() { return turn; }
    public boolean isPlayer() { return turn == 'b'; }
    public int getMoveNumber() { return moveNumber; }

    public void setTurn(char turn) { this.turn = turn; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public void clearHand(boolean isBlack) {
        if (isBlack) blackHand.clear();
        else whiteHand.clear();
    }

    public void addToHand(boolean isBlack, int pieceId, int count) {
        List<int[]> hand = isBlack ? blackHand : whiteHand;
        for (int[] p : hand) {
            if (p[1] == pieceId) {
                p[0] += count;
                return;
            }
        }
        hand.add(new int[]{count, pieceId});
    }

    public void applyUSI(String usi) {
        Position temp = PositionUtil.fromSfen(usi);  // 利用工具类解析

        for (int i = 0; i < 9; i++) {
            System.arraycopy(temp.board[i], 0, this.board[i], 0, 9);
        }

        this.blackHand.clear();
        this.whiteHand.clear();
        this.blackHand.addAll(temp.getBlackHand());
        this.whiteHand.addAll(temp.getWhiteHand());

        this.turn = temp.getTurn();
        this.moveNumber = temp.getMoveNumber();
    }

    public String toUSI() {
        return PositionUtil.toSfen(this);
    }

    public int move(int fromPos, int toPos, boolean promote) {
        // 验证位置范围
        if (fromPos < 0 || fromPos > 98 || toPos < 0 || toPos > 98 ) {
            System.out.println("Invalid position range (0-89)");
            return -1;
        }

        // 获取起始棋子和目标棋子
        int fromPiece = getPieceByPointNum(fromPos);
        int toPiece = getPieceByPointNum(toPos);

        if (fromPiece == 0) {
            System.out.println(" No piece at fromPos " + fromPos);
            return -1;
        }

        // 判断是否当前回合方棋子
        boolean isBlackTurn = (turn == 'b');
        boolean isFromBlack = JChessUtil.isBlack(fromPiece);  // 10-23 为黑方
        if (isFromBlack != isBlackTurn) {
            System.out.println("Not your turn - piece at fromPos " + fromPos + " belongs to opponent");
            return -1;
        }

        // 判断 fromPos 是否在驹台
        boolean fromInHand = (fromPos >= 81);
        boolean toInHand = (toPos >= 81);

        if (fromInHand) {
            // 从驹台打入
            if (toInHand) {
                return -1;
            }
            if (toPiece != 0) {
                return -1;
            }

            // 执行打入：棋盘放置棋子，驹台对应棋子减1
            int boardToJ = toPos % 9;
            int boardToI = toPos / 9;
            setPieceAt(boardToJ, boardToI, fromPiece);

            // 从驹台移除
            List<int[]> hand = isBlackTurn ? blackHand : whiteHand;
            removeFromHand(hand, fromPiece);

        } else {
            // 棋盘上移动
            int finalPieceId = fromPiece;                // 默认不升变
            if (promote) {
                finalPieceId = PositionUtil.promotePiece(fromPiece);  // 转为升变后的 ID
                if (finalPieceId == -1) {
                    return -1;
                }
            }

            if (toInHand) {
                return -1;
            }

            if (toPiece != 0) {
                // 目标有棋子
                boolean toIsBlack = JChessUtil.isBlack(toPiece);
                if (toIsBlack == isBlackTurn) {
                    // 己方棋子
                    return -1;
                }
                // 对方棋子：捕获加入驹台
                int boardFromJ = fromPos % 9;
                int boardFromI = fromPos / 9;
                int boardToJ = toPos % 9;
                int boardToI = toPos / 9;

                // 移动棋子
                int capturedPieceId = PositionUtil.convertCapturedPiece(toPiece, isBlackTurn);
                if (capturedPieceId != -1) {
                    setPieceAt(boardFromJ, boardFromI, 0);
                    setPieceAt(boardToJ, boardToI, finalPieceId);
                }

                addToHand(isBlackTurn, capturedPieceId, 1);
            } else {
                // 空位：直接移动
                int boardFromJ = fromPos % 9;
                int boardFromI = fromPos / 9;
                int boardToJ = toPos % 9;
                int boardToI = toPos / 9;

                setPieceAt(boardFromJ, boardFromI, 0);
                setPieceAt(boardToJ, boardToI, finalPieceId);
            }
        }

        // 切换回合
        turn = (turn == 'b') ? 'w' : 'b';
        moveNumber++;

        return toPos;
    }


    /**
     * 从驹台中移除指定棋子 1 个
     * @param hand 驹台列表
     * @param pieceId 棋子ID
     */
    private void removeFromHand(List<int[]> hand, int pieceId) {
        for (int i = 0; i < hand.size(); i++) {
            int[] p = hand.get(i);
            if (p[1] == pieceId) {
                p[0]--;
                if (p[0] <= 0) {
                    hand.remove(i);
                }
                return;
            }
        }
        // 如果没找到，理论上不应该发生
        System.out.println("Warning: Piece not found in hand: " + pieceId);
    }


    public int makeMove(String usiMove) {
        if (usiMove == null || usiMove.isEmpty()) {
            System.err.println("Error: Empty move string");
            return -1;
        }

        boolean promote = false;
        String move = usiMove.trim();

        // 检测是否强制升变（结尾有 +）
        if (move.endsWith("+")) {
            promote = true;
            move = move.substring(0, move.length() - 1);
        }

        try {
            if (move.contains("*")) {
                // ------------------- 打入 -------------------
                // 格式例如：P*3d   (大写字母 + * + 目标坐标)
                if (move.length() != 4) {
                    System.err.println("Error: Invalid drop format: " + usiMove);
                    return -1;
                }
                char pieceChar = move.charAt(0);                 // P,L,N,S,G,B,R
                String toStr   = move.substring(2);              // "3d"

                int toPos = PositionUtil.usiToPos(toStr);                     // 目标棋盘位置 0~80
                if (toPos < 0 || toPos > 80) {
                    System.err.println("Error: Invalid drop target: " + toStr);
                    return -1;
                }

                // 根据当前回合决定是哪一方的驹台
                boolean isBlackTurn = (turn == 'b');
                int pieceId = PositionUtil.pieceCharToId(pieceChar, isBlackTurn); // 转为黑/白方的未升变 ID

                if (pieceId == -1) {
                    System.err.println("Error: Unknown drop piece: " + pieceChar);
                    return -1;
                }

                // 检查驹台里是否真的有这枚棋子
                List<int[]> hand = isBlackTurn ? blackHand : whiteHand;
                boolean hasPiece = false;
                for (int[] p : hand) {
                    if (p[1] == pieceId) {
                        hasPiece = true;
                        break;
                    }
                }
                if (!hasPiece) {
                    System.err.println("Error: No " + pieceChar + " in hand to drop");
                    return -1;
                }

                // 目标位置必须为空
                int toJ = toPos % 9;
                int toI = toPos / 9;
                if (board[toI][toJ] != 0) {
                    System.err.println("Error: Drop target is occupied");
                    return -1;
                }

                // 执行打入（直接调用已有的 move 方法，打入时 fromPos 用驹台槽位）
                int fromSlot = -1;
                int begin = isBlackTurn? 81 : 90; // 对于驹台中的坐标，先手81开始， 后手90开始
                for (int i = 0; i < hand.size(); i++) {
                    if (hand.get(i)[1] == pieceId) {
                        fromSlot = begin + i;
                        break;
                    }
                }
                return move(fromSlot, toPos, false);   // move 里已经处理了打入的所有逻辑
            }
            else {
                // ------------------- 普通移动（可能升变） -------------------
                // 格式：7g7f  或 8h2b+
                if (move.length() != 4) {
                    System.err.println("Error: Invalid move format: " + usiMove);
                    return -1;
                }
                String fromStr = move.substring(0, 2);
                String toStr   = move.substring(2);

                int fromPos = PositionUtil.usiToPos(fromStr);
                int toPos   = PositionUtil.usiToPos(toStr);

                if (fromPos < 0 || fromPos > 80 || toPos < 0 || toPos > 80) {
                    System.err.println("Error: Invalid board position in move: " + usiMove);
                    return -1;
                }

                // 调用已有的 move 方法
                return move(fromPos, toPos, promote);
            }
        } catch (Exception e) {
            System.err.println("Error parsing move '" + usiMove + "': " + e.getMessage());
            return -1;
        }
    }

    public boolean canPromote(int fromPos, int toPos) {
        // 必须是从棋盘移动（打入不能升变）
        if (fromPos >= 81) return false;
        if (toPos >= 81) return false; // 不能走到驹台

        int pieceId = getPieceByPointNum(fromPos);
        if (pieceId == 0) return false;

        // 判断棋子本身是否属于“可升变”的原始棋子
        boolean isPromotablePiece = switch (pieceId) {
            case 17, 31 -> true; // 步兵
            case 16, 30 -> true; // 香车
            case 15, 29 -> true; // 桂马
            case 12, 26 -> true; // 银将
            case 13, 27 -> true; // 飞车
            case 14, 28 -> true; // 角行
            default -> false;    // 金、王、已升变棋子都不可升变
        };

        if (!isPromotablePiece) return false; // 棋子本身不能升变，直接返回 false

        int fromI = fromPos / 9;
        int toI = toPos / 9;

        boolean isBlackTurn = (turn == 'b');
        int enemyCampStart = isBlackTurn ? 0 : 6;  // 先手敌营：0~2行，后手敌营：6~8行
        int enemyCampEnd = isBlackTurn ? 2 : 8;

        boolean fromInEnemyCamp = fromI >= enemyCampStart && fromI <= enemyCampEnd;
        boolean toInEnemyCamp = toI >= enemyCampStart && toI <= enemyCampEnd;

        return fromInEnemyCamp || toInEnemyCamp;
    }

    public boolean mustPromote(int fromPos, int toPos) {
        if (fromPos >= 81 || toPos >= 81) return false; // 打入不能升变

        int pieceId = getPieceByPointNum(fromPos);
        if (pieceId == 0) return false;

        int toI = toPos / 9;
        boolean isBlackTurn = (turn == 'b');

        // 步兵 (17/31) 或 香车 (16/30)
        if (pieceId == 17 || pieceId == 31 || pieceId == 16 || pieceId == 30) {
            int enemyLastRank = isBlackTurn ? 0 : 8;
            if (toI == enemyLastRank) return true;
        }

        // 桂马 (15/29)
        if (pieceId == 15 || pieceId == 29) {
            int enemyLastRank = isBlackTurn ? 0 : 8;
            int enemySecondLastRank = isBlackTurn ? 1 : 7;
            if (toI == enemyLastRank || toI == enemySecondLastRank) return true;
        }

        return false;
    }

    /**
     * 判断玩家是否合法走法（棋盘移动 + 打入 + 不王手己王）
     * @return true = 合法，false = 非法
     */
    public boolean isLegalMove(int fromPos, int toPos) {
        int fromPiece = getPieceByPointNum(fromPos);
        if (fromPiece == 0) return false;

        boolean isBlackTurn = (turn == 'b');
        if (JChessUtil.isBlack(fromPiece) != isBlackTurn) return false; // 不是自己棋子

        boolean fromInHand = (fromPos >= 81 && fromPos <= 89);
        if (fromInHand) {
            // 打入规则
            return PositionUtil.isValidDrop(fromPiece, toPos, this);
        } else {
            // 棋盘走法规则
            return isValidBoardMove(fromPiece, fromPos, toPos, this);
        }
    }

    /**
     * 判断当前回合方的王是否正在被攻击（王手）
     */
    public boolean isCheck() {
        boolean isBlackTurn = (turn == 'b');
        int kingId = isBlackTurn ? 10 : 24; // 先手王=10，后手王=24

        // 1. 找到王的位置
        int kingPos = -1;
        for (int pos = 0; pos < 81; pos++) {
            if (getPieceByPointNum(pos) == kingId) {
                kingPos = pos;
                break;
            }
        }
        if (kingPos == -1) return false; // 理论上不可能

        int kingI = kingPos / 9;
        int kingJ = kingPos % 9;

        // 2. 遍历所有敌方棋子（包括驹台），看是否能攻击到王
        boolean enemyIsBlack = !isBlackTurn;

        // 棋盘上的敌方棋子
        for (int pos = 0; pos < 81; pos++) {
            int piece = getPieceByPointNum(pos);
            if (piece != 0 && JChessUtil.isBlack(piece) == enemyIsBlack) {
                if (canPieceAttack(piece, pos, kingPos, this)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isKingUnderAttack(boolean checkBlackKing) {
        int kingId = checkBlackKing ? 10 : 24;
        int kingPos = -1;

        for (int pos = 0; pos < 81; pos++) {
            if (getPieceByPointNum(pos) == kingId) {
                kingPos = pos;
                break;
            }
        }
        if (kingPos == -1) return true; // 王没了，算被吃

        boolean enemyIsBlack = !checkBlackKing;

        for (int pos = 0; pos < 81; pos++) {
            int piece = getPieceByPointNum(pos);
            if (piece != 0 && JChessUtil.isBlack(piece) == enemyIsBlack) {
                if (canPieceAttack(piece, pos, kingPos, this)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断指定棋子从 fromPos 是否能攻击到 toPos（不实际移动）
     */
    public static boolean canPieceAttack(int pieceId, int fromPos, int toPos, Position pos) {
        if (pieceId == 0) return false;

        int fromI = fromPos / 9, fromJ = fromPos % 9;
        int toI = toPos / 9, toJ = toPos % 9;

        boolean isBlack = JChessUtil.isBlack(pieceId);
        int dir = isBlack ? 1 : -1;

        // 复用我们之前写的 isValidBoardMove，但要临时忽略目标格有己方棋子
        int targetPiece = pos.getPieceAt(toJ, toI);
        pos.setPieceAt(toJ, toI, 0); // 临时清空目标格（模拟攻击）

        boolean canAttack = PositionUtil.isValidBoardMove(pieceId, fromPos, toPos, pos);

        pos.setPieceAt(toJ, toI, targetPiece); // 恢复
        return canAttack;
    }

    /**
     * 判断当前回合方是否被将死（王手且无解）
     */
    public boolean isMate() {
        if (!isCheck()) return false;

        boolean mySideIsBlack = (turn == 'b');
        int kingId = mySideIsBlack ? 10 : 24;

        // 1. 动王
        int kingPos = -1;
        for (int p = 0; p < 81; p++) if (getPieceByPointNum(p) == kingId) { kingPos = p; break; }
        if (kingPos == -1) return true;

        int ki = kingPos / 9, kj = kingPos % 9;
        int[] di = {-1,-1,-1,0,0,1,1,1};
        int[] dj = {-1, 0, 1,-1,1,-1,0,1};

        for (int d = 0; d < 8; d++) {
            int ni = ki + di[d], nj = kj + dj[d];
            if (ni >= 0 && ni < 9 && nj >= 0 && nj < 9) {
                int to = ni * 9 + nj;
                int target = getPieceAt(nj, ni);
                if (target == 0 || JChessUtil.isBlack(target) != mySideIsBlack) {
                    Position copy = deepCopy();
                    copy.move(kingPos, to, false);
                    if (!copy.isKingUnderAttack(mySideIsBlack)) { // 关键：检查我方王！
                        return false;
                    }
                }
            }
        }

        // 2. 棋盘移动 + 打入
        List<int[]> myHand = mySideIsBlack ? blackHand : whiteHand;

        // 棋盘移动
        for (int from = 0; from < 81; from++) {
            int piece = getPieceByPointNum(from);
            if (piece == 0 || JChessUtil.isBlack(piece) != mySideIsBlack) continue;

            for (int to = 0; to < 99; to++) {
                if (!isLegalMove(from, to)) continue;

                boolean canP = canPromote(from, to);
                boolean mustP = mustPromote(from, to);

                // 不升变
                {
                    Position copy = deepCopy();
                    if (copy.move(from, to, mustP ? true : false) != -1) {
                        if (!copy.isKingUnderAttack(mySideIsBlack)) return false;
                    }
                }
                // 可选升变时再试一次升变
                if (canP && !mustP) {
                    Position copy = deepCopy();
                    if (copy.move(from, to, true) != -1) {
                        if (!copy.isKingUnderAttack(mySideIsBlack)) return false;
                    }
                }
            }
        }

        // 打入
        for (int[] entry : myHand) {
            int pieceId = entry[1];
            if (entry[0] <= 0) continue;

            for (int to = 0; to < 81; to++) {
                if (getPieceAt(to % 9, to / 9) != 0) continue;
                if (!PositionUtil.isValidDrop(pieceId, to, this)) continue;

                int fromSlot = -1;
                List<int[]> hand = mySideIsBlack ? blackHand : whiteHand;
                for (int i = 0; i < hand.size(); i++) {
                    if (hand.get(i)[1] == pieceId) {
                        fromSlot = (mySideIsBlack ? 81 : 90) + i;
                        break;
                    }
                }
                if (fromSlot == -1) continue;

                Position copy = deepCopy();
                if (copy.move(fromSlot, to, false) != -1) {
                    if (!copy.isKingUnderAttack(mySideIsBlack)) {
                        return false;
                    }
                }
            }
        }
        System.out.println("被将死力");

        return true; // 所有解法都无效 → 将死
    }

    /**
     * 检查是否千日手（历史中同一局面>=4次）
     * @param history 局面历史栈（USI字符串列表）
     * @return true=千日手成立
     */
    public boolean isRepetition(List<String> history) {
        String currentKey = this.toUSI();
        int count = 0;
        for (String past : history) {
            if (past.equals(currentKey)) count++;
            if (count >= 4) return true;
        }
        return false;
    }

    public Position deepCopy() {
        Position copy = new Position();
        // 复制棋盘
        for (int i = 0; i < 9; i++) {
            System.arraycopy(this.board[i], 0, copy.board[i], 0, 9);
        }
        // 复制手牌
        copy.blackHand = new ArrayList<>();
        copy.whiteHand = new ArrayList<>();
        for (int[] p : this.blackHand) copy.blackHand.add(p.clone());
        for (int[] p : this.whiteHand) copy.whiteHand.add(p.clone());
        // 复制状态
        copy.turn = this.turn;
        copy.moveNumber = this.moveNumber;
        return copy;
    }
}