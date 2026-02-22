package org.cubeRhythm.editor;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 编辑器管理器 - 管理所有玩家的编辑器会话
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorManager {
    private static EditorManager instance;
    private final Map<UUID, EditorSession> sessions;

    private EditorManager() {
        this.sessions = new HashMap<>();
    }

    public static EditorManager getInstance() {
        if (instance == null) {
            instance = new EditorManager();
        }
        return instance;
    }

    /**
     * 创建编辑器会话
     */
    public EditorSession createSession(Player player) {
        EditorSession session = new EditorSession(player);
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    /**
     * 获取编辑器会话
     */
    public EditorSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * 检查玩家是否在编辑模式
     */
    public boolean isInEditorMode(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * 获取所有编辑器会话
     */
    public Collection<EditorSession> getAllSessions() {
        return sessions.values();
    }

    /**
     * 结束编辑器会话
     */
    public void endSession(Player player) {
        EditorSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            // 清理预览光标
            session.cleanupPreviewCursor();
            // 清理音符显示
            EditorNoteRenderer.clearNoteDisplays(session);
            // 清理所有音符
            session.clearAllNotes();
        }
    }

    /**
     * 清理所有会话
     */
    public void cleanup() {
        for (EditorSession session : sessions.values()) {
            // 清理预览光标
            session.cleanupPreviewCursor();
            // 清理音符显示
            EditorNoteRenderer.clearNoteDisplays(session);
            // 清理所有音符
            session.clearAllNotes();
        }
        sessions.clear();
    }
}
