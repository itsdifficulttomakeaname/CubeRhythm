package org.cubeRhythm.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cubeRhythm.Main;
import org.jetbrains.annotations.NotNull;

/**
 * GUI命令 - 打开谱面选择界面
 */
public class GUICommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }

        // 打开谱面选择GUI
        Main.instance.getGuiListener().getChartSelector().open(player);

        return true;
    }
}
