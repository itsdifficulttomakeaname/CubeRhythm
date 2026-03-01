# 编辑器原始实现流程分析 (基于 scripts/editor.sk)

## 1. 核心数据结构

### 全局变量
- `{editMode}`: 布尔值，是否处于编辑模式
- `{editor::bpm}`: 数字，每分钟拍数（默认 120）
- `{editor::tick}`: 整数，当前时间刻（基于步长）
- `{editor::length}`: 整数，步长（1拍分成几份，默认 1）
- `{editor::preTime}`: 数字，第一拍前的额外时间（默认 0）
- `{editor::noteType}`: 字符串，当前选择的音符类型（"tap", "double", "drag", "hold", "flickL", "flickR"）
- `{editor::face}`: 字符串，当前选择的判定面（"w", "a", "s", "d"） <- 新实现:音符直接渲染到对应面上，放置音符时的预览音符直接渲染到对应判定面
- `{editor::glowing}`: 布尔值，音符是否发光

### 音符存储 (editorSave)
#### 旧实现
每个音符使用 UUID 作为 key，存储以下数据：
- `{editorSave::type::%id%}`: 音符类型
- `{editorSave::time::%id%}`: 音符时间（秒）
- `{editorSave::loc::%id%}`: 音符位置（Location 对象）
- `{editorSave::face::%id%}`: 判定面
- `{editorSave::turn::%id%}`: FLICK 方向（"left" 或 "right"）
- `{editorSave::glowing::%id%}`: 是否发光
- `{editorSave::tag::%id%}`: 标签文本
- `{editorSave::section::%id%}`: EXECUTION 音符的 section 名称
#### 新实现
动态存储，按照json标准化格式直接输出到铺面文件中

## 2. 初始化流程 (on load)

```
设置默认值（如果未设置）:
  {editor::bpm} = 120
  {editor::tick} = 0
  {editor::length} = 1
  {editor::preTime} = 0
```

## 3. 进入/退出编辑模式 (/editor 命令)

### 进入编辑模式
```
1. 清空 {editorSave::*}
2. 设置 {editMode} = true
3. 初始化编辑器状态:
   - {editor::noteType} = "tap"
   - {editor::face} = "w" # 新实现中不再以文本形式区分朝向，而是直接区分朝向
   - {editor::glowing} = false
4. 显示帮助信息
5. 清空玩家背包
6. 给予编辑工具:
   - Slot 0: 浅蓝色羊毛（音符类型指示器）
   - Slot 4: 魔杖（放置/删除工具）
   - Slot 8: 白色玻璃（判定面指示器）
7. 设置 hotbar slot = 4（魔杖）
8. 在 z=4 处放置玻璃块标记判定面:
   - 范围: x ∈ [-3, 3], y ∈ [-3, 3], z = 4
   # 新实现因为要直接在每一个面上渲染(预览)音符，所以每一个判定面都要填充玻璃
   # 判定面的位置参考 facing-explanation.md 文件中的介绍
```

### 退出编辑模式
```
1. 删除 {editMode}
2. 清空玩家背包
3. 清空所有编辑器数据:
   - {editorSave::*}
   - {editor::noteType}
4. 移除判定面标记（设置为空气）
5. 删除所有 BlockDisplay 实体
```

## 4. Action Bar 显示 (every 2 ticks)

**显示格式:**
```
BPM: {bpm}   第 {小节号}小节 {拍位置}/{步长}拍 ({时间}s)   共计 {音符数量} Notes
```

**计算公式:**
- 小节号 = floor(tick / length) + 1
- 拍位置 = tick % length
- 时间 = tick / length * (60 / bpm) + preTime
- 音符数量 = size of {editorSave::type::*}

**示例:**
```
BPM: 120   第 5小节 2/4拍 (2.5s)   共计 15 Notes
```

## 5. 预览光标系统 (every ticks)

### 触发条件
- {editMode} = true
- 玩家手持魔杖
- 玩家不是旁观模式

### 光标位置计算

