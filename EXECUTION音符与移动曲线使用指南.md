# EXECUTION 音符与移动曲线使用指南

## EXECUTION 音符

EXECUTION 音符是一种特殊的音符类型，不需要玩家击打，而是在特定时间自动触发预设的动作。一个 EXECUTION 音符可以同时触发多个动作。

### 基本格式

```json
{
  "type": "execution",
  "time": 2.0,
  "actions": [
    {
      "type": "action_type",
      "enabled": true,
      // 其他参数...
    }
  ]
}
```

**注意：**
- `actions` 是一个数组，可以包含多个动作
- 每个动作都有 `type` 字段指定动作类型
- 每个动作都有 `enabled` 字段控制是否启用
- EXECUTION 音符不需要 `tag` 字段

### 支持的动作类型

#### 1. 显示标题 (title)
在屏幕中央显示大标题和副标题。

```json
{
  "type": "execution",
  "time": 2.0,
  "actions": [
    {
      "type": "title",
      "enabled": true,
      "title": "§6§lCubeRhythm",
      "subtitle": "§e准备开始！",
      "fadeIn": 10,
      "stay": 40,
      "fadeOut": 10
    }
  ]
}
```

参数说明：
- `title`: 主标题文本（支持 Minecraft 颜色代码）
- `subtitle`: 副标题文本（可选）
- `fadeIn`: 淡入时间（tick，20 tick = 1 秒）
- `stay`: 停留时间（tick）
- `fadeOut`: 淡出时间（tick）

#### 2. 显示 ActionBar (actionbar)
在屏幕下方显示文本。

```json
{
  "type": "execution",
  "time": 7.0,
  "actions": [
    {
      "type": "actionbar",
      "enabled": true,
      "text": "§b注意节奏！"
    }
  ]
}
```

#### 3. 发送聊天消息 (chat)
在聊天框发送消息。

```json
{
  "type": "execution",
  "time": 9.0,
  "actions": [
    {
      "type": "chat",
      "enabled": true,
      "message": "§a[提示] 即将出现 Hold 音符"
    }
  ]
}
```

#### 4. 给予药水效果 (potion)
给玩家添加药水效果。

```json
{
  "type": "execution",
  "time": 11.0,
  "actions": [
    {
      "type": "potion",
      "enabled": true,
      "effectType": "SPEED",
      "duration": 100,
      "amplifier": 1,
      "ambient": false,
      "particles": true,
      "icon": true
    }
  ]
}
```

参数说明：
- `effectType`: 药水效果类型（如 SPEED, BLINDNESS, JUMP_BOOST 等）
- `duration`: 持续时间（tick）
- `amplifier`: 效果等级（0 = I 级，1 = II 级）
- `ambient`: 是否为环境效果（减少粒子）
- `particles`: 是否显示粒子效果
- `icon`: 是否显示状态图标

常用药水效果：
- `SPEED`: 速度
- `SLOWNESS`: 缓慢
- `JUMP_BOOST`: 跳跃提升
- `BLINDNESS`: 失明
- `NIGHT_VISION`: 夜视
- `INVISIBILITY`: 隐身
- `GLOWING`: 发光

#### 5. 移除药水效果 (remove_potion)
移除特定的药水效果。

```json
{
  "type": "execution",
  "time": 15.0,
  "actions": [
    {
      "type": "remove_potion",
      "enabled": true,
      "effectType": "SPEED"
    }
  ]
}
```

#### 6. 清除所有药水效果 (clear_effects)
清除玩家的所有药水效果。

```json
{
  "type": "execution",
  "time": 16.0,
  "actions": [
    {
      "type": "clear_effects",
      "enabled": true
    }
  ]
}
```

### 多动作组合

一个 EXECUTION 音符可以同时触发多个动作，这些动作会按顺序执行：

