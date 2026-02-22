package org.cubeRhythm.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cubeRhythm.Main;
import org.jetbrains.annotations.NotNull;

/**
 * 退出命令
 * 用于中途退出当前游戏
 */
public class ExitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }

        // 检查是否有正在进行的游戏
        if (Main.instance.getCurrentSession() == null) {
            player.sendMessage("§c当前没有正在进行的游戏");
            return true;
        }

        // 停止游戏
        Main.instance.getCurrentSession().stop();
        Main.instance.setCurrentSession(null);

        player.sendMessage("§e已退出游戏");
        return true;
    }
}
