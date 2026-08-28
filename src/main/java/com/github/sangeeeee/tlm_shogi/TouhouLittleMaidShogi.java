package com.github.sangeeeee.tlm_shogi;

import com.github.sangeeeee.tlm_shogi.init.*;
import com.github.sangeeeee.tlm_shogi.network.NetworkHandler;
import net.neoforged.fml.loading.FMLEnvironment;
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
    public static boolean DEBUG = !FMLEnvironment.production;


    public TouhouLittleMaidShogi(IEventBus modEventBus, ModContainer modContainer) {
        initRegister(modEventBus);
//        registerConfiguration(modContainer);
//        CommonDefaultPack.initCommonDefaultPack();
//        AquacultureCompat.init(modEventBus);



    }

    private static void initRegister(IEventBus eventBus) {
        InitBlocks.BLOCKS.register(eventBus);
        InitBlocks.TILE_ENTITIES.register(eventBus);
        InitItems.ITEMS.register(eventBus);

        eventBus.addListener(NetworkHandler::registerPacket);
    }

}
