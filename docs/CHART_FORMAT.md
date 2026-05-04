# 铺面格式说明

## JSON 格式规范

铺面文件存放于 `plugins/CubeRhythm/{chartId}.json`，格式如下：

```json
{
  "version": "1.0.0",
  "metadata": {
    "id": "simpletone",
    "title": "simpletone",
    "artist": "CRE",
    "charter": "PiraTom",
    "difficulty": {
      "name": "Tutorial 1",
      "color": "&b",
      "level": 1
    },
    "audio": "cr.simpletone",
    "duration": 51,
    "offset": 0,
    "bpm": 130
  },
  "notes": [
    {
      "type": "tap",
      "time": 14.77,
      "face": "w",
      "position": {"x": 1, "y": 0},
      "glowing": false,
      "tag": ""
    }
  ]
}
```

## 音符类型

| 类型 | 颜色 | 触发方式 | 说明 |
|------|------|---------|------|
| tap | 浅蓝 | 单击 | 基础点击音符 |
| hold | 白色 | 按住 | 长按音符，无需对准 |
| drag | 黄色 | 自动（准心） | 准心瞄准自动判定 |
| flick | 品红 | 自动（视角） | 到达时检测视角方向 |
| double | 橙色 | 双击 | 两个位置同时击打 |
| execution | 不可见 | 自动 | 触发预设动作，不计入判定 |

### 各类型字段

**tap / hold / drag:**
```json
{"type": "tap", "time": 1.5, "face": "w", "position": {"x": 0, "y": 0}, "glowing": false, "tag": ""}
```

**flick:**
```json
{"type": "flick", "time": 3.0, "face": "w", "turn": "left", "glowing": false, "tag": ""}
```
- `turn`: `"left"` 或 `"right"`

**double:**
```json
{"type": "double", "time": 2.0, "face": "w", "positions": [{"x": -1, "y": 0}, {"x": 1, "y": 0}], "glowing": false, "tag": ""}
```

**execution:**
```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {"type": "title", "enabled": true, "title": "§fCube Rhythm", "subtitle": "", "fadeIn": 10, "stay": 40, "fadeOut": 10},
    {"type": "actionbar", "enabled": true, "text": "§b注意节奏！"},
    {"type": "chat", "enabled": true, "message": "§a提示"},
    {"type": "potion", "enabled": true, "effectType": "SPEED", "duration": 100, "amplifier": 1, "ambient": false, "particles": true, "icon": true},
    {"type": "remove_potion", "enabled": true, "effectType": "SPEED"},
    {"type": "clear_effects", "enabled": true},
    {"type": "blind", "enabled": true, "duration": 40},
    {
      "type": "draw_line",
      "enabled": true,
      "face": "w",
      "from": {"x": -3, "y": 0},
      "to": {"x": 3, "y": 0},
      "duration": 20
    },
    {
      "type": "draw_text",
      "enabled": true,
      "face": "w",
      "position": {"x": 0, "y": 0},
      "scale": 4,
      "opacity": 255,
      "duration": 40,
      "text": "§f文本"
    },
    {
      "type": "change_glow_color",
      "enabled": true,
      "bind_tag": ["tag1"],
      "duration": 20,
      "color": {"r": 255, "g": 0, "b": 0, "a": 255}
    },
    {
      "type": "hide_note",
      "enabled": true,
      "bind_tag": ["tag1"],
      "end_time": 2.0
    },
    {
      "type": "easing_motion",
      "enabled": true,
      "bind_tag": ["tag1"],
      "segments": [
        {
          "keep": 20,
          "x": [{"name": "sin", "freq": 2, "ampl": 1}],
          "y": [{"name": "1"}],
          "v": [{"name": "expo", "lambda": 0.05}]
        },
        {
          "keep": 20,
          "x": [{"name": "expo", "lambda": 2}],
          "y": [{"name": "1"}]
        }
      ]
    }
  ]
}
```

**execution action 说明：**

