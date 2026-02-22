package org.cubeRhythm.input;

import cn.jason31416.planetlib.PlanetLib;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CancelFlagManager {
    private final Map<UUID, Set<CancelFlag>> playerFlags = new ConcurrentHashMap<>();

    public enum CancelFlag {
        INTERACT,
        SHOOT,
        DAMAGE,
        ANIMATION,
        INTERACT_ENTITY
    }

    public CancelFlagManager() {
    }

    /**
     * 检查操作是否应该被取消（已处理）
     * @param player 玩家
     * @param flag 取消标志类型
     * @return 如果应该取消（重复）返回 true，如果应该处理返回 false
     */
    public boolean shouldCancel(Player player, CancelFlag flag) {
        Set<CancelFlag> flags = playerFlags.computeIfAbsent(
            player.getUniqueId(),
            k -> Collections.synchronizedSet(new HashSet<>())
        );

        if (flags.contains(flag)) {
            return true;
        }

        flags.add(flag);

        // 1 tick 后移除标志
        PlanetLib.getScheduler().runLater(() -> {
            flags.remove(flag);
        }, 1L);

        return false;
    }

    /**
     * 清除玩家的所有标志
     */
    public void clearFlags(Player player) {
        playerFlags.remove(player.getUniqueId());
    }

    /**
     * 清除所有标志
     */
    public void clearAll() {
        playerFlags.clear();
    }
}
