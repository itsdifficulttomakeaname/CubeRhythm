package org.cubeRhythm.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.cubeRhythm.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Prevents players from changing position during gameplay
 * Players can still rotate (change yaw/pitch) but cannot move (x, y, z)
 */
public class MovementRestriction implements Listener {

    // Cooldown map to prevent message spam (player UUID -> last message time)
    private final Map<UUID, Long> messageCooldown = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 2000; // 2 seconds

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player has an active game session
        GameSession session = Main.instance.getCurrentSession();
        if (session == null) {
            return;
        }

        // Only restrict if this is the player in the session
        if (!session.getPlayer().equals(player)) {
            return;
        }

        // Only restrict during PLAYING state
        if (session.getState() != GameState.PLAYING) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if position changed (x, y, z)
        boolean positionChanged = from.getX() != to.getX() ||
                                  from.getY() != to.getY() ||
                                  from.getZ() != to.getZ();

        if (positionChanged) {
            // Cancel position change but preserve rotation
            Location corrected = from.clone();
            corrected.setYaw(to.getYaw());
            corrected.setPitch(to.getPitch());
            event.setTo(corrected);

            // Send warning message with cooldown
            sendWarningMessage(player);
        }
    }

    /**
     * Send warning message to player with cooldown to prevent spam
     */
    private void sendWarningMessage(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Long lastMessageTime = messageCooldown.get(playerId);
        if (lastMessageTime == null || currentTime - lastMessageTime >= MESSAGE_COOLDOWN_MS) {
            Component message = Component.text("游戏中不能移动位置！只能转动视角。", NamedTextColor.RED);
            player.sendActionBar(message);
            messageCooldown.put(playerId, currentTime);
        }
    }

    /**
     * Clean up cooldown data for a player
     */
    public void clearCooldown(UUID playerId) {
        messageCooldown.remove(playerId);
    }
}
