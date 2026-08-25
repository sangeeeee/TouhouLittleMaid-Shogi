package com.github.sangeeeee.tlm_shogi.network.message;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.PlayerPlatformSupport;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessPlatformSupportPackage(boolean supported) implements CustomPacketPayload {
    public static final Type<JChessPlatformSupportPackage> TYPE =
            new Type<>(getResourceLocation("jchess_platform_support"));

    public static final StreamCodec<ByteBuf, JChessPlatformSupportPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, JChessPlatformSupportPackage::supported,
            JChessPlatformSupportPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JChessPlatformSupportPackage message, IPayloadContext context) {
        if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player) {
            PlayerPlatformSupport.update(player, message.supported);
        }
    }
}
