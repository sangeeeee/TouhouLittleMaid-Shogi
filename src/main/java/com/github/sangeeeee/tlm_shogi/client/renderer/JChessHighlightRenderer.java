package com.github.sangeeeee.tlm_shogi.client.renderer;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.block.BlockJChess;
import com.github.sangeeeee.tlm_shogi.block.properties.ShogiPart;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import com.github.sangeeeee.tlm_shogi.util.JChessUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import java.util.List;

@EventBusSubscriber(modid = TouhouLittleMaidShogi.MOD_ID, value = Dist.CLIENT)
public final class JChessHighlightRenderer {
    private JChessHighlightRenderer() {
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        BlockPos blockPos = hit.getBlockPos();
        BlockState state = level.getBlockState(blockPos);
        if (!(state.getBlock() instanceof BlockJChess)) {
            return;
        }

        ShogiPart part = state.getValue(BlockJChess.PART);
        BlockPos centerPos = blockPos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
        if (!(level.getBlockEntity(centerPos) instanceof TileEntityJChess chess)) {
            return;
        }

        Vec3 clickPos = JChessUtil.toPlayerOrientedClick(
                hit.getLocation(), blockPos, part, state.getValue(BlockJChess.FACING));
        int handIndex = JChessUtil.getPlayerHandPosition(clickPos, chess.getChessData());
        List<int[]> hand = chess.getChessData().getBlackHand();
        if (handIndex < 0 || handIndex >= hand.size()) {
            return;
        }

        VoxelShape stackShape = BlockJChess.getPlayerHandStackShape(
                state, handIndex, hand.get(handIndex)[0]);
        if (stackShape.isEmpty()) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderVoxelShape(
                event.getPoseStack(), lines, stackShape,
                blockPos.getX() - cameraPos.x,
                blockPos.getY() - cameraPos.y,
                blockPos.getZ() - cameraPos.z,
                0.0F, 0.0F, 0.0F, 0.4F, false);
        event.setCanceled(true);
    }
}
