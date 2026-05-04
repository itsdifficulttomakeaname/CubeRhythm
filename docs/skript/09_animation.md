# 09 动画与视觉特效

本文档整理自原始 Skript 实现（`judge.sk`、`main.sk`、`result.sk`、`load.sk`、`api.sk`），描述 CubeRhythm 的所有视觉特效逻辑。坐标均以游戏中心为原点。

---

## 一、音符渲染

### 1.1 各音符类型的 BlockDisplay 参数
> 已实现

| 类型     | 方块                         | Scale (x,y,z)       | 发光颜色 (ARGB)               |
|--------|----------------------------|---------------------|---------------------------|
| TAP    | `light_blue_concrete`      | `(1, 1, 0.5×speed)` | `(255, 0, 50, 255)` 蓝色    |
| HOLD   | `white_concrete`           | `(1, 1, 3×speed)`   | `(255, 255, 255, 255)` 白色 |
| DRAG   | `yellow_concrete`          | `(1, 1, 0.5×speed)` | `(255, 255, 235, 42)` 黄绿色 |
| FLICK  | `white_stained_glass_pane` | `(5, 5, 1)`         | —                         |
| DOUBLE | `orange_concrete`          | `(1, 1, 0.5×speed)` | `(255, 255, 180, 0)` 橙黄色  |

所有音符：
- `interpolation start: 0`，`interpolation duration: 100`
- `brightness: displayBrightness(15, 15)`
- 每 tick 通过 `translation` 向判定面方向移动：`vector(0, 0, -100×speed)`（w/s 面），`vector(0, 0, 100×speed)`（a/d 面）

### 1.2 FLICK 音符附加箭头
> 现在的设计中箭头的存在不太自然，需要按照获取到的原设计进行修正

```
TextDisplay（方向箭头）:
  text: "§f←" 或 "§f→"
  scale: vector(20, 20, 0)
  background: bukkitColor(0, 0, 0, 0)
  metadata "linkbind": 关联 BlockDisplay uuid
  位置偏移（w 面）: (x+0.75, y-2.75, z+0.4)
```

### 1.3 DOUBLE 音符连接线
> 需实现

两个 BlockDisplay 之间生成一条细线：
```
block: white_concrete
scale: vector(0.1, 两点距离, 0.1)
pitch: acos(|xa-xb| / distance) - 90
发光颜色: (255, 255, 255, 255)
```

---

## 二、落点指示器（Anchor）
> 已实现

每 tick 遍历所有 Interaction 实体，当音符距判定面 < 25 格时，在判定面上生成 TextDisplay 作为落点指示。

### 2.1 透明度与缩放计算

```
alpha = clamp(100 - (距判定面距离 - 4) × 4, 1, 100)
scale = alpha / 25   // 范围 0~4
```

### 2.2 各判定面 Anchor 坐标

| 判定面  | 坐标（相对中心）               | Yaw  |
|------|------------------------|------|
| w（北） | `(noteX, noteY+1, 5)`  | 180° |
| a（西） | `(5, noteY+1, noteZ)`  | 90°  |
| s（南） | `(noteX, noteY+1, -4)` | 0°   |
| d（东） | `(-4, noteY+1, noteZ)` | -90° |

### 2.3 TextDisplay 参数

```
text: "§f{颜色}█"（alpha > 10），"§f"（alpha ≤ 10）
background: bukkitColor(0, 0, 0, 0)
opacity: alpha × 2（最大 200）
scale: vector(scale, scale, scale)
translation: vector(scale×-0.015, scale×-0.15, 0)
metadata "anchor": 1
metadata "belongsto": 音符 uuid
```

颜色对应：TAP → `§3`（深青），DRAG → `§e`（黄），DOUBLE → `§6`（金），FLICK → 不生成 anchor。

---

## 三、小节提示线（Beat Line）
> 弃用

每隔 2 拍在四个判定面生成一条白色细线。

### 3.1 生成条件

`lineDistance < 50` 且 `(beat - 1) mod 2 == 0`

### 3.2 drawLine 参数

```
位置: x = ±4.5，y = -4，z = lineDistance
pitch: 0（竖直）
length: 9
颜色: RGB(255, 255, 255, 255) 白色

BlockDisplay:
  block: white_concrete
  scale: vector(0.05, length, 0.05)
  interpolation duration: 100
  brightness: displayBrightness(15, 15)
  translation: vector(-100×speed, 0, 0)  // 随谱面移动
```

持续时间 = `floor((lineDistance - 4) / speed)` ticks，到期自动删除。

---

## 四、击中特效
> 需实现

特效位置计算（以音符位置为基础，偏移到判定面处）：