```json
{
  "type": "execution",
  "time": 11.0,
  "actions": [
    {
      "type": "potion",
      "enabled": true,
      "effectType": "SPEED",
      "duration": 100,
      "amplifier": 1,
      "ambient": false,
      "particles": true,
      "icon": true
    },
    {
      "type": "actionbar",
      "enabled": true,
      "text": "§e速度提升！"
    },
    {
      "type": "title",
      "enabled": true,
      "title": "§b§lSPEED BOOST",
      "subtitle": "",
      "fadeIn": 5,
      "stay": 15,
      "fadeOut": 5
    }
  ]
}
```

这个例子会在时间 11.0 秒时：
1. 给予玩家速度效果
2. 在 ActionBar 显示提示文本
3. 显示标题

### 启用/禁用动作

每个动作都有 `enabled` 字段，可以用来临时禁用某个动作而不删除它：

```json
{
  "type": "execution",
  "time": 13.0,
  "actions": [
    {
      "type": "potion",
      "enabled": false,  // 禁用此动作
      "effectType": "BLINDNESS",
      "duration": 40,
      "amplifier": 0
    }
  ]
}
```

## 音符移动曲线

MovementCurve 类提供了多种速度曲线，使音符移动更具表现力和视觉冲击力。

### 支持的曲线类型

#### 1. LINEAR（线性）
默认的匀速移动，速度恒定。

#### 2. EASE_IN（缓入）
开始慢，逐渐加速。适合营造紧张感。

#### 3. EASE_OUT（缓出）
开始快，逐渐减速。适合营造放松感。

#### 4. EASE_IN_OUT（缓入缓出）
开始和结束都慢，中间快。最平滑的过渡。

#### 5. SINE（正弦波）
基于正弦函数的平滑曲线。

#### 6. EXPONENTIAL（指数）
非常慢的开始，然后急剧加速。适合制造惊喜效果。

#### 7. BOUNCE（弹跳）
模拟弹跳效果，音符会有"弹"的感觉。

#### 8. ELASTIC（弹性）
模拟弹簧效果，音符会有"拉伸"的感觉。

### 使用方法

在代码中使用移动曲线：

```java
import org.cubeRhythm.note.MovementCurve;
import org.cubeRhythm.note.MovementCurve.CurveType;

// 计算带曲线的距离
double distance = MovementCurve.calculateDistance(
    noteTime,
    currentTime,
    speed,
    CurveType.EASE_IN_OUT
);
```

### 在铺面中指定曲线（未来功能）

未来可以在铺面 JSON 中为每个音符指定移动曲线：

```json
{
  "type": "tap",
  "time": 5.0,
  "face": "w",
  "position": {"x": 0, "y": 0},
  "curve": "EASE_IN_OUT"
}
```

## 颜色代码参考

Minecraft 颜色代码（使用 § 符号）：

- `§0` - 黑色
- `§1` - 深蓝色
- `§2` - 深绿色
- `§3` - 深青色
- `§4` - 深红色
- `§5` - 深紫色
- `§6` - 金色
- `§7` - 灰色
- `§8` - 深灰色
- `§9` - 蓝色
- `§a` - 绿色
- `§b` - 青色
- `§c` - 红色
- `§d` - 粉色
- `§e` - 黄色
- `§f` - 白色

格式代码：
- `§l` - 粗体
- `§m` - 删除线
- `§n` - 下划线
- `§o` - 斜体
- `§r` - 重置

## 示例铺面

完整的示例铺面请参考 `example_chart.json`，其中包含了所有类型的 EXECUTION 音符示例。

## 技术实现

### ExecutionAction 类
提供静态方法执行各种动作：
- `showTitle()` - 显示标题
- `showActionBar()` - 显示 ActionBar
- `sendChatMessage()` - 发送聊天消息
- `givePotionEffect()` - 给予药水效果
- `removePotionEffect()` - 移除药水效果
- `clearAllPotionEffects()` - 清除所有药水效果

### ExecutionHandler 类
处理 EXECUTION 音符的执行逻辑，解析 JSON 配置并调用相应的 ExecutionAction 方法。

### MovementCurve 类
提供各种数学曲线函数，用于计算音符的移动轨迹。

### NoteSpawner 更新
现在会自动分离普通音符和 EXECUTION 音符，在正确的时间触发 EXECUTION 动作。
