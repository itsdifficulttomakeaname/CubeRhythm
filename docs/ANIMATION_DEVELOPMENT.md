# Animation Event System - Development Plan

> Status: **Planned** (runtime application removed, data layer preserved)
>
> The event system's data structures, JSON format, and chart parsing are fully implemented.
> Runtime application (TrackEvaluator evaluation + NoteRenderer application) has been temporarily removed
> pending a more robust, incremental implementation approach.

## Architecture Overview

```
Chart JSON
  |
  v
ChartLoader (parses groupEvents + inline events)
  |
  v
NoteSpawner (pre-filters matched tracks per note, time window check)
  |
  v
TrackEvaluator.evaluate(entity, currentTime) -> EvalResult
  |
  v
NoteRenderer.updateNote(..., evalResult) -> applies visual channels
```

## JSON Format Specification

### GroupEvent (global, selector-based)

```json
{
  "groupEvents": [
    {
      "selector": {
        "face": "w",              // or ["w", "a"] - optional
        "type": "tap",            // or ["tap", "hold"] - optional
        "tag": "spiral",          // or ["spiral", "drop"] - optional
        "timeRange": [4.0, 12.0]  // [start, end] closed interval - optional
      },
      "events": {
        "x": [
          {"time": 4.0, "value": 0, "easing": "linear"},
          {"time": 6.0, "value": 2, "easing": "sineOut"},
          {"time": 8.0, "value": 0, "easing": "sineIn"}
        ],
        "y": [...],
        "z": [...],
        "scale": [...],
        "rotate": [...],
        "alpha": [...],
        "color_r": [...],
        "material": [...]
      }
    }
  ]
}
```

- Selector fields are AND-combined; within a field, values are OR-combined
- `"tag"` in selector matches against note's `tags` set (intersection check)
- Time values are **absolute seconds** (game time)

### Inline Event (per-note, relative time)

```json
{
  "type": "tap",
  "time": 14.0,
  "face": "w",
  "position": {"x": 0, "y": 0},
  "events": {
    "y": [
      {"rtime": -2.0, "value": 2, "easing": "linear"},
      {"rtime": 0.0, "value": 0, "easing": "quadOut"}
    ]
  }
}
```

- Uses `rtime` (relative to note's `time`): absolute_time = noteTime + rtime
- `rtime: 0.0` = the moment the note reaches the judgment face
- `rtime: -2.0` = 2 seconds before reaching judgment face

### Note Tags (multi-tag support)

```json
// Single tag (legacy format, still supported)
{"tag": "spiral"}

// Multiple tags
{"tags": ["spiral", "glow_effect"]}
```

## Available Channels

| Channel | Type | Default | Description |
|---------|------|---------|-------------|
| `x` | Additive | 0 | Horizontal offset (blocks) |
| `y` | Additive | 0 | Vertical offset (blocks) |
| `z` | Additive | 0 | Depth offset, positive = farther from judgment face |
| `alpha` | Additive | 1.0 | Opacity (0 = invisible, 1 = fully visible) |
| `scale_x` | Multiplicative | 1.0 | X-axis scale multiplier |
| `scale_y` | Multiplicative | 1.0 | Y-axis scale multiplier |
| `scale_z` | Multiplicative | 1.0 | Z-axis scale multiplier |
| `scale` | Sugar | 1.0 | Sets scale_x/y/z simultaneously |
| `rotate` | Additive | 0 | Self-rotation around flight axis (degrees) |
| `color_r` | Additive | -1 | Glow color red (0-255, -1 = use face default) |
| `color_g` | Additive | -1 | Glow color green |
| `color_b` | Additive | -1 | Glow color blue |
| `color_a` | Additive | 255 | Glow color alpha |
| `material` | Discrete | null | Block material name (no interpolation) |

### Channel Accumulation Rules

When multiple tracks affect the same note:
- **Additive channels** (x, y, z, rotate, alpha, color): values are summed
- **Multiplicative channels** (scale_x, scale_y, scale_z): values are multiplied
- **Material channel**: last track with a value wins

## Easing Functions

Supported easing names (applied from previous keyframe to current):

- `linear` - constant rate
- `hold` - no interpolation, holds previous value
- `quadIn`, `quadOut`, `quadInOut`
- `cubicIn`, `cubicOut`, `cubicInOut`
- `sineIn`, `sineOut`, `sineInOut`
- `expoIn`, `expoOut`, `expoInOut`
- `backOut` - slight overshoot

First keyframe's easing is ignored (no previous frame to interpolate from).

## Key Design Decisions

### Time Window Filtering Rule

**Rule**: `track.getEndTime() <= noteTime`

If a note arrives at the judgment face before its bound event track fully ends,
that entire track is NOT applied. The note uses default linear movement instead.

- Applied per-track (not per-channel)
- Applied equally to both groupEvents and inline events
- For inline events with `rtime: 0.0` as last frame: endTime = noteTime, condition satisfied (<=)
- Rationale: ensures notes always arrive at predictable positions on the judgment face

### Sampling Boundary Behavior

- **Before first keyframe** (`t <= firstFrame.time`): return first frame's value
- **After last keyframe** (`t >= lastFrame.time`): return channel's default value (not last frame's value)
- Rationale: events should not persist their effect after completion