```
face=w: loc.add(0, 0, -noteZ + 5)
face=a: loc.add(-noteX + 5, 0, 0)
face=s: loc.add(0, 0, -noteZ - 4)
face=d: loc.add(-noteX - 4, 0, 0)
loc.add(0, 1, 0)  // 上移 1 格
```

### 4.1 Exact 特效（偏差 ≤ 80ms）

生成两个 TextDisplay，动画持续 10 tick：

**文字实体：**
```
text: "&bExact"（或 "&b+Xms" 偏移值）
scale: vector(3, 3, 3) → 每 tick ×0.9 缩小，最小 1
translation: vector(-0.045, -0.45, 0)（随 scale 同步）
opacity: 255 → 每 tick -24
background: bukkitColor(0, 0, 0, 0)
shadow strength: 0
```

**光晕实体：**
```
text: "&b█"
scale: 从 8 开始，每 tick 乘以衰减系数 0.7（向外扩散）
opacity: alpha/4（alpha > 40 时），否则 14
background: bukkitColor(0, 0, 0, 0)
```

声音：`entity.player.hurt`，pitch=2（hitSound 开启时）

### 4.2 Just 特效（偏差 80~200ms）

与 Exact 相同结构，区别：
- 颜色：`&e`（黄色）
- 文字：`"&eJust"`
- 光晕衰减系数：`0.5`（扩散更快）
- ActionBar：Early → `"&bEarly"`，Late → `"&6Late"`

### 4.3 Miss 特效

```
combo 清零
subtitle: "&4Miss"，持续 0.5s，淡入 0.1s，淡出 0.3s
```
无粒子，无声音。

---

## 五、游戏中 HUD

HUD 由 `createComboDisplay()` 初始化，在四个判定面各放置一组 TextDisplay。

### 5.1 Combo 数字（四面）

| 面 | 坐标               | Yaw  |
|---|------------------|------|
| w | `(0.5, -8, 50)`  | 180° |
| a | `(50, -8, 0.5)`  | 90°  |
| s | `(0.5, -8, -49)` | 0°   |
| d | `(-49, -8, 0.5)` | -90° |

```
scale: vector(50, 50, 0)
background: bukkitColor(0, 0, 0, 0)
brightness: displayBrightness(0, 2)
metadata "combo": true
```

### 5.2 分数显示（四面）

| 面 | 坐标                | Yaw  |
|---|-------------------|------|
| w | `(2.5, 3.2, 5)`   | 180° |
| a | `(5, 3.2, -1.5)`  | 90°  |
| s | `(-1.5, 3.2, -4)` | 0°   |
| d | `(-4, 3.2, 2.5)`  | -90° |

```
scale: vector(2, 2, 0)
alignment: left aligned
opacity: 127（基础），击中音符时短暂高亮
metadata "score": true
```

分数透明度动画：击中时 `alphaTime = 11`，每 tick -1，`opacity = -127 + alphaTime × 12.6`。

### 5.3 难度等级显示（四面）

| 面 | 坐标                | Yaw  |
|---|-------------------|------|
| w | `(-1.5, 3.2, 5)`  | 180° |
| a | `(5, 3.2, 2.5)`   | 90°  |
| s | `(2.5, 3.2, -4)`  | 0°   |
| d | `(-4, 3.2, -1.5)` | -90° |

```
scale: vector(2, 2, 0)
alignment: left aligned
metadata "level": true
```

### 5.4 曲目信息显示（四面）

| 面 | 坐标                | Yaw  |
|---|-------------------|------|
| w | `(0.5, -2.8, 5)`  | 180° |
| a | `(5, -2.8, 0.5)`  | 90°  |
| s | `(0.5, -2.8, -4)` | 0°   |
| d | `(-4, -2.8, 0.5)` | -90° |

```
line width: 400
scale: vector(0.8, 0.8, 0)
background: bukkitColor(0, 0, 0, 0)
metadata "soundinfo": true
```

### 5.5 Combo 颜色逻辑

每 tick 根据状态更新颜色：
- 无 miss 且无 just → `§e` 黄色（Perfect 状态）
- 无 miss 但有 just → `§b` 青色（Full Combo 状态）
- 有 miss → `§f` 白色

---

## 六、结算界面

### 6.1 过渡动画（end()）

1. `wait 30 ticks`（1.5 秒）
2. 进度条归零
3. **30 tick 上升动画**：
   - score/level 实体：每 tick 上移 0.01 格
   - combo/perfect 实体：每 tick 上移 0.05 格
4. 删除所有 TextDisplay
5. `wait 5 ticks`
6. 依次调用 `result()`、`result2()`、`result3()`、`result4()`

### 6.2 Full Combo / Perfect Performance 特效

