# 编辑器功能废弃说明

## 更改日期
2026-02-17

## 更改原因

在 Minecraft 中实现完整的铺面编辑器存在以下技术限制：

1. **多面坐标转换复杂**: 4个判定面的坐标系统转换逻辑复杂，容易出错
2. **射线投射精度问题**: 需要同时检测4个面，边界情况处理困难
3. **实体管理开销大**: 预览光标和音符渲染需要频繁创建/删除实体，影响性能
4. **用户体验受限**: Minecraft 的输入系统不适合精确编辑，缺少撤销/重做等基本功能
5. **维护成本高**: 代码复杂度高，难以维护和扩展

## 已执行的更改

### 1. 代码标记

所有编辑器相关类已标记为 `@Deprecated`：

```
src/main/java/org/cubeRhythm/editor/
├── EditorSession.java          ✓ @Deprecated
├── EditorManager.java          ✓ @Deprecated
├── EditorNote.java             ✓ @Deprecated
├── EditorFileUtil.java         ✓ @Deprecated
├── EditorCommand.java          ✓ @Deprecated
├── StepCommand.java            ✓ @Deprecated
├── BeatCommand.java            ✓ @Deprecated
├── EditorListener.java         ✓ @Deprecated
├── EditorNoteRenderer.java     ✓ @Deprecated
├── EditorUpdateTask.java       ✓ @Deprecated
├── EditorPreviewCursor.java    ✓ @Deprecated
└── EditorFaceDetector.java     ✓ @Deprecated
```

### 2. 功能禁用

**Main.java** 中已注释掉：
- EditorListener 注册
- EditorUpdateTask 启动
- /editor 命令注册
- /step 命令注册
- /b 命令注册
- EditorManager 清理

**plugin.yml** 中已注释掉：
- /editor 命令定义
- /step 命令定义
- /b 命令定义

### 3. 文档更新

**新增文档**:
- `EDITOR_DESIGN.md`: 编辑器设计文档，包含实现思路和替代方案

**更新文档**:
- `CLAUDE.md`: 标记编辑器命令和包为废弃状态

**保留文档**:
- `editor_implementation.md`: 原始 Skript 实现分析（保留作为参考）
- `scripts/editor.sk`: 原始 Skript 实现（保留作为参考）

## 推荐的替代方案

### 方案 1: 外部 Web 编辑器（推荐）

开发独立的 Web 应用：

**技术栈**:
- 前端: React + TypeScript
- 渲染: Canvas 2D 或 Three.js
- 音频: Web Audio API
- 状态管理: Redux 或 Zustand

**优势**:
- 完整的用户界面
- 音频波形可视化
- 撤销/重做支持
- 精确的鼠标控制
- 批量编辑功能
- 跨平台支持

**示例项目结构**:
```
cubeRhythm-editor/
├── src/
│   ├── components/
│   │   ├── Timeline.tsx
│   │   ├── NoteEditor.tsx
│   │   ├── Waveform.tsx
│   │   └── Inspector.tsx
│   ├── core/
│   │   ├── Chart.ts
│   │   ├── Note.ts
│   │   └── Coordinate.ts
│   └── utils/
│       ├── json.ts
│       └── audio.ts
└── public/
    └── index.html
```

### 方案 2: 桌面应用

使用 Electron 或 Tauri 开发桌面应用：

**优势**:
- 原生性能
- 文件系统访问
- 更好的音频处理
- 离线使用

### 方案 3: 简化命令行编辑器

如果必须在游戏内编辑，可以实现简化的命令行界面：

```
/note add tap w 0.5 0.5 1.5
/note delete <id>
/note list
/note move <id> <x> <y>
```

**优势**:
- 实现简单
- 性能开销小
- 适合快速调整

**劣势**:
- 缺少可视化
- 操作繁琐

## 代码保留说明

虽然编辑器功能已废弃，但代码仍保留在项目中：

1. **作为参考**: 未来可能需要参考某些实现细节
2. **避免破坏**: 删除代码可能影响其他模块
3. **历史记录**: 保留开发历史和设计思路

如果确定不再需要，可以在未来版本中完全删除 `org.cubeRhythm.editor` 包。

## 迁移指南

如果你正在使用编辑器功能：

1. **导出现有铺面**: 确保所有铺面已保存为 JSON 格式
2. **切换到外部工具**: 使用推荐的外部编辑器
3. **更新工作流程**: 在外部编辑，然后将 JSON 文件放入 `plugins/CubeRhythm/` 目录

## 相关文档

- `EDITOR_DESIGN.md`: 详细的设计文档和替代方案
- `editor_implementation.md`: 原始实现分析
- `CLAUDE.md`: 项目总体文档
- `facing-explanation.md`: 判定面坐标系统说明

## 联系方式

如有问题或建议，请通过以下方式联系：
- GitHub Issues: [项目仓库]
- 开发者: AluminumNitrate (Little AcidAluminum) with Jason31416
