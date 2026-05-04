# Skript 原始实现分析

本目录记录对 `scripts/` 下原始 Skript 实现的逐模块分析，目的是理解原版设计思路，为 Java 重写提供参考。

## 文件结构

| 文件          | 行数  | 职责                                                             |
|-------------|-----|----------------------------------------------------------------|
| `main.sk`   | 823 | 初始化、输入处理、游戏主循环、音符移动、场景布置                                       |
| `load.sk`   | 485 | 音符注册函数、各类型 draw 函数（渲染生成）                                       |
| `judge.sk`  | 319 | 判定逻辑（exact/great/miss）、DRAG 自动判定、FLICK 转头检测                    |
| `gui.sk`    | 300 | 谱面选择 GUI、设置 GUI、谱面加载流程                                         |
| `result.sk` | 193 | 游戏结算、Full Combo/Perfect 动画、结算界面                                |
| `api.sk`    | 670 | 工具函数库（drawLine、blind、forceExact、setTextDisplay、easingMotion 等） |
| `editor.sk` | 488 | 游戏内编辑器（已废弃）                                                    |

## 分析文档

- [01_init_and_flow.md](01_init_and_flow.md) — 初始化、玩家加入、游戏启动流程（`main.sk` 初始化部分 + `gui.sk` 加载流程）
- [02_input.md](02_input.md) — 玩家输入处理（右键/左键/射击/伤害/挥臂，防重复触发机制）
- [03_game_loop.md](03_game_loop.md) — 游戏主循环（`every ticks`，音符生成时机、移动、自动判定、MISS 检测）
- [04_note_render.md](04_note_render.md) — 音符渲染（`drawTap/Hold/Drag/Flick/Double`，坐标转换，实体结构）
- [05_judgment.md](05_judgment.md) — 判定系统（`hitNote`、`exact`、`great`、`miss`、`checkIfTurned`，时间窗口计算）
- [06_hud_scene.md](06_hud_scene.md) — 场景与 HUD（`createComboDisplay`，连击/分数显示，小节线，indicator 颜色）
- [07_result.md](07_result.md) — 结算系统（`end`、`result1~4`、Full Combo/Perfect 动画，easingMotion）
- [08_api.md](08_api.md) — API 工具库（`drawLine`、`blind`、`forceExact`、`setTextDisplay`、`easingMotion`/`calcEasingValue`）

## 全局变量速查

| 变量 | 类型 | 说明 |
|------|------|------|
| `{actor}` | player | 当前游玩的玩家 |
| `{timer}` | number | 游戏 tick 计数器（每 tick +1） |
| `{playing}` | text | 当前谱面 ID |
| `{speed::%uuid%}` | number | 玩家音符流速 |
| `{offset::%uuid%}` | number | 玩家偏移（ms） |
| `{combo}` | number | 当前连击数 |
| `{score}` | number | 当前分数 |
| `{hitNotes}` | number | 已击中音符数（Just 计 0.7） |
| `{maxNote}` | number | 总可击打音符数 |
| `{holding}` | number | 当前持续按键计数（用于 HOLD 判定） |
| `{loadedNotes::type::%id%}` | text | 已加载音符的类型 |
| `{loadedNotes::time::%id%}` | number | 已加载音符的时间（秒） |
| `{statistics::exact/just/miss/exactHold}` | number | 判定统计 |
| `{best::%uuid%::%chartId%}` | number | 玩家最佳成绩 |
| `{cancel::A/B/C/D}` | boolean | 防重复触发标志 |
| `{autoPlay::%uuid%}` | text | 自动演奏开关（"&a开"/"&c关"） |
| `{hitSound::%uuid%}` | text | 打击音效开关 |
| `{indicator::%uuid%}` | text | FC/Perfect 指示器开关 |
| `{showOffset::%uuid%}` | text | 显示偏移值开关 |
| `{showLine::%uuid%}` | text | 小节线开关 |
