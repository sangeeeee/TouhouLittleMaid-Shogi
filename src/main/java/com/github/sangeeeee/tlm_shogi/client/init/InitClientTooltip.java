package com.github.sangeeeee.tlm_shogi.client.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.client.tooltip.ClientTsumeBoardStateTooltip;
import com.github.sangeeeee.tlm_shogi.inventory.tooltip.TsumeBoardStateTooltip;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(modid = TouhouLittleMaidShogi.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class InitClientTooltip {
    private InitClientTooltip() {
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(TsumeBoardStateTooltip.class, ClientTsumeBoardStateTooltip::new);
    }
}
