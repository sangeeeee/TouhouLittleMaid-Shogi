package com.github.sangeeeee.tlm_shogi.network;

import com.github.sangeeeee.tlm_shogi.network.message.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    private static final String VERSION = "1.1.0";

    private NetworkHandler() {
    }

    public static void registerPacket(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION).optional();

        registrar.playToClient(JChessToClientPackage.TYPE, JChessToClientPackage.STREAM_CODEC, JChessToClientPackage::handle);
        registrar.playToServer(JChessToServerPackage.TYPE, JChessToServerPackage.STREAM_CODEC, JChessToServerPackage::handle);
        registrar.playToClient(JChessPromoteOpenPackage.TYPE, JChessPromoteOpenPackage.STREAM_CODEC, JChessPromoteOpenPackage::handle);
        registrar.playToServer(JChessPromoteResultPackage.TYPE, JChessPromoteResultPackage.STREAM_CODEC, JChessPromoteResultPackage::handle);
    }

}