**射线投射算法:**
```
1. 目标平面: z = 4
2. 玩家位置: (playerX, playerY, playerZ)
3. 视线方向向量: (dirX, dirY, dirZ)

4. 计算射线参数 t:
   rate = abs(4 - playerZ) / dirZ # 这个实现基于玩家在 0,y,0 的位置，而且是基于旧实现只在W面操作，需要更新

5. 计算交点坐标:
   targetX = dirX * rate + playerX
   targetY = dirY * rate + playerY + eyeOffset

   eyeOffset = 1.4 (如果按住 Shift)
             = 1.6 (正常情况)

6. 范围检查:
   targetX ∈ [-3, 4]
   targetY ∈ [-3, 4]

7. 网格对齐（如果按住 Shift）:
   targetX = round(targetX * 2) / 2
   targetY = round(targetY * 2) / 2
```

### 预览实体创建
```
1. 删除所有旧的预览实体（metadata "isPreview" = true） # 不一定是定时任务，可以是当玩家视角发生变化时(PlayerMoveEvent)

2. 如果坐标在有效范围内:
   创建 BlockDisplay 在 location(targetX, targetY, 4):
     - 材质根据 {editor::noteType} 选择:
       * tap: light_blue_concrete
       * double: orange_concrete
       * hold: white_concrete
       * drag: yellow_concrete
       * flickL: magenta_concrete
       * flickR: red_concrete
     - scale = vector(1, 1, 1)
     - glowing = true
     - translation = vector(-0.5, -0.5, 0.1)
     - glow color 根据 {editor::face} 选择:
       * w: RGB(255, 255, 255) 白色
       * a: RGB(255, 235, 42) 黄色
       * s: RGB(255, 150, 0) 橙色
       * d: RGB(255, 0, 0) 红色
     # 新实现没有文本区分朝向，不需要在颜色上下功夫
     - display name = "&a+++" (如果 {editor::glowing} = true)
     - metadata "isPreview" = true
```

## 6. 音符渲染系统 (addTick 函数)

### 触发时机
- 时间导航时（前进/后退）
- 通过 tool change event 调用

### 渲染流程

```
1. 计算当前时间:
   currentTime = tick / length * (60 / bpm) + preTime

2. 删除所有旧的音符实体:
   遍历所有 BlockDisplay
   如果 metadata "id" 存在，删除该实体

3. 遍历所有已保存的音符:
   对于每个 {editorSave::time::%id%}:

   a. 计算 z 偏移:
      noteTime = {editorSave::time::%id%}
      z = (noteTime - currentTime) * 20 * speed

   b. 可见性检查:
      只渲染 z ∈ [-1, 50] 范围内的音符

   c. 计算渲染位置:
      renderLoc = {editorSave::loc::%id%}.add(0, 0, z)

   d. 创建 BlockDisplay 在 renderLoc:
      - 材质根据音符类型选择（同预览光标）
      - scale = vector(1, 1, 1)
      - 特殊: HOLD 音符 scale = vector(1, 1, 3*speed)
      - glowing = true
      - translation = vector(-0.5, -0.5, 0.1)
      - 特殊: HOLD 音符 translation = vector(-0.5, -0.5, 0.2)
      - glow color 根据 face 选择
      - 特殊: EXECUTION 音符 glow color = RGB(0, 255, 0) 绿色
      - display name 根据 glowing 和 tag 设置:
        * EXECUTION: "Section: {section名称}"
        * glowing=true 且 tag 非空: "&aTag: {tag}"
        * glowing=true 且 tag 为空: "&a+++"
        * glowing=false 且 tag 非空: "&fTag: {tag}"
        * glowing=false 且 tag 为空: 无名称
      - metadata "id" = {id}
```

### 渲染范围说明
- **可见范围**: z ∈ [-1, 50]
  - z = -1: 音符刚过判定线
  - z = 50: 音符在远处即将进入视野
- **判定线位置**: z = 4（玩家所在平面）
- **音符移动**: 从 z=50 向 z=4 移动
```
此处基于玩家在 Location:{0,y,0} 的位置来设计，需要转化为相对坐标
```

## 7. 时间导航 (tool change event)

### 触发条件
- {editMode} = true
- 玩家不是旁观模式

