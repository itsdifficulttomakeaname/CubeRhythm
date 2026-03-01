# CubeRhythm 完整铺面示例（JSON格式）

本文档提供基于 `simpletone` 教程谱面的完整JSON格式示例。

---

## 示例1: simpletone 完整谱面

### 谱面信息

- **ID**: simpletone
- **曲名**: simpletone
- **曲师**: CRE
- **谱师**: PiraTom
- **难度**: Tutorial 1
- **时长**: 51秒
- **BPM**: 130
- **偏移**: 0ms
- **音符数**: 67个（不含execution）

---

### 完整JSON格式

```json
{
  "version": "1.0.0",
  "metadata": {
    "id": "simpletone",
    "title": "simpletone",
    "artist": "CRE",
    "charter": "PiraTom",
    "difficulty": {
      "name": "Tutorial 1",
      "displayName": "&bTutorial 1",
      "color": "&b",
      "level": 1,
      "category": "tutorial"
    },
    "audio": {
      "file": "cr.simpletone",
      "format": "ogg",
      "duration": 51
    },
    "timing": {
      "bpm": 130,
      "offset": 0,
      "timeSignature": "4/4"
    },
    "tags": ["tutorial", "beginner", "教程"],
    "description": "新手教程谱面，介绍所有音符类型"
  },

  "events": [
    {
      "id": "title1",
      "type": "execution",
      "time": 0,
      "action": "display_title",
      "params": {
        "main": "&7欢迎来到 &fCube Rhythm",
        "subtitle": "&f &f",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title2",
      "type": "execution",
      "time": 1.85,
      "action": "display_title",
      "params": {
        "main": "&7下面将进行&f玩法教学",
        "subtitle": "&f &f",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title3",
      "type": "execution",
      "time": 3.69,
      "action": "display_title",
      "params": {
        "main": "&7我们先从&f最基础的东西&7开始",
        "subtitle": "&f &f",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title4",
      "type": "execution",
      "time": 7.38,
      "action": "display_title",
      "params": {
        "main": "&f由方框包围起来的部分",
        "subtitle": "&f是判定面，位于你的前后左右",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title5",
      "type": "execution",
      "time": 9.23,
      "action": "display_title",
      "params": {
        "main": "&f左上角是你的分数",
        "subtitle": "&f中间是你的连击数",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title6",
      "type": "execution",
      "time": 11.08,
      "action": "display_title",
      "params": {
        "main": "&f接下来介绍音符",
        "subtitle": "&f &f",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title7",
      "type": "execution",
      "time": 12.92,
      "action": "display_note_tutorial",
      "params": {
        "noteType": "tap",
        "title": "&b&lTap&f 音符",
        "description": [
          "&f与判定面重合时",
          "&f对准它",
          "&e单击&f鼠标&a左&f或&d右&f键"
        ],
        "position": "left",
        "duration": 5
      }
    },
    {
      "id": "title8",
      "type": "execution",
      "time": 20.31,
      "action": "display_note_tutorial",
      "params": {
        "noteType": "double",
        "title": "&6&lDouble&f 音符",
        "description": [
          "&f与判定面重合时",
          "&f对准&e其中一个",
          "&6双击&f鼠标&a左&d右&f键"
        ],
        "position": "right",
        "duration": 5
      }
    },
    {
      "id": "title9",
      "type": "execution",
      "time": 27.69,
      "action": "display_note_tutorial",
      "params": {
        "noteType": "hold",
        "title": "&f&lHold&f 音符",
        "description": [
          "&f与判定面重合时",
          "&c按住&f鼠标&d右&f键",
          "&f按住后&a无需对准"
        ],
        "position": "left",
        "duration": 5
      }
    },
    {
      "id": "title10",
      "type": "execution",
      "time": 35.07,
      "action": "display_note_tutorial",
      "params": {
        "noteType": "flick",
        "title": "&d&lFlick&f 音符",
        "description": [
          "&f与判定面重合时",
          "&f按&b箭头&f方向",
          "&e旋转&f90°到邻近的判定面"
        ],
        "position": "right",
        "duration": 5
      }
    },
    {
      "id": "title11",
      "type": "execution",
      "time": 40.61,
      "action": "display_note_tutorial",
      "params": {
        "noteType": "drag",
        "title": "&e&lDrag&f 音符",
        "description": [
          "&f与判定面重合时",
          "&f对准它",
          "&a无需按键"
        ],
        "position": "left",
        "duration": 3.5
      }
    },
    {
      "id": "title12",
      "type": "execution",
      "time": 44.30,
      "action": "display_title",
      "params": {
        "main": "&f玩法教学到此结束",
        "subtitle": "&f &f",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title13",
      "type": "execution",
      "time": 46.15,
      "action": "display_title",
      "params": {
        "main": "&f如果想再看一遍教程",
        "subtitle": "&f重新加载这张谱面即可",
        "duration": 1,
        "fadeIn": 0.5,
        "fadeOut": 0.5
      }
    },
    {
      "id": "title14",
      "type": "execution",
      "time": 48,
      "action": "cleanup"
    }
  ],

  "notes": [
    {
      "type": "tap",
      "time": 14.77,
      "face": "w",
      "position": {"x": 1, "y": 0},
      "glowing": false,
      "tag": "",
      "description": "第一个Tap音符"
    },
    {
      "type": "tap",
      "time": 16.61,
      "face": "w",
      "position": {"x": 1, "y": 1},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "tap",
      "time": 18.46,
      "face": "w",
      "position": {"x": 1, "y": -1},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "tap",
      "time": 20.31,
      "face": "w",
      "position": {"x": 1, "y": 0},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "double",
      "time": 22.15,
      "face": "w",
      "positions": [
        {"x": -1, "y": -1},
        {"x": -2, "y": 1}
      ],
      "glowing": false,
      "tag": "",
      "description": "第一个Double音符"
    },
    {
      "type": "double",
      "time": 24,
      "face": "w",
      "positions": [
        {"x": -1, "y": 1},
        {"x": -2, "y": -1}
      ],
      "glowing": false,
      "tag": ""
    },
    {
      "type": "double",
      "time": 25.84,
      "face": "w",
      "positions": [
        {"x": 1, "y": 2},
        {"x": -2, "y": -1}
      ],
      "glowing": false,
      "tag": ""
    },
    {
      "type": "double",
      "time": 27.69,
      "face": "w",
      "positions": [
        {"x": 1, "y": -1},
        {"x": -1, "y": 2}
      ],
      "glowing": false,
      "tag": ""
    },
    {
      "type": "hold_sequence",
      "startTime": 29.54,
      "endTime": 30.84,
      "face": "w",
      "position": {"x": 2, "y": 1},
      "segments": [
        {"time": 29.54, "isStart": true},
        {"time": 29.55},
        {"time": 29.64},
        {"time": 29.74},
        {"time": 29.84},
        {"time": 29.94},
        {"time": 30.04},
        {"time": 30.14},
        {"time": 30.24},
        {"time": 30.34},
        {"time": 30.44},
        {"time": 30.54},
        {"time": 30.64},
        {"time": 30.74},
        {"time": 30.84}
      ],
      "glowing": false,
      "tag": "",
      "description": "第一条Hold长按轨道"
    },
    {
      "type": "hold_sequence",
      "startTime": 31.38,
      "endTime": 32.68,
      "face": "w",
      "position": {"x": 1, "y": 0},
      "segments": [
        {"time": 31.38, "isStart": true},
        {"time": 31.39},
        {"time": 31.48},
        {"time": 31.58},
        {"time": 31.68},
        {"time": 31.78},
        {"time": 31.88},
        {"time": 31.98},
        {"time": 32.08},
        {"time": 32.18},
        {"time": 32.28},
        {"time": 32.38},
        {"time": 32.48},
        {"time": 32.58},
        {"time": 32.68}
      ],
      "glowing": false,
      "tag": "",
      "description": "第二条Hold长按轨道"
    },
    {
      "type": "hold_sequence",
      "startTime": 33.23,
      "endTime": 34.53,
      "face": "w",
      "position": {"x": 0, "y": -1},
      "segments": [
        {"time": 33.23, "isStart": true},
        {"time": 33.24},
        {"time": 33.33},
        {"time": 33.43},
        {"time": 33.53},
        {"time": 33.63},
        {"time": 33.73},
        {"time": 33.83},
        {"time": 33.93},
        {"time": 34.03},
        {"time": 34.13},
        {"time": 34.23},
        {"time": 34.33},
        {"time": 34.43},
        {"time": 34.53}
      ],
      "glowing": false,
      "tag": "",
      "description": "第三条Hold长按轨道"
    },
    {
      "type": "flick",
      "time": 36.92,
      "face": "w",
      "turn": "left",
      "glowing": false,
      "tag": "",
      "description": "从前往左的Flick音符"
    },
    {
      "type": "flick",
      "time": 38.77,
      "face": "a",
      "turn": "right",
      "glowing": false,
      "tag": "",
      "description": "从左往前的Flick音符"
    },
    {
      "type": "drag_path",
      "startTime": 42.46,
      "endTime": 44.30,
      "face": "w",
      "path": [
        {"time": 42.46, "position": {"x": 2, "y": 1}},
        {"time": 42.69, "position": {"x": 2, "y": 0.5}},
        {"time": 42.92, "position": {"x": 2, "y": 0}},
        {"time": 43.15, "position": {"x": 2, "y": -0.5}},
        {"time": 43.38, "position": {"x": 2, "y": -1}},
        {"time": 43.50, "position": {"x": 1.5, "y": -1}},
        {"time": 43.61, "position": {"x": 1, "y": -1}},
        {"time": 43.73, "position": {"x": 0.5, "y": -1}},
        {"time": 43.84, "position": {"x": 0, "y": -1}},
        {"time": 43.96, "position": {"x": -0.5, "y": -1}},
        {"time": 44.07, "position": {"x": -1, "y": -1}},
        {"time": 44.19, "position": {"x": -1.5, "y": -1}},
        {"time": 44.30, "position": {"x": -2, "y": -1}}
      ],
      "glowing": false,
      "tag": "",
      "description": "L形Drag拖动轨迹"
    }
  ],

  "statistics": {
    "totalNotes": 67,
    "noteBreakdown": {
      "tap": 4,
      "double": 4,
      "hold": 44,
      "flick": 2,
      "drag": 13
    },
    "scorableNotes": 67,
    "maxCombo": 67,
    "maxScore": 1000000,
    "notePerSecond": 1.31
  }
}
```

