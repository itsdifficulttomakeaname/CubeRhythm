package org.cubeRhythm.editor;

import cn.jason31416.planetlib.PlanetLib;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 编辑器更新任务 - 定期更新预览光标和Action Bar
 * 使用方法：
 * 在 EditorManager 中添加定时任务，每2 ticks运行一次
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorUpdateTask implements Runnable {
    private final EditorManager editorManager;

    public EditorUpdateTask(EditorManager editorManager) {
        this.editorManager = editorManager;
    }

    @Override
    public void run() {
        // 遍历所有编辑器会话
        for (EditorSession session : editorManager.getAllSessions()) {
            Player player = session.getPlayer();

            if (player == null || !player.isOnline()) {
                continue;
            }

            // 计算光标位置
            double[] cursorPos = EditorPreviewCursor.calculateCursorPosition(player);
            double targetX = cursorPos[0];
            double targetY = cursorPos[1];

            // 更新预览光标
            EditorPreviewCursor.updatePreviewCursor(session, player);

            // 发送Action Bar信息
            EditorPreviewCursor.sendActionBarInfo(session, player, targetX, targetY);
        }
    }

    /**
     * 启动定时任务
     */
    public static void start(EditorManager editorManager) {
        EditorUpdateTask task = new EditorUpdateTask(editorManager);
        // 每2 ticks运行一次（0.1秒）
        PlanetLib.getScheduler().runTimer(wrappedTask -> task.run(), 0, 2);
    }
}
