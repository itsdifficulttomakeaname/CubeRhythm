# Manager 包说明文档

manager 包包含了 CubeRhythm 插件的核心管理器类，负责协调各个系统的运作。

## 管理器概览

### 1. SongManager（谱面管理器）

**职责：** 管理谱面的加载、缓存和查询

**主要功能：**
- 扫描谱面文件夹，发现所有可用谱面
- 加载谱面 JSON 文件
- 缓存已加载的谱面，提高性能
- 提供谱面查询和重载功能

**使用示例：**
```java
SongManager songManager = new SongManager();

// 加载谱面
Chart chart = songManager.loadChart("example");

// 获取所有可用谱面
List<String> charts = songManager.getAvailableCharts();

// 重新加载谱面（清除缓存）
Chart reloaded = songManager.reloadChart("example");

// 检查谱面是否存在
boolean exists = songManager.chartExists("example");

// 清除所有缓存
songManager.clearCache();
```

**文件位置：** 谱面文件存放在 `plugins/CubeRhythm/charts/` 目录下

---

### 2. GameManager（游戏管理器）

**职责：** 管理所有玩家的游戏会话

**主要功能：**
- 为玩家创建和启动游戏会话
- 管理多个玩家同时进行的游戏
- 提供暂停、恢复、停止游戏的功能
- 跟踪活跃的游戏会话数量
- 插件卸载时清理所有会话

**使用示例：**
```java
GameManager gameManager = new GameManager(songManager);

// 创建玩家设置
PlayerSettings settings = new PlayerSettings();
settings.setSpeed(1.0);
settings.setOffset(0);

// 启动游戏
boolean success = gameManager.startGame(player, "example", settings);

// 检查玩家是否在游戏中
if (gameManager.isInGame(player)) {
    // 暂停游戏
    gameManager.pauseGame(player);

    // 恢复游戏
    gameManager.resumeGame(player);

    // 结束游戏（显示结果）
    gameManager.endGame(player);

    // 或停止游戏（不显示结果）
    gameManager.stopGame(player);
}

// 获取玩家的游戏会话
GameSession session = gameManager.getSession(player);

// 获取活跃会话数量
int count = gameManager.getActiveSessionCount();

// 清理所有会话（插件卸载时）
gameManager.cleanup();
```

---

### 3. ConfigManager（配置管理器）

**职责：** 管理插件的配置文件

**主要功能：**
- 加载和保存配置文件
- 提供默认配置值
- 提供配置项的 getter 和 setter 方法
- 支持配置重载

**配置项：**

#### 游戏设置 (game)
- `default-speed`: 默认速度倍率（默认：1.0）
- `default-offset`: 默认音频偏移（默认：0ms）
- `max-concurrent-games`: 最大同时进行的游戏数量（默认：10）

#### 分数设置 (score)
- `auto-save`: 是否自动保存分数（默认：true）

#### 判定设置 (judgment)
- `exact-window`: Exact 判定窗口（默认：80ms）
- `just-window`: Just 判定窗口（默认：200ms）

#### 渲染设置 (rendering)
- `max-entities`: 最大同时存在的音符实体数量（默认：100）
- `spawn-distance`: 音符生成距离（默认：50.0 格）

**使用示例：**
```java
ConfigManager configManager = new ConfigManager();

// 获取配置值
double speed = configManager.getDefaultSpeed();
int offset = configManager.getDefaultOffset();
int maxGames = configManager.getMaxConcurrentGames();
boolean autoSave = configManager.isAutoSaveScores();

// 设置配置值
configManager.setDefaultSpeed(1.5);
configManager.setDefaultOffset(50);
configManager.setMaxConcurrentGames(20);
configManager.setAutoSaveScores(false);

// 重新加载配置
configManager.reloadConfig();

// 获取原始配置对象
FileConfiguration config = configManager.getConfig();
```

**配置文件示例 (config.yml)：**
```yaml
game:
  default-speed: 1.0
  default-offset: 0
  max-concurrent-games: 10

score:
  auto-save: true

judgment:
  exact-window: 80
  just-window: 200

rendering:
  max-entities: 100
  spawn-distance: 50.0
```

---

## 管理器初始化顺序

在插件主类 (Main.java) 中，建议按以下顺序初始化管理器：

```java
public class Main extends JavaPlugin {
    private ConfigManager configManager;
    private SongManager songManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        // 1. 首先加载配置
        configManager = new ConfigManager();

        // 2. 初始化谱面管理器
        songManager = new SongManager();

        // 3. 初始化游戏管理器
        gameManager = new GameManager(songManager);

        getLogger().info("CubeRhythm 已启用！");
    }

    @Override
    public void onDisable() {
        // 清理游戏会话
        if (gameManager != null) {
            gameManager.cleanup();
        }

        getLogger().info("CubeRhythm 已禁用！");
    }

    // Getter 方法供其他类使用
    public static ConfigManager getConfigManager() {
        return instance.configManager;
    }

    public static SongManager getSongManager() {
        return instance.songManager;
    }

    public static GameManager getGameManager() {
        return instance.gameManager;
    }
}
```

---

## 管理器之间的关系

```
ConfigManager (独立)
    ↓
SongManager (独立)
    ↓
GameManager (依赖 SongManager)
    ↓
GameSession (由 GameManager 创建和管理)
```

---

## 最佳实践

### 1. 单例访问
通过插件主类的静态方法访问管理器：
```java
SongManager songManager = Main.getSongManager();
GameManager gameManager = Main.getGameManager();
```

### 2. 错误处理
所有管理器都内置了错误处理和日志记录：
```java
Chart chart = songManager.loadChart("invalid");
if (chart == null) {
    // 谱面加载失败，已记录日志
    player.sendMessage("§c谱面加载失败！");
}
```

### 3. 资源清理
在插件禁用时，确保调用清理方法：
```java
@Override
public void onDisable() {
    gameManager.cleanup();  // 清理所有游戏会话
    songManager.clearCache();  // 清除谱面缓存
}
```

### 4. 配置热重载
支持在不重启服务器的情况下重载配置：
```java
configManager.reloadConfig();
songManager.scanCharts();  // 重新扫描谱面
```

---

## 扩展建议

未来可以添加的管理器：

1. **ScoreManager** - 管理玩家分数记录和排行榜
2. **AudioManager** - 管理音频播放（如果实现）
3. **PermissionManager** - 管理权限和玩家等级
4. **StatisticsManager** - 统计玩家游戏数据
5. **LeaderboardManager** - 管理排行榜系统

---

## 性能考虑

### SongManager 缓存
- 已加载的谱面会被缓存，避免重复读取文件
- 大型服务器建议定期清理缓存：`songManager.clearCache()`

### GameManager 会话限制
- 通过 `max-concurrent-games` 配置限制同时进行的游戏数量
- 防止服务器资源耗尽

### 实体数量限制
- 通过 `rendering.max-entities` 配置限制同时存在的音符实体
- 平衡视觉效果和性能
