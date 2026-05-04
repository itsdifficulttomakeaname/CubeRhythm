# 04 音符渲染（draw 函数）

## 坐标转换规则

所有 draw 函数接收面局部坐标 `(x, y, z)`，转换为世界坐标的规则：

| 面 | BlockDisplay 位置   | Interaction 位置偏移            |
|---|-------------------|-----------------------------|
| W | `(x, y, z)`       | `(x+0.5, y-0.5, z+0.5/1)`   |
| A | `(z, y, -x)`      | `(z+0.5/1, y-0.5, -x+0/1)`  |
| S | `(-x+1, y, -z+1)` | `(-x+0/-1, y-0.5, -z+0/-1)` |
| D | `(-z+1, y, x+1)`  | `(-z+0/-1, y-0.5, x+0/1)`   |

Interaction 实体放置在 BlockDisplay 右下角偏移处，尺寸 2×2，作为点击碰撞箱。

---

## drawTap

- **BlockDisplay**：`light_blue_concrete`，scale `(1, 1, 0.5*speed)`
- **Interaction**：2×2，notetype="tap"
- 生成后 2 tick 设置亮度 15/15，并设置 translation 使其沿运动方向插值移动：
  ```skript
  set display translation of {_e} to vector(0, 0, -100*speed)  # W/S 面
  set display translation of {_e} to vector(0, 0, 100*speed)   # A/D 面
  ```
  这是 Minecraft Display Entity 的插值动画——设置 translation 后实体会在 `interpolation duration`（100 tick）内平滑移动到目标偏移，配合每 tick teleport 实现流畅移动。

---

## drawHold

- **BlockDisplay**：`white_concrete`，scale `(1, 1, 3*speed)`（比 tap 更长）
- Interaction 的 Y 偏移 +20（放到屏幕外，避免误触）
- translation 固定为 `vector(0, 0, -100*speed)`（只有 W/S 方向，A/D 面代码相同）

**[已确认]** Hold A 面 yaw=-90 与 Tap A 面 yaw=90 的差异可能是笔误，但不影响游戏体验，Java 版保持现有实现即可。

---

## drawDrag

- **BlockDisplay**：`yellow_concrete`，scale `(1, 1, 0.5*speed)`
- 与 drawTap 结构完全相同，只是颜色不同
- Interaction 的 notetype="drag"

---

## drawFlick

结构最复杂，包含 3 个实体：

1. **BlockDisplay**（5×5 玻璃板）：`white_stained_glass_pane[east=true,west=true]`，scale `(5,5,1)`，位置在面中心偏左下 `(-2, -2, z)`
2. **TextDisplay**（箭头）：`"§f←"` 或 `"§f→"`，scale `(20,20,0)`，通过 `metadata "linkbind"` 绑定到 BlockDisplay UUID
3. **Interaction**：Y 偏移 +20（放到屏幕外），notetype="flick"，存储 `turn` 方向

translation 设置：
```skript
# W/S 面：向 Z 负方向移动
set display translation of {_e1} to vector(0, 0, -100*speed)
# A/D 面：向 Z 正方向移动（局部坐标系不同）
set display translation of {_e1} to vector(0, 0, 100*speed)
```

---

## drawDouble

包含 5 个实体：两组 BlockDisplay+Interaction（橙色方块），加一条连线：

**连线**：`white_concrete`，scale `(0.1, distance, 0.1)`，旋转角度由两点距离计算：
```skript
set {_d1} to distance between {_loc1} and {_loc3}
set {_d2} to abs({_xa} - {_xb})
set {_acos} to acos({_d2}/{_d1}) - 90
```

两个 Interaction 互相通过 `metadata "linknote"` 存储对方 UUID，击中一个时自动删除另一个。

**排序**：若 `xa < xb` 则交换两点，确保连线方向一致。
**[Java 版]** 可尝试用 Minecraft 原生线条实现 Double 连线（Java 版待验证）。小节线不需要实现。

---

## 实体关联结构

```
BlockDisplay  ←── linkuuid ──  Interaction (notetype, face, tag)
                                    │
                               linknote (仅 double)
                                    │
                               另一个 Interaction
```

`deleteNote()` 通过 Interaction 的 `linkuuid` 找到并删除对应 BlockDisplay，再删除所有 `belongsto` 该 UUID 的 TextDisplay。