---

## 示例2: 优化的Hold音符格式

### 方案A: 分离音符（与Skript对应）

```json
{
  "notes": [
    {
      "type": "tap",
      "time": 29.54,
      "face": "w",
      "position": {"x": 2, "y": 1},
      "glowing": false,
      "tag": "",
      "metadata": {"startHold": true}
    },
    {
      "type": "hold",
      "time": 29.55,
      "face": "w",
      "position": {"x": 2, "y": 1},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "hold",
      "time": 29.64,
      "face": "w",
      "position": {"x": 2, "y": 1},
      "glowing": false,
      "tag": ""
    }
    // ... 更多hold节点
  ]
}
```

### 方案B: 合并为序列（推荐）

```json
{
  "notes": [
    {
      "type": "hold_sequence",
      "startTime": 29.54,
      "endTime": 30.84,
      "face": "w",
      "position": {"x": 2, "y": 1},
      "interval": 0.09,
      "duration": 1.3,
      "segmentCount": 15,
      "glowing": false,
      "tag": ""
    }
  ]
}
```

---

## 示例3: Drag路径的不同表示

### 方案A: 完整路径点

```json
{
  "type": "drag_path",
  "startTime": 42.46,
  "endTime": 44.30,
  "face": "w",
  "path": [
    {"time": 42.46, "position": {"x": 2, "y": 1}},
    {"time": 42.69, "position": {"x": 2, "y": 0.5}},
    {"time": 42.92, "position": {"x": 2, "y": 0}}
    // ... 更多点
  ]
}
```

