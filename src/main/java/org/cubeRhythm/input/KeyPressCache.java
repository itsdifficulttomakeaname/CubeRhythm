package org.cubeRhythm.input;

import cn.jason31416.planetlib.PlanetLib;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按键状态机
 * 当有键按下时，设置状态为true，300ms后自动设为false
 */
public class KeyPressCache {
    private final Map<UUID, Boolean> keyPressState = new ConcurrentHashMap<>();

    /**
     * 记录按键按下，并在300ms后自动清除
     */
    public void recordKeyPress(UUID playerId) {
        keyPressState.put(playerId, true);

        // 300ms后自动设为false
        PlanetLib.getScheduler().runLater(() -> {
            keyPressState.put(playerId, false);
        }, 6L);  // 300ms = 6 ticks
    }

    /**
     * 检查玩家当前是否处于按键按下状态
     */
    public boolean hasKeyPressedRecently(UUID playerId) {
        return keyPressState.getOrDefault(playerId, false);
    }

    /**
     * 清除玩家的状态
     */
    public void clear(UUID playerId) {
        keyPressState.remove(playerId);
    }

    /**
     * 清除所有状态
     */
    public void clearAll() {
        keyPressState.clear();
    }
}
