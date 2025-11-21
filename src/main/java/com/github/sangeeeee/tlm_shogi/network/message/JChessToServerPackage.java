package com.github.sangeeeee.tlm_shogi.network.message;

import com.github.sangeeeee.tlm_shogi.block.BlockJChess;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessToServerPackage(BlockPos pos, String move, boolean maidLost,
                                    boolean playerLost) implements CustomPacketPayload {
    public static final Type<JChessToServerPackage> TYPE = new Type<>(getResourceLocation("jchess_to_server"));

    public static final StreamCodec<ByteBuf, JChessToServerPackage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, JChessToServerPackage::pos,
            ByteBufCodecs.STRING_UTF8, JChessToServerPackage::move,
            ByteBufCodecs.BOOL, JChessToServerPackage::maidLost,
            ByteBufCodecs.BOOL, JChessToServerPackage::playerLost,
            JChessToServerPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JChessToServerPackage message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer sender)) {
                    return;
                }
                Level level = sender.level();
                if (!level.isLoaded(message.pos)) {
                    return;
                }
                BlockJChess.maidMove(sender, level, message.pos, message.move, message.maidLost, message.playerLost);
            });
        }
    }
}