### 方案B: 关键帧+插值

```json
{
  "type": "drag_path",
  "startTime": 42.46,
  "endTime": 44.30,
  "face": "w",
  "keyframes": [
    {"time": 42.46, "position": {"x": 2, "y": 1}},
    {"time": 43.38, "position": {"x": 2, "y": -1}},
    {"time": 44.30, "position": {"x": -2, "y": -1}}
  ],
  "interpolation": {
    "type": "linear",
    "sampleInterval": 0.11
  }
}
```

### 方案C: 分段描述

```json
{
  "type": "drag_path",
  "segments": [
    {
      "segment": 1,
      "type": "vertical_line",
      "startTime": 42.46,
      "endTime": 43.38,
      "from": {"x": 2, "y": 1},
      "to": {"x": 2, "y": -1},
      "steps": 5
    },
    {
      "segment": 2,
      "type": "horizontal_line",
      "startTime": 43.38,
      "endTime": 44.30,
      "from": {"x": 2, "y": -1},
      "to": {"x": -2, "y": -1},
      "steps": 8
    }
  ],
  "face": "w"
}
```

---

## 示例4: 完整铺面（简化版）

这是一个不包含教程事件的纯游戏谱面示例。

```json
{
  "version": "1.0.0",
  "metadata": {
    "id": "example_chart",
    "title": "Example Chart",
    "artist": "Artist Name",
    "charter": "Charter Name",
    "difficulty": {
      "name": "Hard 15",
      "displayName": "&615",
      "color": "&6",
      "level": 15
    },
    "audio": {
      "file": "cr.example",
      "duration": 120
    },
    "timing": {
      "bpm": 180,
      "offset": 200,
      "timeSignature": "4/4"
    },
    "tags": ["hard", "rhythm"]
  },

  "notes": [
    {
      "type": "tap",
      "time": 1.0,
      "face": "w",
      "position": {"x": 0, "y": 0},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "tap",
      "time": 1.5,
      "face": "w",
      "position": {"x": 1, "y": 1},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "double",
      "time": 2.0,
      "face": "w",
      "positions": [
        {"x": -1, "y": -1},
        {"x": 1, "y": 1}
      ],
      "glowing": false,
      "tag": ""
    },
    {
      "type": "flick",
      "time": 3.0,
      "face": "w",
      "turn": "right",
      "glowing": false,
      "tag": ""
    },
    {
      "type": "flick",
      "time": 3.5,
      "face": "d",
      "turn": "right",
      "glowing": false,
      "tag": ""
    },
    {
      "type": "hold_sequence",
      "startTime": 4.0,
      "endTime": 5.0,
      "face": "s",
      "position": {"x": 0, "y": 0},
      "interval": 0.1,
      "glowing": false,
      "tag": ""
    },
    {
      "type": "drag_path",
      "startTime": 6.0,
      "endTime": 7.0,
      "face": "a",
      "path": [
        {"time": 6.0, "position": {"x": -2, "y": 0}},
        {"time": 6.25, "position": {"x": -1, "y": 1}},
        {"time": 6.5, "position": {"x": 0, "y": 0}},
        {"time": 6.75, "position": {"x": 1, "y": -1}},
        {"time": 7.0, "position": {"x": 2, "y": 0}}
      ],
      "glowing": false,
      "tag": ""
    }
  ]
}
```

