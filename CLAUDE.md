# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CubeRhythm is a 3D rhythm game built as a Minecraft plugin (Paper/Spigot 1.20.1). Players stand in a cube's center and hit notes flying from four directions (front/left/back/right) in sync with music. The project has a fully functional Java implementation with legacy Skript scripts still present.

**Current Status (2026-02-22):**
- ✅ **Core Game System**: Fully functional and actively maintained
  - Chart loading and management
  - Note rendering and judgment system
  - Player settings and GUI system
  - Score calculation and result screen
  - Performance optimizations (entity pooling, async loading, view frustum culling)
- ❌ **In-Game Editor**: Deprecated and disabled
  - Code exists in `org.cubeRhythm.editor` package but is not active
  - Commands are commented out in `Main.java`
  - Use external tools for chart editing instead
- 📦 **Legacy Skript System**: Present but not primary

**Tech Stack:**
- Java 17
- Paper API 1.20.1
- Maven build system with Shade plugin (version 3.5.3)
- PlanetLib library (jason31416/planetlib v1.4.0) - JSON parsing and scheduling
- Apache Commons IO 2.21.0
- Lombok 1.18.38 for boilerplate reduction
- Skript scripts (legacy, in `scripts/` directory)

## Build Commands

```bash
# Build the plugin JAR
mvn clean package

# Compile only
mvn compile

# Clean build artifacts
mvn clean
```

The built JAR will be in `target/CubeRhythm-1.0.jar` with dependencies shaded.

## Available Commands

All commands are registered in `plugin.yml` and initialized in `Main.onEnable()`:

**Game Commands:**
- `/play [chartId] [speed] [difficulty]` - Start playing a chart with optional speed multiplier and difficulty (1=Easy, 2=Normal, 3=Hard)
  - No arguments: Lists all available charts
  - With chartId only: Uses player's saved settings
  - With speed: Overrides player's speed setting for this session
  - With difficulty: Overrides player's difficulty setting for this session
- `/exit` (alias: `/quit`) - Stop the current game session
- `/gui` (aliases: `/menu`, `/charts`) - Open the chart selector GUI

**Editor Commands (DEPRECATED):**
> **Note**: The in-game editor has been deprecated due to technical limitations and complexity.
> See `EDITOR_DESIGN.md` for details and alternative approaches.
> All editor-related classes are marked with `@Deprecated` and functionality is disabled.

- ~~`/editor` - Enter/exit editor mode~~
- ~~`/editor new <chartId>` - Create new chart with real-time file saving~~
- ~~`/editor load <chartId>` - Load existing chart into editor~~
- ~~`/editor save` - Manually save chart (auto-saves by default)~~
- ~~`/editor bpm <value>` - Set BPM (affects HOLD note length)~~
- ~~`/editor pretime <seconds>` - Set pre-time before first beat~~
- ~~`/editor speed <value>` - Set note speed multiplier~~
- ~~`/editor help` - Show editor help~~
- ~~`/step <integer>` - Set editor step length (e.g., /step 4 = 1/4 beat)~~
- ~~`/b <beat>` - Jump to specific beat in editor~~

**Recommended**: Use external tools for chart editing (see `EDITOR_DESIGN.md` for suggestions).

## Architecture

### Current State (Hybrid)

The codebase has both systems:
- **Java System**: Fully functional game implementation in `src/main/java/org/cubeRhythm/`
- **Legacy Skript System**: Original implementation in `scripts/` (still present but Java is primary)

### Package Structure

**org.cubeRhythm** - Main plugin class
- `Main`: Plugin entry point, initializes ChartRegistry, PlayerSettingsManager, ConfigManager, GUI system, and registers commands
- Singleton accessible via `Main.instance`
- Manages current GameSession instance
- Registers event listeners: GUIListener, MovementRestriction
- **Note**: EditorListener and EditorUpdateTask are disabled (editor functionality deprecated)

**org.cubeRhythm.chart** - Chart data and loading
- `Chart`: Container for chart metadata and notes
- `ChartMetadata`: Song info (title, artist, BPM, difficulty, duration)
- `ChartLoader`: Parses JSON chart files using PlanetLib's MapTree
- `AsyncChartLoader`: Asynchronous chart loading for improved performance, loads charts in background threads
- `ChartRegistry`: Manages all loaded charts, loads from `plugins/CubeRhythm/*.json`
  - **Note**: Currently used by the main system. `SongManager` in the manager package provides similar functionality with caching but is not actively used.

**org.cubeRhythm.game** - Game session management
- `GameSession`: Core game loop, manages entire play session (20 TPS tick)
- `GameState`: Enum (IDLE, LOADING, PLAYING, PAUSED, RESULTS)
- `PlayerSettings`: Player preferences (speed, offset, hitSound, autoPlay, autoFlickRotation, showBeatLines, difficulty)
  - Difficulty levels: 1=Easy (2x hitbox), 2=Normal (1.5x hitbox), 3=Hard (1x hitbox)
  - Auto-play Flick rotation: When enabled with auto-play, automatically rotates player view 90° for Flick notes with smooth ease-out animation
