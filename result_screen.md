# Result Screen Animation System

## Overview

This document describes the result screen animation system based on the original Skript implementation (`scripts/result.sk`). The result screen displays game statistics with smooth animations and visual effects.

## Animation Sequence

### Phase 1: Pre-Result Animation (30 ticks / 1.5 seconds)

**Floating Animation:**
- All existing text displays (score, level, combo, perfect indicators) float upward
- Animation runs for 30 ticks with gradual upward movement
- Movement calculation:
  - Base increment: 0.01 per tick
  - Score/Level displays: move by base amount
  - Combo/Perfect displays: move by base × 5 (faster movement)
- After animation completes, all text displays are deleted

### Phase 2: Achievement Display

**Full Combo Achievement:**
- Triggered when: combo equals max notes (all notes hit)
- Display: "§bFull Combo" text
- Animation:
  - Initial state: Large scale (40), rotated pitch (90°)
  - 30-tick animation with exponential easing
  - Scale decreases: `size = size × 0.7` per tick
  - Rotation decreases: `angle = angle - (22 × 0.8^tick)` per tick
  - Final scale: ~5, Final rotation: ~0°
  - Translation adjusts with scale: `vector(0, -1.5 × scale × 0.1, 0)`

**Perfect Performance Achievement:**
- Triggered when: all notes hit with Exact judgment
- Display: "§ePerfect Performance!" text (two lines)
- Animation: Same as Full Combo (30 ticks, exponential easing)

### Phase 3: Result Display (if not auto-play)

The result display consists of 4 animated elements that appear sequentially:

#### 3.1 Rank Display (`result()`)
- **Position**: Moves from `(-3.5, 0.5, 12)` to `(5.5, 0.5, 12)`
- **Animation**: Circular easing (cirb) over 25 ticks
- **Content**: Rank letter (SSS+, SSS, SS+, SS, S+, S, A+, A, B+, B, C+, C, D)
- **Style**:
  - Scale: 10×10×10
  - Color: §e (yellow)
  - Translation: `vector(-0.2, -1, 0)`
- **Special Effects**:
  - First play: Shows "§e初次游玩!" title
  - New record: Shows "§e新记录!" title
  - Sound: "entity.player.levelup"

#### 3.2 Score Display (`result2()`)
- **Position**: Moves from `(-7, 2, 10)` to `(-1.5, 2, 10)`
- **Animation**: Circular easing (cirb) over 30 ticks
- **Content**: Score with counting animation
- **Style**:
  - Scale: 8×8×8
  - Color: §f (white) for visible digits, §8 (dark gray) for leading zeros
  - Translation: `vector(0, -1.5, 0)`
- **Counting Animation** (60 ticks):
  - Exponential easing: `displayScore = score - (score × 0.75^tick)`
  - Leading zeros shown in dark gray
  - Format: 7 digits (e.g., "§8000§f1234567" or "§f1000000")

#### 3.3 Statistics Display (`result3()`)
- **Position**: Moves from `(-5.5, 2, 10)` to `(0, 2, 10)`
- **Animation**: Circular easing (cirb) over 30 ticks
- **Content**: Detailed judgment statistics
  - Line 1: `&bExact &f{count}&e(+{hold}%)`
  - Line 2: `&eJust &f{count}`
  - Line 3: `&4Miss &c{count}`
  - Line 4: (empty)
  - Line 5: `&7输入 [/reset] 重置游戏`
- **Style**:
  - Scale: 2×2×2
  - Line width: 400
  - Alignment: Left aligned
  - Translation: `vector(0, -4.5, 0)`
- **Fade-in Animation** (30 ticks):
  - Initial opacity: 14
  - Final opacity: 255
  - Increment: 255/10 per tick

#### 3.4 Song Info Display (`result4()`)
- **Position**: Moves from `(-2.9 - length, 3, 10)` to `(2.6 - length, 3, 10)`
  - Length calculated based on text width: `length × 0.12`
- **Animation**: Circular easing (cirb) over 30 ticks
- **Content**: `&f{song_name}&f {difficulty_level}`
- **Style**:
  - Scale: 3×3×3
  - Line width: 400
  - Alignment: Left aligned
  - Opacity: 255 (no fade-in)

## Technical Details

### Display Entity Properties

All text displays use these common properties:
- **Brightness**: `displayBrightness(15, 15)` (maximum brightness)
- **Background**: `bukkitColor(0, 0, 0, 0)` (transparent)
- **Metadata**: Tagged with "result" for cleanup
- **Interpolation**: Delay of 2 ticks for smooth transitions

### Easing Function

The Skript implementation uses `easingMotion()` with "cirb" (circular back) easing:
- Provides smooth, natural-looking motion
- Creates slight overshoot effect for visual appeal
- Parameters: entity, start location, end location, easing type, duration

### Timing Summary

| Phase            | Duration         | Description                      |
|------------------|------------------|----------------------------------|
| Pre-result float | 30 ticks (1.5s)  | Existing displays float up       |
| Cleanup wait     | 5 ticks (0.25s)  | Delete old displays              |
| Achievement      | 30 ticks (1.5s)  | Full Combo / Perfect Performance |
| Rank display     | 25 ticks (1.25s) | Rank letter slides in            |
| Score counting   | 60 ticks (3s)    | Score counts up with animation   |
| Statistics fade  | 30 ticks (1.5s)  | Stats slide in and fade in       |
| Song info        | 30 ticks (1.5s)  | Song name slides in              |

**Total Duration**: ~7-8 seconds for complete result screen

## Implementation Notes

### Current Java Implementation

The existing `ResultScreen.java` provides:
- Basic result display with rank calculation
- Statistics (Exact/Just/Miss counts and percentages)
- Max combo display
- Achievement detection (Perfect Play, Full Combo)
- Sound effects based on performance

### Missing Features (To Be Implemented)

1. **Animated Text Displays**:
   - Floating pre-result animation
   - Sliding entrance animations with easing
   - Score counting animation
   - Fade-in effects

2. **Achievement Animations**:
   - Full Combo rotating/scaling animation
   - Perfect Performance rotating/scaling animation
   - Proper positioning and timing

3. **Easing System**:
   - Circular back easing for smooth motion
   - Exponential easing for score counting
   - Interpolation delay support

4. **Visual Polish**:
   - Leading zero formatting for score
   - Dynamic positioning based on text length
   - Proper text alignment (left/center)
   - Multi-line text support

5. **Timing Control**:
   - Sequential animation phases
   - Proper delays between elements
   - Synchronized display updates

## Recommended Implementation Approach

1. Create an `AnimatedTextDisplay` class to handle text display entities with animations
2. Implement easing functions (circular, exponential) for smooth motion
3. Create animation phases as separate methods with proper timing
4. Use PlanetLib scheduler for tick-based animations
5. Add metadata tagging for proper cleanup
6. Implement score counting animation with exponential easing
7. Add achievement detection and animation triggers
8. Test timing and visual appearance in-game

## Questions for Clarification

1. Should the result screen be skippable by the player?
不能跳过(本质上因为很短所以不需要额外处理跳过)
2. Should there be a transition animation when exiting the result screen?
不需要
3. Should the result screen support different display modes (compact vs. detailed)?
只要详细版
4. Should achievement animations play sound effects?
需要
5. Should the result screen save screenshots or replay data?
不需要，等玩家自己手动处理