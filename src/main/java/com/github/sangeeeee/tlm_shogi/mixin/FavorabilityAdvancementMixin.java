package com.github.sangeeeee.tlm_shogi.mixin;

import com.github.sangeeeee.tlm_shogi.advancements.maid.TriggerType;
import com.github.sangeeeee.tlm_shogi.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.advancements.maid.MaidEventTrigger;
import com.github.tartaricacid.touhoulittlemaid.datagen.advancement.FavorabilityAdvancement;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(FavorabilityAdvancement.class)
public abstract class FavorabilityAdvancementMixin {

    /**
     * 在 generateJoy 方法完全执行完毕后（TAIL）追加日本将棋胜利成就
     */
    @Inject(method = "generateJoy",
            at = @At("TAIL"),
            remap = false)
    private static void injectExtraJoyAdvancement(
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper,
            AdvancementHolder root,
            CallbackInfo ci) {

        MutableComponent title = Component.translatable(
                "advancements.touhou_little_maid.favorability.win_jchess.title");
        MutableComponent desc = Component.translatable(
                "advancements.touhou_little_maid.favorability.win_jchess.description");

        Advancement.Builder builder = Advancement.Builder.advancement()
                .display(
                        InitItems.JCHESS.get(),
                        title,
                        desc,
                        ResourceLocation.fromNamespaceAndPath("touhou_little_maid",
                                "textures/advancements/backgrounds/stone.png"),
                        AdvancementType.GOAL,
                        true, true, false
                )
                .parent(root)
                .addCriterion("maid_event", MaidEventTrigger.create(TriggerType.WIN_JCHESS));

        builder.save(saver, id("favorability/win_jchess"), existingFileHelper);
    }

    @Shadow
    private static ResourceLocation id(String id) {
        throw new AssertionError();
    }
}