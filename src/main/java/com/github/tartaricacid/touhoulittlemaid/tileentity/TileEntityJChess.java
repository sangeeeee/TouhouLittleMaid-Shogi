package com.github.tartaricacid.touhoulittlemaid.tileentity;

import com.github.tartaricacid.touhoulittlemaid.api.block.IBoardGameEntityBlock;
import com.github.tartaricacid.touhoulittlemaid.api.game.jchess.Position;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.util.JChessUtil;
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
}