**位置：** `(0.5, 0.5, 10)`，Yaw=-90°，Pitch=90°

```
TextDisplay:
  text: "§bFull Combo" 或 "§ePerfect          \n    Performance!"
  scale: vector(40, 40, 40)
  background: bukkitColor(0, 0, 0, 0)
  right rotation: axisAngle(90°, 0, 1, 0)（绕 Y 轴旋转 90°）
  translation: vector(0, -1.5, 0)
  brightness: displayBrightness(15, 15)
  metadata "perfect": true
```

**弹入动画（30 tick）：**
```
初始: angle=90°, del=22, size=40
每 tick:
  del    *= 0.8
  size   *= 0.7
  angle  -= del
  pitch   = angle（从 90° 弹到约 0°）
  scale   = vector(size+5, size+5, size+5)
  translation = vector(0, -1.5×(size+5)×0.1, 0)
```

### 6.3 评级显示（result()）

**起始位置：** `(-3.5, 0.5, 12)`，Yaw=180°
**终止位置：** `(5.5, 0.5, 12)`，Yaw=180°

```
TextDisplay:
  scale: vector(10, 10, 10)
  translation: vector(-0.2, -1, 0)
  background: bukkitColor(0, 0, 0, 0)
  brightness: displayBrightness(15, 15)
  opacity: 255
  metadata "result": true
```

动画：`easingMotion(entity, 起始, 终止, "cirb", 25 ticks)`（easeOutCirc，从左侧滑入）

60 tick 内持续更新文字为 `getRank(displayScore)`。

**评级对照表：**

| 分数        | 评级   | 颜色   |
|-----------|------|------|
| 1,000,000 | SSS+ | `§e` |
| ≥990,000  | SSS  | `§e` |
| ≥980,000  | SS   | `§e` |
| ≥960,000  | S    | `§e` |
| ≥950,000  | AAA  | `§a` |
| ≥940,000  | AA   | `§a` |
| ≥930,000  | A    | `§a` |
| ≥920,000  | BBB  | `§b` |
| ≥910,000  | BB   | `§b` |
| ≥900,000  | B    | `§b` |
| ≥850,000  | C    | `§2` |
| <850,000  | D    | `§7` |

### 6.4 分数滚动动画（result2()）

**起始位置：** `(-7, 2, 10)`，Yaw=180°
**终止位置：** `(-1.5, 2, 10)`，Yaw=180°

```
TextDisplay:
  scale: vector(8, 8, 8)
  translation: vector(0, -1.5, 0)
  background: bukkitColor(0, 0, 0, 0)
  brightness: displayBrightness(15, 15)
  opacity: 255
```

动画：`easingMotion(..., "cirb", 30 ticks)`

**分数滚动逻辑（60 tick）：**
```
初始: tempScore = score × 0.75
每 tick:
  tempScore    *= 0.75（指数衰减）
  displayScore  = round(score - tempScore)（从 0 快速增长到 score）
```

**分数显示格式（补前导零，颜色 `§f` 白色，前导零 `§8` 灰色）：**
```
1,000,000 → "{color}{score}"
≥100,000  → "§80{color}{score}"
≥10,000   → "§800{color}{score}"
≥1,000    → "§8000{color}{score}"
≥100      → "§80000{color}{score}"
≥10       → "§800000{color}{score}"
else      → "§8000000{color}{score}"
```

### 6.5 统计数据显示（result3()）

**起始位置：** `(-5.5, 2, 10)`，Yaw=180°
**终止位置：** `(0, 2, 10)`，Yaw=180°

```
TextDisplay:
  text: "&bExact &f{exact}&e(+{exacthold})\n&eJust &f{just}\n&4Miss &c{miss}\n\n&7输入 [/reset] 重置游戏"
  scale: vector(2, 2, 2)
  translation: vector(0, -4.5, 0)
  line width: 400
  alignment: left aligned
  background: bukkitColor(0, 0, 0, 0)
  opacity: 14（初始）→ 每 tick +25.5，30 tick 后达到 255
```

动画：`easingMotion(..., "cirb", 30 ticks)` + 同步淡入

### 6.6 曲目标题显示（result4()）

位置根据标题长度动态计算：
```
length = len("§f{song}§f {level}") × 0.12
起始: (-2.9 - length, 3, 10)，Yaw=180°
终止: (2.6 - length, 3, 10)，Yaw=180°
```

```
TextDisplay:
  text: "§f{song}§f {level}"
  scale: vector(3, 3, 3)
  line width: 400
  alignment: left aligned
  background: bukkitColor(0, 0, 0, 0)
  brightness: displayBrightness(15, 15)
  opacity: 255
```

动画：`easingMotion(..., "cirb", 30 ticks)`