---

## 高级特性示例

### 1. 带标签的音符组

```json
{
  "notes": [
    {
      "type": "tap",
      "time": 10.0,
      "face": "w",
      "position": {"x": 0, "y": 0},
      "glowing": true,
      "tag": "chorus_start",
      "metadata": {
        "section": "chorus",
        "color": "blue"
      }
    },
    {
      "type": "tap",
      "time": 10.5,
      "face": "w",
      "position": {"x": 1, "y": 0},
      "glowing": true,
      "tag": "chorus_start"
    }
  ]
}
```

### 2. 多判定面组合

```json
{
  "notes": [
    {
      "type": "tap",
      "time": 15.0,
      "face": "w",
      "position": {"x": 0, "y": 0},
      "glowing": false,
      "tag": ""
    },
    {
      "type": "tap",
      "time": 15.0,
      "face": "s",
      "position": {"x": 0, "y": 0},
      "glowing": false,
      "tag": "",
      "description": "同时在前后两个判定面出现"
    }
  ]
}
```

### 3. 动态BPM变化

```json
{
  "timing": {
    "bpm": 180,
    "bpmChanges": [
      {"time": 0, "bpm": 180},
      {"time": 30, "bpm": 200},
      {"time": 60, "bpm": 160}
    ],
    "offset": 0
  }
}
```

