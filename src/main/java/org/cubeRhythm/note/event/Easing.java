package org.cubeRhythm.note.event;

/**
 * 缓动函数求值器
 * 替代旧的 MovementCurve，提供统一的缓动计算入口
 *
 * 所有函数接受 t ∈ [0, 1]，返回 [0, 1]（backOut 可能略超 1）
 */
public final class Easing {

    private Easing() {}

    // ── 正弦查找表（256 点），避免每 tick 调 Math.sin ──
    private static final int LUT_SIZE = 256;
    private static final double[] SINE_LUT = new double[LUT_SIZE + 1];

    static {
        for (int i = 0; i <= LUT_SIZE; i++) {
            SINE_LUT[i] = Math.sin(i * Math.PI / 2.0 / LUT_SIZE);
        }
    }

    /**
     * 统一求值入口
     * @param name 缓动名（如 "sineOut", "cubicIn"）
     * @param t 进度 [0, 1]
     * @return 缓动后的值 [0, 1]（部分缓动可能略超范围）
     */
    public static double eval(String name, double t) {
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;

        if (name == null || name.isEmpty()) return t; // 默认 linear

        return switch (name) {
            case "linear" -> t;
            case "hold" -> 0.0; // 保持上一帧值，不插值

            // 二次方
            case "quadIn" -> t * t;
            case "quadOut" -> 1.0 - (1.0 - t) * (1.0 - t);
            case "quadInOut" -> t < 0.5
                    ? 2.0 * t * t
                    : 1.0 - 2.0 * (1.0 - t) * (1.0 - t);

            // 立方
            case "cubicIn" -> t * t * t;
            case "cubicOut" -> 1.0 - Math.pow(1.0 - t, 3);
            case "cubicInOut" -> t < 0.5
                    ? 4.0 * t * t * t
                    : 1.0 - Math.pow(-2.0 * t + 2.0, 3) / 2.0;

            // 正弦（使用 LUT）
            case "sineIn" -> 1.0 - sineLut(1.0 - t);
            case "sineOut" -> sineLut(t);
            case "sineInOut" -> t < 0.5
                    ? (1.0 - sineLut(1.0 - 2.0 * t)) / 2.0
                    : (1.0 + sineLut(2.0 * t - 1.0)) / 2.0;

            // 指数（用位运算近似 2^x）
            case "expoIn" -> expApprox(10.0 * t - 10.0);
            case "expoOut" -> 1.0 - expApprox(-10.0 * t);
            case "expoInOut" -> t < 0.5
                    ? expApprox(20.0 * t - 10.0) / 2.0
                    : (2.0 - expApprox(-20.0 * t + 10.0)) / 2.0;

            // 回弹（带轻微 overshoot）
            case "backOut" -> {
                double c1 = 1.70158;
                double c3 = c1 + 1.0;
                double tm = t - 1.0;
                yield 1.0 + c3 * tm * tm * tm + c1 * tm * tm;
            }

            default -> t; // 未知缓动名回退到 linear
        };
    }

    /**
     * 正弦 LUT 查表 + 线性插值
     * 输入 t ∈ [0, 1]，输出 sin(t * π/2)
     */
    private static double sineLut(double t) {
        double idx = t * LUT_SIZE;
        int i = (int) idx;
        if (i >= LUT_SIZE) return 1.0;
        double frac = idx - i;
        return SINE_LUT[i] + frac * (SINE_LUT[i + 1] - SINE_LUT[i]);
    }

    /**
     * 2^x 近似（用于 expo 缓动）
     * 精度足够用于视觉动画
     */
    private static double expApprox(double x) {
        // 对于动画范围内的 x（约 -10 到 0），直接用 Math.pow 即可
        // JIT 会内联优化；如果 profiling 显示瓶颈再换 bit hack
        return Math.pow(2.0, x);
    }
}
