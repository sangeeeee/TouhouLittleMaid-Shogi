package com.github.sangeeeee.tlm_shogi.item;

import com.github.sangeeeee.tlm_shogi.inventory.tooltip.TsumeBoardStateTooltip;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/** A Touhou Little Maid board-state item whose payload is a tsume-shogi SFEN. */
public final class ItemTsumeBoardState extends ItemBoardState {
    @Override
    @OnlyIn(Dist.CLIENT)
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!Screen.hasShiftDown()) {
            return Optional.empty();
        }
        String[] state = ItemBoardState.getState(stack);
        if (state == null || StringUtils.isBlank(state[0])) {
            return Optional.empty();
        }
        return Optional.of(new TsumeBoardStateTooltip(state[0]));
    }
}
