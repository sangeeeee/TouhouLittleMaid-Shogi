package com.github.sangeeeee.tlm_shogi.tsume;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Per-player persistent set of solved tsume positions, shared by every maid on a server. */
@EventBusSubscriber(modid = TouhouLittleMaidShogi.MOD_ID)
public final class TsumePlayerProgress {
    private static final String SOLVED_KEY = TouhouLittleMaidShogi.MOD_ID + ":solved_tsume";

    private TsumePlayerProgress() {
    }

    /** Returns true only for the first completion of this puzzle by this player. */
    public static boolean markSolved(ServerPlayer player, String puzzleId) {
        ListTag solved = player.getPersistentData().getList(SOLVED_KEY, Tag.TAG_STRING);
        for (Tag value : solved) {
            if (value instanceof StringTag string && string.getAsString().equals(puzzleId)) {
                return false;
            }
        }
        solved.add(StringTag.valueOf(puzzleId));
        player.getPersistentData().put(SOLVED_KEY, solved);
        return true;
    }

    /** Player entities are recreated on death; retain the server-side puzzle history. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Tag solved = event.getOriginal().getPersistentData().get(SOLVED_KEY);
        if (solved != null) {
            event.getEntity().getPersistentData().put(SOLVED_KEY, solved.copy());
        }
    }
}
