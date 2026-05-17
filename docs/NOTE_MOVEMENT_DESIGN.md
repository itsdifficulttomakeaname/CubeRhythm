# 音符轨迹与事件系统设计

> 状态：设计提案 v0.2  
> 目标版本：CubeRhythm Chart Format 2.0.0  
> 撰写日期：2026-05-16

由于现版本几乎还没有正式铺面产出，本设计直接替换现行 `easing_motion`，不保留向前兼容；游戏内编辑器已废弃，本设计也不为其留接入点。

---

## 1. 现状的问题

当前缓动通过 `execution` 类型音符在某时间点向带相同 `tag` 的目标广播：

```json
{ "type": "execution", "time": 5.0, "actions": [
  { "type": "easing_motion", "bind_tag": ["chorus"],
    "segments": [{ "keep": 20, "x": [{"name":"sin","freq":2,"ampl":1}] }] }
]}
```

四个主要痛点：

- **反向绑定**：要给一个音符加轨迹，得另写一个 execution 音符并维护 tag 同步。
- **速度语义而非位置**：`x: [{name:"sin"}]` 描述的是每 tick 的**位移增量**，不是位置。编谱者要心算积分才能预想出轨迹形状。
- **视觉通道空白**：alpha、缩放、旋转、颜色都没法驱动。
- **不可复用**：同一形状要复制多份 segments。

---

## 2. 设计思路

主要参考 Phigros / RPE 的"事件流（event list）"模型：每个属性是一条独立的时间轴，由若干**关键帧（keyframe）**和**缓动函数（easing function）**组成。

> **关键帧**：在时间轴上某一时刻给属性定一个目标值。
> **缓动函数**：连接相邻两个关键帧的"形状"，比如线性（匀速）、二次方（先慢后快）、正弦、回弹等。

CubeRhythm 的玩法几何（玩家在立方体中心、四面固定）不允许像 Phigros 那样自由移动判定线，所以事件直接挂在**音符**或**音符组**上，不引入 JudgeLine 概念。

---

## 3. 数据结构

```
Chart
├─ metadata
├─ groupEvents     // 群组事件：选择器命中一批音符并施加事件
└─ notes
   └─ note.events  // 单音符事件
```

两个层级，按"广 → 窄"叠加：

| 层级            | 作用域        | 用途              |
|---------------|------------|-----------------|
| `groupEvents` | 选择器命中的所有音符 | 整面下沉、整体淡入等场面性效果 |
| `note.events` | 仅当前音符      | 单点演出特例          |

**叠加规则**：同一通道（例如 `x`）若两层都有，各层独立求值后相加；`scale` 类按相乘。

---

## 4. 时间表达

事件帧的时间字段二选一：

```jsonc
{ "time": 12.5, "value": 1.0 }   // 绝对秒（chart 起点 = 0）
{ "rtime": -0.5, "value": 1.0 }  // 相对音符 noteTime 的秒（负=之前，0=到达判定面）
```

默认值：
- `note.events` 默认 `rtime`（音符自己最关心"距离判定还有多久"）
- `groupEvents` 默认 `time`（一段场面演出有固定的开始结束秒数）

> 不支持"按拍"时间。BPM 变化暂未列入需求；如未来需要再加。

---

## 5. 通道（事件类型）

每个通道是一条独立的属性时间轴。

### 5.1 位置通道

| 通道  | 含义                            | 单位    | 默认 |
|-----|-------------------------------|-------|----|
| `x` | 横向偏移（叠加到 `note.position.x` 上） | block | 0  |
| `y` | 纵向偏移                          | block | 0  |
| `z` | 飞行方向偏移（**正值=远离判定面**）          | block | 0  |

直接给位置而不是速度。例如下面这条曲线让音符从 `x=-3` 沿正弦缓动到 `x=0`：

```jsonc
"x": [
  { "rtime": -1.5, "value": -3, "easing": "sineInOut" },
  { "rtime":  0.0, "value":  0 }
]
```

### 5.2 视觉通道

| 通道                             | 含义                         | 单位    | 默认       |
|--------------------------------|----------------------------|-------|----------|
| `alpha`                        | 不透明度                       | 0–1   | 1        |
| `scale`                        | 整体缩放                       | ×     | 1        |
| `scaleX` / `scaleY` / `scaleZ` | 单轴缩放（覆盖 `scale`）           | ×     | 1        |
| `rotate`                       | 绕飞行方向自转                    | 度     | 0        |
| `colorR` / `colorG` / `colorB` | 发光颜色（仅 `glowing=true` 时生效） | 0–255 | face 默认色 |
| `colorA`                       | 发光颜色 alpha                 | 0–255 | 255      |