- `ResultScreen`: Displays game results with rank calculation (SSS+ to D), statistics, and achievements
  - Achievement displays (Full Combo/Perfect Performance) now disappear 1 second after animation completes
- `GameHUD`: In-game HUD system displaying score, combo, song name, and difficulty on all 4 faces
  - Text size: 1.125x scale (1.5x larger than original 0.75x)
  - Combo color: Gold if All Perfect, Cyan if Full Combo, White otherwise
- `MovementRestriction`: Event listener that prevents player position changes during gameplay (allows rotation only)

**org.cubeRhythm.note** - Note system
- `Note`: Data class for note timing, type, position, face
- `NoteType`: Enum (TAP, DRAG, HOLD, FLICK, DOUBLE, EXECUTION)
- `NoteEntity`: Runtime note with Minecraft entities (BlockDisplay, Interaction)
- `NoteSpawner`: Spawns notes when they enter render distance (< 50 blocks)
- `NoteRenderer`: Updates note positions and visual effects each tick
- `MovementCurve`: Custom movement curves for EXECUTION notes
- `ExecutionAction`/`ExecutionHandler`: Script execution system for EXECUTION notes

**org.cubeRhythm.judgment** - Scoring and timing
- `JudgmentManager`: Calculates timing offsets and judges hits
- `JudgmentResult`: Enum (EXACT, JUST, MISS) with display text and colors
- `JudgmentWindow`: Timing windows (±80ms Exact, ±200ms Just)
- `ScoreManager`: Tracks score, combo, accuracy, perfect detection
  - `isPerfect()`: Dynamic check during gameplay (justCount == 0 && missCount == 0)
  - `isFullPerfect()`: Check if all notes completed with EXACT (exactCount == totalNotes && justCount == 0 && missCount == 0)

**org.cubeRhythm.input** - Player input handling
- `InputHandler`: Listens for player clicks (left/right), handles TAP/FLICK judgment
- `KeyPressCache`: Tracks recent key presses for HOLD notes
- `ViewDirectionHelper`: Raycasting for DRAG notes and FLICK direction detection
- `SnowballManager`: Gives players snowballs to capture right-click events
- `CancelFlagManager`: Prevents double-hits on same note

**org.cubeRhythm.coordinate** - 3D coordinate system
- `Face`: Enum (W, A, S, D) for four judgment planes
- `CoordinateSystem`: Converts local note coordinates to world coordinates
- `NotePosition`: 2D position on a face (x, y)

**org.cubeRhythm.entity** - Minecraft entity management
- `EntityManager`: Tracks all active note entities, handles cleanup
- `DisplayEntityFactory`: Creates BlockDisplay and Interaction entities
- `EntityPool`: Entity pooling system for reusing BlockDisplay, Interaction, and TextDisplay entities (max 200 per pool)

**org.cubeRhythm.command** - Player commands
- `PlayCommand`: `/play [chartId] [speed] [difficulty]` - Start a chart with optional parameters
- `ExitCommand`: `/exit` or `/quit` - Stop current game
- `GUICommand`: `/gui` (aliases: /menu, /charts) - Open chart selector GUI

**org.cubeRhythm.gui** - Graphical user interface
- `ChartSelectorGUI`: Visual chart selection interface with difficulty-based colors
- `SettingsGUI`: In-game settings interface for adjusting speed, offset, difficulty, and toggles
- `GUIListener`: Event handler for GUI interactions and navigation

**org.cubeRhythm.manager** - Utility managers
- `GameManager`: Game state management
- `ConfigManager`: Configuration file management (config.yml)
  - Manages game defaults (speed, offset, max concurrent games)
  - Judgment window configuration (Exact: 80ms, Just: 200ms)
  - Rendering limits (max entities: 100, spawn distance: 50 blocks)
  - Game location configuration (center coordinates)
  - Auto-save score settings
- `PlayerSettingsManager`: Persistent player settings storage (YAML files in `player_settings/`)
- `SongManager`: Alternative chart management with caching (similar to ChartRegistry)

**org.cubeRhythm.editor** - In-game chart editor (DEPRECATED)

> **⚠️ DEPRECATED**: The in-game editor has been deprecated due to technical limitations.
> All classes in this package are marked with `@Deprecated` and functionality is disabled.
> See `EDITOR_DESIGN.md` for details and alternative approaches.

