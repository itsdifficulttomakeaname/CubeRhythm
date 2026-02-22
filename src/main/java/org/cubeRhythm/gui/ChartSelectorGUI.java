package org.cubeRhythm.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.chart.ChartRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 谱面选择GUI
 */
public class ChartSelectorGUI {
    private final ChartRegistry chartRegistry;
    private static final int GUI_SIZE = 54; // 6行

    public ChartSelectorGUI(ChartRegistry chartRegistry) {
        this.chartRegistry = chartRegistry;
    }

    /**
     * 打开谱面选择界面
     */
    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "§6§l选择谱面");

        List<Chart> charts = new ArrayList<>(chartRegistry.getAllCharts());

        // 填充谱面物品
        for (int i = 0; i < Math.min(charts.size(), GUI_SIZE - 9); i++) {
            Chart chart = charts.get(i);
            ItemStack item = createChartItem(chart);
            gui.setItem(i, item);
        }

        // 底部控制栏
        // 刷新按钮
        ItemStack refreshItem = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName("§a§l刷新谱面");
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add("§7重新加载所有谱面文件");
        refreshLore.add("§7并预加载谱面数据");
        refreshMeta.setLore(refreshLore);
        refreshItem.setItemMeta(refreshMeta);
        gui.setItem(GUI_SIZE - 9, refreshItem);

        // 设置按钮
        ItemStack settingsItem = new ItemStack(Material.COMPARATOR);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        settingsMeta.setDisplayName("§e§l设置");
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add("§7点击打开设置界面");
        settingsMeta.setLore(settingsLore);
        settingsItem.setItemMeta(settingsMeta);
        gui.setItem(GUI_SIZE - 5, settingsItem);

        // 关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§l关闭");
        closeItem.setItemMeta(closeMeta);
        gui.setItem(GUI_SIZE - 1, closeItem);

        player.openInventory(gui);
    }

    /**
     * 创建谱面物品
     */
    private ItemStack createChartItem(Chart chart) {
        // 根据难度选择不同的材料
        Material material = switch (chart.getMetadata().getDifficulty().getLevel()) {
            case 1, 2, 3, 4, 5 -> Material.LIME_CONCRETE;
            case 6, 7, 8 -> Material.YELLOW_CONCRETE;
            case 9, 10, 11 -> Material.ORANGE_CONCRETE;
            case 12, 13, 14 -> Material.RED_CONCRETE;
            default -> Material.PURPLE_CONCRETE;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // 设置显示名称
        meta.setDisplayName("§f§l" + chart.getMetadata().getTitle());

        // 设置Lore（描述信息）
        List<String> lore = new ArrayList<>();
        lore.add("§7作曲: §f" + chart.getMetadata().getArtist());
        lore.add("§7谱师: §f" + chart.getMetadata().getCharter());
        lore.add("");
        lore.add("§7难度: " + chart.getMetadata().getDifficulty().getColor() +
                 chart.getMetadata().getDifficulty().getName() + " §7[" +
                 chart.getMetadata().getDifficulty().getLevel() + "]");
        lore.add("§7BPM: §f" + chart.getMetadata().getBpm());
        lore.add("§7时长: §f" + formatDuration(chart.getMetadata().getDuration()));
        lore.add("§7音符数: §f" + chart.getTotalNotes());
        lore.add("");
        lore.add("§e§l左键 §7开始游戏");
        lore.add("§e§l右键 §7查看详情");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 格式化时长（秒 -> 分:秒）
     */
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}
