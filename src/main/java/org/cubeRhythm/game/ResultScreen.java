package org.cubeRhythm.game;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.Sound;
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
 * 结算界面系统 - 增强版
 * 显示游戏结束后的详细统计信息，带有流畅的动画效果
 */
public class ResultScreen {
    private final Player player;
    private final Chart chart;
    private final ScoreManager scoreManager;
    private final PlayerSettings settings;
    private final List<TextDisplay> resultPanelEntities = new ArrayList<>();

    private final Location centerLoc;
    private int displayScore = 0;

    public ResultScreen(Player player, Chart chart, ScoreManager scoreManager, PlayerSettings settings) {
        this.player = player;
        this.chart = chart;
        this.scoreManager = scoreManager;
        this.settings = settings;
        this.centerLoc = player.getLocation();
    }

    /**
     * 显示结算界面（带动画）
     */
    public void show() {
        boolean isPerfect = scoreManager.isFullPerfect();
        boolean isFullCombo = scoreManager.getMaxCombo() == chart.getTotalNotes();

        // Phase 1: Wait 30 ticks (1.5s) for pre-result animation
        // (In the Skript version, existing displays float up - we skip this as we don't have existing displays)

        // Phase 2: Show achievement animation if applicable
        PlanetLib.getScheduler().runLater(() -> {
            if (isPerfect) {
                showPerfectPerformance();
            } else if (isFullCombo) {
                showFullCombo();
            }
        }, 35L); // 30 ticks wait + 5 ticks cleanup

        // Phase 3: Show result displays sequentially
        long baseDelay = 35L + (isPerfect || isFullCombo ? 30L : 0L); // Add 30 ticks if achievement shown

        PlanetLib.getScheduler().runLater(this::showRankDisplay, baseDelay);
        PlanetLib.getScheduler().runLater(this::showScoreDisplay, baseDelay + 5L);
        PlanetLib.getScheduler().runLater(this::showStatisticsDisplay, baseDelay + 10L);
        PlanetLib.getScheduler().runLater(this::showSongInfoDisplay, baseDelay + 15L);

        // Play sound
        if (isPerfect) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else if (isFullCombo) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        // Auto-cleanup after 30 seconds
        PlanetLib.getScheduler().runLater(this::cleanupVisualPanel, 600L);
    }

