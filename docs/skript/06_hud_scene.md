# 06 场景与 HUD

## createComboDisplay()

在 `on load` 和 `quitGame()` 时调用，创建所有常驻 TextDisplay 实体。

### 大型方向指示（4 个面，远处）

```skript
# 位置示例（W 面）
set {_loc1} to location(0.5, -8, 50, world "world", 180, 0)
summon text display at {_loc1}:
    set display text to "§fReady"   # 游戏开始前显示 Ready/→/↓/←
    set display scale to vector(50, 50, 0)
    set metadata "combo" to true
```

四个面分别显示：W=Ready（后改为连击数）、A=→、S=↓、D=←。
游戏开始后 `every ticks` 将其更新为当前连击数。

### 分数/连击显示（判定面附近）

```skript
# 位置（W 面左上角）
set {_loc5} to location(2.5, 3.2, 5, world "world", 180, 0)
summon text display:
    set metadata "score" to true
    set display scale to vector(2, 2, 0)
```

共 8 个 TextDisplay（4 面 × score + level），通过 metadata 标记类型：
- `metadata "score"` → 显示分数
- `metadata "level"` → 显示难度/歌曲名

---

## 每 tick HUD 更新

```skript
every ticks:
    {timer} > 0
    # indicator 颜色逻辑
    if miss = 0 and just = 0: color = "§e"  # 金色（Perfect）
    if miss = 0 and just > 0: color = "§b"  # 青色（Full Combo）
    else: color = "§f"                        # 白色
    
    loop all text displays:
        if metadata "combo":
            if combo >= 3: show combo
            else: show "§f"（隐藏）
        if metadata "score":
            # 补零显示（7位数）
            "§80000000{color}{score}"
            set opacity to {scoreEntityAlpha}
        if metadata "level":
            show {level} if set
```

**alphaTime 机制**：击中音符时 `{alphaTime} = 11`，每 tick -1，分数透明度 = `-127 + alphaTime * 12.6`，实现击中时分数短暂高亮的效果。

---

## 小节线（summonBeatLine）
> 在某处声明了这个不需要在java版中实现
```skript
every ticks:
    {timer} is set and {bpm} is set and showLine is "开"
    set {_ticksPerBeat} to 60 / bpm * 20
    summonBeatLine({_ticksPerBeat})

function summonBeatLine(ticksPerBeat):
    set {_lineDistance} to (ticksPerBeat * beat - timer + offset*0.02 + chartOffset*0.02 + 62) * speed
    if {_lineDistance} < 50:
        add 1 to {beat}
        mod(beat-1, 2) = 0   # 每隔一拍生成一条（每2拍一条）
        drawLine("w", 4.5, -4, distance, 0, 9, duration, ...)
        drawLine("w", -4.5, -4, distance, ...)
        # 同样在 a/s/d 面各生成两条
```

`{beat}` 从 -8 开始（/start 时初始化），每次生成一条线后 +1。`mod(beat-1, 2) = 0` 确保只在偶数拍生成（每 2 拍一条线）。

线的持续时间 `{_dura} = floor((distance-4)/speed)`，即音符从当前位置到达判定线所需的 tick 数。
