package com.github.sangeeeee.tlm_shogi.item;

import com.github.sangeeeee.tlm_shogi.init.InitDataComponents;
import com.github.sangeeeee.tlm_shogi.inventory.tooltip.TsumeBoardStateTooltip;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** A Touhou Little Maid board-state item whose payload is a tsume-shogi SFEN. */
public final class ItemTsumeBoardState extends ItemBoardState {
    public static final int DEFAULT_MAXIMUM_PLY = 3;
    public static final String DESCRIPTION = "board_state.tlm_shogi.tsume";
    public static final String MASTERPIECE_DESCRIPTION = DESCRIPTION + ".masterpiece";
    private static final String UNKNOWN_AUTHOR = "board_state.tlm_shogi.tsume.author.unknown";
    private static final Pattern LEGACY_DESCRIPTION = Pattern.compile(
            Pattern.quote(DESCRIPTION) + "\\.mate\\d+(?:_\\d+)?");
    private static final Pattern LEGACY_MASTERPIECE_DESCRIPTION = Pattern.compile(
            Pattern.quote(MASTERPIECE_DESCRIPTION) + "\\.mate\\d+");
    private static final Pattern LEGACY_SOURCE_AUTHOR = Pattern.compile("mate\\d+\\.sfen");

    public static int getMaximumPly(ItemStack stack) {
        return stack.getOrDefault(InitDataComponents.TSUME_MAXIMUM_PLY, DEFAULT_MAXIMUM_PLY);
    }

    public static boolean isMasterpiece(ItemStack stack) {
        return stack.getOrDefault(InitDataComponents.TSUME_MASTERPIECE, false);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return isMasterpiece(stack) ? name.copy().withStyle(ChatFormatting.LIGHT_PURPLE) : name;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isMasterpiece(stack) || super.isFoil(stack);
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

        tooltip.add(descriptionComponent(stack, state[1]).withStyle(ChatFormatting.GRAY));

        Component author = authorComponent(state[2]);
        tooltip.add(Component.translatable("tooltips.touhou_little_maid.board_state.author", author)
                .withStyle(ChatFormatting.GRAY));

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("board_state.touhou_little_maid.show_picture")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /** Keeps board-state items generated before the parameterized titles were introduced readable. */
    private static MutableComponent descriptionComponent(ItemStack stack, String storedKey) {
        String descriptionKey = normalizeDescriptionKey(storedKey, isMasterpiece(stack));
        if (DESCRIPTION.equals(descriptionKey) || MASTERPIECE_DESCRIPTION.equals(descriptionKey)) {
            return Component.translatable(descriptionKey, getMaximumPly(stack));
        }
        return Component.translatable(descriptionKey);
    }

    private static String normalizeDescriptionKey(String storedKey, boolean masterpiece) {
        if (StringUtils.isBlank(storedKey)) {
            return masterpiece ? MASTERPIECE_DESCRIPTION : DESCRIPTION;
        }
        if (LEGACY_MASTERPIECE_DESCRIPTION.matcher(storedKey).matches()) {
            return MASTERPIECE_DESCRIPTION;
        }
        if (LEGACY_DESCRIPTION.matcher(storedKey).matches()) {
            return DESCRIPTION;
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
