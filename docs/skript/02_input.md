# 02 玩家输入处理

## 输入事件概览

原版使用 4 个事件 + 1 个辅助函数来捕获玩家点击，并通过 `{cancel::A/B/C/D}` 标志防止同一次操作被多个事件重复触发。

| 事件               | 标志 | 说明            |
|------------------|----|---------------|
| `on right click` | A  | 右键点击/交互实体     |
| `on shoot`       | B  | 投掷雪球（捕获右键远距离） |
| `on left click`  | C  | 左键点击          |
| `on damage`      | D  | 攻击实体          |
| `on arm swing`   | E  | 挥臂（兜底，无标志）    |

---

## 防重复触发机制

```skript
on right click:
    set {cancel::A} to true
    # ... 处理逻辑 ...
    wait 1 tick
    delete {cancel::A}
```

`on shoot` 在执行前检查：
```skript
set {_blocked} to true if {cancel::A} is true
{_blocked} is not set   # 如果已被 A 处理则跳过
set {cancel::B} to true
```

`on arm swing`（挥臂）是兜底事件，检查所有标志：
```skript
# 对于 double 音符：只检查 A/B/D（不检查 C，因为左键不触发 double）
# 对于其他音符：检查 A/B/C/D 全部
{_blocked} is not set
```

**设计意图**：Minecraft 的一次点击会同时触发多个事件（right click + shoot + arm swing 等），通过 cancel 标志确保 `hitNote` 只被调用一次。

---

## holding() 函数

```skript
function holding():
    add 1 to {holding}
    wait 0.3 seconds
    remove 1 from {holding}
```

每次点击事件调用此函数，使 `{holding}` 在 0.3 秒内保持 > 0。游戏主循环用 `{holding} > 0` 来判断 HOLD 音符是否被按住。

---

## 目标实体获取

```skript
# right click 优先用 target（准心指向），fallback 用 clicked entity
set {_entity} to target of player ignoring blocks if target ... is set
set {_entity} to clicked entity if target ... is not set
```

获取到实体后，检查其 metadata `notetype`：
- `"tap"` 或 `"double"` → 调用 `hitNote({_entity})`
- `"flick"` → 调用 `hitFlick({_entity})`（注：原版 FLICK 需要点击触发，与 Java 版自动判定不同）

---

## 自动演奏模式

所有输入事件开头都有：
```skript
{autoPlay::%uuid of {actor}%} is not "&a开"
```
自动演奏开启时完全跳过玩家输入，由游戏主循环直接调用 `exact()`。

---

## 与 Java 版的差异

| 方面       | Skript 原版               | Java 版                            |
|----------|-------------------------|-----------------------------------|
| FLICK 判定 | 点击触发，0.5s 后检测转头         | 自动判定（到达判定线时检测视角）                  |
| 右键捕获     | 雪球投掷事件                  | `ProjectileLaunchEvent`           |
| 防重复      | cancel 标志 + wait 1 tick | `CancelFlagManager`               |
| HOLD 检测  | `{holding}` 计数器         | `KeyPressCache.isAnyKeyPressed()` |
