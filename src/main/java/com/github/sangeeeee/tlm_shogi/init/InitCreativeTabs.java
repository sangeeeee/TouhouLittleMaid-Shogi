package com.github.sangeeeee.tlm_shogi.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;


@EventBusSubscriber(modid = TouhouLittleMaidShogi.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InitCreativeTabs {
    private InitCreativeTabs() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();

        if (tabId.equals(ResourceLocation.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "main"))) {
            event.accept(new ItemStack(InitItems.JCHESS.get()));
            event.accept(new ItemStack(InitItems.JCHESS_BOARD_STATE.get()));
            event.accept(new ItemStack(InitItems.MICROCOSMOS.get()));
        }
    }
}
