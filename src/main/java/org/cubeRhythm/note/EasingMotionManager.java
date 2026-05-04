package org.cubeRhythm.note;

import org.cubeRhythm.entity.EntityManager;

import java.util.*;

/**
 * 管理 easing_motion action 的状态与每 tick 更新。
 *
 * x/y 缓动：speed(t) 即每 tick 的位移增量，直接叠加到 xOffset/yOffset。
 * 飞行方向缓动（expo/log）：通过积分预计算 appearBefore，运行时由 NoteSpawner/NoteRenderer 使用。
 */
public class EasingMotionManager {

    // 每个活跃的 easing_motion 段
    private record ActiveSegment(
        List<NoteEntity> targets,
        List<Map<String, Object>> xFuncs,
        List<Map<String, Object>> yFuncs,
        List<Map<String, Object>> vFuncs,
        int keepTicks,
        int startTick
    ) {}

    private final EntityManager entityManager;
    private final List<ActiveSegment> activeSegments = new ArrayList<>();
    private int globalTick = 0;

    public EasingMotionManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 注册一个 easing_motion action，立即开始执行。
     * @param action  easing_motion 的 action map
     * @param taggedEntities 已按 tag 筛选好的目标音符
     */
    public void register(Map<String, Object> action, List<NoteEntity> taggedEntities) {
        Object segsObj = action.get("segments");
        if (!(segsObj instanceof List<?> segsList) || taggedEntities.isEmpty()) return;

        int segStartTick = globalTick;
        for (Object segObj : segsList) {
            if (!(segObj instanceof Map<?, ?> rawSeg)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> seg = (Map<String, Object>) rawSeg;

            int keep = ((Number) seg.getOrDefault("keep", 20)).intValue();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> xFuncs = seg.containsKey("x")
                ? (List<Map<String, Object>>) seg.get("x") : List.of();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> yFuncs = seg.containsKey("y")
                ? (List<Map<String, Object>>) seg.get("y") : List.of();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> vFuncs = seg.containsKey("v")
                ? (List<Map<String, Object>>) seg.get("v") : List.of();

            activeSegments.add(new ActiveSegment(
                new ArrayList<>(taggedEntities), xFuncs, yFuncs, vFuncs, keep, segStartTick
            ));
            segStartTick += keep;
        }
    }

    /** 每 tick 调用，更新所有活跃段的音符偏移 */
    public void tick() {
        globalTick++;
        activeSegments.removeIf(seg -> {
            int t = globalTick - seg.startTick();
            if (t < 0) return false;
            if (t >= seg.keepTicks()) {
                // 段结束，清除 v 缓动
                if (!seg.vFuncs().isEmpty()) {
                    for (NoteEntity entity : seg.targets()) {
                        entity.setEasingType(null);
                        entity.setEasingLambda(null);
                        entity.setEasingStartTime(null);
                    }
                }
                return true;
            }

            double dx = evalFuncs(seg.xFuncs(), t);
            double dy = evalFuncs(seg.yFuncs(), t);

            for (NoteEntity entity : seg.targets()) {
                if (!entity.isHit()) {
                    entity.setXOffset(entity.getXOffset() + dx);
                    entity.setYOffset(entity.getYOffset() + dy);

                    // 首次激活 v 缓动时记录开始状态
                    if (!seg.vFuncs().isEmpty() && t == 0) {
                        Map<String, Object> vFunc = seg.vFuncs().get(0);
                        String name = (String) vFunc.getOrDefault("name", "1");
                        double lambda = ((Number) vFunc.getOrDefault("lambda", 1)).doubleValue();
                        entity.setEasingType(name);
                        entity.setEasingLambda(lambda);
                        entity.setEasingStartTime((double) globalTick);
                    }
                }
            }
            return false;
        });
    }

    /**
     * 对一组缓动函数求和，得到该 tick 的位移增量。
     * t 单位为 tick（从段开始计）。
     */
    private double evalFuncs(List<Map<String, Object>> funcs, int t) {
        double sum = 0;
        for (Map<String, Object> f : funcs) {
            String name = (String) f.getOrDefault("name", "0");
            sum += switch (name) {
                case "0" -> 0;
                case "1" -> 1;
                case "2" -> t * t;
                case "3" -> t * t * t;
                case "4" -> t * t * t * t;
                case "5" -> t * t * t * t * t;
                case "sin" -> {
                    double freq = ((Number) f.getOrDefault("freq", 1)).doubleValue();
                    double ampl = ((Number) f.getOrDefault("ampl", 1)).doubleValue();
                    yield ampl * Math.sin(2 * Math.PI * freq * t / 20.0);
                }
                case "expo" -> {
                    double lambda = ((Number) f.getOrDefault("lambda", 1)).doubleValue();
                    // speed(t) = 2^(lambda*t)，此处直接作为位移增量
                    yield Math.pow(2, lambda * t);
                }
                case "log" -> {
                    double lambda = ((Number) f.getOrDefault("lambda", 1)).doubleValue();
                    // speed(t) = log2(lambda*t + 1)
                    yield Math.log(lambda * t + 1) / Math.log(2);
                }
                default -> 0;
            };
        }
        return sum;
    }

    // ── 静态工具：积分反推飞行方向缓动的 appearBefore ──────────────────────

    /**
     * 给定飞行方向缓动类型和 lambda，计算音符从生成到到达判定面所需的 tick 数。
     * 即求解 ∫₀ᵀ speed(t) dt = d 中的 T。
     * <p>
     * expo: ∫₀ᵀ 2^(λt) dt = (2^(λT) - 1) / (λ·ln2) = d
     * log:  ∫₀ᵀ log₂(λt+1) dt = [(λT+1)·log₂(λT+1) - (λT+1)/ln2 + 1/ln2] / λ = d
     *
     * @param type   "expo" 或 "log"
     * @param lambda λ 参数
     * @param d      生成距离（默认 50 - 4 = 46 blocks）
     * @return 所需 tick 数（秒 = ticks/20）
     */
    public static double solveArrivalTicks(String type, double lambda, double d) {
        return switch (type) {
            case "expo" -> {
                // 2^(λT) = d·λ·ln2 + 1  →  T = log2(d·λ·ln2 + 1) / λ
                double val = d * lambda * Math.log(2) + 1;
                if (val <= 0) yield d; // fallback
                yield Math.log(val) / Math.log(2) / lambda;
            }
            case "log" -> {
                // Newton 法求解 integralLog(lambda, T) = d
                // f(T)  = [(λT+1)ln(λT+1) - λT] / (λ·ln2) - d
                // f'(T) = log₂(λT+1)  （被积函数）
                double T = d; // 初始猜测
                for (int i = 0; i < 20; i++) {
                    double fT = integralLog(lambda, T) - d;
                    double dfT = Math.log(lambda * T + 1) / Math.log(2);
                    if (Math.abs(dfT) < 1e-15) break;
                    double step = fT / dfT;
                    T -= step;
                    if (T < 0) T = 1e-9;
                    if (Math.abs(step) < 1e-10) break;
                }
                yield T;
            }
            default -> d; // 线性 fallback
        };
    }

    /** ∫₀ᵀ speed(t) dt，用于运行时计算已飞行距离 */
    public static double integralSpeed(String type, double lambda, double T) {
        return switch (type) {
            case "expo" -> (Math.pow(2, lambda * T) - 1) / (lambda * Math.log(2));
            case "log"  -> integralLog(lambda, T);
            default     -> T; // 线性
        };
    }

    /** ∫₀ᵀ log₂(λt+1) dt */
    private static double integralLog(double lambda, double T) {
        if (lambda == 0) return 0;
        double u = lambda * T + 1;
        return (u * Math.log(u) / Math.log(2) - u / Math.log(2) + 1.0 / Math.log(2)) / lambda;
    }
}