- `EditorSession`: Manages individual player's editing state
- `EditorManager`: Global editor session manager (singleton)
- `EditorNote`: Editor-specific note data structure
- `EditorFileUtil`: Real-time file saving/loading utilities
- `EditorCommand`: Main editor command handler (`/editor`)
- `StepCommand`: Step length command (`/step`)
- `BeatCommand`: Beat jump command (`/b`)
- `EditorListener`: Event listener for mouse wheel navigation and hotkey interactions
- `EditorNoteRenderer`: Renders notes in editor mode
- `EditorUpdateTask`: Periodic task for preview cursor and action bar updates
- `EditorPreviewCursor`: Real-time preview cursor system
- `EditorFaceDetector`: Automatic face detection system

**org.cubeRhythm.util** - Utility classes
- `ViewFrustumCuller`: View frustum culling optimization to only render notes within player's field of view (90° horizontal FOV, 60 blocks max distance)

### Skript System (Legacy)

Located in `scripts/` - Original Skript-based implementation, still present but Java system is now primary

## Configuration System

The plugin uses a YAML configuration file at `plugins/CubeRhythm/config.yml`.

### Configuration Structure

```yaml
# Game center location (x,y,z coordinates)
location: 0,320,0

# Game defaults
game:
  default-speed: 1.0
  default-offset: 0
  max-concurrent-games: 10

# Score settings
score:
  auto-save: true

# Judgment timing windows (milliseconds)
judgment:
  exact-window: 80
  just-window: 200

# Rendering limits
rendering:
  max-entities: 100
  spawn-distance: 50.0
```

### ConfigManager API

The `ConfigManager` class provides methods to access and modify configuration:

```java
ConfigManager config = Main.instance.getConfigManager();

// Game settings
double speed = config.getDefaultSpeed();
int offset = config.getDefaultOffset();
int maxGames = config.getMaxConcurrentGames();

// Judgment windows
int exactWindow = config.getExactWindow();  // Default: 80ms
int justWindow = config.getJustWindow();    // Default: 200ms

// Rendering limits
int maxEntities = config.getMaxEntities();        // Default: 100
double spawnDistance = config.getSpawnDistance(); // Default: 50.0

// Game location
double[] location = config.getGameLocation(); // Returns [x, y, z]

// Score settings
boolean autoSave = config.isAutoSaveScores();

// Modify and save
config.setDefaultSpeed(1.5);
config.setDefaultOffset(50);
config.saveConfig();
```

### Configuration Notes

- Configuration is loaded on plugin enable
- Missing values are automatically set to defaults
- Game location format: "x,y,z" (comma-separated, no spaces)
- Invalid location format falls back to "0,320,0"
- Changes via setter methods are automatically saved

## Chart System

### File Format

Charts use JSON format stored in `plugins/CubeRhythm/{chartId}.json`:

```json
{
  "version": "1.0.0",
  "metadata": {
    "id": "chart_id",
    "title": "Song Name",
    "artist": "Composer",
    "charter": "Charter",
    "difficulty": {
      "name": "Hard",
      "level": 10,
      "color": "#FF5555"
    },
    "audio": "custom.music_key",
    "duration": 120,
    "offset": 0,
    "bpm": 120
  },
  "notes": [
    {
      "type": "tap",
      "time": 14.77,
      "face": "w",
      "position": {"x": 1, "y": 0},
      "glowing": false,
      "tag": ""
    }
  ]
}
```

Legacy Skript format (`.sk` files) still exists in `scripts/charts/` but is no longer the primary format.

### Note Types

- **TAP**: Single click note (light blue)
- **HOLD**: Hold until it reaches judgment line (white)
  - Length calculated as `(60 / BPM) × speed` to ensure beat-aligned connections
- **DRAG**: Auto-judges when crosshair aims at it (yellow)
- **FLICK**: Click then turn camera within 0.5s (magenta/red)
- **DOUBLE**: Two notes hit simultaneously (orange)
  - Both positions have visible cursors for better visibility
- **EXECUTION**: Triggers custom script at specified time (invisible)

### Judgment Faces

Four judgment planes around the player:
- `w`: Front (white marker, 0° rotation)
- `a`: Left (yellow marker, 90° rotation)
- `s`: Back (orange marker, 180° rotation)
- `d`: Right (red marker, 270° rotation)

## GUI System

The plugin includes a visual interface system for chart selection and settings management.

### Chart Selector GUI

Accessed via `/gui` (aliases: `/menu`, `/charts`):
- Displays all available charts in a 6-row inventory interface
- Charts shown as colored concrete blocks based on difficulty level:
  - **Green** (Lime): Levels 1-5
  - **Yellow**: Levels 6-8
  - **Orange**: Levels 9-11
  - **Red**: Levels 12-14
  - **Purple**: Levels 15+
- Each chart item displays:
  - Title, artist, charter
  - Difficulty name, level, and BPM
  - Duration and note count
  - Left-click to start game
  - Right-click to view details (future feature)
