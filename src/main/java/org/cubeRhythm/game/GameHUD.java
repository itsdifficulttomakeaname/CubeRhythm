package org.cubeRhythm.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.cubeRhythm.judgment.ScoreManager;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game HUD system that displays game information on all 4 faces
 * Shows: combo, score, song name, and difficulty
 */
public class GameHUD {
    private final Player player;
    private final Chart chart;
    private final ScoreManager scoreManager;
    private final PlayerSettings settings;

    // Center coordinates for coordinate transformation
    private final double centerX;
    private final double centerY;
    private final double centerZ;

    private final List<TextDisplay> hudEntities = new ArrayList<>();

    // HUD positions (relative to judgment line)
    private static final double HUD_DISTANCE = 4.5;  // Distance from center
    private static final double TOP_Y = 4.0;         // Top elements (after adjustment)
    private static final double BOTTOM_Y = -2.0;     // Bottom elements (after adjustment)

    public GameHUD(Player player, Chart chart, ScoreManager scoreManager, PlayerSettings settings) {
        this.player = player;
        this.chart = chart;
        this.scoreManager = scoreManager;
        this.settings = settings;

        // Store center coordinates
        Location center = player.getLocation();
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
    }

    /**
     * Initialize HUD displays on all 4 faces
     */
    public void initialize() {
        for (Face face : Face.values()) {
            createFaceHUD(face);
        }
    }

    /**
     * Create HUD elements for a specific face
     */
    private void createFaceHUD(Face face) {
        // Autoplay indicator (top center) - only if autoplay is enabled
        if (settings.isAutoPlay()) {
            TextDisplay autoPlayDisplay = createHUDText(face, 0, TOP_Y + 1.5, "§a自动播放");
            hudEntities.add(autoPlayDisplay);
        } else {
            // Add null placeholder to maintain index consistency
            hudEntities.add(null);
        }

        // Adjusted positions (for front face: X+ is left, X- is right)
        // Score (left side): moved left-up by 1 block
        TextDisplay scoreDisplay = createHUDText(face, 2.5, TOP_Y, "");
        hudEntities.add(scoreDisplay);

        // Combo (right side): moved right-up by 1 block
        TextDisplay comboDisplay = createHUDText(face, -2.5, TOP_Y, "");
        hudEntities.add(comboDisplay);

        // Difficulty (left side): moved left-down by 1 block (after moving up 2)
        String difficultyText = chart.getMetadata().getDifficulty().getColor() +
                chart.getMetadata().getDifficulty().getName() + " " +
                chart.getMetadata().getDifficulty().getLevel();
        TextDisplay difficultyDisplay = createHUDText(face, 2.5, BOTTOM_Y, difficultyText);
        hudEntities.add(difficultyDisplay);

        // Song name (right side): moved right-down by 1 block (after moving up 2)
        TextDisplay songDisplay = createHUDText(face, -2.5, BOTTOM_Y, chart.getMetadata().getTitle());
        hudEntities.add(songDisplay);
    }

    /**
     * Create a TextDisplay at specified position on a face
     */
    private TextDisplay createHUDText(Face face, double x, double y, String initialText) {
        Location loc = CoordinateSystem.transformCoordinates(
            player.getWorld(),
            face,
            x, y, HUD_DISTANCE,
            centerX, centerY, centerZ
        );

        TextDisplay textDisplay = DisplayEntityFactory.createTextDisplay(
            player.getWorld(),
            loc,
            initialText,
            0
        );

        // Set text properties
        textDisplay.setBillboard(TextDisplay.Billboard.FIXED);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

        // Set transparent background (alpha = 0)
        textDisplay.setBackgroundColor(Color.fromARGB(0x00000000)); // ARGB format: 0x00 = fully transparent

        // Set transformation to face the player (perpendicular to judgment face)
        // The transformation rotation is relative to the entity's base orientation
        // W and S faces need 180° rotation, A and D faces need 0° rotation
        Transformation transformation = textDisplay.getTransformation();

        float yaw;
        if (face == Face.W || face == Face.S) {
            // W and S faces: rotate 180° to face inward
            yaw = 180f;
        } else {
            // A and D faces: no additional rotation (0°)
            yaw = 0f;
        }

        transformation.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(yaw), 0, 1, 0));
        transformation.getScale().set(new Vector3f(1.125f, 1.125f, 1.125f)); // 1.5x larger font (0.75 * 1.5)
        textDisplay.setTransformation(transformation);

        return textDisplay;
    }

    /**
     * Update HUD displays with current game state
     */
    public void update() {
        int index = 0;
        for (Face face : Face.values()) {
            // Skip autoplay indicator (index 0) - it's static
            index++;

            // Update score (top left, index 1)
            TextDisplay scoreDisplay = hudEntities.get(index++);
            Component scoreText = Component.text("Score\n", NamedTextColor.GRAY)
                .append(Component.text(String.format("%,d", scoreManager.getScore()), NamedTextColor.WHITE));
            scoreDisplay.text(scoreText);

            // Update combo (top right, index 2)
            // Color based on performance: Gold if All Perfect, Blue if Full Combo, White otherwise
            TextDisplay comboDisplay = hudEntities.get(index++);
            TextColor comboColor;
            if (scoreManager.isPerfect()) {
                // All Perfect: Gold color
                comboColor = NamedTextColor.GOLD;
            } else if (scoreManager.getMissCount() == 0) {
                // Full Combo (no Miss but has Just): Blue color
                comboColor = TextColor.color(0x55FFFF); // Aqua/Cyan
            } else {
                // Has Miss: White color
                comboColor = NamedTextColor.WHITE;
            }
            Component comboText = Component.text("Combo\n", NamedTextColor.GRAY)
                .append(Component.text(scoreManager.getCombo(), comboColor));
            comboDisplay.text(comboText);

            // Difficulty and song name are static, skip updating (index 3, 4)
            index += 2;
        }
    }

    /**
     * Clean up all HUD entities
     */
    public void cleanup() {
        for (TextDisplay entity : hudEntities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        hudEntities.clear();
    }
}