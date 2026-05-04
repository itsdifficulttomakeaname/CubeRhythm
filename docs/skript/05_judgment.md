# 05 判定系统

## 时间偏移计算

`hitNote()` 和 DRAG 自动判定都用同一套公式计算音符偏移：

```skript
# W/A 面（正方向）
set {_time} to ({_x} - 4) / speed * 50
# S/D 面（负方向）
set {_time} to (-1 * {_x} - 4) / speed * 50
```

其中 `{_x}` 是音符当前的坐标轴值（W/S 面取 Z，A/D 面取 X）。
- 值为 4 时 = 正好在判定线 → offset = 0
- 值 > 4（还未到）→ offset 为正（Early）
- 值 < 4（已过）→ offset 为负（Late）

单位：毫秒（`/ speed * 50` 将 block 距离转为 ms）

---

## 判定窗口

```skript
function hitNote(entity):
    if abs({_time}) <= 80:
        exact(entity, time)
    else if abs({_time}) <= 200:
        if metadata "forceexact" is true:
            exact(entity, time)
        else:
            great(entity, "early"/"late", time)
    # 超出 200ms：不处理（音符继续移动直到 MISS 区域）
```

**[Java 版]** 保持对称判定窗口（±80ms EXACT，±200ms JUST），与原版一致。

---

## exact() 函数

```skript
function exact(entity, time):
    if notetype != "hold":
        add 1 to {combo}
        play hit sound
        add 1 to {hitNotes}
        add 1 to {statistics::exact}
    else:
        add 1 to {statistics::exactHold}  # hold 不计入 combo/hitNotes
    set {alphaTime} to 11
    set {score} to floor(1000000 / maxNote * hitNotes)
    # 在判定面位置生成 "Exact" 文字动画（蓝色，10 tick 淡出缩放）
```

HOLD 音符单独统计 `exactHold`，不影响 combo 和 hitNotes（分数计算时在结算时加上）。

---

## great() 函数（Just 判定）

```skript
function great(entity, el, time):
    play hit sound
    add 1 to {combo}
    add 0.7 to {hitNotes}   # Just 只计 0.7
    add 1 to {statistics::just}
    send action bar "Early" / "Late"
    # 生成 "Just" 文字动画（黄色）
```

---

## miss() 函数

```skript
function miss():
    set {combo} to 0
    add 1 to {statistics::miss}
    send title "" with subtitle "&4Miss" for 0.5s
```

不扣分，只清零 combo 并统计。

---

## DRAG 自动判定（judge.sk every ticks）

```skript
every ticks:
    loop all players where {actor} is loop-player:
        set {_entity} to target of player ignoring blocks
        if notetype is "drag":
            计算 offset
            if abs(offset) <= 200:
                teleport entity up 20 blocks  # 移出屏幕（视觉消失）
                set metadata "hited" to true
```

DRAG 不直接调用 `exact()`，而是设置 `hited=true` 并将实体上移 20 格。主循环检测到 `hited=true` 时再调用 `exact()`。

---

## FLICK 转头检测（checkIfTurned）

```skript
function checkIfTurned(entity):
    exact(entity, 0)   # 先给 exact（到达判定线时）
    wait 0.5 seconds   # 等待玩家转头
    检查玩家 yaw 是否在目标方向范围内：
```

各面的判定范围（yaw 已归一化到 -180~180）：

| 面 | turn  | 通过范围                |
|---|-------|---------------------|
| W | left  | -135 ~ -45          |
| W | right | 45 ~ 135            |
| A | left  | 135~180 或 -180~-135 |
| A | right | -45 ~ 45            |
| S | left  | 45 ~ 135            |
| S | right | -135 ~ -45          |
| D | right | 135~180 或 -180~-135 |
| D | left  | -45 ~ 45            |

不在范围内 → `miss()`

**[Java 版]** 使用新版判定：音符到达判定线时直接检测视角，无需点击触发。效果更直观。

---

## deleteNote()

```skript
function deleteNote(entity):
    set {_uuid} to uuid of entity
    set {_linkuuid} to metadata "linkuuid" of entity
    # 删除关联的 BlockDisplay（通过 linkuuid）
    # 删除 double 连线（通过 note1/note2 metadata）
    # 删除 interaction 本身
    wait 1 tick
    # 删除所有 belongsto={_uuid} 的 TextDisplay
    # 删除所有 linkbind={_linkuuid} 的 TextDisplay（flick 箭头）
```
