package org.cubeRhythm.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.cubeRhythm.Main;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.game.GameSession;
import org.cubeRhythm.game.PlayerSettings;

/**
 * GUI事件处理器
 */
public class GUIListener implements Listener {
    private final Main plugin;
    private final ChartSelectorGUI chartSelector;
    private final SettingsGUI settingsGUI;

    public GUIListener(Main plugin) {
        this.plugin = plugin;
        this.chartSelector = new ChartSelectorGUI(plugin.getChartRegistry());
        this.settingsGUI = new SettingsGUI();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        // 谱面选择界面
        if (title.equals("§6§l选择谱面")) {
            event.setCancelled(true);
            handleChartSelector(event, player);
            return;
        }

        // 设置界面
        if (title.equals("§6§l游戏设置")) {
            event.setCancelled(true);
            handleSettings(event, player);
            return;
        }
    }

    private void handleChartSelector(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String displayName = clicked.getItemMeta().getDisplayName();

        // 关闭按钮
        if (displayName.equals("§c§l关闭")) {
            player.closeInventory();
            return;
        }

        // 刷新按钮
        if (displayName.equals("§a§l刷新谱面")) {
            player.sendMessage("§a正在重新加载谱面...");
            plugin.getChartRegistry().loadAllCharts();
            player.sendMessage("§a谱面已刷新！共加载 " + plugin.getChartRegistry().getChartCount() + " 个谱面");
            chartSelector.open(player);
            return;
        }

        // 设置按钮
        if (displayName.equals("§e§l设置")) {
            settingsGUI.open(player);
            return;
        }

        // 谱面选择
        if (displayName.startsWith("§f§l")) {
            String chartTitle = displayName.substring(4); // 移除颜色代码
            Chart chart = findChartByTitle(chartTitle);

            if (chart != null) {
                player.closeInventory();

                // 加载玩家设置
                PlayerSettings settings = plugin.getPlayerSettingsManager().getSettings(player);

                // 创建并启动游戏会话
                GameSession session = new GameSession(player, chart, settings);
                plugin.setCurrentSession(session);
                session.start();
            }
        }
    }

    private void handleSettings(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String displayName = clicked.getItemMeta().getDisplayName();
        PlayerSettings settings = plugin.getPlayerSettingsManager().getSettings(player);
        boolean shift = event.isShiftClick();
        boolean leftClick = event.isLeftClick();

        // 返回按钮
        if (displayName.equals("§a§l返回")) {
            chartSelector.open(player);
            return;
        }

        // 流速设置
        if (displayName.startsWith("§e§l流速:")) {
            double change = shift ? 0.5 : 0.1;
            if (leftClick) {
                settings.setSpeed(Math.min(5.0, settings.getSpeed() + change));
            } else {
                settings.setSpeed(Math.max(0.1, settings.getSpeed() - change));
            }
            plugin.getPlayerSettingsManager().saveSettings(player, settings);
            settingsGUI.open(player);
            return;
        }

        // 偏移设置
        if (displayName.startsWith("§e§l偏移:")) {
            int change = shift ? 50 : 10;
            if (leftClick) {
                settings.setOffset(settings.getOffset() + change);
            } else {
                settings.setOffset(settings.getOffset() - change);
            }
            plugin.getPlayerSettingsManager().saveSettings(player, settings);
            settingsGUI.open(player);
            return;
        }

        // 难度设置
        if (displayName.startsWith("§e§l难度:")) {
            int newDifficulty = settings.getDifficulty() + 1;
            if (newDifficulty > 3) newDifficulty = 1;
            settings.setDifficulty(newDifficulty);
            plugin.getPlayerSettingsManager().saveSettings(player, settings);
            settingsGUI.open(player);
            return;
        }

        // 打击音效
        if (displayName.startsWith("§e§l打击音效:")) {
            settings.setHitSound(!settings.isHitSound());
            plugin.getPlayerSettingsManager().saveSettings(player, settings);
            settingsGUI.open(player);
            return;
        }

        // 自动演奏
        if (displayName.startsWith("§e§l自动演奏:")) {
            settings.setAutoPlay(!settings.isAutoPlay());
            plugin.getPlayerSettingsManager().saveSettings(player, settings);
            settingsGUI.open(player);
            return;
        }

        // Flick自动转向
        if (displayName.startsWith("§e§lFlick自动转向:")) {
            settings.setAutoFlickRotation(!settings.isAutoFlickRotation());
            plugin.getPlayerSettingsManager().saveSettings(player, settings);
            settingsGUI.open(player);
            return;
        }
    }

    private Chart findChartByTitle(String title) {
        for (Chart chart : plugin.getChartRegistry().getAllCharts()) {
            if (chart.getMetadata().getTitle().equals(title)) {
                return chart;
            }
        }
        return null;
    }

    public ChartSelectorGUI getChartSelector() {
        return chartSelector;
    }
}
