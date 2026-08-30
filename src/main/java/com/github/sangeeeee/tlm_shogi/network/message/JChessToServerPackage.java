package com.github.sangeeeee.tlm_shogi.network.message;

import com.github.sangeeeee.tlm_shogi.block.BlockJChess;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessToServerPackage(BlockPos pos, String expectedSfen, String move, boolean maidLost,
                                    boolean playerLost) implements CustomPacketPayload {
    public static final Type<JChessToServerPackage> TYPE = new Type<>(getResourceLocation("jchess_to_server"));

    public static final StreamCodec<ByteBuf, JChessToServerPackage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, JChessToServerPackage::pos,
            ByteBufCodecs.STRING_UTF8, JChessToServerPackage::expectedSfen,
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
                if (!(level.getBlockEntity(message.pos) instanceof TileEntityJChess chess)
                        || !chess.getChessData().toSfen().equals(message.expectedSfen)) {
                    // The board was reset or advanced while the client was searching.
                    return;
                }
                switch (message.move) {
                    case "no engine" ->
                            sender.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.noengine"));
                    case "engine error" ->
                            sender.sendSystemMessage(Component.translatable("message.tlm_shogi.jchess.engineerr"));
                    case null, default ->
                            BlockJChess.maidMove(sender, level, message.pos, message.expectedSfen,
                                    message.move, message.maidLost, message.playerLost);
                }
            });
        }
    }
}
