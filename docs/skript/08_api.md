# 08 API 工具库

## pasteSchematic(id, loc)
> 在某处声明了结构相关的无需实现

粘贴结构文件，先粘贴 `empty.schem` 清空区域，再粘贴目标结构：
```skript
paste schematic "plugins/Skript/schematics/empty.schem" at loc
wait 1 tick
paste schematic "plugins/Skript/schematics/{id}.schem" at loc
```

---

## drawLine(face, x, y, z, pitch, length, duration, glowing, gc1-4)
> 可以添加到 execution 音符中

```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {
      "type": "draw_line",
      "enabled": true,
      "from": {
        "x": 1,
        "y": 1
      },
      "to": {
        "x": -1,
        "y": -1
      }
    }
  ]
}
```

在判定面上生成一条线段（BlockDisplay），随谱面移动，到时间后删除。

- scale: `(0.05, length, 0.05)`（细长条）
- pitch: 旋转角度（0=竖直，90=水平）
- 通过 translation 实现随音符同步移动
- `duration` tick 后自动删除

`drawLineNoTranslation` 是不加 translation 的版本（静止线段）。

---

## blind(duration)
> 已经添加到 execution 音符中了
给玩家施加失明效果，同时让所有装饰性 BlockDisplay 发光（提升可见度）：
```skript
apply blindness to {actor} for 1 hour
loop all block displays where decoration=true: set glowing to true
wait duration
remove blindness
loop all block displays where decoration=true: set glowing to false
```

---

## forceExact(ticks, tag)
> 弃用

在指定时间内，将特定 tag 的音符标记为 `forceexact=true`，使其在 Just 窗口内也判定为 Exact：
```skript
loop ticks times:
    loop all interactions where tag matches: set forceexact to true
    wait 1 tick
loop all interactions where tag matches: delete forceexact
```

---

## setTextDisplay(face, x, y, z, pitch, scale, opacity, duration, text)
> 可以添加到 execution 音符中

```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {
      "type": "draw_text",
      "enabled": true,
      "position": {
        "x": 0,
        "y": 0
      },
      "scale": 20,
      "opacity": 50,
      "duration": 20, // 单位为 tick
      "text": "文本内容"
    }
  ]
}
```

在指定判定面坐标生成文字展示实体，持续一段时间后删除。坐标自动按面转换（以 W 面为基准输入）。

---

## changeGlowingColor(tag, opacity, r, g, b, duration)
> 可以添加到 execution 中

```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {
      "type": "change_glow_color",
      "enabled": true,
      "bind_tag": [
        "tag1",
        "tag2"
      ],
      // 对含有指定标签(包含于此列表的)的NOTE生效 
      "color": {
        "r": 255,
        "g": 255,
        "b": 255,
        "a": 0
      }
      // rgba 颜色
    }
  ]
}
```

在指定时间内持续更改特定 tag 音符的发光颜色。

## hideNote(tag, duration)
> 可以添加到 execution 音符中

```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {
      "type": "draw_text",
      "enabled": true,
      "end_time": 1.5,
      // 在第1.5s后解除隐藏
      "bing_tag": [
        "tag1", 
        "tag2"
      ]
      // 对含有指定标签(包含于此列表的)的NOTE生效 
    }
  ]
}
```

在指定时间内持续删除特定 tag 的 BlockDisplay（实现隐藏音符效果）。

---

## easingMotion(entity, from, to, Xtype, Ytype, Ztype, Ptype, duration)
> 可以添加到 execution 中

```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {
      "type": "draw_text",
      "enabled": true,
      "actions": [ // 支持多段拼接
        {
          "keep": 10, // 持续 10 tick
          "x": [
            // 支持顺序嵌套 即假设有 对x有a,b,c三个缓动函数 则x <- c(b(a(x)))
            {
              "name": "缓动函数名称，以下文calcEasingValue定义为准"
              // 对某些函数 需要提供以下参数
            },
            {
              "name": "sin",
              "freq": "6",
              "ampl": "2"
            }
          ],
          "y": [
            {
              "name": "1"
            }
          ]
        },
        {
          "keep": 10, // 持续 10 tick
          "x": [
            // 支持顺序嵌套 即假设有 对x有a,b,c三个缓动函数 则x <- c(b(a(x)))
            {
              "name": "缓动函数名称，以下文calcEasingValue定义为准"
              // 对某些函数 需要提供以下参数
            },
            {
              "name": "sin",
              "freq": "6",
              "ampl": "2"
            }
          ],
          "y": [
            {
              "name": "1"
            }
          ]
        }
      ],
      "bind-tag": [
        "tag1",
        "tag2"
      ]
    }
  ]
}
```
> 统一解析为 W 面再根据音符所在面进行转椅 向左为X- 向右为X+ 向上为Y+ 向下为Y-
在 `duration` tick 内将实体从 `from` 移动到 `to`，各轴可独立指定缓动函数：

```skript
loop duration times:
    tickrate = tick / duration
    x = x1 + (x2-x1) * calcEasingValue(tickrate, Xtype)
    y = y1 + (y2-y1) * calcEasingValue(tickrate, Ytype)
    ...
    teleport entity to (x, y, z, yaw, pitch)
    wait 1 tick
```

## calcEasingValue(x, type) → number
> 内置，提供调用接口即可

将 0~1 的进度值通过缓动函数转换。支持的类型：

**[Java 版]** 精简为以下实用缓动函数：

| 名称 | 参数 | 说明 |
|------|------|------|
| `0` | — | 不移动（恒为 0） |
| `1` | — | 线性（1次方） |
| `2` / `3` / `4` / `5` | — | N次方缓动 |
| `sin` | `freq`（频率）, `ampl`（振幅） | 正弦缓动 |
| `expo` | `base`（底数） | 指数缓动，指数 = tick × 0.1 |
| `log` | `base`（底数，必须 > 0） | 对数缓动，真数 = tick × 0.1 |

结算界面使用 easeOutCirc（原 `"cirb"`）实现 UI 滑入动画，Java 版内置实现即可。

