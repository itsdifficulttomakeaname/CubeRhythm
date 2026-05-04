# 07 结算系统

## end() 流程

```
end()
  ├─ 清空玩家背包
  ├─ 检查成就：
  │    combo == maxNote && exact == maxNote → perfectPerformance()
  │    combo == maxNote                     → fullCombo()
  ├─ wait 30 ticks
  ├─ 清除进度条
  ├─ loop 30 times（30 tick 上浮动画）：
  │    score/level 实体 Y += 0.01/tick
  │    combo/perfect 实体 Y += 0.05/tick（更快）
  │    wait 1 tick
  ├─ delete all text displays
  ├─ wait 5 ticks
  └─ 非 autoPlay：result() + result2() + result3() + result4()
     autoPlay：quitGame()
```

---

## Full Combo / Perfect Performance 动画

两者结构相同，区别只在文字内容：

```skript
summon text display at location(0.5, 0.5, 10, world "world", -90, 90):
    set display right rotation to axisAngle(90°, 0, 1, 0)  # 旋转朝向玩家
    set display text to "§bFull Combo" / "§ePerfect Performance!"
    set display scale to vector(40, 40, 40)
    set metadata "perfect" to true

# 30 tick 弹入动画
loop 30 times:
    del = del * 0.8      # 指数衰减
    size = size * 0.7
    angle -= del         # 从 90° 转到 0°（翻转落下）
    set pitch to angle
    set scale to (size+5, size+5, size+5)
    wait 1 tick
```

动画效果：文字从侧面（pitch=90°）翻转落下到正面（pitch≈0°），同时缩小到稳定大小。

---

## result() — 评级显示

```skript
# 从屏幕左侧滑入（easingMotion cirb = easeOutCirc）
easingMotion(entity, loc(-7,0,12), loc(-1.5,0,12), "cirb", ..., 25)
# 60 tick 内持续更新显示 getRank({displayScore})
loop 60 times:
    set text to getRank({displayScore})
    wait 1 tick
```

评级跟随 `{displayScore}` 变化（由 result2 驱动的滚动分数）。

## result2() — 分数滚动

```skript
set {_tempScore} to score * 0.75   # 初始显示 25% 的分数
loop 60 times:
    {_tempScore} = 0.75 * {_tempScore}   # 每 tick 乘 0.75（指数衰减趋近 0）
    {displayScore} = round(score - {_tempScore})  # 实际显示值趋近 score
    wait 1 tick
```

分数从 0 快速滚动到最终值（指数缓动）。

## result3() — 统计信息

显示 Exact/Just/Miss 数量，30 tick 淡入动画（opacity 从 14 增到 255）。

## result4() — 歌曲信息

显示歌曲名 + 难度，位置根据文字长度动态计算：
```skript
set {_length} to length of "&f{song}&f {level}" * 0.12
set {_loc1} to location(-2.9 - {_length}, 3, 10, ...)
```

---

## getRank() 评级表

| 分数范围          | 评级   | 颜色 |
|---------------|------|----|
| 1000000       | SSS+ | §e |
| 990000~999999 | SSS  | §e |
| 980000~989999 | SS   | §e |
| 960000~979999 | S    | §e |
| 950000~959999 | AAA  | §a |
| 940000~949999 | AA   | §a |
| 930000~939999 | A    | §a |
| 920000~929999 | BBB  | §b |
| 910000~919999 | BB   | §b |
| 900000~909999 | B    | §b |
| 850000~899999 | C    | §2 |
| ≤849999       | D    | §7 |

注：原版评级与 Java 版不同（原版有 AAA/AA/A/BBB/BB/B，Java 版改为 SSS+/SS+/S+ 等）。**[Java 版]** 以原版评级表为准。
