# 03 游戏主循环

## 概览

`main.sk` 中有两个 `every ticks` 块负责游戏运行：
1. **音符生成与移动**（核心循环，需要 `{timer}` 已设置）
2. **场景/HUD 更新**（连击数、分数显示颜色）

---

## 音符生成逻辑

```
每 tick：
  统计当前场上 interaction 数量 {_count}
  if {_count} < 100:（实体上限）
    遍历 {loadedNotes::type::*}，计算每个音符的预生成距离
    distance = speed * 20 * (noteTime - timer/20) + 4
    统计 distance < 50 的音符数 {_preSummon}
    
    循环 {_preSummon} 次，找到第一个 distance < 50 的音符：
      将 noteTime 转为相对时间（减去 timer/20）
      计算 z = speed * 20 * relativeTime + 4
      调用对应 draw 函数生成实体
      删除该音符的所有 loadedNotes 字段
      exit loop（每次只生成一个）
```

**关键点**：
- `distance < 50`：生成距离阈值（50 blocks）
- 每 tick 最多生成一个音符（`exit loop`）
- EXECUTION 音符不生成实体，而是在 `time <= timer/20` 时直接 `run section`

---

## 音符移动逻辑

每 tick 遍历所有 interaction 实体，按 `metadata "face"` 分四个方向处理：

### W 面（前方，Z 轴负方向移动）
```skript
{_loc1}.add(0, 0, speed * -1)   # Z 减小（向玩家靠近）
teleport loop-entity to {_loc1}
```

判定线检测（`z <= 4 + speed`）：
- autoPlay 开启 → 直接 `exact()`
- DRAG：检查 `metadata "hited"` 是否为 true（由 judge.sk 的 every ticks 设置）
- HOLD：检查 `{holding} > 0`
- FLICK：调用 `checkIfTurned()`

MISS 检测（`z < 4 - speed*4`）：超过判定线太远 → `miss()`

### A/S/D 面
逻辑相同，只是轴方向不同：
- A 面：X 轴负方向
- S 面：Z 轴正方向
- D 面：X 轴正方向

---

## 淡入光晕效果

音符接近判定线时（距离 < 25），在判定面位置生成 TextDisplay 作为光晕：

```skript
set {_alpha} to 100 - (distance - 4) * 4   # 距离越近越亮
set {_scale} to alpha / 25
# 颜色：tap=§3(青), drag=§e(黄), double=§6(金)
set display text of anchor to "§f{color}█"
```

光晕实体通过 `metadata "belongsto"` 绑定到对应 interaction 的 UUID，随音符一起移动（每 tick 重新生成或更新）。

**[已解析]** 该段代码用于 FLICK 音符：由于 FLICK 默认占满整个判定面，其光晕实体直接固定在判定面位置（Z=5），而非跟随音符移动。没有 anchor 时才生成新实体，避免重复创建。

---

## 游戏结束检测

```skript
if {_notes} = 0:      # loadedNotes 已全部生成
    if {_count} = 0:  # 场上没有剩余 interaction
        delete {timer}
        end()
```

两个条件同时满足才结束：所有音符已生成 **且** 场上无剩余实体（全部被击中或 MISS）。
