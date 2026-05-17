package org.cubeRhythm.note.event;

/**
 * 事件通道枚举
 * 每个通道代表音符的一个可动画属性
 */
public enum Channel {
    // 位置通道
    X,          // 横向偏移 (block)
    Y,          // 纵向偏移 (block)
    Z,          // 飞行方向偏移，正值=远离判定面 (block)

    // 视觉通道
    ALPHA,      // 不透明度 (0-1)
    SCALE_X,    // X轴缩放 (×)
    SCALE_Y,    // Y轴缩放 (×)
    SCALE_Z,    // Z轴缩放 (×)
    ROTATE,     // 绕飞行方向自转 (度)

    // 颜色通道 (仅 glowing=true 时生效)
    COLOR_R,    // 0-255
    COLOR_G,    // 0-255
    COLOR_B,    // 0-255
    COLOR_A,    // 0-255

    // 离散通道
    MATERIAL;   // 方块材质名 (hold 语义，无插值)

    public static final int COUNT = values().length;

    /**
     * 从 JSON 字段名解析通道
     */
    public static Channel fromString(String name) {
        return switch (name.toLowerCase()) {
            case "x" -> X;
            case "y" -> Y;
            case "z" -> Z;
            case "alpha" -> ALPHA;
            case "scale" -> SCALE_X; // "scale" 同时设 X/Y/Z，由解析层处理
            case "scalex", "scale_x" -> SCALE_X;
            case "scaley", "scale_y" -> SCALE_Y;
            case "scalez", "scale_z" -> SCALE_Z;
            case "rotate" -> ROTATE;
            case "colorr", "color_r" -> COLOR_R;
            case "colorg", "color_g" -> COLOR_G;
            case "colorb", "color_b" -> COLOR_B;
            case "colora", "color_a" -> COLOR_A;
            case "material" -> MATERIAL;
            default -> null;
        };
    }

    /**
     * 是否为缩放类通道（叠加时用乘法而非加法）
     */
    public boolean isMultiplicative() {
        return this == SCALE_X || this == SCALE_Y || this == SCALE_Z;
    }

    /**
     * 获取通道的默认值（无事件时的值）
     */
    public double getDefaultValue() {
        return switch (this) {
            case X, Y, Z, ROTATE -> 0.0;
            case ALPHA -> 1.0;
            case SCALE_X, SCALE_Y, SCALE_Z -> 1.0;
            case COLOR_R, COLOR_G, COLOR_B -> -1.0; // -1 表示使用 face 默认色
            case COLOR_A -> 255.0;
            case MATERIAL -> 0.0; // 0 = 使用默认材质
        };
    }
}
