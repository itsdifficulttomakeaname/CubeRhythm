# 非对称判定窗口系统实现

## 实现日期
2026-02-15

## 判定系统规则

### 判定窗口（除 FLICK 外所有音符）

**时间轴:**
```
... | -80ms | 0ms (判定线) | +80ms | +200ms | ...
    |  EXACT  |    EXACT    | JUST  |       |
    |         判定窗口        |       | MISS  |
不处理                                      MISS
```

**判定规则:**
- **< -80ms**: 不处理（避免误触）
- **-80ms ~ +80ms**: EXACT 判定
- **+81ms ~ +200ms**: JUST 判定
- **> +200ms**: MISS 判定

### 非对称设计原因

**早打（Early）更严格:**
- 只允许提前 80ms
- 超过 80ms 不处理，避免误触

**晚打（Late）更宽容:**
- 允许延迟 200ms
- 符合音游习惯（晚打比早打更容易接受）

## 实现细节

### 1. JudgmentResult.fromTimingOffset()

**修改前（对称窗口）:**
```java
int absOffset = Math.abs(offsetMs);
if (absOffset <= 80) return EXACT;
else if (absOffset <= 200) return JUST;
else return MISS;
```

**修改后（非对称窗口）:**
```java
// 判定面前超过 80ms：不在判定范围内
if (offsetMs < -80) {
    return null;  // 不处理，避免误触
}

// 判定面后超过 200ms：MISS
if (offsetMs > 200) {
    return MISS;
}

// EXACT 窗口：-80ms 到 +80ms
if (offsetMs >= -80 && offsetMs <= 80) {
    return EXACT;
}

// JUST 窗口：+81ms 到 +200ms
if (offsetMs >= 81 && offsetMs <= 200) {
    return JUST;
}
```

### 2. InputHandler 更新

**添加 null 检查:**
```java
JudgmentResult judgment = judgmentManager.judge(timingOffset);

// 如果不在判定窗口内（提前超过 80ms），不处理
if (judgment == null) {
    org.cubeRhythm.Main.instance.getLogger().info(
        String.format("点击过早，不在判定窗口内: type=%s, offset=%dms",
            noteEntity.getType(), timingOffset)
    );
    return;
}
```

**更新位置:**
- `onPlayerInteractEntity()` - 直接点击实体判定
- `handleNoteClick()` - 射线检测判定

### 3. GameSession DRAG 判定更新

**修改前:**
```java
// 只处理在 200ms 判定窗口内的音符
if (Math.abs(timingOffset) <= 200) {
    // 判定处理
}
```

**修改后:**
```java
JudgmentResult judgment = judgmentManager.judge(timingOffset);

// 如果不在判定窗口内，不处理
if (judgment == null) {
    continue;  // 提前超过 80ms，不处理
}
```

## 判定窗口对比

### 修改前（对称窗口）
| 时间范围 | 判定结果 |
|---------|---------|
| < -200ms | MISS |
| -200ms ~ -81ms | JUST |
| -80ms ~ +80ms | EXACT |
| +81ms ~ +200ms | JUST |
| > +200ms | MISS |

### 修改后（非对称窗口）
| 时间范围 | 判定结果 |
|---------|---------|
| < -80ms | **不处理** |
| -80ms ~ +80ms | EXACT |
| +81ms ~ +200ms | JUST |
| > +200ms | MISS |

## 优势

### 1. 避免误触
- 玩家提前点击不会触发判定
- 减少意外 MISS
- 提升游戏体验

### 2. 符合音游习惯
- 早打窗口更严格（80ms）
- 晚打窗口更宽容（200ms）
- 符合玩家直觉

### 3. 更精确的判定
- 明确的判定边界
- 不会处理过早的输入
- 减少判定混乱

## 适用音符类型

**适用:**
- ✅ TAP
- ✅ DRAG
- ✅ HOLD
- ✅ DOUBLE

**不适用:**
- ❌ FLICK（有特殊判定逻辑）
- ❌ EXECUTION（不需要判定）

## 测试建议

### 功能测试
1. **早打测试:**
   - 提前 100ms 点击 → 不应触发判定
   - 提前 80ms 点击 → 应该是 EXACT
   - 提前 50ms 点击 → 应该是 EXACT

2. **晚打测试:**
   - 延迟 50ms 点击 → 应该是 EXACT
   - 延迟 100ms 点击 → 应该是 JUST
   - 延迟 200ms 点击 → 应该是 JUST
   - 延迟 250ms 点击 → 应该是 MISS

3. **DRAG 音符测试:**
   - 提前瞄准 → 不应触发
   - 在窗口内瞄准 → 应该触发

### 边界测试
- -81ms → 不处理
- -80ms → EXACT
- +80ms → EXACT
- +81ms → JUST
- +200ms → JUST
- +201ms → MISS

## 修改文件

- `JudgmentResult.java` - 实现非对称判定逻辑
- `InputHandler.java` - 添加 null 检查（2 处）
- `GameSession.java` - 更新 DRAG 判定逻辑

## 已知影响

### 正面影响
- ✅ 减少误触
- ✅ 判定更准确
- ✅ 游戏体验更好

### 需要注意
- ⚠️ 玩家需要适应新的判定窗口
- ⚠️ 早打习惯的玩家可能需要调整
- ⚠️ 日志中会出现"点击过早"的信息

## 未来优化

1. **可配置判定窗口:**
   - 允许在 config.yml 中配置窗口大小
   - 支持不同难度使用不同窗口

2. **视觉反馈:**
   - 显示"Too Early"提示
   - 帮助玩家理解判定窗口

3. **统计信息:**
   - 记录过早点击次数
   - 帮助玩家改进时机

---

*实现者: Claude Code*
*状态: 已完成*
*测试状态: 待测试*
