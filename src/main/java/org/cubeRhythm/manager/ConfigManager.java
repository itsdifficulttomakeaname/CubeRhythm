package org.cubeRhythm.manager;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.cubeRhythm.Main.instance;

/**
 * 配置管理器
 * 管理插件的配置文件
 */
public class ConfigManager {
    private final Logger logger;
    /**
     * -- GETTER --
     *  获取原始配置对象
     *
     * @return FileConfiguration
     */
    @Getter
    private FileConfiguration config;
    private File configFile;

    // 默认配置值
    private static final double DEFAULT_SPEED = 1.0;
    private static final int DEFAULT_OFFSET = 0;
    private static final boolean DEFAULT_AUTO_SAVE_SCORES = true;
    private static final int DEFAULT_MAX_CONCURRENT_GAMES = 10;

    public ConfigManager() {
        this.logger = instance.getLogger();
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        configFile = new File(instance.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            instance.saveDefaultConfig();
            logger.info("创建默认配置文件");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults();
    }

    /**
     * 设置默认配置值
     */
    private void setDefaults() {
        boolean modified = false;

        if (!config.contains("game.default-speed")) {
            config.set("game.default-speed", DEFAULT_SPEED);
            modified = true;
        }

        if (!config.contains("game.default-offset")) {
            config.set("game.default-offset", DEFAULT_OFFSET);
            modified = true;
        }

        if (!config.contains("game.max-concurrent-games")) {
            config.set("game.max-concurrent-games", DEFAULT_MAX_CONCURRENT_GAMES);
            modified = true;
        }

        if (!config.contains("score.auto-save")) {
            config.set("score.auto-save", DEFAULT_AUTO_SAVE_SCORES);
            modified = true;
        }

        if (!config.contains("judgment.exact-window")) {
            config.set("judgment.exact-window", 80);
            modified = true;
        }

        if (!config.contains("judgment.just-window")) {
            config.set("judgment.just-window", 200);
            modified = true;
        }

        if (!config.contains("rendering.max-entities")) {
            config.set("rendering.max-entities", 100);
            modified = true;
        }

        if (!config.contains("rendering.spawn-distance")) {
            config.set("rendering.spawn-distance", 50.0);
            modified = true;
        }

        if (modified) {
            saveConfig();
        }
    }

    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
            logger.info("配置文件已保存");
        } catch (IOException e) {
            logger.severe("保存配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("配置文件已重新加载");
    }

    // Getter 方法

    public double getDefaultSpeed() {
        return config.getDouble("game.default-speed", DEFAULT_SPEED);
    }

    public int getDefaultOffset() {
        return config.getInt("game.default-offset", DEFAULT_OFFSET);
    }

    public int getMaxConcurrentGames() {
        return config.getInt("game.max-concurrent-games", DEFAULT_MAX_CONCURRENT_GAMES);
    }

    public boolean isAutoSaveScores() {
        return config.getBoolean("score.auto-save", DEFAULT_AUTO_SAVE_SCORES);
    }

    public int getExactWindow() {
        return config.getInt("judgment.exact-window", 80);
    }

    public int getJustWindow() {
        return config.getInt("judgment.just-window", 200);
    }

    public int getMaxEntities() {
        return config.getInt("rendering.max-entities", 100);
    }

    public double getSpawnDistance() {
        return config.getDouble("rendering.spawn-distance", 50.0);
    }

    /**
     * 获取游戏中心位置
     * @return [x, y, z] 数组
     */
    public double[] getGameLocation() {
        String locationStr = config.getString("location", "0,320,0");
        String[] parts = locationStr.split(",");
        if (parts.length != 3) {
            logger.warning("配置文件中的 location 格式错误，使用默认值 0,320,0");
            return new double[]{0, 320, 0};
        }
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            return new double[]{x, y, z};
        } catch (NumberFormatException e) {
            logger.warning("配置文件中的 location 格式错误，使用默认值 0,320,0");
            return new double[]{0, 320, 0};
        }
    }

    // Setter 方法

    public void setDefaultSpeed(double speed) {
        config.set("game.default-speed", speed);
        saveConfig();
    }

    public void setDefaultOffset(int offset) {
        config.set("game.default-offset", offset);
        saveConfig();
    }

    public void setMaxConcurrentGames(int max) {
        config.set("game.max-concurrent-games", max);
        saveConfig();
    }

    public void setAutoSaveScores(boolean autoSave) {
        config.set("score.auto-save", autoSave);
        saveConfig();
    }

}