### 操作映射

```
Slot 1/2/3 (前进):
  1. addTick(player, 1)
  2. 显示 subtitle: "&a>>> &7&o#{小节号} {拍位置}/{步长} &a>>>"
  3. 播放音效: "block.note_block.hat" pitch 2

Slot 5/6/7 (后退):
  1. addTick(player, -1)
  2. 显示 subtitle: "&a<<< &7&o#{小节号} {拍位置}/{步长} &a<<<"
  3. 播放音效: "block.note_block.hat" pitch 1.5

Slot 0 (切换音符类型):
  循环顺序: tap → double → drag → hold → flickL → flickR → tap
  1. 更新 {editor::noteType}
  2. 更新 slot 0 的羊毛颜色
  3. 显示 subtitle 显示当前类型
  4. 播放音效: "ui.button.click" pitch 2

Slot 8 (切换判定面):
  循环顺序: w → a → s → d → w
  1. 更新 {editor::face}
  2. 更新 slot 8 的玻璃颜色
  3. 显示 subtitle: "&7判定面: {面名称}"
  4. 播放音效: "ui.button.click" pitch 2
```

## 8. 放置音符 (right click)

### 触发条件
- {editMode} = true
- 玩家手持魔杖
- 玩家不是旁观模式

### 放置流程

```
1. 计算放置位置（与预览光标相同算法）: <- 注意适配新实现
   - 射线投射到 z=4 平面
   - 计算交点 (targetX, targetY)
   - 范围检查: x ∈ [-3, 4], y ∈ [-3, 4]
   - Shift 时对齐到 0.5 网格

2. 显示坐标 subtitle: "{targetX - 0.5}, {targetY - 0.5}"

3. 生成音符 ID:
   id = random uuid

4. 计算 HOLD 音符长度:
   length = 3 * speed

5. 创建 BlockDisplay 在 location(targetX, targetY, 4):
   - 材质根据 {editor::noteType} 选择
   - scale = vector(1, 1, 1)
   - 特殊: HOLD 音符 scale = vector(1, 1, length)
   - glowing = true
   - translation = vector(-0.5, -0.5, 0.1)
   - 特殊: HOLD 音符 translation = vector(-0.5, -0.5, 0.2)
   - glow color 根据 {editor::face} 选择
   - display name = "&a+++" (如果 {editor::glowing} = true)
   - metadata "id" = id

6. 保存音符数据: <- 新实现动态存储
   {editorSave::type::%id%} = {editor::noteType}
   {editorSave::time::%id%} = tick/length*(60/bpm)+preTime
   {editorSave::loc::%id%} = location(targetX, targetY, 4)
   {editorSave::face::%id%} = {editor::face}
   {editorSave::turn::%id%} = "left" (如果 flickL) 或 "right" (如果 flickR)
   {editorSave::glowing::%id%} = {editor::glowing}
   {editorSave::tag::%id%} = ""
```

## 9. 删除音符 (left click)

### 触发条件
- {editMode} = true
- 玩家手持魔杖
- 玩家不是旁观模式

### 删除流程

```
1. 找到预览光标实体:
   遍历所有 BlockDisplay
   找到 metadata "isPreview" = true 的实体

2. 找到最近的音符:
   nearest block display relative to 预览光标
   距离 <= 1

3. 获取音符 ID:
   id = metadata "id" of 最近的音符

4. 删除音符数据: <- 新实现直接在铺面文件中移除这个NOTE
   delete {editorSave::type::%id%}
   delete {editorSave::time::%id%}
   delete {editorSave::loc::%id%}
   delete {editorSave::face::%id%}
   delete {editorSave::turn::%id%}
   delete {editorSave::section::%id%}
   delete {editorSave::glowing::%id%}
   delete {editorSave::tag::%id%}

5. 删除音符实体:
   delete 最近的音符实体
```

## 10. 其他命令

### /b <number> - 跳转到指定拍
```
1. 检查 {editMode} = true
2. 计算 tick:
   tick = (number - 1) * length
3. 显示确认 subtitle
```

