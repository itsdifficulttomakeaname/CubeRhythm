package org.cubeRhythm.game;

import lombok.Data;

@Data
public class PlayerSettings {
    private double speed = 1.0;
    private int offset = 0;
    private boolean hitSound = true;
    private boolean autoPlay = false;
    private boolean autoFlickRotation = false;  // 自动演奏时是否自动转向Flick
    private boolean showBeatLines = true;
    private int difficulty = 2;  // 判定难度：1=简单(2x), 2=普通(1.5x), 3=困难(1x)

    public PlayerSettings() {
    }

    public PlayerSettings(double speed, int offset) {
        this.speed = speed;
        this.offset = offset;
    }

    public PlayerSettings(double speed, int offset, int difficulty) {
        this.speed = speed;
        this.offset = offset;
        this.difficulty = difficulty;
    }

    /**
     * 根据难度获取碰撞箱缩放倍数
     */
    public float getHitboxScale() {
        return switch (difficulty) {
            case 1 -> 2.0f;    // 简单：2倍大小
            case 3 -> 1.0f;    // 困难：1倍大小
            default -> 1.5f;   // 普通：1.5倍大小
        };
    }

    /**
     * 获取难度名称
     */
    public String getDifficultyName() {
        return switch (difficulty) {
            case 1 -> "§a简单";
            case 3 -> "§c困难";
            default -> "§e普通";
        };
    }
}
