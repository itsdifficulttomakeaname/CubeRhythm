package org.cubeRhythm.note.event;

import lombok.Data;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.note.Note;
import org.cubeRhythm.note.NoteType;

import java.util.Set;

/**
 * 群组事件选择器
 * 用结构化条件命中一批音符，取代 tag 单向广播
 *
 * 字段间为"与"关系，单字段内集合为"或"关系
 * 所有字段均可为 null（表示不限制该维度）
 */
@Data
public class Selector {
    private Set<Face> faces;
    private Set<NoteType> types;
    private Set<String> tags;
    private double[] timeRange; // [start, end] 闭区间，null 表示不限

    /**
     * 判断一个音符是否被此选择器命中
     */
    public boolean matches(Note note) {
        // face 过滤
        if (faces != null && !faces.isEmpty()) {
            if (note.getFace() == null || !faces.contains(note.getFace())) {
                return false;
            }
        }

        // type 过滤
        if (types != null && !types.isEmpty()) {
            if (!types.contains(note.getType())) {
                return false;
            }
        }

        // tag 过滤（交集匹配：音符的 tags 与选择器的 tags 有交集即命中）
        if (tags != null && !tags.isEmpty()) {
            java.util.Set<String> noteTags = note.getTags();
            if (noteTags == null || noteTags.isEmpty()) {
                return false;
            }
            boolean hasIntersection = false;
            for (String t : noteTags) {
                if (tags.contains(t)) {
                    hasIntersection = true;
                    break;
                }
            }
            if (!hasIntersection) {
                return false;
            }
        }

        // timeRange 过滤
        if (timeRange != null && timeRange.length == 2) {
            double noteTime = note.getTime();
            if (noteTime < timeRange[0] || noteTime > timeRange[1]) {
                return false;
            }
        }

        return true;
    }
}
