# FLICK 音符判定修正说明

## 修正日期
2025-02-14

## 问题描述

用户指出 FLICK 音符的判定逻辑理解错误：

**错误理解:**
- FLICK 音符需要先点击，然后在 0.5 秒内检测转头

**正确理解:**
- FLICK 音符不需要点击
- 当音符到达判定面时，自动检测玩家视角是否在目标方向范围内（45°-135°）
- 如果视角在范围内，判为 EXACT；否则判为 MISS

---

## 修正方案

### FLICK 音符的正确判定逻辑

FLICK 音符应该是**自动判定类型**，类似于 DRAG 和 HOLD：

| 音符类型 | 判定方式 | 检测内容 |
|---------|---------|---------|
| DRAG | 自动 | 准心是否指向音符 |
| HOLD | 自动 | 是否有按键按下 |
| FLICK | 自动 | 视角是否在目标方向范围内 |
| TAP | 点击 | 点击时间 |
| DOUBLE | 点击 | 双击时间 |

---

## 实现细节

### 1. 移除点击处理

**修改位置:** `InputHandler.java`

**修改内容:**
- 从 `handleClickInput()` 中移除 FLICK 音符的处理
- 删除 `handleFlickNote()` 方法
- 删除 `flickWaiting` Map
- FLICK 音符不再响应点击事件

```java
// 修改后的 handleClickInput
private void handleClickInput(Player player) {
    // ... 射线检测代码 ...

    // 处理特殊音符类型
    if (noteEntity.getType() == NoteType.DOUBLE) {
        handleDoubleNote(noteEntity, judgment, timingOffset);
    } else if (noteEntity.getType() == NoteType.TAP) {
        processJudgment(noteEntity, judgment, timingOffset);
    }
    // DRAG、HOLD 和 FLICK 音符不应该通过点击判定
}
```

### 2. 添加自动判定逻辑

**修改位置:** `GameSession.java`

**修改内容:**
- 在 `tick()` 方法的自动判定逻辑中添加 FLICK 音符处理
- 当 FLICK 音符接近判定线时（距离 < 2 格），检测玩家视角
- 使用 `checkFlickDirection()` 方法判断视角是否在目标方向

```java
} else if (entity.getType() == org.cubeRhythm.note.NoteType.FLICK) {
    // FLICK: 检查玩家视角是否在目标方向范围内（45°-135°）
    String targetDirection = entity.getTurn();
    if (targetDirection != null) {
        float playerYaw = org.cubeRhythm.input.ViewDirectionHelper.getPlayerYaw(player);

        // 根据判定面和转向方向计算目标 yaw 范围
        boolean isInRange = checkFlickDirection(entity.getFace(), targetDirection, playerYaw);

        if (isInRange) {
            int timingOffset = judgmentManager.calculateTimingOffset(entity, settings.getSpeed());
            org.cubeRhythm.judgment.JudgmentResult judgment = judgmentManager.judge(timingOffset);

            if (judgment != org.cubeRhythm.judgment.JudgmentResult.MISS) {
                // FLICK 音符无 JUST 判定，转换为 EXACT
                if (judgment == org.cubeRhythm.judgment.JudgmentResult.JUST) {
                    judgment = org.cubeRhythm.judgment.JudgmentResult.EXACT;
                }

                entity.setHit(true);
                scoreManager.recordJudgment(judgment);

                // 播放打击音效
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.5f, 1.5f);

                // 显示判定文本
                if (inputHandler != null) {
                    inputHandler.showJudgmentText(entity, judgment, timingOffset);
                }

                // 清理音符实体
                entity.cleanup();
                entityManager.unregisterEntity(entity.getLinkUUID());
            }
        }
    }
}
```

### 3. 实现方向检测方法

**修改位置:** `GameSession.java`

**新增方法:** `checkFlickDirection()`

**逻辑说明:**

1. **获取判定面的基准 yaw:**
   - W (前方): 0°
   - A (左侧): 90°
   - S (后方): 180°
   - D (右侧): -90°

2. **计算目标方向的 yaw:**
   - 如果 turn = "left": 基准 yaw + 90°
   - 如果 turn = "right": 基准 yaw - 90°

3. **检查玩家 yaw 是否在范围内:**
   - 计算玩家 yaw 与目标 yaw 的角度差
   - 如果角度差 ≤ 45°，则在范围内

**代码实现:**
```java
private boolean checkFlickDirection(org.cubeRhythm.coordinate.Face face, String targetDirection, float playerYaw) {
    // 标准化玩家 yaw 到 -180 到 180 范围
    playerYaw = normalizeAngle(playerYaw);

    // 获取判定面的基准 yaw
    float baseYaw = switch (face) {
        case W -> 0f;      // 前方
        case A -> 90f;     // 左侧
        case S -> 180f;    // 后方
        case D -> -90f;    // 右侧
    };

    // 计算目标方向的 yaw
    float targetYaw;
    if (targetDirection.equalsIgnoreCase("left")) {
        targetYaw = normalizeAngle(baseYaw + 90f);
    } else {
        targetYaw = normalizeAngle(baseYaw - 90f);
    }

    // 计算角度差
    float angleDiff = Math.abs(normalizeAngle(playerYaw - targetYaw));

    // 检查是否在 ±45° 范围内
    return angleDiff <= 45f;
}

private float normalizeAngle(float angle) {
    while (angle > 180f) {
        angle -= 360f;
    }
    while (angle < -180f) {
        angle += 360f;
    }
    return angle;
}
```

