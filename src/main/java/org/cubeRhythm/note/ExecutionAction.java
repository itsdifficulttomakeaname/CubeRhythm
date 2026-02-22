package org.cubeRhythm.note;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

/**
 * EXECUTION 音符的动作工具类
 * 提供在特定时间触发的各种效果
 */
public class ExecutionAction {

    /**
     * 显示标题和副标题
     * @param player 目标玩家
     * @param title 主标题文本（支持颜色代码）
     * @param subtitle 副标题文本（支持颜色代码）
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    public static void showTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        Component titleComponent = Component.text(title);
        Component subtitleComponent = subtitle != null ? Component.text(subtitle) : Component.empty();

        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );

        Title titleObj = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(titleObj);
    }

    /**
     * 显示 ActionBar 文本
     * @param player 目标玩家
     * @param text 文本内容（支持颜色代码）
     */
    public static void showActionBar(Player player, String text) {
        player.sendActionBar(Component.text(text));
    }

    /**
     * 发送聊天消息
     * @param player 目标玩家
     * @param message 消息内容（支持颜色代码）
     */
    public static void sendChatMessage(Player player, String message) {
        player.sendMessage(message);
    }

    /**
     * 给予玩家药水效果
     * @param player 目标玩家
     * @param effectType 药水效果类型（如 "BLINDNESS", "SPEED" 等）
     * @param duration 持续时间（tick）
     * @param amplifier 效果等级（0 = 等级 I）
     * @param ambient 是否为环境效果（减少粒子）
     * @param particles 是否显示粒子
     * @param icon 是否显示图标
     */
    public static void givePotionEffect(Player player, String effectType, int duration,
                                        int amplifier, boolean ambient, boolean particles, boolean icon) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectType);
            if (type != null) {
                PotionEffect effect = new PotionEffect(
                    type,
                    duration,
                    amplifier,
                    ambient,
                    particles,
                    icon
                );
                player.addPotionEffect(effect);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 移除玩家的特定药水效果
     * @param player 目标玩家
     * @param effectType 药水效果类型
     */
    public static void removePotionEffect(Player player, String effectType) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectType);
            if (type != null) {
                player.removePotionEffect(type);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 清除玩家的所有药水效果
     * @param player 目标玩家
     */
    public static void clearAllPotionEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
}