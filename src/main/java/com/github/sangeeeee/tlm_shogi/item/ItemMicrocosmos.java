package com.github.sangeeeee.tlm_shogi.item;

import com.github.sangeeeee.tlm_shogi.inventory.tooltip.TsumeBoardStateTooltip;
import com.github.sangeeeee.tlm_shogi.tsume.MicrocosmosRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

/** Fixed board-state item for Hashimoto Koji's 1,525-ply Microcosmos. */
public final class ItemMicrocosmos extends ItemTsumeBoardState {
    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.DARK_PURPLE);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable(MicrocosmosRecord.DESCRIPTION_KEY)
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltips.touhou_little_maid.board_state.author",
                        Component.literal(MicrocosmosRecord.AUTHOR))
                .withStyle(ChatFormatting.GRAY));
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("board_state.touhou_little_maid.show_picture")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!Screen.hasShiftDown()) {
            return Optional.empty();
        }
        return Optional.of(new TsumeBoardStateTooltip(MicrocosmosRecord.INITIAL_SFEN));
    }
}
