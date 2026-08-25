package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the platform capability reported by each connected client.
 *
 * <p>The engine runs on the player's client, not on the logical server. A
 * dedicated server's operating system therefore cannot be used for this
 * decision.</p>
 */
public final class PlayerPlatformSupport {
    private static final Set<UUID> SUPPORTED_PLAYERS = ConcurrentHashMap.newKeySet();

    private PlayerPlatformSupport() {
    }

    public static void update(ServerPlayer player, boolean supported) {
        if (supported) {
            SUPPORTED_PLAYERS.add(player.getUUID());
        } else {
            SUPPORTED_PLAYERS.remove(player.getUUID());
        }
    }

    public static boolean isSupported(Player player) {
        return SUPPORTED_PLAYERS.contains(player.getUUID());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SUPPORTED_PLAYERS.remove(player.getUUID());
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SUPPORTED_PLAYERS.remove(player.getUUID());
        }
    }
}
