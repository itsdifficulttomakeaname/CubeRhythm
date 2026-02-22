package org.cubeRhythm.editor;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.NoteType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 编辑器中的音符数据
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
@Data
public class EditorNote {
    private String id;  // 使用String而不是UUID
    private NoteType type;
    private double time;  // 音符时间（秒）
    private Face face;
    private NotePosition position;
    private List<NotePosition> positions;  // For DOUBLE notes
    private boolean glowing;
    private String tag;
    private String turn;  // For FLICK notes: "left" or "right"
    private String section;  // For EXECUTION notes

    // 渲染相关
    private BlockDisplay blockDisplay;
    private List<BlockDisplay> additionalDisplays;  // For DOUBLE notes

    public EditorNote() {
        this.id = UUID.randomUUID().toString();
        this.glowing = false;
        this.tag = "";
        this.positions = new ArrayList<>();
        this.additionalDisplays = new ArrayList<>();
    }

    /**
     * 清理显示实体
     */
    public void cleanup() {
        if (blockDisplay != null && !blockDisplay.isDead()) {
            blockDisplay.remove();
        }
        for (BlockDisplay display : additionalDisplays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        additionalDisplays.clear();
    }
}
