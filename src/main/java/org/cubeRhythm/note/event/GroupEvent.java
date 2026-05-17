package org.cubeRhythm.note.event;

import lombok.Data;

/**
 * 群组事件：选择器 + 事件轨道
 * 加载期一次性预筛命中的音符，运行时不再过滤
 */
@Data
public class GroupEvent {
    private Selector selector;
    private EventTrack events;
}