### Rotation Pivot Fix

BlockDisplay rotates around corner (0,0,0) by default. To rotate around block center:

```java
Vector3f center = new Vector3f(scaleX/2, scaleY/2, scaleZ/2);
Vector3f rotatedCenter = new Vector3f(center);
new Quaternionf().set(rotationAxisAngle).transform(rotatedCenter);
translation.add(center).sub(rotatedCenter); // compensate offset
```

### Cursor Offset Precomputation

At spawn time, sample x/y channels at noteTime to determine where the note will
land on the judgment face. This offset is used for the cursor (landing point indicator)
display positioning.

## Implementation Plan

### Phase 1: Single-channel validation
- Implement only `x` and `y` channels (position offset)
- Test with simple inline events (rtime-based)
- Verify visual correctness with a minimal test chart

### Phase 2: Scale and rotation
- Add `scale` / `scale_x` / `scale_y` / `scale_z`
- Add `rotate` with pivot fix
- Test with inline events

### Phase 3: Visual channels
- Add `alpha` (via viewRange)
- Add `color_r/g/b/a` (glow color override)
- Add `material` (discrete, no interpolation)

### Phase 4: GroupEvents
- Enable groupEvent pre-filtering in NoteSpawner
- Apply time window filtering rule
- Test with groupEvent-tagged notes

### Phase 5: Multi-track accumulation
- Test notes with both groupEvent + inline events
- Verify additive/multiplicative accumulation
- Test cursor offset precomputation

### Phase 6: DOUBLE and FLICK support
- Apply evalResult to updateDoubleNote
- Apply evalResult to updateFlickNote
- Verify both positions in DOUBLE receive same event offset

## Existing Code (Data Layer - Preserved)

These classes are fully implemented and ready for reuse:

- `org.cubeRhythm.note.event.Channel` - Channel enum with defaults and accumulation type
- `org.cubeRhythm.note.event.Keyframe` - Time/value/easing data
- `org.cubeRhythm.note.event.EventTrack` - Channel -> Keyframe list mapping
- `org.cubeRhythm.note.event.EvalResult` - Evaluation output container
- `org.cubeRhythm.note.event.Easing` - Easing function evaluator (LUT-optimized sine)
- `org.cubeRhythm.note.event.TrackEvaluator` - Core sampling + evaluation engine
- `org.cubeRhythm.note.event.GroupEvent` - Selector + EventTrack pair
- `org.cubeRhythm.note.event.Selector` - Multi-field note matcher
- `org.cubeRhythm.chart.ChartLoader` - Parses events/groupEvents from JSON (preserved)
- `org.cubeRhythm.note.Note` - Has `Set<String> tags` and `EventTrack events` fields

## Test Chart

`plugins/CubeRhythm/charts/event_test_full.json` contains comprehensive test cases
covering all channels and combinations. Keep it for future validation.
