package org.cubeRhythm.note;

import lombok.Data;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.event.EventTrack;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class Note {
    private double time;
    private double appearBefore;
    private NoteType type;
    private Face face;
    private NotePosition position;  // For single-position notes (TAP, HOLD, DRAG, FLICK)
    private List<NotePosition> positions;  // For DOUBLE notes (multiple positions)
    private boolean glowing;
    private Set<String> tags;  // 多标签支持，一个音符可属于多个 tag 组
    private String turn; // For flick notes: "left" or "right"
    private List<Map<String, Object>> actions; // For execution notes: list of action configurations
    private EventTrack events; // 单音符内联事件（可为 null）

    /**
     * 兼容旧代码：获取单标签（返回第一个 tag 或空字符串）
     */
    public String getTag() {
        if (tags == null || tags.isEmpty()) return "";
        return tags.iterator().next();
    }

    /**
     * 兼容旧代码：设置单标签
     */
    public void setTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            this.tags = null;
        } else {
            this.tags = new java.util.HashSet<>();
            this.tags.add(tag);
        }
    }
}
