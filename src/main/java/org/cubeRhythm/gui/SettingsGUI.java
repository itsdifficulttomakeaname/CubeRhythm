package org.cubeRhythm.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubeRhythm.Main;
import org.cubeRhythm.game.PlayerSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置界面GUI
 */
public class SettingsGUI {

    /**
     * 打开设置界面
     */
    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§l游戏设置");

        PlayerSettings settings = Main.instance.getPlayerSettingsManager().getSettings(player);

        // 流速设置
        gui.setItem(10, createSpeedItem(settings.getSpeed()));

        // 偏移设置
        gui.setItem(11, createOffsetItem(settings.getOffset()));

        // 难度设置
        gui.setItem(12, createDifficultyItem(settings.getDifficulty()));

        // 打击音效
        gui.setItem(13, createToggleItem(Material.NOTE_BLOCK, "§e§l打击音效",
                settings.isHitSound(), "开启打击音效", "关闭打击音效"));

        // 自动演奏
        gui.setItem(14, createToggleItem(Material.REDSTONE, "§e§l自动演奏",
                settings.isAutoPlay(), "开启自动演奏", "关闭自动演奏"));

        // Flick自动转向（仅在自动演奏时生效）
        gui.setItem(15, createToggleItem(Material.COMPASS, "§e§lFlick自动转向",
                settings.isAutoFlickRotation(), "开启Flick自动转向（自动演奏时生效）", "关闭Flick自动转向"));

        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§a§l返回");
        backItem.setItemMeta(backMeta);
        gui.setItem(22, backItem);

        player.openInventory(gui);
    }

    private ItemStack createSpeedItem(double speed) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l流速: §f" + speed);

        List<String> lore = new ArrayList<>();
        lore.add("§7当前流速: §f" + speed);
        lore.add("");
        lore.add("§e§l左键 §7+0.1");
        lore.add("§e§l右键 §7-0.1");
        lore.add("§e§lShift+左键 §7+0.5");
        lore.add("§e§lShift+右键 §7-0.5");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOffsetItem(int offset) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l偏移: §f" + offset + "ms");

        List<String> lore = new ArrayList<>();
        lore.add("§7当前偏移: §f" + offset + "ms");
        lore.add("§7音频相对谱面的偏移");
        lore.add("");
        lore.add("§e§l左键 §7+10ms");
        lore.add("§e§l右键 §7-10ms");
        lore.add("§e§lShift+左键 §7+50ms");
        lore.add("§e§lShift+右键 §7-50ms");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDifficultyItem(int difficulty) {
        Material material = switch (difficulty) {
            case 1 -> Material.LIME_CONCRETE;
            case 3 -> Material.RED_CONCRETE;
            default -> Material.YELLOW_CONCRETE;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String diffName = switch (difficulty) {
            case 1 -> "§a简单";
            case 3 -> "§c困难";
            default -> "§e普通";
        };

        meta.setDisplayName("§e§l难度: " + diffName);

        List<String> lore = new ArrayList<>();
        lore.add("§7当前难度: " + diffName);
        lore.add("");
        lore.add("§7简单: 碰撞箱 2.0x");
        lore.add("§7普通: 碰撞箱 1.5x");
        lore.add("§7困难: 碰撞箱 1.0x");
        lore.add("");
        lore.add("§e§l点击 §7切换难度");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleItem(Material material, String name, boolean enabled,
                                        String enabledDesc, String disabledDesc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name + ": " + (enabled ? "§a开启" : "§c关闭"));

        List<String> lore = new ArrayList<>();
        lore.add("§7状态: " + (enabled ? "§a开启" : "§c关闭"));
        lore.add("§7" + (enabled ? enabledDesc : disabledDesc));
        lore.add("");
        lore.add("§e§l点击 §7切换");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