- Bottom toolbar:
  - Settings button (Comparator) - Opens settings GUI
  - Close button (Barrier) - Closes GUI

### Settings GUI

Accessed from chart selector or directly:
- **Speed**: Adjust note approach speed (±0.1 or ±0.5 with Shift)
- **Offset**: Audio timing offset in milliseconds (±10ms or ±50ms with Shift)
- **Difficulty**: Toggle between Easy (2x hitbox), Normal (1.5x hitbox), Hard (1x hitbox)
- **Hit Sound**: Toggle hit sound effects
- **Auto Play**: Toggle auto-play mode
- **Flick Auto Rotation**: Toggle automatic camera rotation for Flick notes during auto-play
  - When enabled with auto-play, player view automatically rotates 90° (left/right) for Flick notes
  - Uses smooth ease-out animation (fast then slow) over 15 ticks
  - Player cannot manually rotate during auto-rotation
- Settings are automatically saved to `plugins/CubeRhythm/player_settings/{uuid}.yml`

### GUI Implementation

- `GUIListener`: Handles all inventory click events
- `ChartSelectorGUI.open(Player)`: Opens chart selection interface
- `SettingsGUI.open(Player)`: Opens settings interface
- Settings persist across sessions via `PlayerSettingsManager`

## In-Game Chart Editor (DEPRECATED)

> **⚠️ DEPRECATED**: The in-game chart editor has been deprecated due to technical limitations and complexity.
> All editor-related classes are marked with `@Deprecated` and functionality is disabled in `Main.java`.
> See `EDITOR_DESIGN.md` for details and alternative approaches.
> **Recommended**: Use external tools for chart editing.

The plugin previously included a comprehensive in-game chart editor with real-time file saving. While the code still exists in the `org.cubeRhythm.editor` package, it is no longer active or maintained.

### Editor Features

**File Management:**
- Create new charts with unique ID validation
- Load existing charts for editing
- Real-time auto-save on all modifications
- Standard JSON format compatible with game system

**Editing Tools:**
- Time navigation: Mouse scroll (forward/backward by step), `/step`, `/b` commands
- Note placement: Right-click with wand
- Note deletion: Left-click with wand
- Grid snapping: Shift + right-click (snaps to 0.5 block grid)
- Note type cycling: Scroll to slot 1 (hotkey detected via `PlayerItemHeldEvent`)
  - Cycle order: TAP → DOUBLE → DRAG → HOLD → FLICK(left) → FLICK(right) → TAP
- Face cycling: Scroll to slot 9 (hotkey detected via `PlayerItemHeldEvent`)
  - Cycle order: W → A → S → D → W
  - **Note**: Face is automatically detected based on player view direction
- Glow toggle: Press `F` key
- Mouse wheel navigation: Scroll forward (slots 5-7) to advance, scroll backward (slots 1-3) to retreat
- Real-time preview cursor: Shows where note will be placed at crosshair position
  - Color-coded by face (W=white, A=yellow, S=orange, D=red)
  - Displays current note type material
  - Half-transparent with glow effect
  - Only visible when looking at valid judgment face
- Action bar display: Shows current beat, time, position, note type, face, glow status, and total note count

**Parameter Control:**
- BPM setting (affects HOLD note length)
- Pre-time adjustment (time before first beat)
- Speed multiplier (note approach speed)
- Step length (beat subdivision: 1/4, 1/8, 1/16, etc.)

**Time Calculation:**
- Editor uses tick-based time system
- Current time (seconds) = `(tick / stepLength) × (60 / BPM) + preTime`
- Current beat = `tick / stepLength + 1`
- Beat position = `tick % stepLength`
- Example: tick=8, stepLength=4, BPM=120 → Beat 3, Position 0/4, Time = 1.0s + preTime

### Editor Implementation

- `EditorSession`: Tracks editing state per player
  - Current time position (tick-based)
  - Selected note type and face
  - Chart metadata (BPM, pre-time, speed)
  - File reference for auto-save
  - DOUBLE note placement mode tracking (two-step placement process)
  - Preview cursor entity reference
- `EditorManager`: Singleton managing all active sessions
- `EditorFileUtil`: Handles JSON serialization/deserialization
- `EditorNote`: Editor-specific note representation with display entities
- `EditorListener`: Handles mouse wheel events via `PlayerItemHeldEvent` for navigation and cycling
- `EditorNoteRenderer`: Renders visible notes within ±5 seconds of current time position
- `EditorUpdateTask`: Runs every 2 ticks (0.1 seconds) to update preview cursor and action bar
- `EditorPreviewCursor`: Calculates cursor position using raycast, handles grid snapping, displays preview entity
- `EditorFaceDetector`: Automatically detects which judgment face player is looking at using raycast intersection

### Preview Cursor System

The editor includes a real-time preview system that shows where notes will be placed:

