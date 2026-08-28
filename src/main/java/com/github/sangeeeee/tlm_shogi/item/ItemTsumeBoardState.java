package com.github.sangeeeee.tlm_shogi.item;

import com.github.sangeeeee.tlm_shogi.init.InitDataComponents;
import com.github.sangeeeee.tlm_shogi.inventory.tooltip.TsumeBoardStateTooltip;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A Touhou Little Maid board-state item whose payload is a tsume-shogi SFEN. */
public final class ItemTsumeBoardState extends ItemBoardState {
    public static final int DEFAULT_MAXIMUM_PLY = 3;
    private static final String DESCRIPTION_PREFIX = "board_state.tlm_shogi.tsume.mate";
    private static final String UNKNOWN_AUTHOR = "board_state.tlm_shogi.tsume.author.unknown";
    private static final Pattern BUILTIN_DESCRIPTION = Pattern.compile(
            Pattern.quote(DESCRIPTION_PREFIX) + "(\\d+)(?:_\\d+)?");
    private static final Pattern LEGACY_SOURCE_AUTHOR = Pattern.compile("mate\\d+\\.sfen");

    public static int getMaximumPly(ItemStack stack) {
        return stack.getOrDefault(InitDataComponents.TSUME_MAXIMUM_PLY, DEFAULT_MAXIMUM_PLY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag tooltipFlag) {
        String[] state = ItemBoardState.getState(stack);
        if (state == null) {
            tooltip.add(Component.translatable("tooltips.touhou_little_maid.board_state.empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        String descriptionKey = normalizeDescriptionKey(state[1], getMaximumPly(stack));
        tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));

        Component author = authorComponent(state[2]);
        tooltip.add(Component.translatable("tooltips.touhou_little_maid.board_state.author", author)
                .withStyle(ChatFormatting.GRAY));

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("board_state.touhou_little_maid.show_picture")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /** Keeps board-state items generated before the generic titles were introduced readable. */
    private static String normalizeDescriptionKey(String storedKey, int maximumPly) {
        if (StringUtils.isBlank(storedKey)) {
            return DESCRIPTION_PREFIX + maximumPly;
        }
        Matcher matcher = BUILTIN_DESCRIPTION.matcher(storedKey);
        if (matcher.matches()) {
            return DESCRIPTION_PREFIX + matcher.group(1);
        }
        return storedKey;
    }

    private static Component authorComponent(String storedAuthor) {
        if (StringUtils.isBlank(storedAuthor)
                || UNKNOWN_AUTHOR.equals(storedAuthor)
                || LEGACY_SOURCE_AUTHOR.matcher(storedAuthor).matches()) {
            return Component.translatable(UNKNOWN_AUTHOR);
        }
        if (storedAuthor.startsWith("board_state.")) {
            return Component.translatable(storedAuthor);
        }
        return Component.literal(storedAuthor);
    }

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
