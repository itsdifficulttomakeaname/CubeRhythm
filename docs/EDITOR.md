# 游戏内编辑器（已废弃）

> **⚠️ 废弃说明**：游戏内编辑器已于 2026-02-17 废弃，所有类标记 `@Deprecated`，功能在 `Main.java` 中已禁用。
> **推荐使用外部工具编辑铺面**（Web 编辑器或桌面应用）。

## 废弃原因

1. 多面坐标转换逻辑复杂，容易出错
2. 射线投射边界情况处理困难
3. 预览光标频繁创建/删除实体，性能开销大
4. Minecraft 输入系统不适合精确编辑，缺少撤销/重做
5. 维护成本高

## 推荐替代方案

**外部 Web 编辑器（推荐）**：React + Canvas/WebGL + Web Audio API，支持波形可视化、撤销/重做、批量编辑。

**桌面应用**：Electron 或 Tauri，原生性能，完整文件系统访问。

工作流程：外部工具编辑 → 保存为 JSON → 放入 `plugins/CubeRhythm/` → 游戏内 `/play` 测试。

## 已废弃的命令

- ~~`/editor new <chartId>`~~ / ~~`/editor load <chartId>`~~
- ~~`/editor save`~~ / ~~`/editor bpm`~~ / ~~`/editor pretime`~~ / ~~`/editor speed`~~
- ~~`/step <n>`~~ / ~~`/b <beat>`~~

## 代码结构（仅供参考）

`org.cubeRhythm.editor` 包中的类（均已 `@Deprecated`）：

| 类 | 职责 |
|----|------|
| `EditorSession` | 单玩家编辑状态（tick位置、音符类型、BPM等） |
| `EditorManager` | 全局会话管理单例 |
| `EditorNote` | 编辑器专用音符数据结构 |
| `EditorFileUtil` | JSON 序列化/反序列化，实时自动保存 |
| `EditorCommand` | `/editor` 命令处理 |
| `StepCommand` / `BeatCommand` | `/step` 和 `/b` 命令 |
| `EditorListener` | 鼠标滚轮导航（`PlayerItemHeldEvent`）、热键 |
| `EditorNoteRenderer` | 渲染当前时间 ±5s 内的音符 |
| `EditorUpdateTask` | 每 2 tick 更新预览光标和 ActionBar |
| `EditorPreviewCursor` | 射线投射计算光标位置，显示半透明预览实体 |
| `EditorFaceDetector` | 自动检测玩家视线指向哪个判定面 |

## 时间系统（参考）

```
time(s) = tick / stepLength × (60 / bpm) + preTime
beat    = floor(tick / stepLength) + 1
```

## DOUBLE 音符放置流程（参考）

两步放置：右键第一个位置 → 右键第二个位置完成；左键或切换类型取消。
