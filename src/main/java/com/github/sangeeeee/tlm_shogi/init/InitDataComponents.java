package com.github.sangeeeee.tlm_shogi.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent item metadata specific to tsume-shogi puzzles. */
public final class InitDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(TouhouLittleMaidShogi.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TSUME_MAXIMUM_PLY =
            DATA_COMPONENTS.register("tsume_maximum_ply", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(1, 255))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private InitDataComponents() {
    }
}