**Automatic Face Detection:**
- Raycasts player view direction to find intersection with judgment planes
- Validates intersection is within face boundaries (x/z ∈ [-3, 3], y ∈ [-3, 3])
- Updates face selection automatically as player looks around
- Returns null if not looking at any valid face (hides preview)

**Cursor Positioning:**
- Calculates intersection point with judgment plane at distance 4
- Applies 1.6 offset for normal placement, 1.4 offset with Shift
- Grid snapping when Shift held: rounds to nearest 0.5 block
- Validates coordinates are within valid range ([-3, 4] for x/y)

**Visual Feedback:**
- BlockDisplay entity at calculated position
- Material matches selected note type (TAP=light blue, HOLD=white, etc.)
- Glowing effect with face-specific color
- Half-transparent (brightness 10/10)
- FLICK notes show 5×5 preview at center
- HOLD notes show extended length (scaleZ=3.0)

**Action Bar Information:**
- BPM and current beat/position (e.g., "第 5小节 2/4拍")
- Current time in seconds
- Cursor coordinates (x, y)
- Selected note type and face
- Glow status
- Total note count
- DOUBLE placement mode indicator (when placing second position)

### HOLD Note Length Calculation

HOLD notes use BPM-aware length calculation:
```java
float scaleZ = (float) ((60.0 / bpm) * speed)
```

This ensures:
- One beat of HOLD connects perfectly with next beat
- Consistent visual spacing across different BPMs
- Proper alignment for consecutive HOLD notes

### DOUBLE Note Rendering

DOUBLE notes render with:
- Two BlockDisplay entities (one per position)
- Two Interaction entities (hitboxes)
- Two cursor TextDisplays (gold colored)
- All elements update synchronously

### DOUBLE Note Placement in Editor

DOUBLE notes require a two-step placement process:
1. **First Position**: Right-click to place first position, enters DOUBLE placement mode
2. **Second Position**: Right-click again to place second position, completes the note
3. **Cancellation**: Left-click or change note type to cancel and remove incomplete DOUBLE note

The action bar shows "§e[DOUBLE 第2个位置]" indicator during placement mode.

## Core Game Mechanics

### Game Flow

1. Player runs `/play [chartId] [speed]` or uses `/gui` to open chart selector
2. `PlayCommand` creates `GameSession` with chart and player settings from `PlayerSettingsManager`
3. `GameSession.start()` initializes:
   - `NoteSpawner` for spawning notes
   - `InputHandler` for capturing player input
   - `EntityManager` for tracking note entities
   - `JudgmentManager` and `ScoreManager` for scoring
   - `GameHUD` for displaying real-time game information on all 4 faces
   - Movement restriction to prevent player position changes
4. Game loop runs at 20 TPS:
   - Updates `currentTime` based on elapsed milliseconds + offset
   - `NoteSpawner` spawns notes when distance < 50 blocks
   - `NoteRenderer` updates all note positions each tick
   - Auto-judgment for DRAG (raycast), HOLD (key press), FLICK (camera angle)
   - Manual judgment for TAP via `InputHandler` click events
   - Removes notes that pass judgment line (distance < 4 - speed*4)
5. Game ends when `currentTime >= chart.duration`
6. `GameSession.end()` displays `ResultScreen` with detailed statistics and cleans up

### Timing System

```
Note Distance = speed × 20 × (noteTime - currentTime) + 4
```

- Notes spawn when distance < 50 blocks
- Judgment line is at distance = 4 blocks
- Speed multiplier affects note approach rate
- Timing offset calculated from distance: `offset = (distance - 4) / speed * 50` (in ms)

### Judgment Windows

- **Exact**: ±80ms (1.0 score multiplier, blue)
- **Just**: ±81-200ms (0.7 multiplier, yellow, shows Early/Late)
- **Miss**: Beyond 200ms (0 multiplier, red)

Note type-specific behavior:
- **TAP**: Full judgment (Exact/Just/Miss)
- **DRAG/HOLD/FLICK**: Only Exact or Miss (Just is converted to Exact)
- **DOUBLE**: Requires hitting all positions within timing window

### Judgment Implementation

Each note type has different judgment logic in `GameSession.tick()`:

- **TAP**: Triggered by `InputHandler` on player click, finds closest note on clicked face
- **DRAG**: Auto-judges when player's crosshair aims at note (raycast check)
- **HOLD**: Auto-judges when any key is pressed and note is in judgment zone
- **FLICK**: Auto-judges when player camera angle matches target direction (±90° range)
- **DOUBLE**: Handled as multiple TAP notes with shared timing
- **EXECUTION**: Triggers custom actions at specified time (no judgment)

### Scoring

```
Base Score = 1,000,000 / totalNotes × hitNotes
Perfect = All Exact + All Hold perfect = 1,000,000
```

