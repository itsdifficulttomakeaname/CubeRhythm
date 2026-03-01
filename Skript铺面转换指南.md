# Skript 铺面转换指南

## 已完成的转换

### simpletone.json
已手动转换完成，位于 `plugins/CubeRhythm/charts/simpletone.json`

包含：
- 3 个 EXECUTION 音符（教程标题）
- 多个 TAP、HOLD、DRAG、FLICK、DOUBLE 音符
- 总计约 80+ 个音符

## 自动转换脚本

已创建 Python 转换脚本：`convert_charts.py`

### 使用方法

```bash
# 在项目根目录运行
python convert_charts.py
```

### 脚本功能

1. **自动扫描** `scripts/charts/` 目录中的所有 Skript 铺面
2. **解析元数据** 从 `{name}_properties.sk` 文件中提取：
   - 谱面 ID
   - 曲名、曲师、谱师
   - 难度名称和颜色
   - 时长、偏移、BPM
3. **解析音符** 从 `-{name}.sk` 文件中提取：
   - TAP 音符：`tap(time, face, x, y, glowing, tag)`
   - HOLD 音符：`hold(time, face, x, y, glowing, tag)`
   - DRAG 音符：`drag(time, face, x, y, glowing, tag)`
   - FLICK 音符：`flick(time, face, turn, glowing, tag)`
   - DOUBLE 音符：`double(time, face, x1, y1, x2, y2, glowing, tag)`
4. **生成 JSON** 输出到 `plugins/CubeRhythm/charts/{name}.json`

### 待转换的铺面

根据扫描结果，以下铺面将被转换：
- Sikkunt_Hardbeat
- world_function
- styx_helix
- falling_asteroid
- Sweden

## Skript 格式说明

### 元数据文件格式 (`{name}_properties.sk`)

```skript
options:
    谱面: chart_id
    曲名: Song Title
    曲师: Artist Name
    谱师: Charter Name
    难度: &bDifficulty Name
    时长: 60
    偏移: 0
    BPM: 120
```

### 铺面文件格式 (`-{name}.sk`)

```skript
options:
    音频: audio.file

function loadChart():
    # 音符定义
    tap(14.77, "w", 1, 0, false, "")
    hold(29.55, "w", 2, 1, false, "")
    drag(42.46, "w", 2, 1, false, "")
    flick(36.92, "w", "left", false, "")
    double(22.15, "w", -1, -1, -2, 1, false, "")
```

## 转换映射

### 难度颜色映射

| Skript 颜色代码 | JSON 颜色 |
|----------------|-----------|
| &b | AQUA |
| &a | GREEN |
| &e | YELLOW |
| &6 | GOLD |
| &c | RED |
| &d | LIGHT_PURPLE |
| &f | WHITE |
| &7 | GRAY |

### 音符类型映射

| Skript 函数 | JSON type |
|------------|-----------|
| tap() | "tap" |
| hold() | "hold" |
| drag() | "drag" |
| flick() | "flick" |
| double() | "double" |

### 判定面映射

| Skript | JSON | 说明 |
|--------|------|------|
| "w" | "w" | 前方（白色） |
| "a" | "a" | 左侧（黄色） |
| "s" | "s" | 后方（橙色） |
| "d" | "d" | 右侧（红色） |

## 注意事项

### EXECUTION 音符

Skript 格式中的 EXECUTION 音符（如教程中的标题显示）需要手动转换，因为它们的格式较为复杂。

示例转换：

**Skript 格式：**
```skript
create section stored in {title1}:
    send title "&7欢迎来到 &fCube Rhythm" with subtitle "&f &f" to all players for 1 second with fade in 0.5 seconds and fade out 0.5 seconds
```

**JSON 格式：**
```json
{
  "type": "execution",
  "time": 0.5,
  "actions": [
    {
      "type": "title",
      "enabled": true,
      "title": "§7欢迎来到 §fCube Rhythm",
      "subtitle": "§f §f",
      "fadeIn": 10,
      "stay": 20,
      "fadeOut": 10
    }
  ]
}
```

### 颜色代码转换

Skript 使用 `&` 作为颜色代码前缀，JSON 格式使用 `§`：
- Skript: `&a绿色文本`
- JSON: `§a绿色文本`

### 坐标系统

坐标系统保持不变：
- X 轴：左右（负数为左，正数为右）
- Y 轴：上下（负数为下，正数为上）
- 原点 (0, 0) 在判定面中心

## 验证转换结果

转换完成后，建议：

1. **检查 JSON 格式**：确保文件是有效的 JSON
2. **验证元数据**：检查曲名、曲师等信息是否正确
3. **测试游戏**：在游戏中加载铺面测试
4. **对比音符数量**：确保所有音符都被正确转换

## 手动调整

某些复杂的 Skript 功能可能需要手动调整：
- 复杂的 EXECUTION 动作
- 特殊的视觉效果
- 自定义的游戏逻辑

## 备份

转换前建议备份原始 Skript 文件：
```bash
cp -r scripts/charts scripts/charts.backup
```
