package com.github.sangeeeee.tlm_shogi.network.message;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.ShogiEngineInteractor;
import com.github.sangeeeee.tlm_shogi.network.message.JChessToServerPackage;
import com.github.sangeeeee.tlm_shogi.util.JChessUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessToClientPackage(BlockPos pos, String fenData) implements CustomPacketPayload {

    public static final Type<JChessToClientPackage> TYPE = new Type<>(getResourceLocation("jchess_to_client"));
    public static final StreamCodec<ByteBuf, JChessToClientPackage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            JChessToClientPackage::pos,
            ByteBufCodecs.STRING_UTF8,
            JChessToClientPackage::fenData,
            JChessToClientPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(JChessToClientPackage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(JChessToClientPackage message) {
        int levelTime = 1000;
        long timeStart = System.currentTimeMillis();
        String move = "";

        Position position = new Position();
        position.applyUSI(message.fenData);

        // 先判断玩家是否赢了
        // 是的，我放客户端，减轻服务端压力，理论上你可直接传布尔值判断女仆输掉来作弊
        // sange: 这河里吗，，，不管了那我也放这儿吧
        boolean maidLost = false;
        boolean playerLost = false;
        if (position.isCheck() && position.isMate() && JChessUtil.isMaid(position)) {
            maidLost = true;
        }

        if (!maidLost) {
            // TODO: 暂时不做女仆的棋技系统

            ShogiEngineInteractor interactor = new ShogiEngineInteractor();
            try {
                String json = "{\"USI_Hash\": \"256\", \"NodesLimit\": \"30000\", \"DepthLimit\": \"8\"}";
                interactor.setup(json);
                move = interactor.interact(message.fenData, null);
                interactor.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            position.makeMove(move);
            if (position.isCheck() && position.isMate() && JChessUtil.isPlayer(position)) {
                playerLost = true;
            }
        }

        // 如果时间还有剩余，那么 sleep 一会儿
        long timeRemain = Math.max(0, levelTime - (int) (System.currentTimeMillis() - timeStart));
        try {
            if (timeRemain > 0) {
                Thread.sleep(timeRemain);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final String moveFinal = move;
        final boolean playerLostFinal = playerLost;
        final boolean maidLostFinal = maidLost;
        Minecraft.getInstance().submitAsync(() -> PacketDistributor.sendToServer(new JChessToServerPackage(message.pos, moveFinal, maidLostFinal, playerLostFinal)));
    }
}
