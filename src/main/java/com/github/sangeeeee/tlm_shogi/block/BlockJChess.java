package com.github.sangeeeee.tlm_shogi.block;

import com.github.sangeeeee.tlm_shogi.advancements.maid.TriggerType;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.PlayerPlatformSupport;
import com.github.tartaricacid.touhoulittlemaid.api.block.IBoardGameBlock;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.tartaricacid.touhoulittlemaid.block.BlockJoy;
import com.github.sangeeeee.tlm_shogi.block.properties.ShogiPart;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.sangeeeee.tlm_shogi.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.github.sangeeeee.tlm_shogi.network.message.JChessPromoteOpenPackage;
import com.github.sangeeeee.tlm_shogi.network.message.JChessToClientPackage;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityJoy;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BlockJChess extends BlockJoy implements IBoardGameBlock {
    public static final EnumProperty<ShogiPart> PART = EnumProperty.create("part", ShogiPart.class);
    public static final int plate = 6;
    public static final int height = 10;
    public static final VoxelShape SHAPE_CENTER = Block.box(0, 0, 0, 16, height, 16);
    public static final VoxelShape SHAPE_RIGHT_NS = Block.box(0, 0, 16 - plate, plate, height, 16);
    public static final VoxelShape SHAPE_LEFT_NS = Block.box(16 - plate, 0, 0, 16, height, plate);
    public static final VoxelShape SHAPE_RIGHT_EW = Block.box(16 - plate, 0, 16 - plate, 16, height, 16);
    public static final VoxelShape SHAPE_LEFT_EW = Block.box(0, 0, 0, plate, height, plate);

    public BlockJChess() {
        super(Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0F, 3.0F).forceSolidOn().noOcclusion());
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


    public static void maidMove(ServerPlayer player, Level level, BlockPos pos, String move, boolean maidLost, boolean playerLost) {
        if (level.getBlockEntity(pos) instanceof TileEntityJChess chess) {
            if (chess.isPlayerTurn()) {
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
            if (toPos != -1) {
                chess.setSelectChessPoint(toPos);
                if (chessData.isCheck() ){
                    player.sendSystemMessage(Component.translatable("message.touhou_little_maid.cchess.check"));
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
                }
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

            // 女仆思考时间，不允许玩家操作
            if (!chess.isPlayerTurn() && !chess.isCheckmate()) {
                return ItemInteractionResult.FAIL;
            }
            ItemStack heldItem = player.getMainHandItem();

            // TODO: 如果是诘将棋道具，那么直接设置诘将棋道具
//            if (heldItem.is(InitItems.WCHESS_BOARD_STATE.get())) {
//                String[] boardState = ItemBoardState.getState(heldItem);
//                if (boardState == null) {
//                    return ItemInteractionResult.FAIL;
//                }
//                String data = boardState[0];
//                if (StringUtils.isEmpty(data)) {
//                    return ItemInteractionResult.FAIL;
//                }
//                chess.setEndgame(data);
//                level.playSound(null, pos, InitSounds.GOMOKU_RESET.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
//                return ItemInteractionResult.SUCCESS;
//            }

            // 只能空手操作
            if (!heldItem.isEmpty()) {
                return ItemInteractionResult.FAIL;
            }

            // 点击坐标的转换
            Direction facing = state.getValue(FACING);
            Vec3 clickPos = hit.getLocation()
                    .subtract(pos.getX(), pos.getY(), pos.getZ())
                    .add(part.getPosX() - 0.5, 0, part.getPosY() - 0.5)
                    .yRot(facing.toYRot() * Mth.DEG_TO_RAD);

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

            // 引擎运行在玩家客户端，因此必须先于女仆检查验证客户端平台。
            if (!PlayerPlatformSupport.isSupported(player)) {
                player.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.unsupported_platform"));
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
            int nowClick = JChessUtil.getClickPosition(clickPos);
            if (nowClick < 0) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }

            // 玩家已经输了，不能下棋
            if (chess.isCheckmate() && chess.isPlayerTurn()) {
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
                        if (copy.isKingUnderAttack(true)) {
                            return ItemInteractionResult.FAIL;
                        }
                        chessData.move(preClick, nowClick, true);
                        finishMove(chess, nowClick, level, pos, player, centerPos);
                    }
                } else if (can) {
                    // 弹出升变选择框
                    if (player instanceof ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new JChessPromoteOpenPackage(centerPos, preClick, nowClick));
                    }
                } else {
                    // 普通移动
                    Position copy = chessData.deepCopy();
                    if (copy.move(preClick, nowClick, false) != -1) {
                        if (copy.isKingUnderAttack(true)) {
                            return ItemInteractionResult.FAIL;
                        }
                        chessData.move(preClick, nowClick, false);
                        finishMove(chess, nowClick, level, pos, player, centerPos);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    private static void finishMove(TileEntityJChess chess, int nowClick, Level level, BlockPos pos, Player player, BlockPos centerPos) {
        chess.setSelectChessPoint(nowClick);
        chess.setChessCounter(chess.getChessData().getMoveNumber());
        chess.addHistoryAfterMove();
        chess.refresh();
        level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
        if (chess.isRepeat()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new JChessToClientPackage(centerPos, chess.getChessData().toUSI()));
        }
    }

    public static void handlePromoteResult(ServerLevel level, BlockPos centerPos, int fromPos, int toPos, int choice, ServerPlayer player) {
        BlockEntity be = level.getBlockEntity(centerPos);
        if (be instanceof TileEntityJChess chess) {
            Position data = chess.getChessData();

            if (choice == 0) {
                // 0 = 玩家取消，什么都不做，保留选中状态
                // 什么都不执行，玩家可以重新选择走法
                return;
            }
            boolean promote = (choice == 1); // 1=是, 2=否

            // 执行带升变结果的移动
            Position copy = data.deepCopy();
            if (copy.move(fromPos, toPos, promote) != -1) {
                if (copy.isKingUnderAttack(true)) {
                    return;
                }
                data.move(fromPos, toPos, promote);
                finishMove(chess, toPos, level, centerPos, player, centerPos);
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
        return switch (pState.getValue(PART)) {
            case CENTER -> SHAPE_CENTER;
            case LEFT_CENTER_EW -> SHAPE_LEFT_EW;
            case RIGHT_CENTER_EW -> SHAPE_RIGHT_EW;
            case LEFT_CENTER_NS -> SHAPE_LEFT_NS;
            case RIGHT_CENTER_NS -> SHAPE_RIGHT_NS;
        };
    }
}
