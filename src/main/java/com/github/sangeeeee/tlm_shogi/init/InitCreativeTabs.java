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
    // 类似于原模组的 DeferredRegister，但这里是事件处理

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();  // 获取 Tab 的 ResourceLocation

        // 使用工厂方法替换构造函数（假设 Tab 名为 "main"，详见下文确认方式）
        if (tabId.equals(ResourceLocation.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "main"))) {
            // 添加你的物品
            event.accept(new ItemStack(InitItems.JCHESS.get()));
        }
    }
}