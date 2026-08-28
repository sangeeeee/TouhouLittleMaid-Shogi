package com.github.sangeeeee.tlm_shogi.block;

import com.github.sangeeeee.tlm_shogi.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.api.block.IBoardGameBlock;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.tartaricacid.touhoulittlemaid.block.BlockJoy;
import com.github.sangeeeee.tlm_shogi.block.properties.ShogiPart;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.sangeeeee.tlm_shogi.init.InitItems;
import com.github.sangeeeee.tlm_shogi.item.ItemTsumeBoardState;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.github.sangeeeee.tlm_shogi.network.message.JChessPromoteOpenPackage;
import com.github.sangeeeee.tlm_shogi.network.message.JChessToClientPackage;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import com.github.sangeeeee.tlm_shogi.tsume.MicrocosmosRecord;
import com.github.sangeeeee.tlm_shogi.tsume.TsumePlayerProgress;
import com.github.sangeeeee.tlm_shogi.tsume.TsumePuzzleId;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityJoy;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import com.github.sangeeeee.tlm_shogi.util.JChessUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class BlockJChess extends BlockJoy implements IBoardGameBlock {
    private static final int MASTERPIECE_FAVOR_MULTIPLIER = 3;
    private static final int MICROCOSMOS_FAVOR_MULTIPLIER = 5;
    private static final Type TSUME_MASTERPIECE_WIN = new Type(
            "TsumeMasterpieceWin",
            Type.WCHESS_WIN.getPoint() * MASTERPIECE_FAVOR_MULTIPLIER,
            Type.WCHESS_WIN.getCooldown());
    private static final Type MICROCOSMOS_WIN = new Type(
            "MicrocosmosWin",
            Type.WCHESS_WIN.getPoint() * MICROCOSMOS_FAVOR_MULTIPLIER,
            Type.WCHESS_WIN.getCooldown());
    public static final EnumProperty<ShogiPart> PART = EnumProperty.create("part", ShogiPart.class);
    public static final int plate = 6;
    public static final int height = 10;
    public static final VoxelShape SHAPE_CENTER = Block.box(0, 0, 0, 16, height, 16);
    public static final VoxelShape SHAPE_RIGHT_NS = Block.box(0, 0, 16 - plate, plate, height, 16);
    public static final VoxelShape SHAPE_LEFT_NS = Block.box(16 - plate, 0, 0, 16, height, plate);
    public static final VoxelShape SHAPE_RIGHT_EW = Block.box(16 - plate, 0, 16 - plate, 16, height, 16);
    public static final VoxelShape SHAPE_LEFT_EW = Block.box(0, 0, 0, plate, height, plate);

    public BlockJChess() {
        super(Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0F, 3.0F)
                .forceSolidOn().noOcclusion().dynamicShape());
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, ShogiPart.CENTER).setValue(FACING, Direction.NORTH));
    }

    private static void handleJChessRemove(Level world, BlockPos pos, BlockState state) {
        if (!world.isClientSide) {
            ShogiPart part = state.getValue(PART);
            Direction facing = state.getValue(FACING);
            boolean extendAlongX = facing.getAxis() == Direction.Axis.X;
            BlockPos centerPos = pos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
            BlockEntity te = world.getBlockEntity(centerPos);
            popResource(world, centerPos, InitItems.JCHESS.get().getDefaultInstance());
            if (te instanceof TileEntityJChess) {
                for (int i = -1; i < 2; i++) {
                    if (extendAlongX) {
                        world.setBlockAndUpdate(centerPos.offset(0, 0, i), Blocks.AIR.defaultBlockState());
                    } else {
                        world.setBlockAndUpdate(centerPos.offset(i, 0, 0), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }


    public static void maidMove(ServerPlayer player, Level level, BlockPos pos, String expectedSfen,
                                String move, boolean maidLost, boolean playerLost) {
        if (level.getBlockEntity(pos) instanceof TileEntityJChess chess) {
            if (!chess.getChessData().toUSI().equals(expectedSfen)) {
                // A board-state item or a later move replaced this asynchronous search.
                return;
            }
            if (chess.isPlayerTurn()) {
                return;
            }

            if (chess.isTsumeMode()) {
                applyTsumeDefense(player, level, pos, chess, move);
                return;
            }

            Position chessData = chess.getChessData();
            UUID sitId = chess.getSitId();
            // 女仆输，以防作弊，再检查一次 （但是可以直接改sfen，没什么屌用）
            if (maidLost && JChessUtil.isMaid(chessData) && chessData.isMate()) {
                chess.setCheckmate(true);
                chess.refresh();

                if (level instanceof ServerLevel serverLevel && serverLevel.getEntity(sitId) instanceof EntitySit sit
                    && sit.getFirstPassenger() instanceof EntityMaid maid && maid.isOwnedBy(player)) {
                    // TODO: 暂时不加段位系统
                    maid.getFavorabilityManager().apply(Type.WCHESS_WIN);
                    maid.getGameRecordManager().markStatue(false);
                    InitTrigger.MAID_EVENT.get().trigger(player, TriggerType.WIN_JCHESS);
                }
                return;
            }

            int toPos = chessData.makeMove(move);
            if (toPos == -1) {
                player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.engineerr"));
                return;
            }
            chess.setSelectChessPoint(toPos);
            if (chessData.isCheck() ){
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid.cchess.check"));
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
            }
            chess.setCheckmate(playerLost);
            if (level instanceof ServerLevel serverLevel && serverLevel.getEntity(sitId) instanceof EntitySit sit && sit.getFirstPassenger() instanceof EntityMaid maid) {
                maid.swing(InteractionHand.MAIN_HAND);
                if (playerLost) {
                    maid.getGameRecordManager().markStatue(true);
                }
            }
            level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
            chess.setChessCounter(chessData.getMoveNumber());
            chess.addHistoryAfterMove();
            chess.refresh();
        }
    }

    private static void applyTsumeDefense(ServerPlayer player, Level level, BlockPos pos,
                                          TileEntityJChess chess, String move) {
        if (chess.isMicrocosmosOnRecord()
                && MicrocosmosRecord.defenseAfter(chess.getTsumePly())
                .filter(move::equals)
                .isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.engineerr"));
            return;
        }
        if (!isLegalEngineMove(chess.getChessData().toUSI(), move)) {
            player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.engineerr"));
            return;
        }

        int toPos = chess.getChessData().makeMove(move);
        if (toPos < 0) {
            player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.engineerr"));
            return;
        }

        chess.advanceTsumePly();
        chess.setSelectChessPoint(toPos);
        chess.setChessCounter(chess.getChessData().getMoveNumber());

        // A legal defense that counter-mates the attacker is an incorrect solution.
        TsumeCheckStatus status = inspectTsumePosition(chess.getChessData());
        if (status.checkmate() && JChessUtil.isPlayer(chess.getChessData())) {
            markTsumeIncorrect(player, level, pos, chess);
            return;
        }

        EntityMaid maid = getSeatedMaid(level, chess);
        if (maid != null) {
            maid.swing(InteractionHand.MAIN_HAND);
        }
        level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS,
                1.0f, 0.8F + level.random.nextFloat() * 0.4F);
        chess.refresh();
    }

    private static boolean isLegalEngineMove(String sfen, String moveText) {
        try {
            com.github.sangeeeee.tlm_shogi.engine.core.Position position =
                    com.github.sangeeeee.tlm_shogi.engine.core.Position.parse(sfen);
            return com.github.sangeeeee.tlm_shogi.engine.core.Move.parseSfen(moveText)
                    .filter(position::validateMove)
                    .isPresent();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void startMaidSit(EntityMaid maid, BlockState state, Level worldIn, BlockPos pos) {
        if (worldIn instanceof ServerLevel serverLevel && worldIn.getBlockEntity(pos) instanceof TileEntityJoy joy) {
            Entity oldSitEntity = serverLevel.getEntity(joy.getSitId());
            if (oldSitEntity != null && oldSitEntity.isAlive()) {
                return;
            }
            Direction face = state.getValue(FACING).getOpposite();
            Vec3 position = new Vec3(0.5 + face.getStepX(), 0.1, 0.5 + face.getStepZ());
            EntitySit newSitEntity = new EntitySit(worldIn, Vec3.atLowerCornerWithOffset(pos, position.x, position.y, position.z), this.getTypeName(), pos);
            newSitEntity.setYRot(face.getOpposite().toYRot() + this.sitYRot());
            worldIn.addFreshEntity(newSitEntity);
            joy.setSitId(newSitEntity.getUUID());
            joy.setChanged();
            maid.startRiding(newSitEntity);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        handleJChessRemove(world, pos, state);
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level world, BlockPos pos, Explosion explosion) {
        handleJChessRemove(world, pos, state);
        super.onBlockExploded(state, world, pos, explosion);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos centerPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        boolean extendAlongX = facing.getAxis() == Direction.Axis.X;
        for (int i = -1; i < 2; i++) {
            BlockPos searchPos;
            if (extendAlongX) {
                searchPos = centerPos.offset(0, 0, i);
            } else {
                searchPos = centerPos.offset(i, 0, 0);
            }
            if (!context.getLevel().getBlockState(searchPos).canBeReplaced(context)) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @javax.annotation.Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isClientSide) {
            return;
        }
        Direction facing = state.getValue(FACING);
        boolean extendAlongX = facing.getAxis() == Direction.Axis.X;

        for (int i = -1; i < 2; i++) {
            BlockPos searchPos;
            ShogiPart part;
            if (extendAlongX) {
                searchPos = pos.offset(0, 0, i);
                part = ShogiPart.getPartByPos(0, i);
            } else {
                searchPos = pos.offset(i, 0, 0);
                part = ShogiPart.getPartByPos(i, 0);
            }
            if (part != null && !part.isCenter()) {
                worldIn.setBlock(searchPos, state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && hand == InteractionHand.MAIN_HAND) {
            ShogiPart part = state.getValue(PART);
            BlockPos centerPos = pos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
            BlockEntity te = level.getBlockEntity(centerPos);

            if (!(te instanceof TileEntityJChess chess)) {
                return ItemInteractionResult.FAIL;
            }

            ItemStack heldItem = player.getMainHandItem();

            // Board-state loading deliberately precedes every turn/game-over check: it always
            // replaces the current game, including an in-flight engine search or finished game.
            if (heldItem.is(InitItems.MICROCOSMOS.get())) {
                String puzzleId = TsumePuzzleId.fromSfen(MicrocosmosRecord.INITIAL_SFEN);
                chess.resetToMicrocosmos(puzzleId);
                EntityMaid seatedMaid = getSeatedMaid(level, chess);
                if (seatedMaid != null) {
                    seatedMaid.getGameRecordManager().resetStatue();
                }
                chess.refresh();
                level.playSound(null, pos, InitSounds.GOMOKU_RESET.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                player.sendSystemMessage(Component.translatable(
                        "message.tlm_shogi.jchess.microcosmos.introduction"));
                return ItemInteractionResult.SUCCESS;
            }

            if (heldItem.is(InitItems.JCHESS_BOARD_STATE.get())) {
                String[] boardState = ItemBoardState.getState(heldItem);
                if (boardState == null || boardState[0].isBlank()) {
                    return ItemInteractionResult.FAIL;
                }
                try {
                    String puzzleId = TsumePuzzleId.fromSfen(boardState[0]);
                    chess.resetToTsume(boardState[0], puzzleId,
                            ItemTsumeBoardState.getMaximumPly(heldItem),
                            ItemTsumeBoardState.isMasterpiece(heldItem));
                } catch (IllegalArgumentException exception) {
                    player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.tsume.invalid"));
                    return ItemInteractionResult.FAIL;
                }

                EntityMaid seatedMaid = getSeatedMaid(level, chess);
                if (seatedMaid != null) {
                    seatedMaid.getGameRecordManager().resetStatue();
                }
                chess.refresh();
                level.playSound(null, pos, InitSounds.GOMOKU_RESET.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }

            // 只能空手操作
            if (!heldItem.isEmpty()) {
                return ItemInteractionResult.FAIL;
            }

            // 点击坐标的转换
            Direction facing = state.getValue(FACING);
            Vec3 clickPos = JChessUtil.toPlayerOrientedClick(hit.getLocation(), pos, part, facing);

            // 重置棋盘
            boolean clickResetArea = JChessUtil.isClickResetArea(clickPos);
            if (clickResetArea) {
                level.playSound(null, centerPos, InitSounds.GOMOKU_RESET.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                chess.reset();
                chess.refresh();

                // 重置女仆棋类动画
                Entity sitEntity = serverLevel.getEntity(chess.getSitId());
                if (sitEntity != null && sitEntity.isAlive() && sitEntity.getFirstPassenger() instanceof EntityMaid maid) {
                    maid.getGameRecordManager().resetStatue();
                }

                return ItemInteractionResult.SUCCESS;
            }

            // 女仆思考时间不允许走子，但仍允许重置；异步搜索稍后
            // 返回的旧结果会被 maidMove 中的回合检查安全忽略。
            if (!chess.isPlayerTurn() && !chess.isCheckmate()) {
                return ItemInteractionResult.FAIL;
            }

            // 检查女仆
            Entity sitEntity = serverLevel.getEntity(chess.getSitId());
            if (sitEntity == null || !sitEntity.isAlive() || !(sitEntity.getFirstPassenger() instanceof EntityMaid maid)) {
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid.gomoku.no_maid"));
                return ItemInteractionResult.FAIL;
            }
            // 检查是不是自己的女仆
            if (MaidConfig.MAID_GOMOKU_OWNER_LIMIT.get() && !maid.isOwnedBy(player)) {
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid.gomoku.not_owner"));
                return ItemInteractionResult.FAIL;
            }

            // 没有点击到棋盘上，返回
            int nowClick = JChessUtil.getClickPosition(clickPos, chess.getChessData());
            if (nowClick < 0) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }

            // 玩家已经输了，不能下棋
            if (chess.isCheckmate()) {
                return ItemInteractionResult.FAIL;
            }

            // 50 回合自然限着、长将不能下棋
            if (chess.isMoveNumberLimit() || chess.isRepeat()) {
                return ItemInteractionResult.FAIL;
            }

            // 处理点击棋子的逻辑
            Position chessData = chess.getChessData();
            int preClick = chess.getSelectChessPoint();

            int prePiece = chessData.getPieceByPointNum(preClick); // 棋子ID
            int nowPiece = chessData.getPieceByPointNum(nowClick);

            // 注意 将棋黑先，玩家是黑方
            // 如果前一个选择为空，或者选中的是白方，说明没有选中棋子
            if (prePiece <= 0 || JChessUtil.isWhite(prePiece)) {
                // 当前点击的是黑方棋子
                if (JChessUtil.isBlack(nowPiece)) {
                    chess.setSelectChessPoint(nowClick);
                    chess.refresh();
                    level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
                }
                return ItemInteractionResult.SUCCESS;
            }

            // 如果选的都是黑方棋子，相当于重选
            if (JChessUtil.isBlack(prePiece) && JChessUtil.isBlack(nowPiece)) {
                chess.setSelectChessPoint(nowClick);
                chess.refresh();
                level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
                return ItemInteractionResult.SUCCESS;
            }

            if (JChessUtil.isBlack(prePiece)) {
                if( !chessData.isLegalMove(preClick,nowClick)) {
                    return ItemInteractionResult.FAIL;
                }
                boolean can = chessData.canPromote(preClick, nowClick);
                boolean must = chessData.mustPromote(preClick, nowClick);

                if (must) {
                    // 强制升变
                    Position copy = chessData.deepCopy();
                    if (copy.move(preClick, nowClick, true) != -1) {
                        if (isPlayerKingInvalidAfterMove(chess, copy)
                                || !isCheckingTsumeMove(chess, copy)) {
                            return ItemInteractionResult.FAIL;
                        }
                        String playerMove = toUsiMove(preClick, nowClick, true, prePiece);
                        chessData.move(preClick, nowClick, true);
                        finishMove(chess, nowClick, level, pos, player, centerPos, playerMove);
                    }
                } else if (can) {
                    // 弹出升变选择框
                    if (player instanceof ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new JChessPromoteOpenPackage(
                                centerPos, chessData.toUSI(), preClick, nowClick));
                    }
                } else {
                    // 普通移动
                    Position copy = chessData.deepCopy();
                    if (copy.move(preClick, nowClick, false) != -1) {
                        if (isPlayerKingInvalidAfterMove(chess, copy)
                                || !isCheckingTsumeMove(chess, copy)) {
                            return ItemInteractionResult.FAIL;
                        }
                        String playerMove = toUsiMove(preClick, nowClick, false, prePiece);
                        chessData.move(preClick, nowClick, false);
                        finishMove(chess, nowClick, level, pos, player, centerPos, playerMove);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    private static void finishMove(TileEntityJChess chess, int nowClick, Level level, BlockPos pos,
                                   Player player, BlockPos centerPos, String playerMove) {
        chess.setSelectChessPoint(nowClick);
        chess.setChessCounter(chess.getChessData().getMoveNumber());
        if (chess.isTsumeMode()) {
            chess.advanceTsumePly();
        } else {
            chess.addHistoryAfterMove();
        }
        chess.refresh();
        level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);

        if (chess.isTsumeMode() && player instanceof ServerPlayer serverPlayer) {
            Position position = chess.getChessData();
            TsumeCheckStatus status = inspectTsumePosition(position);
            if (status.checkmate() && JChessUtil.isMaid(position)) {
                completeTsume(serverPlayer, level, centerPos, chess);
                return;
            }
            // Non-checking moves are rejected before being applied. Reaching the final
            // allowed ply without mate is therefore the only ordinary incorrect result.
            if (chess.getTsumePly() >= chess.getTsumeMaxPly()) {
                markTsumeIncorrect(serverPlayer, level, centerPos, chess);
                return;
            }
            String scriptedMove = "";
            if (chess.isMicrocosmosOnRecord()) {
                if (MicrocosmosRecord.isRecordedMove(chess.getTsumePly() - 1, playerMove)) {
                    scriptedMove = MicrocosmosRecord.defenseAfter(chess.getTsumePly()).orElse("");
                } else {
                    chess.leaveMicrocosmosRecord();
                    chess.refresh();
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer,
                    new JChessToClientPackage(centerPos, position.toUSI(), true, scriptedMove));
            return;
        }

        if (chess.isRepeat()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new JChessToClientPackage(centerPos, chess.getChessData().toUSI(), false, ""));
        }
    }

    private static String toUsiMove(int fromPos, int toPos, boolean promote, int pieceId) {
        String destination = toUsiSquare(toPos);
        if (fromPos >= 81) {
            String piece = switch (pieceId) {
                case 17 -> "P";
                case 16 -> "L";
                case 15 -> "N";
                case 12 -> "S";
                case 11 -> "G";
                case 14 -> "B";
                case 13 -> "R";
                default -> "?";
            };
            return piece + "*" + destination;
        }
        return toUsiSquare(fromPos) + destination + (promote ? "+" : "");
    }

    private static String toUsiSquare(int point) {
        int file = 9 - point % 9;
        char rank = (char) ('a' + point / 9);
        return Integer.toString(file) + rank;
    }

    /** Tsume attackers conventionally may omit their king; ordinary games may not. */
    private static boolean isPlayerKingInvalidAfterMove(TileEntityJChess chess, Position position) {
        if (chess.isTsumeMode() && !hasPiece(position, 10)) {
            return false;
        }
        return position.isKingUnderAttack(true);
    }

    private static boolean isCheckingTsumeMove(TileEntityJChess chess, Position position) {
        if (!chess.isTsumeMode()) {
            return true;
        }
        return com.github.sangeeeee.tlm_shogi.engine.core.Position.parse(position.toUSI()).inCheck();
    }

    private static boolean hasPiece(Position position, int pieceId) {
        for (int point = 0; point < 81; point++) {
            if (position.getPieceByPointNum(point) == pieceId) {
                return true;
            }
        }
        return false;
    }

    /** Uses the lightweight engine rule core for an exact legal-evasion check, never a search. */
    private static TsumeCheckStatus inspectTsumePosition(Position gamePosition) {
        com.github.sangeeeee.tlm_shogi.engine.core.Position position =
                com.github.sangeeeee.tlm_shogi.engine.core.Position.parse(gamePosition.toUSI());
        boolean inCheck = position.inCheck();
        return new TsumeCheckStatus(inCheck, inCheck && position.legalMoves().isEmpty());
    }

    private record TsumeCheckStatus(boolean inCheck, boolean checkmate) {
    }

    private static void completeTsume(ServerPlayer player, Level level, BlockPos pos, TileEntityJChess chess) {
        chess.markTsumeSolved();
        boolean masterpiece = chess.isTsumeMasterpiece();
        boolean microcosmos = chess.isMicrocosmos();
        boolean firstCompletion = TsumePlayerProgress.markSolved(player, chess.getTsumePuzzleId());
        if (masterpiece) {
            InitTrigger.MAID_EVENT.get().trigger(player, TriggerType.WIN_TSUME_MASTERPIECE);
        }
        if (microcosmos) {
            InitTrigger.MAID_EVENT.get().trigger(player, TriggerType.WIN_MICROCOSMOS);
        }
        EntityMaid maid = getSeatedMaid(level, chess);
        if (maid != null) {
            maid.swing(InteractionHand.MAIN_HAND);
            maid.getGameRecordManager().markStatue(false);
            if (firstCompletion && maid.isOwnedBy(player)) {
                Type reward = microcosmos ? MICROCOSMOS_WIN
                        : masterpiece ? TSUME_MASTERPIECE_WIN : Type.WCHESS_WIN;
                maid.getFavorabilityManager().apply(reward);
                InitTrigger.MAID_EVENT.get().trigger(player, TriggerType.WIN_JCHESS);
            }
        }
        if (!firstCompletion) {
            player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.tsume.solved_before"));
        }
        chess.refresh();
    }

    private static void markTsumeIncorrect(ServerPlayer player, Level level, BlockPos pos, TileEntityJChess chess) {
        chess.markTsumeIncorrect();
        EntityMaid maid = getSeatedMaid(level, chess);
        if (maid != null) {
            maid.getGameRecordManager().markStatue(true);
        }
        player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.tsume.incorrect"));
        chess.refresh();
    }

    @Nullable
    private static EntityMaid getSeatedMaid(Level level, TileEntityJChess chess) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getEntity(chess.getSitId()) instanceof EntitySit sit
                && sit.getFirstPassenger() instanceof EntityMaid maid) {
            return maid;
        }
        return null;
    }

    public static void handlePromoteResult(ServerLevel level, BlockPos centerPos, String expectedSfen,
                                           int fromPos, int toPos, int choice, ServerPlayer player) {
        BlockEntity be = level.getBlockEntity(centerPos);
        if (be instanceof TileEntityJChess chess) {
            if (!chess.getChessData().toUSI().equals(expectedSfen)
                    || !chess.isPlayerTurn() || chess.isCheckmate()
                    || player.distanceToSqr(Vec3.atCenterOf(centerPos)) > 64.0) {
                return;
            }
            EntityMaid maid = getSeatedMaid(level, chess);
            if (maid == null || (MaidConfig.MAID_GOMOKU_OWNER_LIMIT.get() && !maid.isOwnedBy(player))) {
                return;
            }
            Position data = chess.getChessData();

            if (choice == 0) {
                // 0 = 玩家取消，什么都不做，保留选中状态
                // 什么都不执行，玩家可以重新选择走法
                return;
            }
            if (choice != 1 && choice != 2) {
                return;
            }
            boolean promote = (choice == 1); // 1=是, 2=否

            // 执行带升变结果的移动
            Position copy = data.deepCopy();
            if (copy.move(fromPos, toPos, promote) != -1) {
                if (isPlayerKingInvalidAfterMove(chess, copy)
                        || !isCheckingTsumeMove(chess, copy)) {
                    return;
                }
                int pieceId = data.getPieceByPointNum(fromPos);
                String playerMove = toUsiMove(fromPos, toPos, promote, pieceId);
                data.move(fromPos, toPos, promote);
                finishMove(chess, toPos, level, centerPos, player, centerPos, playerMove);
            }
        }
    }

    @Override
    protected Vec3 sitPosition() {
        return Vec3.ZERO;
    }

    @Override
    protected String getTypeName() {
        return Type.GOMOKU.getTypeName();
    }

    @Override
    protected int sitYRot() {
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART).isCenter()) {
            return new TileEntityJChess(pos, state);
        }
        return null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec((properties) -> new BlockJChess());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape shape = getBaseShape(pState);
        ShogiPart part = pState.getValue(PART);
        BlockPos centerPos = pPos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
        if (!(pLevel.getBlockEntity(centerPos) instanceof TileEntityJChess chess)) {
            return shape;
        }

        List<int[]> hand = chess.getChessData().getBlackHand();
        int slotCount = Math.min(hand.size(), JChessUtil.HAND_COLUMNS * JChessUtil.HAND_ROWS);
        for (int index = 0; index < slotCount; index++) {
            int count = hand.get(index)[0];
            if (count < 1) {
                continue;
            }

            VoxelShape stackShape = getPlayerHandStackShape(pState, index, count);
            if (!stackShape.isEmpty()) {
                shape = Shapes.or(shape, stackShape);
            }
        }
        return shape;
    }

    /** Keep the rendered hand stacks selectable without making them physical obstacles. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getBaseShape(state);
    }

    private static VoxelShape getBaseShape(BlockState state) {
        return switch (state.getValue(PART)) {
            case CENTER -> SHAPE_CENTER;
            case LEFT_CENTER_EW -> SHAPE_LEFT_EW;
            case RIGHT_CENTER_EW -> SHAPE_RIGHT_EW;
            case LEFT_CENTER_NS -> SHAPE_LEFT_NS;
            case RIGHT_CENTER_NS -> SHAPE_RIGHT_NS;
        };
    }

    /** The exact per-stack shape used by both ray picking and the client highlight renderer. */
    public static VoxelShape getPlayerHandStackShape(BlockState state, int handIndex, int count) {
        if (handIndex < 0 || handIndex >= JChessUtil.HAND_COLUMNS * JChessUtil.HAND_ROWS || count < 1) {
            return Shapes.empty();
        }
        int column = handIndex % JChessUtil.HAND_COLUMNS;
        int row = handIndex / JChessUtil.HAND_COLUMNS;
        double minX = JChessUtil.HAND_MIN_X + column * JChessUtil.HAND_SLOT_WIDTH;
        double maxX = minX + JChessUtil.HAND_SLOT_WIDTH;
        double minZ = JChessUtil.HAND_MIN_Z + row * JChessUtil.HAND_SLOT_DEPTH;
        double maxZ = minZ + JChessUtil.HAND_SLOT_DEPTH;
        return getHandStackShape(state.getValue(PART), state.getValue(FACING),
                minX, maxX, minZ, maxZ, count);
    }

    private static VoxelShape getHandStackShape(ShogiPart part, Direction facing,
                                                 double minX, double maxX,
                                                 double minZ, double maxZ, int count) {
        double localMinX = Double.POSITIVE_INFINITY;
        double localMaxX = Double.NEGATIVE_INFINITY;
        double localMinZ = Double.POSITIVE_INFINITY;
        double localMaxZ = Double.NEGATIVE_INFINITY;
        float inverseRotation = -facing.toYRot() * Mth.DEG_TO_RAD;

        for (double x : new double[]{minX, maxX}) {
            for (double z : new double[]{minZ, maxZ}) {
                Vec3 transformed = new Vec3(x, 0, z).yRot(inverseRotation);
                double localX = transformed.x + 0.5 - part.getPosX();
                double localZ = transformed.z + 0.5 - part.getPosY();
                localMinX = Math.min(localMinX, localX);
                localMaxX = Math.max(localMaxX, localX);
                localMinZ = Math.min(localMinZ, localZ);
                localMaxZ = Math.max(localMaxZ, localZ);
            }
        }

        localMinX = Mth.clamp(localMinX, 0.0, 1.0);
        localMaxX = Mth.clamp(localMaxX, 0.0, 1.0);
        localMinZ = Mth.clamp(localMinZ, 0.0, 1.0);
        localMaxZ = Mth.clamp(localMaxZ, 0.0, 1.0);
        if (localMinX >= localMaxX || localMinZ >= localMaxZ) {
            return Shapes.empty();
        }

        return Block.box(
                localMinX * 16.0, JChessUtil.BOARD_SURFACE_Y * 16.0, localMinZ * 16.0,
                localMaxX * 16.0, JChessUtil.handStackTopY(count) * 16.0, localMaxZ * 16.0);
    }
}
