package com.github.sangeeeee.tlm_shogi;

import com.github.sangeeeee.tlm_shogi.api.game.jchess.EngineExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = TouhouLittleMaidShogi.MOD_ID, dist = Dist.CLIENT)
public class TouhouLittleMaidShogiClient {
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(java.util.Locale.ENGLISH).contains("win");
    public TouhouLittleMaidShogiClient(IEventBus modEventBus, ModContainer modContainer) {
        //提取将棋引擎到config/touhou_little_maid
        if(IS_WINDOWS) EngineExtractor.extractIfNeeded();
    }

}
