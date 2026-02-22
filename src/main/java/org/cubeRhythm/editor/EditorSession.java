package org.cubeRhythm.editor;

import lombok.Data;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.NoteType;

import java.io.File;
import java.util.*;

/**
 * 编辑器会话 - 管理单个玩家的编辑状态
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
@Data
public class EditorSession {
    private final Player player;
    private final Map<String, EditorNote> notes;  // 使用String作为key而不是UUID

    // 编辑器状态
    private double bpm;
    private int tick;  // 当前时间刻（基于步长）
    private int stepLength;  // 步长（1拍分成几份）
    private double preTime;  // 第一拍前的额外时间
    private double speed;  // 流速

    // 当前选择
    private NoteType currentNoteType;
    private Face currentFace;
    private boolean glowing;
    private String flickDirection;  // "left" or "right" for FLICK notes

    // DOUBLE音符放置状态
    private boolean doubleNotePlacementMode;  // 是否正在放置DOUBLE音符
    private NotePosition doubleNoteFirstPosition;  // DOUBLE音符的第一个位置
    private String doubleNoteId;  // 正在放置的DOUBLE音符ID

    // 预览光标
    private BlockDisplay previewCursor;  // 预览光标实体

    // 文件相关
    private String chartId;  // 谱面ID（文件名）
    private File chartFile;

    public EditorSession(Player player) {
        this.player = player;
        this.notes = new HashMap<>();

        // 默认值
        this.bpm = 120.0;
        this.tick = 0;
        this.stepLength = 4;  // 默认1/4拍
        this.preTime = 0.0;
        this.speed = 1.0;

        this.currentNoteType = NoteType.TAP;
        this.currentFace = Face.W;
        this.glowing = false;
        this.flickDirection = "left";  // 默认左转

        // DOUBLE音符放置状态
        this.doubleNotePlacementMode = false;
        this.doubleNoteFirstPosition = null;
        this.doubleNoteId = null;

        // 预览光标
        this.previewCursor = null;
    }

    /**
     * 计算当前时间（秒）
     */
    public double getCurrentTime() {
        return (double) tick / stepLength * (60.0 / bpm) + preTime;
    }

    /**
     * 计算当前拍数
     */
    public int getCurrentBeat() {
        return tick / stepLength + 1;
    }

    /**
     * 计算当前拍内的位置
     */
    public int getCurrentBeatPosition() {
        return tick % stepLength;
    }

    /**
     * 添加音符
     */
    public void addNote(EditorNote note) {
        notes.put(note.getId(), note);
    }

    /**
     * 删除音符
     */
    public void removeNote(String noteId) {
        EditorNote note = notes.remove(noteId);
        if (note != null) {
            note.cleanup();
        }
    }

    /**
     * 切换发光状态
     */
    public void toggleGlowing() {
        this.glowing = !this.glowing;
    }

    /**
     * 获取所有音符（按时间排序）
     */
    public List<EditorNote> getSortedNotes() {
        List<EditorNote> sortedNotes = new ArrayList<>(notes.values());
        sortedNotes.sort(Comparator.comparingDouble(EditorNote::getTime));
        return sortedNotes;
    }

    /**
     * 清空所有音符
     */
    public void clearAllNotes() {
        for (EditorNote note : notes.values()) {
            note.cleanup();
        }
        notes.clear();
    }

    /**
     * 计算 HOLD 音符的长度（基于 BPM 和流速）
     * 使得一拍的 HOLD 能与下一拍首尾相接
     */
    public double calculateHoldLength() {
        // 一拍的时间（秒）
        double beatDuration = 60.0 / bpm;
        // 转换为距离：distance = time * speed * 20
        return beatDuration * speed * 20;
    }

    /**
     * 前进/后退指定步数
     */
    public void addTick(int amount) {
        tick += amount;
        if (tick < 0) {
            tick = 0;
        }
    }

    /**
     * 前进/后退指定拍数
     */
    public void addBeat(int amount) {
        tick += amount * stepLength;
        if (tick < 0) {
            tick = 0;
        }
    }

    /**
     * 循环切换音符类型
     */
    public void cycleNoteType() {
        // TAP -> DOUBLE -> DRAG -> HOLD -> FLICK(left) -> FLICK(right) -> TAP
        if (currentNoteType == NoteType.TAP) {
            currentNoteType = NoteType.DOUBLE;
        } else if (currentNoteType == NoteType.DOUBLE) {
            currentNoteType = NoteType.DRAG;
        } else if (currentNoteType == NoteType.DRAG) {
            currentNoteType = NoteType.HOLD;
        } else if (currentNoteType == NoteType.HOLD) {
            currentNoteType = NoteType.FLICK;
            flickDirection = "left";
        } else if (currentNoteType == NoteType.FLICK) {
            if (flickDirection.equals("left")) {
                flickDirection = "right";
            } else {
                currentNoteType = NoteType.TAP;
            }
        } else {
            currentNoteType = NoteType.TAP;
        }
    }

    /**
     * 循环切换判定面
     */
    public void cycleFace() {
        currentFace = switch (currentFace) {
            case W -> Face.A;
            case A -> Face.S;
            case S -> Face.D;
            case D -> Face.W;
        };
    }

    /**
     * 清理预览光标
     */
    public void cleanupPreviewCursor() {
        if (previewCursor != null && !previewCursor.isDead()) {
            previewCursor.remove();
        }
        previewCursor = null;
    }

    /**
     * 开始DOUBLE音符放置（第一个位置）
     */
    public void startDoubleNotePlacement(NotePosition firstPosition, String noteId) {
        this.doubleNotePlacementMode = true;
        this.doubleNoteFirstPosition = firstPosition;
        this.doubleNoteId = noteId;
    }

    /**
     * 取消DOUBLE音符放置
     */
    public void cancelDoubleNotePlacement() {
        this.doubleNotePlacementMode = false;
        this.doubleNoteFirstPosition = null;
        if (this.doubleNoteId != null) {
            // 删除未完成的DOUBLE音符
            removeNote(this.doubleNoteId);
        }
        this.doubleNoteId = null;
    }

    /**
     * 完成DOUBLE音符放置（第二个位置）
     */
    public void completeDoubleNotePlacement() {
        this.doubleNotePlacementMode = false;
        this.doubleNoteFirstPosition = null;
        this.doubleNoteId = null;
    }
}
