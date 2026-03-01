# EditorListener 坐标计算修正

## 需要更新的方法

### 1. handleNotePlacement 方法

将坐标计算部分替换为：

```java
private void handleNotePlacement(Player player, EditorSession session) {
    // 使用统一的坐标计算方法
    double[] cursorPos = EditorPreviewCursor.calculateCursorPosition(player);
    double targetX = cursorPos[0];
    double targetY = cursorPos[1];

    // 检查坐标是否在有效范围内 (-3 到 4)
    if (targetX < -3 || targetX > 4 || targetY < -3 || targetY > 4) {
        player.sendMessage("§c位置超出范围！");
        return;
    }

    // 转换为音符坐标
    double noteX = targetX;
    double noteY = targetY;

    // 显示坐标反馈
    player.sendTitle("", String.format("§7%.1f, %.1f", noteX, noteY), 0, 10, 10);

    // ... 其余代码保持不变
}
```

### 2. handleNoteDeletion 方法

将坐标计算部分替换为：

```java
private void handleNoteDeletion(Player player, EditorSession session) {
    // 使用统一的坐标计算方法
    double[] cursorPos = EditorPreviewCursor.calculateCursorPosition(player);
    double targetX = cursorPos[0];
    double targetY = cursorPos[1];

    // 查找最近的音符（在1格范围内）
    EditorNote nearestNote = null;
    double minDistance = 1.0;

    // ... 其余代码保持不变
}
```

## 修正说明

### 主要变更：

1. **使用 player.getLocation() 而不是 player.getEyeLocation()**
   - 与 Skript 版本保持一致
   - 使用玩家脚部位置作为基准点

2. **Y 坐标偏移**
   - 按住 Shift: +1.4
   - 不按 Shift: +1.6
   - 这与 Skript 版本完全一致

3. **网格对齐**
   - 只在按住 Shift 时对齐到 0.5 格网格
   - 使用 `Math.round(x * 2) / 2.0` 实现

4. **统一的坐标计算**
   - 创建了 `EditorPreviewCursor.calculateCursorPosition()` 方法
   - 所有需要计算光标位置的地方都使用这个方法
   - 确保预览光标、放置位置、删除检测使用相同的坐标系统

### 好处：

1. **一致性**: 预览光标显示的位置就是实际放置的位置
2. **准确性**: 与原始 Skript 版本的行为完全一致
3. **可维护性**: 坐标计算逻辑集中在一个地方，易于修改和调试

## 测试要点

1. 预览光标应该准确显示在将要放置音符的位置
2. 按住 Shift 时，光标应该吸附到 0.5 格网格
3. 放置音符后，音符应该出现在预览光标显示的位置
4. 删除音符时，应该删除光标指向的音符
