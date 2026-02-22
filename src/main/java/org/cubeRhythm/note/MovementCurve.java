package org.cubeRhythm.note;

/**
 * 音符移动曲线工具类
 * 提供各种速度曲线函数，使音符移动更具表现力
 */
public class MovementCurve {

    /**
     * 曲线类型枚举
     */
    public enum CurveType {
        LINEAR,          // 线性（默认）
        EASE_IN,         // 缓入（加速）
        EASE_OUT,        // 缓出（减速）
        EASE_IN_OUT,     // 缓入缓出
        SINE,            // 正弦波
        EXPONENTIAL,     // 指数
        BOUNCE,          // 弹跳
        ELASTIC          // 弹性
    }

    /**
     * 根据曲线类型计算距离
     * @param baseDistance 基础距离（线性计算的距离）
     * @param curveType 曲线类型
     * @param progress 进度（0.0 到 1.0）
     * @return 应用曲线后的距离
     */
    public static double applyCurve(double baseDistance, CurveType curveType, double progress) {
        return switch (curveType) {
            case LINEAR -> baseDistance;
            case EASE_IN -> applyEaseIn(baseDistance, progress);
            case EASE_OUT -> applyEaseOut(baseDistance, progress);
            case EASE_IN_OUT -> applyEaseInOut(baseDistance, progress);
            case SINE -> applySine(baseDistance, progress);
            case EXPONENTIAL -> applyExponential(baseDistance, progress);
            case BOUNCE -> applyBounce(baseDistance, progress);
            case ELASTIC -> applyElastic(baseDistance, progress);
        };
    }

    /**
     * 缓入（二次方）- 开始慢，结束快
     */
    private static double applyEaseIn(double baseDistance, double progress) {
        double factor = progress * progress;
        return baseDistance * factor;
    }

    /**
     * 缓出（二次方）- 开始快，结束慢
     */
    private static double applyEaseOut(double baseDistance, double progress) {
        double factor = 1 - (1 - progress) * (1 - progress);
        return baseDistance * factor;
    }

    /**
     * 缓入缓出 - 开始和结束都慢，中间快
     */
    private static double applyEaseInOut(double baseDistance, double progress) {
        double factor;
        if (progress < 0.5) {
            factor = 2 * progress * progress;
        } else {
            factor = 1 - Math.pow(-2 * progress + 2, 2) / 2;
        }
        return baseDistance * factor;
    }

    /**
     * 正弦波 - 平滑的波动效果
     */
    private static double applySine(double baseDistance, double progress) {
        double factor = Math.sin(progress * Math.PI / 2);
        return baseDistance * factor;
    }

    /**
     * 指数 - 非常慢的开始，然后急剧加速
     */
    private static double applyExponential(double baseDistance, double progress) {
        double factor = progress == 0 ? 0 : Math.pow(2, 10 * progress - 10);
        return baseDistance * factor;
    }

    /**
     * 弹跳 - 模拟弹跳效果
     */
    private static double applyBounce(double baseDistance, double progress) {
        double factor;
        if (progress < 1 / 2.75) {
            factor = 7.5625 * progress * progress;
        } else if (progress < 2 / 2.75) {
            progress -= 1.5 / 2.75;
            factor = 7.5625 * progress * progress + 0.75;
        } else if (progress < 2.5 / 2.75) {
            progress -= 2.25 / 2.75;
            factor = 7.5625 * progress * progress + 0.9375;
        } else {
            progress -= 2.625 / 2.75;
            factor = 7.5625 * progress * progress + 0.984375;
        }
        return baseDistance * factor;
    }

    /**
     * 弹性 - 模拟弹簧效果
     */
    private static double applyElastic(double baseDistance, double progress) {
        if (progress == 0 || progress == 1) {
            return baseDistance * progress;
        }
        double c4 = (2 * Math.PI) / 3;
        double factor = -Math.pow(2, 10 * progress - 10) * Math.sin((progress * 10 - 10.75) * c4);
        return baseDistance * factor;
    }

    /**
     * 计算音符的实际距离（考虑曲线）
     * @param noteTime 音符时间
     * @param currentTime 当前时间
     * @param speed 速度倍率
     * @param curveType 曲线类型
     * @return 实际距离
     */
    public static double calculateDistance(double noteTime, double currentTime, double speed, CurveType curveType) {
        // 基础线性距离计算
        double baseDistance = speed * 20 * (noteTime - currentTime) + 4;

        // 如果是线性，直接返回
        if (curveType == CurveType.LINEAR) {
            return baseDistance;
        }

        // 计算进度（0.0 = 刚生成，1.0 = 到达判定线）
        double maxDistance = 50.0;  // 最大生成距离
        double minDistance = 4.0;   // 判定线距离
        double progress = 1.0 - ((baseDistance - minDistance) / (maxDistance - minDistance));
        progress = Math.max(0.0, Math.min(1.0, progress));  // 限制在 0-1 范围

        // 应用曲线
        return applyCurve(baseDistance, curveType, progress);
    }
}
