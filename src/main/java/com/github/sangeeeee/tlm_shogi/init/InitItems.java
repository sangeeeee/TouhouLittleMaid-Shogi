package com.github.sangeeeee.tlm_shogi.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class InitItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TouhouLittleMaidShogi.MOD_ID);

    public static DeferredItem<Item> JCHESS = ITEMS.register("jchess", () -> new BlockItem(InitBlocks.JCHESS.get(), new Item.Properties()));

}