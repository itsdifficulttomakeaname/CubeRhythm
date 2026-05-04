# TODOs

## 关于各朝向的渲染问题:
> 所有描述的均为测试中观察到的现象，(一般的)对描述取反即为你的修改操作

### 声明
- 从./resources/config.yml中可以获取字段 location 的值，表示玩家在开始游戏后被传送到的地方(不确定是代码加了修正还是游戏机制，传入的是整数坐标，但是玩家被修正到方块正中央，类似于(x.5,y,z.5)，不在乎这个，我们只需要使用直接从config获取的坐标值)
- 假定获取到Location:(x,y,z)
- 所有面向上都是Y+，向下都是Y-
- 对于W面，中心坐标为 (x, y+1, z+3)，向左为X+，向右为X-
- 对于A面，中心坐标为 (x+3, y+1, z)，向左为Z-，向右为Z+
- 对于S面，中心坐标为 (x, y+1, z-3)，向左为X-，向右为X+
- 对于D面，中心坐标为 (x-3, y+1, z)，向左为Z+，向右为Z-
- 对于下文提到的向左/向右，理解为面对判定面的向左或向右，和上面的描述对应即可

### 都有的问题
```markdown
问题1：HOLD 音符 A/D 面判定异常

根本原因：calculateDistance 的符号问题

transformCoordinates 中：
- W 面：worldZ = centerZ + z（z 增大 → 远离中心）
- A 面：worldX = centerX + z（z 增大 → 远离中心）
- S 面：worldZ = centerZ - z（z 增大 → 远离中心）
- D 面：worldX = centerX - z（z 增大 → 远离中心）

calculateDistance 中：
- W 面：notePos.getZ() - (centerZ + 3) ✅ z 大 → distance 大
- A 面：notePos.getX() - (centerX + 3) ✅ x 大 → distance 大
- S 面：(centerZ - 3) - notePos.getZ() ✅ z 小 → distance 大
- D 面：(centerX - 3) - notePos.getX() — 但 D 面 worldX = centerX - z，所以 notePos.getX() = centerX - z，代入得 (centerX-3) - (centerX-z) = z - 3 ✅

这部分是对的。真正的问题在 HOLD 的 scaleZ 方向：

renderNote 中 BlockDisplay 创建时 scale 是 (1, 1, scaleZ)，即沿局部 Z 轴（即 Minecraft 世界 Z 轴）延伸。但对于 A/D 面，音符的移动方向是世界 X 轴，而 BlockDisplay 的 scaleZ 仍然沿世界 Z 轴延伸，导致 HOLD 条实际上是向侧面延伸而非向来向延伸。

因此 holdScaleZ 存储的是视觉上的 Z 轴长度，但 calculateDistance 减去它时假设它等于沿移动方向的长度——对 W/S 面成立，对 A/D 面不成立（A/D 面移动方向是 X 轴，scaleZ 延伸的是 Z 轴）。

结论：A/D 面的 HOLD 条视觉上是横向延伸的（沿 Z 轴），而不是沿移动方向（X 轴）延伸，所以判定时机和视觉都错误。需要对 A/D 面的 BlockDisplay 旋转90度，或者改用 scaleX 而非 scaleZ。

  ---
问题2：DOUBLE 连接线位置错误

问题一：连接线的 scale 参数顺序

DisplayEntityFactory.createBlockDisplay 的参数是 (scaleX, scaleY, scaleZ)，调用时：
DisplayEntityFactory.createBlockDisplay(world, midLoc, Material.WHITE_CONCRETE, lw, (float)lineLen, lw, 100)
即 scaleX=lw, scaleY=lineLen, scaleZ=lw，连接线沿世界 Y 轴延伸 lineLen。

然后通过旋转来对齐方向，translation 设为 (-lo, -lineLen/2, -lo) 将锚点从角移到中心。

问题二：W/S 面旋转轴错误

W/S 面两点差值：dwx ≠ 0, dwy ≠ 0, dwz = 0（两点 distance 相同）。
连接线初始沿 Y 轴，需要旋转到 XY 平面内的方向，应绕 Z 轴旋转，角度 = atan2(dwx, dwy)（从 Y 轴转向 X 轴方向）。这是正确的。

问题三：A/D 面旋转轴和参数

A/D 面两点差值：dwx = 0, dwy ≠ 0, dwz ≠ 0（A 面局部 x 差值映射到世界 Z 轴）。
连接线初始沿 Y 轴，需要旋转到 YZ 平面内，应绕 X 轴旋转，角度 = atan2(dwz, dwy)（从 Y 轴转向 Z 轴方向）。

但代码写的是 atan2(dwy, dwz)，参数顺序反了，导致角度偏差90度。

问题四：translation 在旋转后不正确

translation (-lo, -lineLen/2, -lo) 是在旋转之前的局部坐标系中设置的。旋转后，-lineLen/2 沿 Y 轴的偏移会随旋转一起转动，对 W/S 面（绕 Z 轴旋转）这个偏移会变成沿旋转后方向的偏移，可能正确；但对 A/D 面（绕 X 轴旋转），Y 轴偏移会转到 Z 轴方向，导致连接线整体偏移。

修复建议：
1. A/D 面 atan2 参数改为 atan2(dwz, dwy)
2. HOLD 音符 A/D 面需要对 BlockDisplay 应用旋转，使 scaleZ 方向对齐移动方向（X 轴）
```
- 你需要查找相关修改的地方，并完成上述修改