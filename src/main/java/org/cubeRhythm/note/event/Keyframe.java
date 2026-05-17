package org.cubeRhythm.note.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关键帧：时间轴上某一时刻的属性目标值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Keyframe {
    /**
     * 时间点（绝对秒，rtime 在解析期已转换为绝对秒）
     */
    private double time;

    /**
     * 通道目标值
     */
    private double value;

    /**
     * 从上一帧到本帧使用的缓动函数名
     * 首帧此字段无意义
     */
    private String easing;
}
