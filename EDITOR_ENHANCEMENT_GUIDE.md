# 编辑器增强功能 - 手动更新指南

## 需要手动更新的文件

### 1. EditorListener.java - handleNotePlacement 方法

将 `handleNotePlacement` 方法中的 DOUBLE 音符处理逻辑替换为以下代码：

在 `// 根据音符类型设置位置` 之前添加：

```java
// 处理DOUBLE音符的两步放置
if (session.getCurrentNoteType() == NoteType.DOUBLE) {
    if (!session.isDoubleNotePlacementMode()) {
        // 第一步：放置第一个位置
        UUID noteId = UUID.randomUUID();
        EditorNote note = new EditorNote();
        note.setId(noteId.toString());
        note.setType(NoteType.DOUBLE);
        note.setTime(session.getCurrentTime());
        note.setFace(session.getCurrentFace());
        note.setGlowing(session.isGlowing());
        note.setTag("");
        note.getPositions().add(new NotePosition((int)noteX, (int)noteY));

        // 添加到会话（但还未完成）
        session.addNote(note);

        // 进入DOUBLE放置模式
        session.startDoubleNotePlacement(new NotePosition((int)noteX, (int)noteY), note.getId());

        player.sendMessage("§a已放置第1个位置，请点击放置第2个位置");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        // 重新渲染
        EditorNoteRenderer.renderVisibleNotes(session, player);
        return;
    } else {
        // 第二步：放置第二个位置
        EditorNote note = session.getNotes().get(session.getDoubleNoteId());
        if (note != null) {
            note.getPositions().add(new NotePosition((int)noteX, (int)noteY));

            // 完成DOUBLE音符放置
            session.completeDoubleNotePlacement();

            // 保存到文件
            if (session.getChartFile() != null) {
                EditorFileUtil.saveSession(session);
            }

            player.sendMessage("§a已完成DOUBLE音符放置");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

            // 重新渲染
            EditorNoteRenderer.renderVisibleNotes(session, player);
        }
        return;
    }
}
```

然后删除原来的 DOUBLE 处理代码：
```java
} else if (note.getType() == NoteType.DOUBLE) {
    // DOUBLE音符需要两个位置（暂时只放置一个，需要用户再次点击放置第二个）
    // 这里简化处理，放置在同一位置
    note.getPositions().add(new NotePosition((int)noteX, (int)noteY));
    note.getPositions().add(new NotePosition((int)noteX + 1, (int)noteY));
```

### 2. Main.java - 启动编辑器更新任务

在 `Main.onEnable()` 方法的最后添加：

```java
// 启动编辑器更新任务（预览光标和Action Bar）
EditorUpdateTask.start(EditorManager.getInstance());
```

## 已完成的更新

1. ✅ EditorSession - 添加了 DOUBLE 音符放置状态和预览光标字段
2. ✅ EditorSession - 添加了预览光标管理方法
3. ✅ EditorSession - 添加了 DOUBLE 音符放置管理方法
4. ✅ EditorPreviewCursor - 新建类，处理预览光标渲染和 Action Bar 显示
5. ✅ EditorUpdateTask - 新建类，定期更新预览光标和 Action Bar
6. ✅ EditorManager - 添加了 getAllSessions() 方法
7. ✅ EditorManager - 更新了 cleanup 方法以清理预览光标
8. ✅ EditorNoteRenderer - 更新了渲染逻辑为游戏准确的时间窗口

## 功能说明

### 1. 实时预览光标
- 在判定面上实时显示当前将要放置的音符
- 根据判定面显示正确的发光颜色：
  - W (前): 白色
  - A (左): 黄色
  - S (后): 橙色
  - D (右): 红色
- 根据音符类型显示正确的大小和形状
- 按住 Shift 时对齐到 0.5 格网格

### 2. Action Bar 信息显示
显示内容：
- BPM
- 当前拍数和位置
- 当前时间
- 光标坐标
- 当前音符类型
- 当前判定面
- 发光状态
- 总音符数
- DOUBLE 音符放置模式提示

### 3. DOUBLE 音符两步放置
- 第一次右键：放置第一个位置，进入等待模式
- 第二次右键：放置第二个位置，完成 DOUBLE 音符
- 提供清晰的文字和音效反馈
- 可以通过切换音符类型取消放置

## 测试建议

1. 进入编辑器模式
2. 观察预览光标是否正确显示
3. 切换不同的判定面，检查发光颜色是否正确
4. 切换不同的音符类型，检查预览形状是否正确
5. 测试 DOUBLE 音符的两步放置流程
6. 检查 Action Bar 信息是否正确显示
