# 游戏系统说明

## 判定系统

### 判定窗口（非对称）

| 时间范围           | 结果        |
|----------------|-----------|
| < -80ms        | 不处理（避免误触） |
| -80ms ~ +80ms  | EXACT     |
| +81ms ~ +200ms | JUST      |
| > +200ms       | MISS      |

FLICK 有特殊判定逻辑，不适用上表。

### 各音符判定方式

| 类型        | 触发        | JUST             | 音效 |
|-----------|-----------|------------------|----|
| TAP       | 点击        | ✅                | ✅  |
| DOUBLE    | 双击（0.5s内） | ✅（任一JUST则整体JUST） | ✅  |
| DRAG      | 自动（准心瞄准）  | ❌（转EXACT）        | ✅  |
| HOLD      | 自动（按键按下）  | ❌（转EXACT）        | ❌  |
| FLICK     | 自动（视角方向）  | ❌                | ✅  |
| EXECUTION | 不判定       | -                | -  |

### FLICK 方向判定

音符到达判定线时，检测玩家视角是否在目标方向 ±45° 范围内：

```
W面向左(turn="left")：玩家yaw需在 45°~135° → EXACT，否则 MISS
W面向右(turn="right")：玩家yaw需在 -135°~-45° → EXACT，否则 MISS
```

各面基准 yaw：W=0°，A=90°，S=180°，D=-90°

### 分数计算

```
Base Score = 1,000,000 / totalNotes × hitNotes
EXACT 倍率: 1.0 | JUST 倍率: 0.7 | MISS 倍率: 0.0
```

**评级**（ResultScreen）：

| 分数        | 评级   |
|-----------|------|
| 1,000,000 | SSS+ |
| 990,000+  | SSS  |
| 980,000+  | SS   |
| 960,000+  | S    |
| 950,000+  | AAA  |
| 940,000+  | AA   |
| 930,000+  | A    |
| 920,000+  | BBB  |
| 910,000+  | BB   |
| 900,000+  | B    |
| 850,000+  | C    |
| <850,000  | D    |

**特殊成就**：Perfect Play（全EXACT）、Full Combo（无MISS）

---

## HUD 系统（GameHUD）

游戏中在所有4个判定面显示相同内容：

```
自动播放（绿色，仅autoPlay时显示）
Score [分数]          Combo [连击数]
                                    （连击颜色：金=全Perfect，青=Full Combo，白=有Miss）
[难度名称 Lv.X]       [歌曲名]
```

- TextDisplay 实体，距中心 4.5 blocks，1.125x 缩放
- 每 tick 更新分数和连击数
- 游戏结束时自动清理

---

## 结算界面（ResultScreen）

游戏结束后在正面（W面）显示：
- 评级、准确度、分数、难度
- EXACT/JUST/MISS 详细统计
- 成就动画（30 tick 浮动 + 20 tick 停留后消失）
- 播放对应音效

---

## 移动限制（MovementRestriction）

游戏 PLAYING 状态时：
- 阻止玩家位置（x/y/z）变化
- 允许视角旋转（yaw/pitch）
- 警告消息冷却 2 秒防刷屏
- 优先级 `EventPriority.HIGHEST`

---

## 性能优化

### 实体池（EntityPool）
复用 BlockDisplay、Interaction、TextDisplay 实体，每种最多 200 个。

```java
BlockDisplay d = pool.getBlockDisplay(location);
// 使用后归还
pool.returnBlockDisplay(d);
```

### 视锥剔除（ViewFrustumCuller）
只渲染玩家视野内（水平 90° FOV，最远 60 blocks）的音符。

```java
if (ViewFrustumCuller.isInViewFrustum(player, noteLocation)) { /* 渲染 */ }
```

### 异步谱面加载（AsyncChartLoader）
后台线程加载，回调在主线程执行：

```java
AsyncChartLoader.loadChartAsync(file,
    chart -> registry.addChart(chart),
    err -> logger.severe(err.getMessage())
);
```

---

## 坐标系统

判定面世界坐标（以游戏中心为原点，`z` 为距判定线距离）：

| 面    | 世界坐标映射                                          | 朝内 yaw |
|------|--------------------------------------------------|---------|
| W（前） | `(centerX - x, centerY + y, centerZ + z)`        | 180°    |
| A（左） | `(centerX + z, centerY + y, centerZ + x)`        | 90°     |
| S（后） | `(centerX + x, centerY + y, centerZ - z)`        | 0°      |
| D（右） | `(centerX - z, centerY + y, centerZ - x)`        | 270°    |

判定线距离 = 4 blocks，音符距离公式：
```
distance = speed × 20 × (noteTime + 1.0 - currentTime) + 4
```
（`+1.0` 为前奏缓冲，避免无前奏谱面音符直接跳出）

### 各渲染元素坐标偏移

| 元素              | 局部 x 偏移       | 局部 y 偏移       | 距离 z     |
|-----------------|----------------|----------------|----------|
| 音符 BlockDisplay | `pos.x + 0.5`  | `pos.y + 1.1`  | 实时距离     |
| 碰撞箱 Interaction | `pos.x + 0.5`  | `pos.y + 1.1`  | 实时距离     |
| 落点光标 TextDisplay | `pos.x + 1.0`  | `pos.y + 1.6`  | 固定 4.0   |
| FLICK BlockDisplay | `0`（面中心）     | `0`（面中心）     | 实时距离     |
| 判定文字/光晕 TextDisplay | 音符击中时位置 +1Y | —              | 音符击中时位置 |

FLICK 的 BlockDisplay 通过 translation 居中：`(-2.5, -2.5, -0.5)`，scale = `(5, 5, 1)`。

HOLD 的 scaleZ = `(60 / bpm) × speed × 5.0`（一拍长度）。

光标 TextDisplay 使用 `Billboard.FIXED`，yaw 见上表"朝内 yaw"列。
