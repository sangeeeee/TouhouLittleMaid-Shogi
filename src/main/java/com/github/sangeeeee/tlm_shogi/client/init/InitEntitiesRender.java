package com.github.sangeeeee.tlm_shogi.client.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import com.github.sangeeeee.tlm_shogi.client.renderer.tileentity.TileEntityJChessRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TouhouLittleMaidShogi.MOD_ID, value = Dist.CLIENT)
public final class InitEntitiesRender {
    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        BlockEntityRenderers.register(TileEntityJChess.TYPE, TileEntityJChessRenderer::new);
    }

}