---

## 6. 关键帧

```jsonc
{
  "rtime": -0.5,
  "value": 0,
  "easing": "sineOut"  // 从上一帧到本帧的形状；首帧此字段无意义
}
```

求值规则：
1. t 在第一帧之前 → 取首帧 value（保持不变）。
2. t 在最后一帧之后 → 取末帧 value（保持不变）。
3. 否则在所属段内：`progress = (t - kf_i.t) / (kf_{i+1}.t - kf_i.t)`，先把 progress 喂给缓动得到 `eased`，再线性插值出 value：`kf_i.value + eased × (kf_{i+1}.value - kf_i.value)`。

### 缓动函数集

精简到一组够用的：

| 类别  | 名称                                |
|-----|-----------------------------------|
| 基础  | `linear`（匀速）, `hold`（无插值，保持上一帧值）  |
| 多项式 | `quadIn/Out/InOut`（二次方：先慢/先快/两端慢） |
| 立方  | `cubicIn/Out/InOut`               |
| 三角  | `sineIn/Out/InOut`                |
| 指数  | `expoIn/Out/InOut`                |
| 回弹  | `backOut`（带轻微 overshoot）          |

> `hold` 用于颜色等需要"瞬间跳变"的离散场景，例如：
> ```jsonc
> { "time": 2.0, "value": 0, "easing": "hold" },
> { "time": 2.0001, "value": 1 }
> ```

实现位置：把现有 `MovementCurve` 重写为 `Easing`，统一入口 `Easing.eval(name, t01)`。

---

## 7. 选择器

`groupEvents` 用结构化选择器命中音符，取代 tag 单向广播：

```jsonc
{
  "selector": {
    "face": "w",                  // 单值或数组
    "type": ["tap", "drag"],
    "tag": "chorus",
    "timeRange": [10.0, 14.0]     // noteTime 落在该闭区间
  },
  "events": { ... }
}
```

- 字段间是"与"关系，单字段内数组是"或"关系。
- `tag` 仍作为可选的选择器字段保留，给编谱者命名分组用。
- 命中是**静态预计算**：加载 chart 时一次性算出每个音符要叠加哪些 groupEvents，存到 `NoteEntity` 上，运行时不再过滤。

---

## 8. 完整示例

### 8.1 单音符抛物线进入

```jsonc
{
  "type": "tap",
  "time": 8.0,
  "face": "w",
  "position": { "x": 0, "y": 0 },
  "events": {
    "x": [
      { "rtime": -1.5, "value": -4, "easing": "linear" },
      { "rtime":  0.0, "value":  0 }
    ],
    "y": [
      { "rtime": -1.5, "value":  3, "easing": "quadOut" },
      { "rtime": -0.75,"value": -1, "easing": "quadIn" },
      { "rtime":  0.0, "value":  0 }
    ],
    "alpha": [
      { "rtime": -1.5, "value": 0, "easing": "linear" },
      { "rtime": -1.2, "value": 1 }
    ]
  }
}
```

效果：到达前 1.5 秒从左上 (−4, 3) 出现，先以"先快后慢"曲线下降到 (−4, −1)，再以"先慢后快"上扬到判定面正中心 (0, 0)；同时在前 0.3 秒内淡入。

### 8.2 整面波浪下沉

```jsonc
"groupEvents": [
  {
    "selector": { "face": "w", "type": "tap" },
    "events": {
      "y": [
        { "time": 20.0, "value":  0, "easing": "sineInOut" },
        { "time": 21.0, "value":  1, "easing": "sineInOut" },
        { "time": 22.0, "value": -1, "easing": "sineInOut" },
        { "time": 23.0, "value":  0 }
      ]
    }
  }
]
```

20–23 秒内，W 面所有 TAP 整体上下波动一次。

---

## 9. 实现要点

### 9.1 数据模型（Java 侧）

