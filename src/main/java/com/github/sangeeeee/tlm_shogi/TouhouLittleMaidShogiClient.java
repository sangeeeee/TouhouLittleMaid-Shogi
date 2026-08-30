package com.github.sangeeeee.tlm_shogi;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.ShogiEngineInteractor;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod(value = TouhouLittleMaidShogi.MOD_ID, dist = Dist.CLIENT)
public class TouhouLittleMaidShogiClient {
    public TouhouLittleMaidShogiClient(IEventBus modEventBus, ModContainer modContainer) {
        // Load the Java engine on a client worker so game startup is not
        // blocked by the roughly 45 MiB evaluation table.
        CompletableFuture.runAsync(ShogiEngineInteractor::warmUp, Util.backgroundExecutor());
    }
}
