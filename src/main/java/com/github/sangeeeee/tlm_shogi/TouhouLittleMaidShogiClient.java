package com.github.sangeeeee.tlm_shogi;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.EngineExtractor;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.ShogiEnginePlatform;
import com.github.sangeeeee.tlm_shogi.network.message.JChessPlatformSupportPackage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = TouhouLittleMaidShogi.MOD_ID, dist = Dist.CLIENT)
public class TouhouLittleMaidShogiClient {
    public TouhouLittleMaidShogiClient(IEventBus modEventBus, ModContainer modContainer) {
        //提取将棋引擎到config/touhou_little_maid
        if (ShogiEnginePlatform.isSupported()) {
            EngineExtractor.extractIfNeeded();
        }
        NeoForge.EVENT_BUS.addListener(TouhouLittleMaidShogiClient::onClientLogin);
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        PacketDistributor.sendToServer(new JChessPlatformSupportPackage(ShogiEnginePlatform.isSupported()));
    }
}
