package org.cubeRhythm.editor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 跳转拍数命令 - /b <拍数>
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class BeatCommand implements CommandExecutor {
    private final EditorManager editorManager;

    public BeatCommand() {
        this.editorManager = EditorManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令");
            return true;
        }

        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c你不在编辑模式");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§c用法: /b <拍数>");
            return true;
        }

        try {
            int beat = Integer.parseInt(args[0]);
            if (beat < 1) {
                player.sendMessage("§c拍数必须大于等于1");
                return true;
            }

            EditorSession session = editorManager.getSession(player);
            session.setTick((beat - 1) * session.getStepLength());

            // 更新音符渲染
            EditorNoteRenderer.renderVisibleNotes(session, player);

            player.sendMessage("§a已跳转到第 " + beat + " 拍");
            player.sendTitle("", "§a✔", 10, 20, 10);

        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的拍数: " + args[0]);
        }

        return true;
    }
}
