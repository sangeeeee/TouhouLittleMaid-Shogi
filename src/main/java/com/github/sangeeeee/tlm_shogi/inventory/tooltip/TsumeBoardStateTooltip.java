package com.github.sangeeeee.tlm_shogi.inventory.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Client tooltip payload for a tsume-shogi SFEN. */
public record TsumeBoardStateTooltip(String sfen) implements TooltipComponent {
}
