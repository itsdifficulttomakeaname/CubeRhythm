package org.cubeRhythm.note;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * EXECUTION 音符处理器
 * 在游戏过程中执行特定时间的动作
 */
public class ExecutionHandler {

    /**
     * 执行 EXECUTION 音符的所有动作
     * @param player 目标玩家
     * @param note EXECUTION 音符
     */
    public static void executeActions(Player player, Note note) {
        if (note.getType() != NoteType.EXECUTION || note.getActions() == null) {
            return;
        }

        // 遍历并执行所有动作
        for (Map<String, Object> action : note.getActions()) {
            executeAction(player, action);
        }
    }

    /**
     * 执行单个动作
     * @param player 目标玩家
     * @param action 动作配置
     */
    private static void executeAction(Player player, Map<String, Object> action) {
        String actionType = (String) action.get("type");
        Boolean enabled = (Boolean) action.getOrDefault("enabled", true);

        if (!enabled) {
            return;
        }

        switch (actionType) {
            case "title" -> executeTitle(player, action);
            case "actionbar" -> executeActionBar(player, action);
            case "chat" -> executeChat(player, action);
            case "potion" -> executePotion(player, action);
            case "clear_effects" -> executeClearEffects(player);
            case "remove_potion" -> executeRemovePotion(player, action);
        }
    }

    private static void executeTitle(Player player, Map<String, Object> action) {
        String title = (String) action.getOrDefault("title", "");
        String subtitle = (String) action.get("subtitle");
        int fadeIn = ((Number) action.getOrDefault("fadeIn", 10)).intValue();
        int stay = ((Number) action.getOrDefault("stay", 40)).intValue();
        int fadeOut = ((Number) action.getOrDefault("fadeOut", 10)).intValue();

        ExecutionAction.showTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    private static void executeActionBar(Player player, Map<String, Object> action) {
        String text = (String) action.getOrDefault("text", "");
        ExecutionAction.showActionBar(player, text);
    }

    private static void executeChat(Player player, Map<String, Object> action) {
        String message = (String) action.getOrDefault("message", "");
        ExecutionAction.sendChatMessage(player, message);
    }

    private static void executePotion(Player player, Map<String, Object> action) {
        String effectType = (String) action.getOrDefault("effectType", "SPEED");
        int duration = ((Number) action.getOrDefault("duration", 100)).intValue();
        int amplifier = ((Number) action.getOrDefault("amplifier", 0)).intValue();
        boolean ambient = (Boolean) action.getOrDefault("ambient", false);
        boolean particles = (Boolean) action.getOrDefault("particles", true);
        boolean icon = (Boolean) action.getOrDefault("icon", true);

        ExecutionAction.givePotionEffect(player, effectType, duration, amplifier, ambient, particles, icon);
    }

    private static void executeClearEffects(Player player) {
        ExecutionAction.clearAllPotionEffects(player);
    }

    private static void executeRemovePotion(Player player, Map<String, Object> action) {
        String effectType = (String) action.getOrDefault("effectType", "");
        ExecutionAction.removePotionEffect(player, effectType);
    }
}