**Rank System** (calculated by `ResultScreen`):
- **SSS+**: 100% (Perfect - all Exact, no Just/Miss)
- **SSS**: 99.5%+
- **SS+**: 99.0%+
- **SS**: 98.0%+
- **S+**: 97.0%+
- **S**: 95.0%+
- **A+**: 93.0%+
- **A**: 90.0%+
- **B+**: 85.0%+
- **B**: 80.0%+
- **C+**: 75.0%+
- **C**: 70.0%+
- **D**: <70%

**Special Achievements**:
- **Perfect Play**: All notes hit with Exact judgment
- **Full Combo**: All notes hit (no Miss)

## Development Workflow

### Adding New Charts

1. Create JSON file in `plugins/CubeRhythm/{chartId}.json` following the format above
2. Restart server or reload plugin to load new charts
3. Use `/play {chartId}` to test (or `/gui` to select from GUI)

### Testing

No automated tests currently. Manual testing workflow:
1. Build with `mvn package`
2. Copy `target/CubeRhythm-1.0.jar` to server's `plugins/` folder
3. Start Paper 1.20.1 server
4. Place chart JSON files in `plugins/CubeRhythm/`
5. Use `/gui` to open chart selector or `/play <chartId> [speed] [difficulty]` to test gameplay
6. Use `/exit` to stop current game
7. Player settings are saved in `plugins/CubeRhythm/player_settings/{uuid}.yml`

### Debugging

- Plugin logs: Paper server console (use `Main.instance.getLogger()`)
- Chart loading: Check console for "开始加载谱面" and "成功加载 X 个音符"
- Game session: Check console for "=== 游戏会话开始 ===" and timing info
- Skript logs (if using legacy system): `plugins/Skript/logs/`

## Important Constraints

### Input System

Player input is captured via:
- **Left Click**: Bukkit's `PlayerInteractEvent` with `Action.LEFT_CLICK_AIR/BLOCK`
- **Right Click**: Snowball throw event (`ProjectileLaunchEvent`) - players are given snowballs
- **View Direction**: Raycasting for DRAG notes and FLICK angle detection
- **Key Press Cache**: Tracks recent inputs for HOLD note detection

### Coordinate System

The game uses a 4-face coordinate system. Each face has local coordinates that must be converted to world coordinates:

- Face "w": Front (white marker, 0° rotation, Z = 4)
- Face "a": Left (yellow marker, 90° rotation, X = 4)
- Face "s": Back (orange marker, 180° rotation, Z = -4)
- Face "d": Right (red marker, 270° rotation, X = -4)

**Face Boundaries:**
- All faces: Y ∈ [-3, 3] (vertical range)
- W/S faces: X ∈ [-3, 3] (horizontal range)
- A/D faces: Z ∈ [-3, 3] (horizontal range)

**Coordinate Transformation:**
- W/S faces: No X offset
- A/D faces: +1.0 X offset applied
- Judgment line distance: 4 blocks from center
- Player eye height offset: 1.6 (normal), 1.4 (with Shift)

See `CoordinateSystem.java` for conversion functions and `EditorFaceDetector.java` for face detection logic.

### Performance Limits

- Max 100 concurrent note entities
- Notes spawn only when distance < 50 blocks
- Render loop runs every tick (20 TPS)
- Entity pooling system with max 200 entities per pool (BlockDisplay, Interaction, TextDisplay)
- View frustum culling: Only renders notes within 90° horizontal FOV and 60 blocks distance

### Entity System

Notes use Minecraft Display Entities:
- **Block Display**: Visual representation
- **Interaction Entity**: Collision detection (2×2 hitbox)
- **Text Display**: UI elements and effects

### Performance Optimizations

The plugin includes several performance optimization systems:

**Entity Pooling** (`EntityPool`):
- Reuses BlockDisplay, Interaction, and TextDisplay entities instead of creating/destroying them
- Maintains separate pools for each entity type with max 200 entities per pool
- Automatically resets entity state when returning to pool
- Reduces garbage collection overhead and improves frame rates

**Asynchronous Chart Loading** (`AsyncChartLoader`):
- Loads charts in background threads to avoid blocking main thread
- Provides callback-based API for async operations
- Supports batch preloading of multiple charts
- Callbacks execute on main thread for thread safety

**View Frustum Culling** (`ViewFrustumCuller`):
- Only renders notes within player's field of view (90° horizontal FOV)
- Maximum render distance of 60 blocks
- Reduces entity count and improves performance for complex charts

**In-Game HUD System** (`GameHUD`):
- Displays real-time game information on all 4 judgment faces
- Shows: Score (top left), Combo (top right), Difficulty (bottom left), Song name (bottom right)
- Uses TextDisplay entities positioned at judgment line distance (4.5 blocks)
- Updates every tick with current score and combo
- Automatically cleaned up when game session ends

## Migration Notes

