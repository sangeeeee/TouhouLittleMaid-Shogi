package com.github.sangeeeee.tlm_shogi.init;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.tileentity.TileEntityJChess;
import com.github.sangeeeee.tlm_shogi.block.BlockJChess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class InitBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TouhouLittleMaidShogi.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TouhouLittleMaidShogi.MOD_ID);

    public static final DeferredBlock<Block> JCHESS = BLOCKS.register("jchess", BlockJChess::new);
    public static final Supplier<BlockEntityType<TileEntityJChess>> JCHESS_TE =
            TILE_ENTITIES.register("jchess", () -> TileEntityJChess.TYPE);

    private InitBlocks() {
    }
}
