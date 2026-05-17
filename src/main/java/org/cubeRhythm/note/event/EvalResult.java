package org.cubeRhythm.note.event;

import lombok.Data;

/**
 * 事件求值结果
 * 一次性包含所有通道的当前值，避免多次求值
 */
@Data
public class EvalResult {
    // 位置偏移
    private double x = 0;
    private double y = 0;
    private double z = 0;

    // 视觉
    private double alpha = 1.0;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double scaleZ = 1.0;
    private double rotate = 0;

    // 颜色（-1 表示使用默认）
    private double colorR = -1;
    private double colorG = -1;
    private double colorB = -1;
    private double colorA = 255;

    // 材质（null 表示使用默认）
    private String material = null;

    /**
     * 是否有颜色覆盖
     */
    public boolean hasColorOverride() {
        return colorR >= 0 && colorG >= 0 && colorB >= 0;
    }
}