The Java implementation is now fully functional. When working on the codebase:

1. **Primary System**: Java classes in `src/main/java/org/cubeRhythm/` are the main implementation
2. **Chart Format**: Use JSON format in `plugins/CubeRhythm/`, not Skript `.sk` files
3. **Game Loop**: `GameSession.tick()` is the core game loop (20 TPS)
4. **Entity Management**: All note entities managed by `EntityManager`, cleaned up automatically
5. **Judgment Logic**:
   - TAP: Manual via `InputHandler` click events
   - DRAG: Auto via raycast in `GameSession.tick()`
   - HOLD: Auto via key press detection in `GameSession.tick()`
   - FLICK: Auto via camera angle detection in `GameSession.tick()`
6. **Timing**: Uses `System.currentTimeMillis()` with offset adjustment
7. **Cleanup**: Always call `GameSession.stop()` or `end()` to prevent entity leaks

## Common Development Patterns

### Adding New Note Types

1. Add enum value to `NoteType`
2. Update `ChartLoader` to parse new note data
3. Add rendering logic in `NoteRenderer`
4. Add judgment logic in `GameSession.tick()` or `InputHandler`
5. Update `DisplayEntityFactory` if new visual style needed

### Modifying Judgment Logic

Judgment happens in two places:
- `InputHandler.handleLeftClick()` / `handleRightClick()` for TAP notes
- `GameSession.tick()` for auto-judgment (DRAG, HOLD, FLICK)

Always use `JudgmentManager.calculateTimingOffset()` and `JudgmentManager.judge()` for consistency.

### Working with Entities

- All entities created via `DisplayEntityFactory`
- Registered in `EntityManager` with UUID
- Must call `entity.cleanup()` and `entityManager.unregisterEntity()` when done
- Use `entityManager.cleanupAll()` when ending session
- Consider using `EntityPool` for frequently created/destroyed entities to improve performance

### Working with the Editor (DEPRECATED)

> **⚠️ Note**: The editor system is deprecated and disabled. The code below is for reference only.

The editor system was previously initialized on plugin enable but is now disabled:

```java
// DEPRECATED: Editor is no longer initialized in Main.onEnable()
// - EditorListener is NOT registered (commented out)
// - EditorUpdateTask is NOT started (commented out)
// - Editor commands are NOT registered (commented out)

// The following code is for reference only and will not work:
// Access editor manager
EditorManager manager = EditorManager.getInstance();

// Get player's editor session (null if not in editor)
EditorSession session = manager.getSession(player);

// Create new editor session
manager.createSession(player);

// Remove editor session (cleans up all entities)
manager.removeSession(player);

// Cleanup all sessions (called on plugin disable)
manager.cleanup();
```

**Editor Session Lifecycle:**
1. Player runs `/editor new <chartId>` or `/editor load <chartId>`
2. `EditorManager.createSession()` creates new `EditorSession`
3. `EditorUpdateTask` automatically updates preview cursor every 2 ticks
4. Player edits chart (place/delete notes, navigate time)
5. Changes auto-save to file via `EditorFileUtil`
6. Player runs `/editor` to exit
7. `EditorManager.removeSession()` cleans up all entities and saves final state

### Using Entity Pooling

For better performance with frequently spawned entities:
```java
EntityPool pool = new EntityPool();

// Get entity from pool (creates new if pool is empty)
BlockDisplay display = pool.getBlockDisplay(location);

// Use the entity...

// Return to pool when done (resets state automatically)
pool.returnBlockDisplay(display);

// Clean up all pooled entities when shutting down
pool.clearAll();
```

### Async Chart Loading

For loading charts without blocking the main thread:
```java
// Simple async loading
CompletableFuture<Chart> future = AsyncChartLoader.loadChartAsync(chartFile);

// With callbacks (runs on main thread)
AsyncChartLoader.loadChartAsync(chartFile,
    chart -> {
        // Success callback
        getLogger().info("Loaded: " + chart.getMetadata().getTitle());
    },
    error -> {
        // Error callback
        getLogger().severe("Failed to load chart: " + error.getMessage());
    }
);

// Batch preload multiple charts
AsyncChartLoader.preloadChartsAsync(chartFiles, chart -> {
    // Called for each loaded chart
    registry.addChart(chart);
});
```

### Scheduling Tasks

Use PlanetLib's scheduler, not Bukkit's:
```java
PlanetLib.getScheduler().runTimer(task, delay, period);
PlanetLib.getScheduler().runDelayed(task, delay);
```

### Logging

Always use plugin logger for consistency:
```java
Main.instance.getLogger().info("message");
Main.instance.getLogger().warning("warning");
```

### Working with Player Settings