```java
package org.cubeRhythm.note.event;

public class Keyframe {
    double time;          // 标准化为绝对秒（rtime 在解析时已转换）
    double value;
    EasingType easing;    // 从上一帧过来的形状
}

public class EventTrack {
    Map<Channel, List<Keyframe>> channels;
}

public enum Channel {
    X, Y, Z,
    ALPHA, SCALE, SCALE_X, SCALE_Y, SCALE_Z, ROTATE,
    COLOR_R, COLOR_G, COLOR_B, COLOR_A
}

public class GroupEvent {
    Selector selector;
    EventTrack events;
}
```

`NoteEntity` 上：
- 删除现有的 `xOffset / yOffset / easingType / easingLambda / easingStartTime / easingStartDistance`
- 新增 `List<EventTrack> matchedTracks`（自身 events + 命中的 groupEvents 拼起来）

### 9.2 求值流水线（每 tick）

```
for entity in entityManager:
    t = currentTime
    rt = currentTime - entity.noteTime
    
    # 通道独立求值
    for ch in channels:
        v = (ch is scale*) ? 1.0 : 0.0
        for track in entity.matchedTracks:
            kfs = track.channels[ch]
            if kfs == null: continue
            sampled = sample(kfs, ch is scale ? : ...)   # 按 track 的时间基准
            v += sampled    # scale* 通道改成 v *= sampled
        apply(entity, ch, v)
    
    NoteRenderer.updateNote(entity, ..., distance, ...)
```

`sample(kfs, t)` 用二分查找定位段，O(log N)。

### 9.3 性能预算

- 同屏 100 音符上限不变。
- 平均通道数 ≤ 4，平均关键帧数 ≤ 8 → 单 tick 几千次比较，无压力。
- 加载期一次性给每个音符预筛 `matchedTracks`，运行时不再扫 `groupEvents`。

### 9.4 模块改动一览

| 现有模块                                                   | 改动                                                                                                                 |
|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `org.cubeRhythm.note.MovementCurve`                    | 重写为 `org.cubeRhythm.note.event.Easing`，扩充缓动名                                                                       |
| `org.cubeRhythm.note.EasingMotionManager`              | 删除                                                                                                                 |
| `org.cubeRhythm.note.execution.*`（`easing_motion` 子类型） | 移除该 action 类型；其他 action（draw_line / hide_note 等）保留                                                                 |
| `org.cubeRhythm.note.NoteEntity`                       | 删 6 个 easing_* 字段；加 `matchedTracks`                                                                                |
| `org.cubeRhythm.note.NoteRenderer`                     | `updateNote` 调用前先用 `TrackEvaluator` 求值出 (xΔ, yΔ, zΔ, alpha, scale, rotate, color*)，再传入渲染；删去现有 `xOffset/yOffset` 链路 |
| `org.cubeRhythm.chart.ChartLoader`                     | 解析新增的 `events` / `groupEvents` 字段                                                                                  |
| `Chart`                                                | 加 `List<GroupEvent> groupEvents`                                                                                   |

---

## 10. 渐进实施

| 阶段 | 内容                                                      |
|----|---------------------------------------------------------|
| P0 | `Easing` 求值器 + `Keyframe` / `EventTrack` 数据模型 + JSON 解析 |
| P1 | 单音符 `events` 全通道生效，`NoteRenderer` 适配                    |
| P2 | `groupEvents` + 选择器，加载期预筛                               |
| P3 | 删除旧 `EasingMotionManager` 与 `easing_motion` execution   |
| P4 | 性能优化：评估器结果缓存、与视锥裁剪协同                                    |
| P5 | 重写 `CHART_FORMAT.md`、`EDITOR.md`、写 5+ 示例谱面              |

预计 7–8 个工作日。

---

## 11. 待定

1. `groupEvents` 是否需要"优先级"字段（用于显式控制叠加顺序）？  
   顺序读入、应用事件
2. 是否在视觉通道里加 `material`（离散切换显示方块）？  
   可以加上，读取为空时使用默认材质
3. `rotate` 的旋转轴用"飞行方向"还是"判定面法线"？  
   飞行方向，对玩家视角更自然。
4. 考虑应用实体池优化性能，通过复用音符实体来减小内存和一定的渲染开支，如优化意义 < 重构难度，则忽略此条
5. 如可以，使用数学方法优化路径求值，压缩二分次数
6. 由于Minecraft本身存在时间步长限制，可能会对于精细事件操作有限制作用，尝试找一些方案能够绕开这个限制(假定你是每tick更新)，并且要有开发可能性