---

## 验证和错误检测

### JSON Schema 验证示例

```json
{
  "validation": {
    "errors": [],
    "warnings": [
      {
        "type": "overlap",
        "message": "音符在时间15.0处重叠",
        "details": {
          "time": 15.0,
          "noteCount": 2,
          "faces": ["w", "s"]
        }
      },
      {
        "type": "out_of_bounds",
        "message": "音符位置超出范围",
        "details": {
          "noteIndex": 42,
          "position": {"x": 4, "y": 2},
          "validRange": {"x": [-3, 3], "y": [-3, 3]}
        }
      }
    ],
    "statistics": {
      "totalNotes": 150,
      "averageDensity": 1.25,
      "peakDensity": 3.5,
      "estimatedDifficulty": 16
    }
  }
}
```

---

## 转换工具配置示例

### Skript到JSON转换��配置

```json
{
  "converter": {
    "input": {
      "propertiesFile": "simpletone_properties.sk",
      "contentFile": "simpletone.sk"
    },
    "output": {
      "file": "simpletone.json",
      "format": "pretty",
      "indent": 2
    },
    "options": {
      "mergeHolds": true,
      "mergeDrags": true,
      "extractEvents": true,
      "validateOutput": true,
      "generateStats": true
    },
    "mapping": {
      "tap": "keep_separate",
      "hold": "merge_to_sequence",
      "drag": "merge_to_path",
      "flick": "keep_separate",
      "double": "keep_separate",
      "execution": "convert_to_events"
    }
  }
}
```

---

## 附录：完整字段说明

### Metadata 字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| id | string | ✓ | 谱面唯一标识符 |
| title | string | ✓ | 曲名 |
| artist | string | ✓ | 作曲家 |
| charter | string | ✓ | 谱师 |
| difficulty.name | string | ✓ | 难度名称 |
| difficulty.level | number | - | 难度等级 |
| audio.file | string | ✓ | 音频文件名 |
| timing.bpm | number | ✓ | BPM |
| timing.offset | number | - | 偏移量（毫秒） |

### Note 字段

| 字段 | 类型 | 适用音符 | 说明 |
|------|------|---------|------|
| type | string | 全部 | 音符类型 |
| time | number | 全部 | 时间（秒） |
| face | string | 全部 | 判定面 |
| position | object | tap/hold/drag | 单点位置 |
| positions | array | double | 双点位置 |
| turn | string | flick | 方向 |
| glowing | boolean | 全部 | 是否发光 |
| tag | string | 全部 | 自定义标签 |

---

## 版本历史

- **v1.0.0** (2025-10-26)
  - 初始版本
  - 完整的simpletone谱面JSON示例
  - 多种音符格式方案
  - 高级特性和验证示例