Player settings are managed by `PlayerSettingsManager`:
```java
// Get player settings (loads from cache or file)
PlayerSettings settings = Main.instance.getPlayerSettingsManager().getSettings(player);

// Modify settings
settings.setSpeed(1.5);
settings.setOffset(50);
settings.setDifficulty(3); // 1=Easy, 2=Normal, 3=Hard

// Save settings (updates cache and file)
Main.instance.getPlayerSettingsManager().saveSettings(player, settings);
```

Available settings:
- `speed` (double): Note approach speed multiplier
- `offset` (int): Audio timing offset in milliseconds
- `hitSound` (boolean): Enable hit sound effects
- `autoPlay` (boolean): Enable auto-play mode
- `autoFlickRotation` (boolean): Enable automatic camera rotation for Flick notes during auto-play
- `showBeatLines` (boolean): Show beat line markers
- `difficulty` (int): Hitbox size (1=2x, 2=1.5x, 3=1x)

Settings are stored in `plugins/CubeRhythm/player_settings/{uuid}.yml`

### Displaying Results

After a game session ends, use `ResultScreen` to display results:
```java
ResultScreen resultScreen = new ResultScreen(player, chart, scoreManager, settings);
resultScreen.show(); // Displays animated result screen with rank, statistics, and achievements
```

The result screen shows:
- Chart information (title, artist, difficulty)
- Score, accuracy, and rank (SSS+ to D)
- Detailed statistics (Exact/Just/Miss counts and percentages)
- Max combo
- Special achievements (Perfect Play, Full Combo)
  - Achievement displays animate for 30 ticks, then remain visible for 20 ticks (1 second) before disappearing
- Plays appropriate sound effects based on performance

### Using the HUD System

The HUD displays real-time game information on all 4 faces:
```java
// Initialize HUD when starting game session
GameHUD hud = new GameHUD(player, chart, scoreManager, settings);
hud.initialize(); // Creates TextDisplay entities on all faces

// Update HUD each tick (in game loop)
hud.update(); // Updates score and combo displays

// Clean up when game ends
hud.cleanup(); // Removes all HUD entities
```

HUD layout on each face:
- Top center: "自动播放" indicator (green, only shown when auto-play is enabled)
- Top left: Score with "Score" label
- Top right: Combo with "Combo" label (color changes based on performance)
  - Gold: All Perfect (no Just/Miss)
  - Cyan: Full Combo (no Miss but has Just)
  - White: Has Miss
- Bottom left: Difficulty name and level (colored)
- Bottom right: Song title
- Text size: 1.125x scale for better visibility
- Background: Transparent (no background color)

### Movement Restriction

Movement restriction is automatically applied during gameplay:
- Registered as event listener in `Main.onEnable()`
- Prevents position changes (x, y, z) during PLAYING state
- Allows rotation (yaw, pitch) for gameplay
- Shows warning message with 2-second cooldown
- Automatically disabled when game session ends

## Common Editor Workflows (DEPRECATED)

> **⚠️ Note**: These workflows are deprecated as the editor functionality is disabled. Use external tools for chart editing instead.

### Creating a New Chart (Deprecated)

```java
// 1. Player runs: /editor new my_chart
// 2. EditorCommand creates new EditorSession
// 3. Creates empty chart file: plugins/CubeRhythm/my_chart.json
// 4. Player sets parameters:
//    /editor bpm 140
//    /editor pretime 2.0
//    /editor speed 1.0
// 5. Player places notes by looking at face and right-clicking
// 6. Changes auto-save to file
// 7. Player runs: /editor (to exit)
```

### Editing Existing Chart (Deprecated)

```java
// 1. Player runs: /editor load existing_chart
// 2. EditorCommand loads chart from file
// 3. Creates EditorSession with loaded notes
// 4. Player navigates with mouse wheel or /b command
// 5. Player modifies notes (place/delete)
// 6. Changes auto-save to file
// 7. Player runs: /editor save (manual save, optional)
```

### Editor Navigation Tips (Deprecated)

> **⚠️ Note**: These tips are for the deprecated editor system.

- **Quick navigation**: Use `/b <beat>` to jump to specific beat (deprecated)
- **Fine control**: Use `/step 16` for 1/16 beat precision (deprecated)
- **Grid snapping**: Hold Shift while placing for 0.5 block grid (deprecated)
- **Face selection**: Just look at the face you want to place on (automatic detection) (deprecated)
- **Note type cycling**: Scroll to hotbar slot 1 to cycle through note types (deprecated)
- **Preview feedback**: Watch action bar for current position and cursor coordinates (deprecated)

## Documentation

Comprehensive Chinese documentation in root directory:
- `README - 文档总览.md`: Documentation index
- `游戏流程与功能实现.md`: Game flow and mechanics
- `铺面文件格式说明.md`: Chart format specification
- `音符渲染系统说明.md`: Note rendering system
- `铺面加载流程说明.md`: Chart loading process
- `编辑器数据结构说明.md`: Editor data structures
