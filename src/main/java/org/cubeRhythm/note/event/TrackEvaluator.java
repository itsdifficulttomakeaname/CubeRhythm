package org.cubeRhythm.note.event;

import org.cubeRhythm.note.NoteEntity;

import java.util.List;

/**
 * 事件轨道求值器
 * 对 NoteEntity 的所有 matchedTracks 进行通道独立求值，输出 EvalResult
 *
 * 优化策略：
 * - 段索引缓存：每个 NoteEntity 维护 channelCursors[]，下次从上次位置线性探测
 * - 二分查找仅在首次或跳跃时使用
 */
public final class TrackEvaluator {

    private TrackEvaluator() {}

    /**
     * 对一个音符的所有事件轨道求值
     * @param entity 音符实体（含 matchedTracks 和 channelCursors）
     * @param currentTime 当前绝对时间（秒）
     * @return 所有通道的当前值
     */
    public static EvalResult evaluate(NoteEntity entity, double currentTime) {
        List<EventTrack> tracks = entity.getMatchedTracks();
        if (tracks == null || tracks.isEmpty()) {
            return null; // 无事件，使用默认行为
        }

        EvalResult result = new EvalResult();
        int[] cursors = entity.getChannelCursors();

        for (Channel ch : Channel.values()) {
            if (ch == Channel.MATERIAL) continue; // 材质单独处理

            double accumulated;
            if (ch.isMultiplicative()) {
                accumulated = 1.0;
            } else {
                accumulated = 0.0;
            }

            boolean hasAny = false;
            int cursorIdx = ch.ordinal();

            for (EventTrack track : tracks) {
                List<Keyframe> kfs = track.getChannel(ch);
                if (kfs == null || kfs.isEmpty()) continue;

                hasAny = true;
                double sampled = sample(kfs, currentTime, cursors, cursorIdx, ch);

                if (ch.isMultiplicative()) {
                    accumulated *= sampled;
                } else {
                    accumulated += sampled;
                }
            }

            if (hasAny) {
                applyToResult(result, ch, accumulated);
            }
        }

        // 材质通道：取最后一个有值的 track 的最近左侧关键帧
        for (int i = tracks.size() - 1; i >= 0; i--) {
            List<Keyframe> kfs = tracks.get(i).getChannel(Channel.MATERIAL);
            if (kfs != null && !kfs.isEmpty()) {
                result.setMaterial(sampleMaterial(kfs, currentTime));
                break;
            }
        }

        return result;
    }

    /**
     * 关键帧采样（带段索引缓存优化）
     * 边界行为：首帧前返回首帧值，末帧后返回通道默认值（加法通道=0，乘法通道=1）
     *
     * @param kfs 关键帧列表（已按时间排序）
     * @param t 当前时间
     * @param cursors 段索引缓存数组
     * @param cursorIdx 当前通道在缓存数组中的下标
     * @param ch 通道（用于获取末帧后的默认值），可为 null（兼容旧调用，null 时末帧后返回末帧值）
     * @return 插值后的值
     */
    public static double sample(List<Keyframe> kfs, double t, int[] cursors, int cursorIdx, Channel ch) {
        int size = kfs.size();
        if (size == 0) return 0;
        if (size == 1) {
            // 单帧：首帧前或等于时返回首帧值，超过时返回默认值
            if (t <= kfs.get(0).getTime()) return kfs.get(0).getValue();
            return (ch != null) ? ch.getDefaultValue() : kfs.get(0).getValue();
        }

        // 边界检查：首帧前返回首帧值
        if (t <= kfs.get(0).getTime()) return kfs.get(0).getValue();
        // 末帧后返回通道默认值（事件结束，不再影响音符）
        if (t >= kfs.get(size - 1).getTime()) {
            return (ch != null) ? ch.getDefaultValue() : kfs.get(size - 1).getValue();
        }

        // 从缓存位置开始线性探测
        int cursor = (cursors != null && cursorIdx < cursors.length) ? cursors[cursorIdx] : 0;
        cursor = Math.max(0, Math.min(cursor, size - 2));

        // 向前探测（时间递增，大多数情况只需 +0 或 +1）
        while (cursor < size - 2 && kfs.get(cursor + 1).getTime() <= t) {
            cursor++;
        }
        // 向后探测（罕见：时间跳回）
        while (cursor > 0 && kfs.get(cursor).getTime() > t) {
            cursor--;
        }

        // 更新缓存
        if (cursors != null && cursorIdx < cursors.length) {
            cursors[cursorIdx] = cursor;
        }

        Keyframe from = kfs.get(cursor);
        Keyframe to = kfs.get(cursor + 1);

        // hold 缓动：不插值，保持上一帧值
        if ("hold".equals(to.getEasing())) {
            return from.getValue();
        }

        // 计算进度并应用缓动
        double duration = to.getTime() - from.getTime();
        if (duration <= 0) return to.getValue();

        double progress = (t - from.getTime()) / duration;
        double eased = Easing.eval(to.getEasing(), progress);

        return from.getValue() + eased * (to.getValue() - from.getValue());
    }

    /**
     * 带缓存但不传通道的采样（兼容旧调用，末帧后返回末帧值）
     */
    public static double sample(List<Keyframe> kfs, double t, int[] cursors, int cursorIdx) {
        return sample(kfs, t, cursors, cursorIdx, null);
    }

    /**
     * 无缓存版本的采样（用于外部简单调用，末帧后返回末帧值）
     */
    public static double sample(List<Keyframe> kfs, double t) {
        return sample(kfs, t, null, 0, null);
    }

    /**
     * 材质通道采样：取最近左侧关键帧的值（离散，无插值）
     * 关键帧的 value 字段存储材质名的 hashCode，实际材质名存在 easing 字段
     */
    private static String sampleMaterial(List<Keyframe> kfs, double t) {
        if (kfs == null || kfs.isEmpty()) return null;

        String lastMaterial = null;
        for (Keyframe kf : kfs) {
            if (kf.getTime() <= t) {
                lastMaterial = kf.getEasing(); // 材质名借用 easing 字段存储
            } else {
                break;
            }
        }
        return lastMaterial;
    }

    /**
     * 将通道值写入 EvalResult
     */
    private static void applyToResult(EvalResult result, Channel ch, double value) {
        switch (ch) {
            case X -> result.setX(value);
            case Y -> result.setY(value);
            case Z -> result.setZ(value);
            case ALPHA -> result.setAlpha(value);
            case SCALE_X -> result.setScaleX(value);
            case SCALE_Y -> result.setScaleY(value);
            case SCALE_Z -> result.setScaleZ(value);
            case ROTATE -> result.setRotate(value);
            case COLOR_R -> result.setColorR(value);
            case COLOR_G -> result.setColorG(value);
            case COLOR_B -> result.setColorB(value);
            case COLOR_A -> result.setColorA(value);
            default -> {}
        }
    }
}
