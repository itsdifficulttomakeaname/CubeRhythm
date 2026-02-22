package org.cubeRhythm.input;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 雪球管理器
 * 用于捕捉玩家的右键事件
 */
public class SnowballManager {

    /**
     * 给玩家物品栏填满雪球以捕捉右键事件
     */
    public static void giveSnowballs(Player player) {
        ItemStack snowball = new ItemStack(Material.SNOWBALL, 1);

        // 填满物品栏（除了盔甲槽）
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, snowball);
        }

        player.updateInventory();
    }

    /**
     * 清除玩家物品栏中的所有雪球
     */
    public static void removeSnowballs(Player player) {
        player.getInventory().clear();
        player.updateInventory();
    }

    /**
     * 检查玩家是否持有雪球
     */
    public static boolean hasSnowballs(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && mainHand.getType() == Material.SNOWBALL;
    }
}
