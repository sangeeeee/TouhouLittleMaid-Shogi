package com.github.sangeeeee.tlm_shogi.network.message;

import com.github.sangeeeee.tlm_shogi.client.gui.game.JChessPromoteScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessPromoteOpenPackage(BlockPos chessPos, int fromPos, int toPos)
        implements CustomPacketPayload {

    public static final Type<JChessPromoteOpenPackage> TYPE =
            new Type<>(getResourceLocation("jchess_promote_open"));

    public static final StreamCodec<ByteBuf, JChessPromoteOpenPackage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, JChessPromoteOpenPackage::chessPos,
                    ByteBufCodecs.INT,     JChessPromoteOpenPackage::fromPos,
                    ByteBufCodecs.INT,     JChessPromoteOpenPackage::toPos,
                    JChessPromoteOpenPackage::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JChessPromoteOpenPackage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                onHandle(message);
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(JChessPromoteOpenPackage message) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().setScreen(
                        new JChessPromoteScreen(message.chessPos, message.fromPos, message.toPos)
                )
        );
    }
}