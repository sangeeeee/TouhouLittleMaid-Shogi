package com.github.sangeeeee.tlm_shogi.mixin;

import com.github.sangeeeee.tlm_shogi.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.entity.poi.MaidPoiManager;

import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

import com.google.common.collect.Sets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MaidPoiManager.class)
public class MaidPoiManagerMixin {

    @ModifyReturnValue(method = "getJoyBlock", at = @At("RETURN"))
    private static PoiType modifyJoyBlock(PoiType original) {
        Set<BlockState> originalStates = original.matchingStates();

        Set<BlockState> combined = Sets.newHashSet(originalStates);
        combined.addAll(InitBlocks.JCHESS.get().getStateDefinition().getPossibleStates());

        return new PoiType(ImmutableSet.copyOf(combined), original.maxTickets(), original.validRange());
    }
}