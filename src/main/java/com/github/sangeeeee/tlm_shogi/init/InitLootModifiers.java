package com.github.sangeeeee.tlm_shogi.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.loot.RandomTsumeBoardStateFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class InitLootModifiers {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTION_TYPES =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, TouhouLittleMaidShogi.MOD_ID);

    public static final Supplier<LootItemFunctionType<? extends LootItemConditionalFunction>> TSUME_BOARD_STATE_RANDOMLY =
            LOOT_FUNCTION_TYPES.register("tsume_board_state_randomly",
                    () -> new LootItemFunctionType<>(RandomTsumeBoardStateFunction.CODEC));

    private InitLootModifiers() {
    }
}
