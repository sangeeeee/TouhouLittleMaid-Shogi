package com.github.sangeeeee.tlm_shogi.tileentity;

import com.github.tartaricacid.touhoulittlemaid.api.block.IBoardGameEntityBlock;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.sangeeeee.tlm_shogi.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityJoy;
import com.github.sangeeeee.tlm_shogi.util.JChessUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class TileEntityJChess extends TileEntityJoy implements IBoardGameEntityBlock {
    public static final BlockEntityType<TileEntityJChess> TYPE = BlockEntityType.Builder.of(TileEntityJChess::new, InitBlocks.JCHESS.get()).build(null);

    private static final String CHESS_DATA = "ChessData";
    private static final String CHESS_COUNTER = "ChessCounter";
    private static final String SELECT_CHESS_POINT = "SelectChessPoint";
    private static final String CHECKMATE = "Checkmate";
    private static final String REPEAT = "Repeat";
    private static final String MOVE_NUMBER_LIMIT = "MoveNumberLimit";
    private static final String REPETITION_HISTORY = "RepetitionHistory";
    private static final String TSUME_MODE = "TsumeMode";
    private static final String TSUME_PUZZLE_ID = "TsumePuzzleId";
    private static final String TSUME_MAX_PLY = "TsumeMaxPly";
    private static final String TSUME_PLY = "TsumePly";
    private static final String TSUME_INCORRECT = "TsumeIncorrect";


    private final Position chessData;

    private List<String> repetitionHistory = new ArrayList<>();  // 局面历史

    // 回合计数器 将棋从1开始
    private int chessCounter = 1;
    // 当前选中的棋子
    private int selectChessPoint = -1;
    // 将死（依据下棋方，判断谁输谁赢）
    private boolean checkmate = false;
    // 长打（判和）
    private boolean repeat = false;
    // 50 回限着（判和）
    private boolean moveNumberLimit = false;
    // 诘将棋状态独立于普通对局；ply 从载入题目后的 0 开始计数。
    private boolean tsumeMode = false;
    private String tsumePuzzleId = "";
    private int tsumeMaxPly = 0;
    private int tsumePly = 0;
    private boolean tsumeIncorrect = false;

    public TileEntityJChess(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
        this.chessData = new Position();
        this.chessData.applyUSI(JChessUtil.INIT);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag data = getPersistentData();
        data.putString(CHESS_DATA, chessData.toUSI());
        data.putInt(CHESS_COUNTER, chessCounter);
        data.putInt(SELECT_CHESS_POINT, selectChessPoint);
        data.putBoolean(CHECKMATE, checkmate);
        data.putBoolean(REPEAT, repeat);
        data.putBoolean(MOVE_NUMBER_LIMIT, moveNumberLimit);
        data.putBoolean(TSUME_MODE, tsumeMode);
        data.putString(TSUME_PUZZLE_ID, tsumePuzzleId);
        data.putInt(TSUME_MAX_PLY, tsumeMaxPly);
        data.putInt(TSUME_PLY, tsumePly);
        data.putBoolean(TSUME_INCORRECT, tsumeIncorrect);

        ListTag histTag = new ListTag();
        for (String key : repetitionHistory) histTag.add(StringTag.valueOf(key));
        data.put(REPETITION_HISTORY, histTag);

        super.saveAdditional(tag, provider);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        CompoundTag data = getPersistentData();
        chessCounter = data.getInt(CHESS_COUNTER);
        selectChessPoint = data.getInt(SELECT_CHESS_POINT);
        chessData.applyUSI(data.getString(CHESS_DATA));
        checkmate = data.getBoolean(CHECKMATE);
        repeat = data.getBoolean(REPEAT);
        moveNumberLimit = data.getBoolean(MOVE_NUMBER_LIMIT);
        tsumeMode = data.getBoolean(TSUME_MODE);
        tsumePuzzleId = data.getString(TSUME_PUZZLE_ID);
        tsumeMaxPly = data.getInt(TSUME_MAX_PLY);
        tsumePly = data.getInt(TSUME_PLY);
        tsumeIncorrect = data.getBoolean(TSUME_INCORRECT);
        // 读取局面历史
        repetitionHistory.clear();
        if (data.contains(REPETITION_HISTORY, Tag.TAG_LIST)) {
            ListTag histTag = data.getList(REPETITION_HISTORY, Tag.TAG_STRING);
            for (Tag value : histTag) {
                StringTag tag = (StringTag) value;
                repetitionHistory.add(tag.getAsString());
            }
        }
    }

    public void reset() {
        this.chessCounter = 1;
        this.selectChessPoint = -1;
        this.checkmate = false;
        this.repeat = false;
        this.moveNumberLimit = false;
        this.chessData.applyUSI(JChessUtil.INIT);
        this.repetitionHistory.clear();
        clearTsumeState();
    }

    /** Completely replaces any current game with a fresh player-first tsume position. */
    public void resetToTsume(String sfen, String puzzleId, int maximumPly) {
        if (maximumPly < 1 || (maximumPly & 1) == 0) {
            throw new IllegalArgumentException("Tsume maximum ply must be a positive odd number");
        }
        Position replacement = new Position();
        replacement.applyUSI(sfen);
        if (!replacement.isPlayer()) {
            throw new IllegalArgumentException("Tsume positions must start with the player (black) to move");
        }
        int defendingKings = 0;
        for (int point = 0; point < 81; point++) {
            if (replacement.getPieceByPointNum(point) == 24) {
                defendingKings++;
            }
        }
        if (defendingKings != 1) {
            throw new IllegalArgumentException("Tsume defender must have exactly one king");
        }
        this.chessData.applyUSI(replacement.toUSI());

        this.chessCounter = this.chessData.getMoveNumber();
        this.selectChessPoint = -1;
        this.checkmate = false;
        this.repeat = false;
        this.moveNumberLimit = false;
        this.repetitionHistory.clear();
        this.tsumeMode = true;
        this.tsumePuzzleId = puzzleId;
        this.tsumeMaxPly = maximumPly;
        this.tsumePly = 0;
        this.tsumeIncorrect = false;
    }

    private void clearTsumeState() {
        this.tsumeMode = false;
        this.tsumePuzzleId = "";
        this.tsumeMaxPly = 0;
        this.tsumePly = 0;
        this.tsumeIncorrect = false;
    }

    public void addHistoryAfterMove() {
        Position data = getChessData();
        repetitionHistory.add(data.toUSI());
        // 限制历史长度（优化内存，最近1000步够用）
        if (repetitionHistory.size() > 1000) {
            repetitionHistory.removeFirst();
        }

        // 检查千日手
        if (data.isRepetition(repetitionHistory)) {
            setRepeat(true);
        }
    }

    public Position getChessData() {
        return chessData;
    }

    public boolean isCheckmate() {
        return checkmate;
    }

    public void setCheckmate(boolean checkmate) {
        this.checkmate = checkmate;
    }

    public boolean isPlayerTurn() {
        return this.chessData.isPlayer();
    }

    public int getChessCounter() {
        return chessCounter;
    }

    public void setChessCounter(int chessCounter) {
        this.chessCounter = chessCounter;
    }

    public int getSelectChessPoint() {
        return selectChessPoint;
    }

    public void setSelectChessPoint(int selectChessPoint) {
        this.selectChessPoint = selectChessPoint;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isMoveNumberLimit() {
        return moveNumberLimit;
    }

    public void setMoveNumberLimit(boolean moveNumberLimit) {
        this.moveNumberLimit = moveNumberLimit;
    }

    public boolean isTsumeMode() {
        return tsumeMode;
    }

    public String getTsumePuzzleId() {
        return tsumePuzzleId;
    }

    public int getTsumeMaxPly() {
        return tsumeMaxPly;
    }

    public int getTsumePly() {
        return tsumePly;
    }

    public void advanceTsumePly() {
        if (tsumeMode) {
            tsumePly++;
        }
    }

    public boolean isTsumeIncorrect() {
        return tsumeIncorrect;
    }

    public void markTsumeIncorrect() {
        if (tsumeMode) {
            tsumeIncorrect = true;
            checkmate = true;
        }
    }

    public void markTsumeSolved() {
        if (tsumeMode) {
            tsumeIncorrect = false;
            checkmate = true;
        }
    }
}
