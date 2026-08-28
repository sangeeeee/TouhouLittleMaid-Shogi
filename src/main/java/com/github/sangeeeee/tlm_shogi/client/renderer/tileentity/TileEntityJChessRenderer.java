package com.github.sangeeeee.tlm_shogi.client.renderer.tileentity;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.tartaricacid.touhoulittlemaid.block.BlockGomoku;
import com.github.sangeeeee.tlm_shogi.client.model.JChessPiecesModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.sangeeeee.tlm_shogi.client.resource.BedrockModelLoader;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import com.github.sangeeeee.tlm_shogi.util.JChessUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TileEntityJChessRenderer implements BlockEntityRenderer<TileEntityJChess> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaidShogi.MOD_ID, "textures/bedrock/block/jchess.png");
    private static final ResourceLocation PIECES_TEXTURE = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaidShogi.MOD_ID, "textures/bedrock/block/jchess_pieces.png");
    private static final int TIPS_RENDER_DISTANCE = 16;
    private static final int PIECE_RENDER_DISTANCE = 24;
    private final Font font;
    private final BlockEntityRenderDispatcher dispatcher;
    private final SimpleBedrockModel<Entity> chessModel;
    private final JChessPiecesModel[] chessPiecesModels;
    private final JChessPiecesModel selectedModels;

    public TileEntityJChessRenderer(BlockEntityRendererProvider.Context context) {
        chessModel = BedrockModelLoader.getModel(BedrockModelLoader.JCHESS);
        chessPiecesModels = JChessPiecesModel.initModel();
        selectedModels = JChessPiecesModel.getSelectedModel();
        dispatcher = context.getBlockEntityRenderDispatcher();
        font = context.getFont();
    }

    @Override
    public void render(TileEntityJChess jchess, float pPartialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        Direction facing = jchess.getBlockState().getValue(BlockGomoku.FACING);
        this.renderChessboard(poseStack, bufferIn, combinedLightIn, combinedOverlayIn, facing);
        this.renderPiece(jchess, poseStack, bufferIn, combinedLightIn, combinedOverlayIn, facing);
        this.renderTipsText(jchess, poseStack, bufferIn, combinedLightIn);
    }

    private void renderTipsText(TileEntityJChess chess, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn) {
        boolean showTips = chess.isCheckmate() || chess.isRepeat() || chess.isMoveNumberLimit();
        if (!showTips || !inRenderDistance(chess, TIPS_RENDER_DISTANCE)) {
            return;
        }

        Camera camera = this.dispatcher.camera;
        MutableComponent loseTips = null;
        MutableComponent resetTips = Component.translatable("message.tlm_shogi.jchess.reset").withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.AQUA);
        MutableComponent roundText = chess.isTsumeMode()
                ? Component.translatable("message.tlm_shogi.jchess.tsume.ply",
                        chess.getTsumePly(), chess.getTsumeMaxPly()).withStyle(ChatFormatting.WHITE)
                : Component.translatable("message.touhou_little_maid.gomoku.round",
                        chess.getChessCounter()).withStyle(ChatFormatting.WHITE);
        MutableComponent preRoundIcon = Component.literal("⏹ ").withStyle(ChatFormatting.GREEN);
        MutableComponent postRoundIcon = Component.literal(" ⏹").withStyle(ChatFormatting.GREEN);
        MutableComponent roundTips = preRoundIcon.append(roundText).append(postRoundIcon);

        if (chess.isTsumeMode() && chess.isCheckmate()) {
            loseTips = Component.translatable(chess.isTsumeIncorrect()
                            ? "message.tlm_shogi.jchess.tsume.incorrect"
                            : "message.tlm_shogi.jchess.tsume.correct")
                    .withStyle(ChatFormatting.BOLD).withStyle(
                            chess.isTsumeIncorrect() ? ChatFormatting.RED : ChatFormatting.GREEN);
        } else if (chess.isCheckmate()) {
            if (!chess.isPlayerTurn()) {
                loseTips = Component.translatable("message.touhou_little_maid.gomoku.win").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.DARK_PURPLE);
            } else {
                loseTips = Component.translatable("message.touhou_little_maid.gomoku.lose").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.DARK_PURPLE);
            }
        } else if (chess.isMoveNumberLimit()) {
            loseTips = Component.translatable("message.touhou_little_maid.cchess.move_limit").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.DARK_PURPLE);
        } else if (chess.isRepeat()) {
            loseTips = Component.translatable("message.touhou_little_maid.cchess.repeat").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.DARK_PURPLE);
        }
        if (loseTips == null) {
            return;
        }

        float loseTipsWidth = (float) (-this.font.width(loseTips) / 2);
        float resetTipsWidth = (float) (-this.font.width(resetTips) / 2);
        float roundTipsWidth = (float) (-this.font.width(roundTips) / 2);
        poseStack.pushPose();
        poseStack.translate(0.5, 1.25, 0.5);
        poseStack.mulPose(Axis.YN.rotationDegrees(180 + camera.getYRot()));
        poseStack.mulPose(Axis.XN.rotationDegrees(camera.getXRot()));

        float scale = 0.025F;
        poseStack.scale(scale, -scale, scale);

        this.font.drawInBatch(loseTips, loseTipsWidth, -10, 0xFFFFFF, true, poseStack.last().pose(), bufferIn, Font.DisplayMode.POLYGON_OFFSET, 0, combinedLightIn);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        this.font.drawInBatch(roundTips, roundTipsWidth, -30, 0xFFFFFF, true, poseStack.last().pose(), bufferIn, Font.DisplayMode.POLYGON_OFFSET, 0, combinedLightIn);
        this.font.drawInBatch(resetTips, resetTipsWidth, 0, 0xFFFFFF, true, poseStack.last().pose(), bufferIn, Font.DisplayMode.POLYGON_OFFSET, 0, combinedLightIn);
        poseStack.popPose();
    }

    private void renderPiece(TileEntityJChess jchess, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, Direction facing) {
        if (inRenderDistance(jchess, PIECE_RENDER_DISTANCE)) {
            float p = 0.4218F;
            float h = 1.901F;
            float s = 0.1053f;

            float p1 = 0.2053F;
            float p2 = 0.5205F;
//            float s0 = 0.0320f;
            float s1 = 0.0662f;
            float s2 = 0.0505f;
            float s3 = 0.1096f;
            JChessPiecesModel chessPiecesModel;

            int selectedPoint = jchess.getSelectChessPoint();

            boolean selectOnHand = false;
            int selected_x = -1;
            int selected_y = -1;
            if (0 <= selectedPoint && selectedPoint < 81) {
                selected_x = selectedPoint % 9;
                selected_y = selectedPoint / 9;
            } else if (81 <= selectedPoint && selectedPoint <= 89) {
                selectedPoint -= 81;
                selectOnHand = true;
            }

            VertexConsumer piecesBuff = bufferIn.getBuffer(RenderType.entityCutoutNoCull(PIECES_TEXTURE));
            poseStack.pushPose();

            switch (facing) {
                case NORTH:
                    poseStack.translate(p + 0.5, h, p + 0.5);
                    break;
                case EAST:
                    poseStack.translate(-p + 0.5, h, p + 0.5);
                    break;
                case WEST:
                    poseStack.translate(p + 0.5, h, -p + 0.5);
                    break;
                default:
                    poseStack.translate(-p + 0.5, h, -p + 0.5);
                    break;
            }
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.get2DDataValue() * 90));
            if (facing == Direction.SOUTH || facing == Direction.NORTH) {
                poseStack.mulPose(Axis.YN.rotationDegrees(180));
            }
            Position position = jchess.getChessData();
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    int pieceId = position.getPieceAt(j, i);
                    if (pieceId == 0) {
                        poseStack.translate(s, 0, 0);
                        continue;
                    }
                    chessPiecesModel = this.chessPiecesModels[pieceId];
                    chessPiecesModel.renderToBuffer(poseStack, piecesBuff, combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
                    if (!selectOnHand && i == selected_y && j == selected_x) {
                        selectedModels.renderToBuffer(poseStack, piecesBuff, combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
                    }

                    poseStack.translate(s, 0, 0);
                }
                poseStack.translate(-s * 9, 0, -s);
            }
            poseStack.popPose();

            poseStack.pushPose();
            switch (facing) {
                case NORTH:
                    poseStack.translate(-p2 + 0.5, h, -p1 + 0.5);
                    break;
                case EAST:
                    poseStack.translate(p1 + 0.5, h, -p2 + 0.5);
                    break;
                case WEST:
                    poseStack.translate(-p1 + 0.5, h, p2 + 0.5);
                    break;
                default:
                    poseStack.translate(p2 + 0.5, h, p1 + 0.5);
                    break;
            }
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.get2DDataValue() * 90));
            if (facing == Direction.SOUTH || facing == Direction.NORTH) {
                poseStack.mulPose(Axis.YN.rotationDegrees(180));
            }
            renderHand(position.getBlackHand(), poseStack, piecesBuff,
                    combinedLightIn, combinedOverlayIn, s1, s2, s3, selectOnHand, selectedPoint);

            poseStack.popPose();
            poseStack.pushPose();
            p1 += 0.2180f;
            p2 += 0.3350f;
            switch (facing) {
                case NORTH:
                    poseStack.translate(p2 + 0.5, h, p1 + 0.5);
                    break;
                case EAST:
                    poseStack.translate(-p1 + 0.5, h, p2 + 0.5);
                    break;
                case WEST:
                    poseStack.translate(p1 + 0.5, h, -p2 + 0.5);
                    break;
                default:
                    poseStack.translate(-p2 + 0.5, h, -p1 + 0.5);
                    break;
            }
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.get2DDataValue() * 90));
            if (facing == Direction.SOUTH || facing == Direction.NORTH) {
                poseStack.mulPose(Axis.YN.rotationDegrees(180));
            }
            renderHand(position.getWhiteHand(), poseStack, piecesBuff,
                    combinedLightIn, combinedOverlayIn, s1, s2, s3, false, -1);
            poseStack.popPose();
        }
    }

    private void renderHand(java.util.List<int[]> handList,
                            PoseStack poseStack, VertexConsumer buff,
                            int light, int overlay,
                            float s1, float s2, float s3, boolean selectOnHand, int selectedPoint) {
        if (handList.isEmpty()) return;

        int index = 0;
        int rowCount = 0;
        float rowOffset = 0f;

        while (index < handList.size()) {
            int[] pair = handList.get(index);
            int count = pair[0];
            int pieceId = pair[1];

            // Keep the old slot layout, but use its former number area as padding.
            poseStack.translate(s1, 0, 0);
            rowOffset += s1;

            // Render one physical piece for every captured piece. The renderer's
            // Z rotation inverts local Y, so negative local Y stacks upward.
            JChessPiecesModel model = this.chessPiecesModels[pieceId];
            poseStack.pushPose();
            for (int layer = 0; layer < count; layer++) {
                if (layer > 0) {
                    poseStack.translate(0, -JChessUtil.HAND_STACK_LAYER_HEIGHT, 0);
                }
                model.renderToBuffer(poseStack, buff, light, overlay, 1, 1, 1, 1);
            }
            if (count > 0 && selectOnHand && selectedPoint == index) {
                selectedModels.renderToBuffer(poseStack, buff, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack.popPose();
            poseStack.translate(s2, 0, 0);
            rowOffset += s2;

            index++;
            rowCount++;

            // ---------- 换行 ----------
            if (rowCount == 3 && index < handList.size()) {
                poseStack.translate(-rowOffset, 0, -s3);
                rowOffset = 0f;
                rowCount = 0;
            }
        }
    }

    private void renderChessboard(PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, Direction facing) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        poseStack.mulPose(Axis.YN.rotationDegrees(facing.get2DDataValue() * 90));
        if (facing == Direction.SOUTH || facing == Direction.NORTH) {
            poseStack.mulPose(Axis.YN.rotationDegrees(180));
        }
        VertexConsumer checkerBoardBuff = bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        chessModel.renderToBuffer(poseStack, checkerBoardBuff, combinedLightIn, combinedOverlayIn);
        poseStack.popPose();
    }

    private boolean inRenderDistance(TileEntityJChess chess, int distance) {
        BlockPos pos = chess.getBlockPos();
        return this.dispatcher.camera.getPosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < distance * distance;
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityJChess te) {
        return true;
    }
}
