package com.github.sangeeeee.tlm_shogi.network;

import com.github.sangeeeee.tlm_shogi.network.message.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    private static final String VERSION = "1.1.0";

    public static void registerPacket(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION).optional();

        registrar.playToClient(JChessToClientPackage.TYPE, JChessToClientPackage.STREAM_CODEC, JChessToClientPackage::handle);
        registrar.playToServer(JChessToServerPackage.TYPE, JChessToServerPackage.STREAM_CODEC, JChessToServerPackage::handle);
        registrar.playToClient(JChessPromoteOpenPackage.TYPE, JChessPromoteOpenPackage.STREAM_CODEC, JChessPromoteOpenPackage::handle);
        registrar.playToServer(JChessPromoteResultPackage.TYPE, JChessPromoteResultPackage.STREAM_CODEC, JChessPromoteResultPackage::handle);
    }

    public static void sendToNearby(Entity entity, CustomPacketPayload toSend) {
        if (entity.level() instanceof ServerLevel) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, toSend);
        }
    }

    public static void sendToNearby(Entity entity, CustomPacketPayload toSend, int distance) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = entity.blockPosition();
            PacketDistributor.sendToPlayersNear(serverLevel, null, pos.getX(), pos.getY(), pos.getZ(), distance, toSend);
        }
    }
}
