# 游戏系统规范化更新

## 概述

根据 `facing-explanation.md` 的规范，对游戏系统进行了全面更新，包括判定面朝向、玩家位置、坐标系统的规范化，以及GUI刷新功能。

## 主要更新

### 1. 配置管理器增强

**新增功能**：
- `ConfigManager.getGameLocation()` - 从 config.yml 读取游戏中心位置
- 支持格式：`location: x,y,z` (例如：`0,320,0`)
- 自动解析和错误处理

**配置文件** (`config.yml`):
```yaml
location: 0,320,0  # x,y,z 表示玩家站立的位置
```

### 2. 判定面规范

**判定面朝向定义**：
- W 面：Z+ 方向 (z=4)
- A 面：X+ 方向 (x=4)
- S 面：Z- 方向 (z=-4)
- D 面：X- 方向 (x=-4)

**判定面区域** (假设中心在 {x, y, z}):
- W 面：x ∈ [-3, 3], y ∈ [-3, 3], z = 4
- A 面：x = 4, y ∈ [-3, 3], z ∈ [-3, 3]
- S 面：x ∈ [-3, 3], y ∈ [-3, 3], z = -4
- D 面：x = -4, y ∈ [-3, 3], z ∈ [-3, 3]

**判定面厚度**：1个方块

### 3. 坐标系统验证

**CoordinateSystem.java** 已验证正确：
- W 面：`{centerX + x, centerY + y, centerZ + z}` ✓
- A 面：`{centerX + z, centerY + y, centerZ - x}` ✓
- S 面：`{centerX - x, centerY + y, centerZ - z}` ✓
- D 面：`{centerX - z, centerY + y, centerZ + x}` ✓

当 z=4 (判定线距离) 时：
- W 面位于 z=centerZ+4 ✓
- A 面位于 x=centerX+4 ✓
- S 面位于 z=centerZ-4 ✓
- D 面位于 x=centerX-4 ✓

### 4. GUI 刷新功能

**新增功能**：
- 谱面选择界面新增"刷新谱面"按钮
- 使用 RECOVERY_COMPASS 材质
- 位置：底部工具栏最左侧 (slot 45)

**功能**：
- 重新加载所有谱面文件
- 预加载谱面数据
- 显示加载结果反馈
- 自动刷新GUI显示

**使用方法**：
1. 打开谱面选择界面 (`/gui`)
2. 点击底部的"刷新谱面"按钮
3. 系统重新加载所有谱面
4. GUI自动更新显示

### 5. Main 类更新

**新增**：
- `ConfigManager` 实例
- 在 `onEnable()` 中初始化 ConfigManager

**初始化顺序**：
1. PlanetLib
2. ConfigManager (新增)
3. PlayerSettingsManager
4. ChartRegistry
5. GUI Listener
6. Movement Restriction
7. Editor Listener
8. Editor Update Task
9. Commands

## 待完成的工作

### 1. GameSession 使用配置位置

**需要修改**：`GameSession.java`

**当前代码**：
```java
this.centerX = playerLoc.getX();
this.centerY = playerLoc.getY();
this.centerZ = playerLoc.getZ();
```

**应改为**：
```java
double[] gameLocation = Main.instance.getConfigManager().getGameLocation();
this.centerX = gameLocation[0];
this.centerY = gameLocation[1];
this.centerZ = gameLocation[2];
```

### 2. 玩家位置限制

**需要实现**：
- 在游戏开始时将玩家传送到配置的位置
- 在游戏进行中限制玩家移动（已有 MovementRestriction）
- 确保玩家始终在正确的位置

**建议实现位置**：`GameSession.start()`
```java
// 传送玩家到游戏位置
Location gameLocation = new Location(
    player.getWorld(),
    centerX,
    centerY,
    centerZ
);
player.teleport(gameLocation);
```

### 3. 编辑器使用配置位置

**需要修改**：`EditorSession` 和相关类

**当前**：使用玩家当前位置
**应改为**：使用配置的游戏位置

## 文件变更清单

### 已修改：
- ✅ `ConfigManager.java` - 添加 `getGameLocation()` 方法
- ✅ `Main.java` - 添加 ConfigManager 实例和初始化
- ✅ `ChartSelectorGUI.java` - 添加刷新按钮
- ✅ `GUIListener.java` - 处理刷新按钮点击事件
- ✅ `EditorFaceDetector.java` - 自动面检测（已实现）
- ✅ `EditorPreviewCursor.java` - 自动面检测预览（已实现）

### 待修改：
- ⏳ `GameSession.java` - 使用配置的游戏位置
- ⏳ `EditorSession.java` - 使用配置的游戏位置
- ⏳ `EditorPreviewCursor.java` - 使用配置的游戏位置
- ⏳ `EditorUpdateTask.java` - 使用配置的游戏位置

## 测试要点

### GUI 刷新功能：
1. ✅ 打开谱面选择界面
2. ✅ 点击"刷新谱面"按钮
3. ✅ 查看是否显示加载消息
4. ✅ 确认谱面列表已更新
5. ✅ 验证新添加的谱面是否出现

### 坐标系统：
1. ⏳ 验证玩家被传送到配置的位置
2. ⏳ 验证音符在正确的判定面上渲染
3. ⏳ 验证判定面的朝向正确
4. ⏳ 验证编辑器预览光标位置正确

### 配置读取：
1. ✅ 修改 config.yml 中的 location
2. ⏳ 重启服务器
3. ⏳ 验证游戏使用新的位置

## 兼容性说明

- ✅ 向后兼容现有谱面文件
- ✅ 不影响现有玩家设置
- ✅ 配置文件格式简单易懂
- ✅ 默认值合理（0,320,0）

## 性能影响

- ✅ 刷新功能：轻量级，仅重新加载谱面文件
- ✅ 配置读取：仅在启动时执行一次
- ✅ 坐标转换：无额外开销，使用现有系统

## 后续优化建议

1. **异步刷新**：将谱面加载改为异步，避免阻塞主线程
2. **增量刷新**：只重新加载修改过的谱面文件
3. **刷新进度**：显示加载进度条
4. **自动刷新**：检测文件变化自动刷新
5. **位置验证**：启动时验证配置的位置是否有效
