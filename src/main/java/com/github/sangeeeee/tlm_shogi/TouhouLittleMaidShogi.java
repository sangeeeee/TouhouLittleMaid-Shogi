package com.github.sangeeeee.tlm_shogi;

import com.github.sangeeeee.tlm_shogi.init.InitBlocks;
import com.github.sangeeeee.tlm_shogi.init.InitDataComponents;
import com.github.sangeeeee.tlm_shogi.init.InitItems;
import com.github.sangeeeee.tlm_shogi.init.InitLootModifiers;
import com.github.sangeeeee.tlm_shogi.network.NetworkHandler;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TouhouLittleMaidShogi.MOD_ID)
public class TouhouLittleMaidShogi {
    public static final String MOD_ID = "tlm_shogi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TouhouLittleMaidShogi(IEventBus modEventBus, ModContainer modContainer) {
        initRegister(modEventBus);
    }

    private static void initRegister(IEventBus eventBus) {
        InitBlocks.BLOCKS.register(eventBus);
        InitBlocks.TILE_ENTITIES.register(eventBus);
        InitDataComponents.DATA_COMPONENTS.register(eventBus);
        InitItems.ITEMS.register(eventBus);
        InitLootModifiers.LOOT_FUNCTION_TYPES.register(eventBus);

        eventBus.addListener(NetworkHandler::registerPacket);
    }

}
