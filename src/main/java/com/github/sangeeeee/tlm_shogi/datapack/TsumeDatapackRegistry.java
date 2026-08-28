package com.github.sangeeeee.tlm_shogi.datapack;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = TouhouLittleMaidShogi.MOD_ID)
public final class TsumeDatapackRegistry {
    private TsumeDatapackRegistry() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new TsumeBoardStateReloadListener());
    }
}
