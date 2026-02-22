package org.cubeRhythm.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.cubeRhythm.Main;
import org.cubeRhythm.game.PlayerSettings;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家设置管理器
 * 负责玩家设置的持久化存储和读取
 */
public class PlayerSettingsManager {
    private final Main plugin;
    private final File settingsFolder;
    private final Map<UUID, PlayerSettings> settingsCache;

    public PlayerSettingsManager(Main plugin) {
        this.plugin = plugin;
        this.settingsFolder = new File(plugin.getDataFolder(), "player_settings");
        this.settingsCache = new HashMap<>();

        // 创建设置文件夹
        if (!settingsFolder.exists()) {
            settingsFolder.mkdirs();
        }
    }

    /**
     * 获取玩家设置（从缓存或文件加载）
     */
    public PlayerSettings getSettings(Player player) {
        return getSettings(player.getUniqueId());
    }

    /**
     * 获取玩家设置（从缓存或文件加载）
     */
    public PlayerSettings getSettings(UUID playerId) {
        // 先检查缓存
        if (settingsCache.containsKey(playerId)) {
            return settingsCache.get(playerId);
        }

        // 从文件加载
        PlayerSettings settings = loadSettings(playerId);
        settingsCache.put(playerId, settings);
        return settings;
    }

    /**
     * 保存玩家设置
     */
    public void saveSettings(Player player, PlayerSettings settings) {
        saveSettings(player.getUniqueId(), settings);
    }

    /**
     * 保存玩家设置
     */
    public void saveSettings(UUID playerId, PlayerSettings settings) {
        // 更新缓存
        settingsCache.put(playerId, settings);

        // 保存到文件
        File settingsFile = getSettingsFile(playerId);
        FileConfiguration config = new YamlConfiguration();

        config.set("speed", settings.getSpeed());
        config.set("offset", settings.getOffset());
        config.set("hitSound", settings.isHitSound());
        config.set("autoPlay", settings.isAutoPlay());
        config.set("autoFlickRotation", settings.isAutoFlickRotation());
        config.set("showBeatLines", settings.isShowBeatLines());
        config.set("difficulty", settings.getDifficulty());

        try {
            config.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家设置: " + playerId + " - " + e.getMessage());
        }
    }

    /**
     * 从文件加载玩家设置
     */
    private PlayerSettings loadSettings(UUID playerId) {
        File settingsFile = getSettingsFile(playerId);

        // 如果文件不存在，返回默认设置
        if (!settingsFile.exists()) {
            return createDefaultSettings();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(settingsFile);

        PlayerSettings settings = new PlayerSettings();
        settings.setSpeed(config.getDouble("speed", 1.0));
        settings.setOffset(config.getInt("offset", 0));
        settings.setHitSound(config.getBoolean("hitSound", true));
        settings.setAutoPlay(config.getBoolean("autoPlay", false));
        settings.setAutoFlickRotation(config.getBoolean("autoFlickRotation", false));
        settings.setShowBeatLines(config.getBoolean("showBeatLines", true));
        settings.setDifficulty(config.getInt("difficulty", 2));

        return settings;
    }

    /**
     * 创建默认设置
     */
    private PlayerSettings createDefaultSettings() {
        PlayerSettings settings = new PlayerSettings();
        settings.setSpeed(1.0);
        settings.setOffset(0);
        settings.setHitSound(true);
        settings.setAutoPlay(false);
        settings.setAutoFlickRotation(false);
        settings.setShowBeatLines(true);
        settings.setDifficulty(2); // 默认普通难度

        return settings;
    }

    /**
     * 获取玩家设置文件
     */
    private File getSettingsFile(UUID playerId) {
        return new File(settingsFolder, playerId.toString() + ".yml");
    }

    /**
     * 清除玩家设置缓存
     */
    public void clearCache(UUID playerId) {
        settingsCache.remove(playerId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        settingsCache.clear();
    }

    /**
     * 重新加载玩家设置
     */
    public void reloadSettings(UUID playerId) {
        settingsCache.remove(playerId);
        getSettings(playerId);
    }
}
