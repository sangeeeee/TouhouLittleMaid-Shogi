package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.block.BlockJChess;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessPromoteResultPackage(BlockPos chessPos, int fromPos, int toPos, int choice)
        implements CustomPacketPayload {

    public static final Type<JChessPromoteResultPackage> TYPE =
            new Type<>(getResourceLocation("jchess_promote_result"));

    public static final StreamCodec<ByteBuf, JChessPromoteResultPackage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, JChessPromoteResultPackage::chessPos,
                    ByteBufCodecs.INT,     JChessPromoteResultPackage::fromPos,
                    ByteBufCodecs.INT,     JChessPromoteResultPackage::toPos,
                    ByteBufCodecs.INT,     JChessPromoteResultPackage::choice,
                    JChessPromoteResultPackage::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JChessPromoteResultPackage message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
                    BlockJChess.handlePromoteResult(level, message.chessPos,
                            message.fromPos, message.toPos, message.choice, player);
                }
            });
        }
    }
}