### /step <integer> - 设置步长
```
1. 检查 {editMode} = true
2. 保存旧步长: pastLength = {editor::length}
3. 设置新步长: {editor::length} = integer
4. 调整当前 tick:
   tick = tick * newLength / pastLength
5. 显示确认 subtitle
```

### /editor bpm <number> - 设置 BPM
```
1. 设置 {editor::bpm} = number
2. 显示确认 subtitle
```

### /editor pretime <number> - 设置预时间
```
1. 设置 {editor::preTime} = number
2. 显示确认 subtitle
```

### /editor execution <section> - 放置 EXECUTION 音符
```
1. 检查 {editMode} = true
2. 生成 ID: id = random uuid
3. 创建 BlockDisplay 在 location(0.5, 0.5, 4):
   - 材质: black stained glass
   - scale = vector(1, 1, 1)
   - glowing = true
   - translation = vector(-0.5, -0.5, 0.1)
   - glow color = RGB(255, 0, 255) 紫色
   - display name = "Section: {section}"
   - metadata "id" = id
4. 保存数据:
   {editorSave::type::%id%} = "execution"
   {editorSave::time::%id%} = tick/length*(60/bpm)+preTime
   {editorSave::loc::%id%} = location(0.5, 0.5, 4)
   {editorSave::section::%id%} = section
```

### /editor tag <text> - 设置音符标签
```
1. 检查 {editMode} = true
2. 找到预览光标指向的音符（与删除音符相同逻辑）
3. 设置或删除标签:
   {editorSave::tag::%id%} = text (如果提供)
   {editorSave::tag::%id%} = "" (如果未提供)
4. 更新音符实体的 display name
```

### /editor regive - 重新给予编辑工具
```
1. 检查 {editMode} = true
2. 清空背包
3. 重新给予编辑工具（同进入编辑模式）
```

## 11. 发光切换 (swap hand items)

### 触发条件
- {editMode} = true

### 切换流程
```
1. 取消事件
2. 切换 {editor::glowing}:
   如果 false → true: 显示 "&7发光: &a开"
   如果 true → false: 显示 "&7发光: &c关"
3. 播放音效: "ui.button.click" pitch 2
```

## 12. 关键设计特点

### 坐标系统
- **判定面位置**: 固定在 z = 4
- **坐标范围**: x ∈ [-3, 4], y ∈ [-3, 4]
- **网格对齐**: 0.5 块精度（Shift 时）
- **眼睛高度偏移**: 1.6（正常）或 1.4（Shift）

### 时间系统
- **Tick 系统**: 使用整数 tick 表示时间位置
- **步长**: 可变，表示 1 拍分成几份
- **时间计算**: time = tick / length * (60 / bpm) + preTime
- **小节计算**: beat = floor(tick / length) + 1

### 渲染系统
- **预览光标**: 每 tick 更新，实时跟随视线
- **音符渲染**: 只在时间导航时更新
- **可见范围**: z ∈ [-1, 50]（相对于当前时间）
- **HOLD 音符**: 长度 = 3 * speed

### 单面限制
- **原始实现只支持单个判定面** (z = 4)
- 判定面选择 (w/a/s/d) 只影响音符的 face 属性和发光颜色 
- 所有音符都渲染在同一个 z = 4 平面上
- 没有多面坐标转换系统

## 13. 与 Java 实现的差异

### 需要改进的地方
1. **多面支持**: 需要实现 4 个判定面的坐标转换
2. **自动面检测**: 原始实现需要手动切换面，应该自动检测
3. **音符渲染**: 应该支持多面同时渲染
4. **预览光标**: 应该支持多面预览

### 保持的设计
1. **射线投射算法**: 计算视线与平面交点的方法
2. **网格对齐**: Shift 对齐到 0.5 网格
3. **时间系统**: tick/length 的设计
4. **渲染范围**: z ∈ [-1, 50] 的可见范围
5. **HOLD 长度**: 3 * speed 的计算方式

## 总结
我在sk的实现中添加了一些注释，需要根据那些注释来进一步更正实现

把sk语言转换为java语言表达，而先不实现在项目中
