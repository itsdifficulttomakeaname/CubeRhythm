# 01 初始化与游戏启动流程

## 服务器/脚本加载时（`main.sk` on load）

```skript
on load:
    delete {actor}
    clear all players' inventory
    delete {playing}
    set {spawnLocation} to location(0.5, -1.5, 0.5, world "world", 0, 0)
    set time in world "world" to 0:00
    pasteSchematic("default", {spawnLocation})
```

- 清除当前游玩玩家引用和谱面 ID
- 清空所有玩家背包
- 设置出生点坐标（硬编码为世界原点附近）
- 将世界时间固定为 0:00（防止昼夜变化）
- ~~粘贴默认场景结构（`default.schem`）~~ **[Java 版不实现]** 不支持 WorldEdit 结构粘贴，场景相关逻辑全部抛弃

同时清空所有 interaction/block display/text display 实体，重置所有计分变量。

---

## 玩家加入（`main.sk` on join）

```skript
on join:
    teleport player to location(0.5, -1, 0.5, world "world", 0, 0)
    set player's gamemode to survival
    set player's level to 0
    # 初始化玩家设置（如未设置）
    set {speed::%uuid of player%} to 1 if ...
    set {offset::%uuid of player%} to 0 if ...
    # 等其他设置...
    wait 1 tick
    if online player count > 1:
        set player's gamemode to spectator   # 多人时变旁观者
    else:
        # 首次加入：自动开始 simpletone 教程
        set {actor} to player
        wait 1 second
        playChart("simpletone")
```

**关键设计**：
- 该游戏为**单人模式**，第二个玩家加入会变成旁观者
- 首次加入自动播放 `simpletone` 教程谱面
- 玩家设置（speed/offset/hitSound 等）持久化在全局变量中，以 UUID 为 key

---

## 谱面加载流程（`gui.sk` playChart）

```
playChart(id)
  ├─ 重置 noteID = 0，process = 0
  ├─ [sync] 禁用所有已启用的谱面内容脚本（非 _properties）
  ├─       quitGame()（非编辑模式）
  ├─ wait 1 tick
  ├─ process = 10
  ├─ [async] enable script "charts\-{id}"  → 触发 on load → 调用 loadChart()
  ├─ wait 1 tick
  └─ 非编辑模式：
       process = 25
       set {playing} = id
       wait 0.5s
       execute /start
```
**[Java 版说明]** "禁用所有已启用的谱面内容脚本"是 Skript 特有机制——每个谱面文件用一个函数存储音符，多个脚本同时生效会导致调用异常。Java 版通过文件名区分 JSON，无需此操作。

**加载进度条**：`gui.sk` 顶部的 `every ticks` 监听 `{process}` 变量，用 `·` 字符绘制进度条显示在副标题。

---

## /start 命令（`main.sk`）

```
/start
  ├─ process = 50
  ├─ [async] 按时间升序排序 {loadedNotes::time::*}
  │          将排序结果重新写入 {loadedNotes::*}（重新索引）
  ├─ delete {process}
  ├─ 显示歌曲标题（曲名 + 曲师/谱师）
  ├─ 播放音频预览（2秒后停止）
  ├─ set {actor} = player
  ├─ 给玩家 9 个雪球（用于捕获右键事件）
  ├─ 初始化：timer=0, combo=0, score=0, beat=-8, hitNotes=0, alphaTime=11
  ├─ 初始化统计：exact/just/miss/exactHold = 0
  ├─ 对所有音符时间加偏移：
  │     noteTime += 3（准备时间）
  │     noteTime += offset * 0.001（玩家偏移）
  │     noteTime += chartOffset * 0.001（谱面偏移）
  ├─ wait 3 seconds
  └─ 播放音频（正式开始）
```

**时间偏移说明**：
- 全局 +3 秒：给玩家看标题/准备的时间
- 玩家偏移：正值 = 音符延后生成（音频先于视觉），负值 = 提前
- 谱面偏移：谱面固有的音画同步校正值

---

## 游戏退出（`quitGame()`）

```skript
function quitGame():
    remove blindness from {actor}
    delete {actor} if {playing} is set
    delete {timer}
    clear all players' inventory
    stop all sounds for all players
    delete {loadedNotes::*}
    loop all interactions/block displays/text displays: delete
    重置所有计分变量
    delete {playing}
    set time to 0:00
    # pasteSchematic("default", {spawnLocation})  ← Java 版不实现
```

- 清除所有游戏状态，恢复场景到初始状态
- 注意：只有 `{playing}` 已设置时才删除 `{actor}`（防止在加载阶段退出时误删）
