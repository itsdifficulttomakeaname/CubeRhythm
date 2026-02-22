package org.cubeRhythm.editor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 步长命令 - /step <整数>
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class StepCommand implements CommandExecutor {
    private final EditorManager editorManager;

    public StepCommand() {
        this.editorManager = EditorManager.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令");
            return true;
        }

        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c你不在编辑模式");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§c用法: /step <整数>");
            player.sendMessage("§7例如: /step 4 表示步长为1/4拍");
            return true;
        }

        try {
            int stepLength = Integer.parseInt(args[0]);
            if (stepLength <= 0) {
                player.sendMessage("§c步长必须大于0");
                return true;
            }

            EditorSession session = editorManager.getSession(player);
            int oldStep = session.getStepLength();

            // 调整当前tick以保持相对位置
            session.setTick(session.getTick() * stepLength / oldStep);
            session.setStepLength(stepLength);

            // 更新音符渲染
            EditorNoteRenderer.renderVisibleNotes(session, player);

            player.sendMessage("§a步长已设置为: 1/" + stepLength + " 拍");
            player.sendTitle("", "§a✔", 10, 20, 10);

        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的整数: " + args[0]);
        }

        return true;
    }
}