    /**
     * 显示 Full Combo 成就动画
     */
    private void showFullCombo() {
        Location loc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            0.5, 0.5, 10,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        TextDisplay entity = DisplayEntityFactory.createTextDisplay(
            player.getWorld(), loc, "§bFull Combo", 2
        );

        entity.setBillboard(TextDisplay.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);

        // Initial transformation
        Transformation trans = entity.getTransformation();
        trans.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(180), 0, 1, 0));
        trans.getRightRotation().set(new AxisAngle4f((float) Math.toRadians(90), 0, 1, 0));
        trans.getScale().set(new Vector3f(40, 40, 40));
        trans.getTranslation().set(new Vector3f(0, -1.5f, 0));
        entity.setTransformation(trans);
        entity.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        resultPanelEntities.add(entity);

        // Animate over 30 ticks
        animateAchievement(entity, 40.0f, 90.0f, 22.0f);

        // Play sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Remove entity after animation (30 ticks) + 1 second wait (20 ticks)
        PlanetLib.getScheduler().runLater(() -> {
            if (entity != null && entity.isValid()) {
                entity.remove();
                resultPanelEntities.remove(entity);
            }
        }, 50L);
    }

    /**
     * 显示 Perfect Performance 成就动画
     */
    private void showPerfectPerformance() {
        Location loc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            0.5, 0.5, 10,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        TextDisplay entity = DisplayEntityFactory.createTextDisplay(
            player.getWorld(), loc, "§ePerfect\n    Performance!", 2
        );

        entity.setBillboard(TextDisplay.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);

        // Initial transformation
        Transformation trans = entity.getTransformation();
        trans.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(180), 0, 1, 0));
        trans.getRightRotation().set(new AxisAngle4f((float) Math.toRadians(90), 0, 1, 0));
        trans.getScale().set(new Vector3f(40, 40, 40));
        trans.getTranslation().set(new Vector3f(0, -1.5f, 0));
        entity.setTransformation(trans);
        entity.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        resultPanelEntities.add(entity);

        // Animate over 30 ticks
        animateAchievement(entity, 40.0f, 90.0f, 22.0f);

        // Play sound effect
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

        // Remove entity after animation (30 ticks) + 1 second wait (20 ticks)
        PlanetLib.getScheduler().runLater(() -> {
            if (entity != null && entity.isValid()) {
                entity.remove();
                resultPanelEntities.remove(entity);
            }
        }, 50L);
    }

    /**
     * 成就动画：旋转和缩放
     */
    private void animateAchievement(TextDisplay entity, float initialSize, float initialAngle, float initialDel) {
        final float[] size = {initialSize};
        final float[] angle = {initialAngle};
        final float[] del = {initialDel};

        WrappedTask[] taskHolder = new WrappedTask[1];
        final int[] tick = {0};

        taskHolder[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;

            del[0] *= 0.8f;
            size[0] *= 0.7f;
            angle[0] -= del[0];

            Transformation trans = entity.getTransformation();
            trans.getRightRotation().set(new AxisAngle4f((float) Math.toRadians(angle[0]), 0, 1, 0));
            trans.getScale().set(new Vector3f(size[0] + 5, size[0] + 5, size[0] + 5));
            trans.getTranslation().set(new Vector3f(0, -1.5f * (size[0] + 5) * 0.1f, 0));
            entity.setTransformation(trans);

            if (tick[0] >= 30) {
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    /**
     * 显示评级（带滑动动画）
     */
    private void showRankDisplay() {
        String rank = calculateRank();
        String rankColor = getRankColor(rank);

        Location startLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            -3.5, 0.5, 12,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        Location endLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            5.5, 0.5, 12,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        TextDisplay entity = DisplayEntityFactory.createTextDisplay(
            player.getWorld(), startLoc, rankColor + rank, 2
        );

        entity.setBillboard(TextDisplay.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);

        Transformation trans = entity.getTransformation();
        trans.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(180), 0, 1, 0));
        trans.getScale().set(new Vector3f(10, 10, 10));
        trans.getTranslation().set(new Vector3f(-0.2f, -1, 0));
        entity.setTransformation(trans);
        entity.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        resultPanelEntities.add(entity);

        // Animate sliding from left to center over 25 ticks
        animateSlide(entity, startLoc, endLoc, 25);

        // Update rank text during animation
        WrappedTask[] taskHolder = new WrappedTask[1];
        final int[] tick = {0};
        taskHolder[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;
            entity.setText(rankColor + rank);
            if (tick[0] >= 60) {
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    /**
     * 显示分数（带计数动画）
     */
    private void showScoreDisplay() {
        Location startLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            -7, 2, 10,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        Location endLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            -1.5, 2, 10,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        TextDisplay entity = DisplayEntityFactory.createTextDisplay(
            player.getWorld(), startLoc, "§f", 2
        );

        entity.setBillboard(TextDisplay.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);

        Transformation trans = entity.getTransformation();
        trans.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(180), 0, 1, 0));
        trans.getScale().set(new Vector3f(8, 8, 8));
        trans.getTranslation().set(new Vector3f(0, -1.5f, 0));
        entity.setTransformation(trans);
        entity.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        resultPanelEntities.add(entity);

        // Animate sliding
        animateSlide(entity, startLoc, endLoc, 30);

        // Animate score counting over 60 ticks
        final int finalScore = scoreManager.getScore();
        double tempScore2 = finalScore;
        double[] tempScore = {tempScore2 * 0.75};

        WrappedTask[] taskHolder = new WrappedTask[1];
        final int[] tick = {0};
        taskHolder[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;
            tempScore[0] = 0.75 * tempScore[0];
            displayScore = (int) Math.round(tempScore2 - tempScore[0]);

            // Format with leading zeros
            String scoreText = formatScoreWithLeadingZeros(displayScore);
            entity.setText(scoreText);

            if (tick[0] >= 60) {
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    /**
     * 显示统计信息（带淡入动画）
     */
    private void showStatisticsDisplay() {
        Location startLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            -5.5, 2, 10,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        Location endLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            0, 2, 10,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        // Note: holdPerfect count not currently tracked, using 0
        String statsText = String.format(
            "&bExact &f%d\n&eJust &f%d\n&4Miss &c%d\n\n&7输入 [/gui] 返回选择",
            scoreManager.getExactCount(),
            scoreManager.getJustCount(),
            scoreManager.getMissCount()
        ).replace("&", "§");

        TextDisplay entity = DisplayEntityFactory.createTextDisplay(
            player.getWorld(), startLoc, statsText, 2
        );

        entity.setBillboard(TextDisplay.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.LEFT);
        entity.setLineWidth(400);

        Transformation trans = entity.getTransformation();
        trans.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(180), 0, 1, 0));
        trans.getScale().set(new Vector3f(2, 2, 2));
        trans.getTranslation().set(new Vector3f(0, -4.5f, 0));
        entity.setTransformation(trans);
        entity.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        resultPanelEntities.add(entity);

        // Animate sliding
        animateSlide(entity, startLoc, endLoc, 30);

        // Fade in opacity over 30 ticks
        final int[] alpha = {14};
        WrappedTask[] taskHolder = new WrappedTask[1];
        final int[] tick = {0};
        taskHolder[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;
            alpha[0] += 255 / 10;
            if (alpha[0] > 255) alpha[0] = 255;

            // Note: TextDisplay doesn't have direct opacity control in Bukkit API
            // The opacity is controlled through the text color alpha channel
            // For simplicity, we'll keep it fully visible

            if (tick[0] >= 30) {
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    /**
     * 显示歌曲信息（带滑动动画）
     */
    private void showSongInfoDisplay() {
        String songInfo = "§f" + chart.getMetadata().getTitle() + " §f" +
                         chart.getMetadata().getDifficulty().getLevel();
        double length = songInfo.length() * 0.12;

        Location startLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            -4, -2.5, 11,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        Location endLoc = CoordinateSystem.transformCoordinates(
            player.getWorld(), Face.W,
            0, -2.5, 11,
            centerLoc.getX(), centerLoc.getY(), centerLoc.getZ()
        );

        TextDisplay entity = DisplayEntityFactory.createTextDisplay(
            player.getWorld(), startLoc, songInfo, 2
        );

        entity.setBillboard(TextDisplay.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);
        entity.setLineWidth(400);

        Transformation trans = entity.getTransformation();
        trans.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(180), 0, 1, 0));
        trans.getScale().set(new Vector3f(3f, 3f, 3f));
        entity.setTransformation(trans);
        entity.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        resultPanelEntities.add(entity);

        // Animate sliding
        animateSlide(entity, startLoc, endLoc, 30);
    }

    /**
     * 滑动动画（使用圆形缓动）
     */
    private void animateSlide(TextDisplay entity, Location start, Location end, int duration) {
        final int[] tick = {0};
        WrappedTask[] taskHolder = new WrappedTask[1];

        taskHolder[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;
            double progress = (double) tick[0] / duration;

            // Circular easing out
            double easedProgress = Math.sqrt(1 - Math.pow(progress - 1, 2));

            double x = start.getX() + (end.getX() - start.getX()) * easedProgress;
            double y = start.getY() + (end.getY() - start.getY()) * easedProgress;
            double z = start.getZ() + (end.getZ() - start.getZ()) * easedProgress;

            entity.teleport(new Location(entity.getWorld(), x, y, z, start.getYaw(), start.getPitch()));

            if (tick[0] >= duration) {
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    /**
     * 格式化分数，显示前导零
     */
    private String formatScoreWithLeadingZeros(int score) {
        String scoreStr = String.valueOf(score);
        int leadingZeros = 7 - scoreStr.length();

        if (leadingZeros <= 0) {
            return "§f" + scoreStr;
        }

        StringBuilder result = new StringBuilder("§8");
        for (int i = 0; i < leadingZeros; i++) {
            result.append("0");
        }
        result.append("§f").append(scoreStr);
        return result.toString();
    }

    private String getRankColor(String rank) {
        return switch (rank) {
            case "SSS+", "SSS", "SS", "S" -> "§e";
            case "AAA", "AA", "A" -> "§a";
            case "BBB", "BB", "B" -> "§b";
            case "C" -> "§2";
            default -> "§7";
        };
    }

    private String calculateRank() {
        int score = scoreManager.getScore();
        if (scoreManager.isFullPerfect()) return "SSS+";
        if (score >= 990000) return "SSS";
        if (score >= 980000) return "SS";
        if (score >= 960000) return "S";
        if (score >= 950000) return "AAA";
        if (score >= 940000) return "AA";
        if (score >= 930000) return "A";
        if (score >= 920000) return "BBB";
        if (score >= 910000) return "BB";
        if (score >= 900000) return "B";
        if (score >= 850000) return "C";
        return "D";
    }

    /**
     * Clean up visual panel entities
     */
    public void cleanupVisualPanel() {
        for (TextDisplay entity : resultPanelEntities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        resultPanelEntities.clear();
    }
}
