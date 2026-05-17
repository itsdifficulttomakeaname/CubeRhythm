package org.cubeRhythm.note.event;

import lombok.Data;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 事件轨道：一组通道的关键帧集合
 * 每个通道是一条独立的属性时间轴
 */
@Data
public class EventTrack {
    private Map<Channel, List<Keyframe>> channels = new EnumMap<>(Channel.class);

    /**
     * 是否为空（无任何通道数据）
     */
    public boolean isEmpty() {
        return channels.isEmpty();
    }

    /**
     * 获取指定通道的关键帧列表
     * @return 关键帧列表，若通道不存在返回 null
     */
    public List<Keyframe> getChannel(Channel ch) {
        return channels.get(ch);
    }

    /**
     * 设置指定通道的关键帧列表
     */
    public void setChannel(Channel ch, List<Keyframe> keyframes) {
        if (keyframes != null && !keyframes.isEmpty()) {
            channels.put(ch, keyframes);
        }
    }

    /**
     * 是否包含指定通道
     */
    public boolean hasChannel(Channel ch) {
        return channels.containsKey(ch);
    }

    /**
     * 获取所有通道中最大的结束时间（最后一个关键帧的时间）
     * @return 最大结束时间，若无通道数据返回 Double.NEGATIVE_INFINITY
     */
    public double getEndTime() {
        double maxTime = Double.NEGATIVE_INFINITY;
        for (List<Keyframe> kfs : channels.values()) {
            if (kfs != null && !kfs.isEmpty()) {
                double lastTime = kfs.get(kfs.size() - 1).getTime();
                if (lastTime > maxTime) {
                    maxTime = lastTime;
                }
            }
        }
        return maxTime;
    }
}