| type                | 说明                                 |
|---------------------|------------------------------------|
| `title`             | 显示标题/副标题                           |
| `actionbar`         | 显示 ActionBar                       |
| `chat`              | 发送聊天消息                             |
| `potion`            | 给予药水效果                             |
| `remove_potion`     | 移除药水效果                             |
| `clear_effects`     | 清除所有药水效果                           |
| `blind`             | 失明效果（装饰实体同步发光），`duration` 单位 tick  |
| `draw_line`         | 在判定面绘制线段，随谱面移动，`duration` tick 后消失 |
| `draw_text`         | 在判定面生成文字展示实体，`duration` tick 后消失   |
| `change_glow_color` | 修改指定 tag 音符的发光颜色，`duration` tick   |
| `hide_note`         | 隐藏指定 tag 音符直到 `end_time`（秒）        |
| `easing_motion`     | 对指定 tag 音符施加缓动运动，支持多段拼接            |

**缓动函数（easing_motion 的 name 字段）：**

| name            | 说明     | 参数                                         |
|-----------------|--------|--------------------------------------------|
| `0`             | 不移动    | —                                          |
| `1`             | 线性     | —                                          |
| `2`/`3`/`4`/`5` | N 次方缓动 | —                                          |
| `sin`           | 正弦波    | `freq`（频率），`ampl`（振幅）                      |
| `expo`          | 指数缓动   | `lambda`（λ），速度函数 `speed(t) = 2^(λt)`       |
| `log`           | 对数缓动   | `lambda`（λ），速度函数 `speed(t) = log₂(λt + 1)` |

其中 `t` 为从该段开始执行到当前的时间，单位为 **tick**。

**x/y 方向缓动**：`speed(t)` 直接作为每 tick 的位移增量叠加到音符的横向/纵向坐标上。

**`v` 飞行方向缓动（垂直判定面方向）**：`speed(t)` 表示音符在飞行方向上的速度函数。音符的生成时机通过积分反推计算。

设音符从生成位置到判定面的距离为 `d`，则求解到达判定面所需的 tick 数 `T`：

- `expo`：`∫₀ᵀ 2^(λt) dt = (2^(λT) − 1) / (λ·ln2) = d`，解为 `T = log₂(d·λ·ln2 + 1) / λ`
- `log`：`∫₀ᵀ log₂(λt+1) dt = [(λT+1)·log₂(λT+1) - (λT+1)/ln2 + 1/ln2] / λ = d`，通过数值二分法求解

解出 `T` 后，音符的实际生成时刻 = `noteTime - T/20`（秒），从而保证音符以变速运动恰好在 `noteTime` 到达判定面。

坐标系统：统一以 W 面为基准（X- 向左，X+ 向右，Y+ 向上，Y- 向下），运行时根据音符所在面自动旋转。

## 判定面

| 代码 | 方向 | 旋转角度 | 标记颜色 |
|----|----|------|------|
| w  | 前  | 0°   | 白色   |
| a  | 左  | 90°  | 黄色   |
| s  | 后  | 180° | 橙色   |
| d  | 右  | 270° | 红色   |

坐标范围：x ∈ [-3, 3]，y ∈ [-3, 3]，原点在面中心。

## 音符移动曲线

`MovementCurve` 类支持以下曲线类型（用于 EXECUTION 音符的特效）：
`LINEAR`、`EASE_IN`、`EASE_OUT`、`EASE_IN_OUT`、`SINE`、`EXPONENTIAL`、`BOUNCE`、`ELASTIC`

## Skript 格式（遗留）

旧版铺面使用两个 `.sk` 文件：
- `{name}_properties.sk`：元数据（registerChart、setBPM）
- `-{name}.sk`：音符数据（tap/hold/drag/flick/double/execution 函数调用）

已提供 Python 转换脚本 `convert_charts.py` 将 Skript 格式批量转换为 JSON。

颜色代码映射：Skript 使用 `&`，JSON 使用 `§`（如 `&b` → `§b`）。

## 加载流程（Java 系统）

1. 插件启动时 `ChartRegistry` 扫描 `plugins/CubeRhythm/*.json`
2. `AsyncChartLoader` 异步加载，回调在主线程执行
3. 加载完成后注册到 `ChartRegistry`，可通过 `/gui` 或 `/play` 访问