### 4. 清理不需要的代码

**修改位置:**
- `NoteEntity.java`: 删除 `waitingForFlickRotation` 字段
- `InputHandler.java`: 删除 `flickWaiting` Map 和相关清理代码

---

## 判定范围说明

### FLICK 音符的方向判定

**示例 1: 前方面板 (W) 向左转**
```
音符: face=W, turn="left"
基准 yaw: 0°
目标 yaw: 0° + 90° = 90° (向左看)
判定范围: 45° ~ 135° (目标 yaw ±45°)
```

**示例 2: 左侧面板 (A) 向右转**
```
音符: face=A, turn="right"
基准 yaw: 90°
目标 yaw: 90° - 90° = 0° (向前看)
判定范围: -45° ~ 45° (目标 yaw ±45°)
```

**示例 3: 后方面板 (S) 向左转**
```
音符: face=S, turn="left"
基准 yaw: 180°
目标 yaw: 180° + 90° = 270° = -90° (向右看)
判定范围: -135° ~ -45° (目标 yaw ±45°)
```

### 角度范围可视化

```
        -90° (D面)
           |
           |
180° ------+------ 0° (W面)
(S面)      |
           |
        90° (A面)
```

**FLICK 判定范围 (±45°):**
- 目标方向 ±45° 范围内 → EXACT
- 超出范围 → MISS

---

## 测试验证

### 测试用例

**测试 1: W 面向左 FLICK**
```
音符: face=W, turn="left"
玩家 yaw: 90° (正好向左)
预期: EXACT
```

**测试 2: W 面向左 FLICK (边界)**
```
音符: face=W, turn="left"
玩家 yaw: 45° (左前方)
预期: EXACT (在 ±45° 范围内)
```

**测试 3: W 面向左 FLICK (超出范围)**
```
音符: face=W, turn="left"
玩家 yaw: 0° (正前方)
预期: MISS (超出 ±45° 范围)
```

**测试 4: A 面向右 FLICK**
```
音符: face=A, turn="right"
玩家 yaw: 0° (向前)
预期: EXACT
```

**测试 5: 不看向目标方向**
```
音符: face=W, turn="left"
玩家 yaw: -90° (向右)
预期: MISS
```

---

## 与其他音符类型的对比

| 音符类型 | 触发方式 | 检测内容 | JUST判定 | 音效 |
|---------|---------|---------|---------|------|
| TAP | 点击 | 点击时间 | ✅ 有 | ✅ 有 |
| DOUBLE | 点击 | 双击时间 | ✅ 有 | ✅ 有 |
| DRAG | 自动 | 准心指向 | ❌ 无 | ✅ 有 |
| HOLD | 自动 | 按键状态 | ❌ 无 | ❌ 无 |
| FLICK | 自动 | 视角方向 | ❌ 无 | ✅ 有 |

---

## 修改文件总结

1. **GameSession.java**
   - ✅ 添加 FLICK 自动判定逻辑
   - ✅ 添加 `checkFlickDirection()` 方法
   - ✅ 添加 `normalizeAngle()` 方法
   - ✅ 移除 FLICK 等待检测的特殊处理

2. **InputHandler.java**
   - ✅ 从 `handleClickInput()` 移除 FLICK 处理
   - ✅ 删除 `handleFlickNote()` 方法
   - ✅ 删除 `flickWaiting` Map
   - ✅ 清理相关代码

3. **NoteEntity.java**
   - ✅ 删除 `waitingForFlickRotation` 字段

---

## 验证清单

- [x] FLICK 音符不响应点击
- [x] FLICK 音符在到达判定面时自动检测视角
- [x] 视角在目标方向 ±45° 范围内判为 EXACT
- [x] 视角超出范围判为 MISS
- [x] FLICK 音符无 JUST 判定
- [x] FLICK 音符有打击音效
- [x] 判定文本正确显示

---

## 后续优化建议

### 1. 调整判定范围

如果 ±45° 范围在实际游玩中太严格或太宽松，可以调整：

```java
// 更宽松（±60°）
return angleDiff <= 60f;

// 更严格（±30°）
return angleDiff <= 30f;
```

### 2. 添加视觉反馈

可以考虑添加：
- 当玩家视角进入判定范围时，FLICK 音符发光或变色
- 显示方向指示器，帮助玩家对准
- 判定成功时的特殊粒子效果

### 3. 调试信息

在开发阶段可以添加调试信息：
```java
player.sendActionBar(String.format(
    "§7FLICK: target=%.1f° player=%.1f° diff=%.1f°",
    targetYaw, playerYaw, angleDiff
));
```

---

*修正完成时间: 2025-02-14*
*修正者: Claude Code*
