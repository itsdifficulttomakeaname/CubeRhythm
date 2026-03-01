# 玩家移动限制功能

## 实现日期
2026-02-15

## 功能说明

在游戏过程中，玩家不能改变位置（x, y, z 坐标），但可以自由转动视角（yaw, pitch）。

## 实现细节

### 1. MovementRestriction 监听器
**文件:** `src/main/java/org/cubeRhythm/game/MovementRestriction.java`

**功能:**
- 监听 `PlayerMoveEvent` 事件
- 检查玩家是否在活跃的游戏会话中
- 只在 `GameState.PLAYING` 状态时限制移动
- 取消位置变化但保留视角旋转
- 显示警告消息（带冷却时间防止刷屏）

**工作原理:**
```java
// 检查位置是否改变
boolean positionChanged = from.getX() != to.getX() ||
                          from.getY() != to.getY() ||
                          from.getZ() != to.getZ();

if (positionChanged) {
    // 创建修正后的位置：保持原位置，但使用新的视角
    Location corrected = from.clone();
    corrected.setYaw(to.getYaw());
    corrected.setPitch(to.getPitch());
    event.setTo(corrected);
}
```

### 2. 消息冷却系统

**目的:** 防止玩家尝试移动时消息刷屏

**实现:**
- 使用 `HashMap<UUID, Long>` 存储每个玩家的最后消息时间
- 冷却时间: 2 秒
- 使用 Adventure API 的 `Component` 显示消息

**消息内容:**
```
游戏中不能移动位置！只能转动视角。
```

### 3. 注册监听器

**文件:** `src/main/java/org/cubeRhythm/Main.java`

在 `onEnable()` 方法中注册:
```java
// Register movement restriction listener
getServer().getPluginManager().registerEvents(new MovementRestriction(), this);
```

## 使用场景

### 何时生效
- 玩家在游戏会话中（`Main.instance.getCurrentSession() != null`）
- 游戏状态为 `PLAYING`
- 玩家是当前会话的玩家

### 何时不生效
- 没有活跃的游戏会话
- 游戏状态为 `IDLE`, `LOADING`, `PAUSED`, 或 `RESULTS`
- 玩家不是当前会话的玩家（多人服务器场景）

## 技术细节

### 事件优先级
使用 `EventPriority.HIGHEST` 确保在其他插件之后处理，避免冲突。

### 位置检测
精确检测 x, y, z 坐标的变化：
- 使用 `!=` 比较浮点数（Bukkit Location 的坐标）
- 不使用容差值，任何微小移动都会被阻止

### 视角保留
完全保留玩家的视角变化：
- Yaw（水平旋转）
- Pitch（垂直旋转）

这允许玩家在游戏中自由观察四个方向的音符。

## 性能考虑

### 优化措施
1. **早期返回:** 如果没有活跃会话，立即返回
2. **状态检查:** 只在 PLAYING 状态时执行位置检查
3. **消息冷却:** 避免频繁发送消息造成性能问题

### 内存使用
- `messageCooldown` HashMap 存储玩家 UUID 和时间戳
- 建议在游戏结束时清理（可选）

## 未来改进建议

### 1. 自动清理冷却数据
在游戏结束时清理玩家的冷却数据：
```java
// In GameSession.stop() or end()
if (movementRestriction != null) {
    movementRestriction.clearCooldown(player.getUniqueId());
}
```

### 2. 可配置的容差值
允许微小的位置变化（例如 0.01 方块）以处理浮点数精度问题：
```java
private static final double POSITION_TOLERANCE = 0.01;

boolean positionChanged =
    Math.abs(from.getX() - to.getX()) > POSITION_TOLERANCE ||
    Math.abs(from.getY() - to.getY()) > POSITION_TOLERANCE ||
    Math.abs(from.getZ() - to.getZ()) > POSITION_TOLERANCE;
```

### 3. 配置选项
在 `config.yml` 中添加开关：
```yaml
gameplay:
  restrict_movement: true
  movement_warning_message: "游戏中不能移动位置！只能转动视角。"
  message_cooldown_seconds: 2
```

### 4. 多玩家支持
如果未来支持多人同时游玩，需要改进会话管理：
- 使用 `Map<UUID, GameSession>` 存储多个会话
- 每个玩家检查自己的会话状态

## 测试建议

### 测试用例
1. **基本功能测试:**
   - 开始游戏后尝试移动（WASD）
   - 验证位置不变
   - 验证可以转动视角（鼠标）

2. **状态测试:**
   - 游戏开始前可以移动
   - 游戏进行中不能移动
   - 游戏结束后可以移动

3. **消息测试:**
   - 尝试移动时显示警告消息
   - 连续尝试移动时消息不刷屏（2秒冷却）

4. **边界测试:**
   - 跳跃（Y轴变化）
   - 潜行（Y轴微小变化）
   - 飞行模式

## 已知限制

1. **浮点数精度:** 某些情况下可能因浮点数精度问题触发限制
2. **服务器延迟:** 高延迟可能导致位置校正不够平滑
3. **单会话限制:** 当前只支持一个活跃会话

## 相关文件

- `MovementRestriction.java` - 主要实现
- `Main.java` - 监听器注册
- `GameSession.java` - 游戏会话管理
- `GameState.java` - 游戏状态枚举

---

*实现者: Claude Code*
*功能状态: 已完成*
*测试状态: 待测试*
