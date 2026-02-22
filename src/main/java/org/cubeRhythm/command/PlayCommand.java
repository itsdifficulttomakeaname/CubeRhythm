package org.cubeRhythm.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.chart.ChartRegistry;
import org.cubeRhythm.game.GameSession;
import org.cubeRhythm.game.PlayerSettings;
import org.jetbrains.annotations.NotNull;

public class PlayCommand implements CommandExecutor {
    private final ChartRegistry chartRegistry;

    public PlayCommand(ChartRegistry chartRegistry) {
        this.chartRegistry = chartRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (args.length == 0) {
            // 列出可用谱面
            sender.sendMessage("§6可用谱面：");
            for (Chart chart : chartRegistry.getAllCharts()) {
                sender.sendMessage("§e- " + chart.getMetadata().getId() + " §7(" +
                    chart.getMetadata().getTitle() + ")");
            }
            sender.sendMessage("§7使用 /play <谱面ID> 开始游戏");
            return true;
        }

        String chartId = args[0];
        Chart chart = chartRegistry.getChart(chartId);

        if (chart == null) {
            sender.sendMessage("§c谱面不存在: " + chartId);
            return true;
        }

        // 从 PlayerSettingsManager 加载玩家设置
        PlayerSettings settings = org.cubeRhythm.Main.instance.getPlayerSettingsManager().getSettings(player);

        // 如果命令行提供了速度参数，覆盖设置
        if (args.length >= 2) {
            try {
                settings.setSpeed(Double.parseDouble(args[1]));
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的速度值");
                return true;
            }
        }

        // 如果命令行提供了难度参数，覆盖设置
        if (args.length >= 3) {
            try {
                int difficulty = Integer.parseInt(args[2]);
                if (difficulty < 1 || difficulty > 3) {
                    sender.sendMessage("§c难度必须是 1(简单), 2(普通), 或 3(困难)");
                    return true;
                }
                settings.setDifficulty(difficulty);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的难度值");
                return true;
            }
        }

        player.sendMessage("§7难度: " + settings.getDifficultyName() + " §7| 速度: §f" + settings.getSpeed());

        GameSession session = new GameSession(player, chart, settings);
        org.cubeRhythm.Main.instance.setCurrentSession(session);
        session.start();

        return true;
    }
}
