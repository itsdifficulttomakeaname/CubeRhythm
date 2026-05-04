package org.cubeRhythm.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.cubeRhythm.Main;
import org.cubeRhythm.manager.OffsetConfig;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        Main.instance.getConfigManager().reloadConfig();
        OffsetConfig.get().reload();
        sender.sendMessage("§aCubeRhythm 配置已重载");
        return true;
    }
}
