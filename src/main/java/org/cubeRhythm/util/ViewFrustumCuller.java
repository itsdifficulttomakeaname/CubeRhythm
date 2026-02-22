package org.cubeRhythm.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * View frustum culling utility to optimize rendering
 * Only renders notes that are within the player's field of view
 */
public class ViewFrustumCuller {
    private static final double FOV_HORIZONTAL = Math.toRadians(90); // 90 degrees horizontal FOV
    private static final double FOV_VERTICAL = Math.toRadians(70);   // 70 degrees vertical FOV
    private static final double MAX_RENDER_DISTANCE = 60.0;          // Maximum render distance

    /**
     * Check if a location is within the player's view frustum
     */
    public static boolean isInViewFrustum(Player player, Location noteLocation) {
        Location playerLoc = player.getEyeLocation();
        Vector toNote = noteLocation.toVector().subtract(playerLoc.toVector());
        double distance = toNote.length();

        // Distance culling
        if (distance > MAX_RENDER_DISTANCE) {
            return false;
        }

        // Normalize direction vector
        toNote.normalize();

        // Get player's look direction
        Vector lookDir = playerLoc.getDirection().normalize();

        // Calculate angle between look direction and note direction
        double dotProduct = lookDir.dot(toNote);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct)));

        // Check if within horizontal FOV
        // Using a slightly larger FOV to account for note size
        return angle <= (FOV_HORIZONTAL / 2.0) + Math.toRadians(20);
    }

    /**
     * Check if a location is behind the player
     */
    public static boolean isBehindPlayer(Player player, Location noteLocation) {
        Location playerLoc = player.getEyeLocation();
        Vector toNote = noteLocation.toVector().subtract(playerLoc.toVector()).normalize();
        Vector lookDir = playerLoc.getDirection().normalize();

        return lookDir.dot(toNote) < 0;
    }

    /**
     * Get distance from player to location
     */
    public static double getDistance(Player player, Location noteLocation) {
        return player.getEyeLocation().distance(noteLocation);
    }
}