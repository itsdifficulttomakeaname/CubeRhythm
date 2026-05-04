package org.cubeRhythm.game;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.entity.EntityManager;
import org.cubeRhythm.input.InputHandler;
import org.cubeRhythm.judgment.JudgmentManager;
import org.cubeRhythm.judgment.ScoreManager;
import org.cubeRhythm.note.*;

import static org.cubeRhythm.Main.instance;

@Getter
public class GameSession {
    private final Player player;
    private final Chart chart;
    private final PlayerSettings settings;

    private GameState state;
    private final EntityManager entityManager;
    private final JudgmentManager judgmentManager;
    private final ScoreManager scoreManager;
    private NoteSpawner noteSpawner;
    private InputHandler inputHandler;
    private GameHUD gameHUD;
    private EasingMotionManager easingMotionManager;

    private WrappedTask gameLoop;
    private WrappedTask previewStopTask;
    private WrappedTask startGameplayTask;
    private long startTime;
    private double currentTime;
    private int globalTick = 0;

    private String playingAudioKey; // 存储正在播放的音乐key，用于停止

    private final World world;
    private final double centerX;
    private final double centerY;
    private final double centerZ;

    public GameSession(Player player, Chart chart, PlayerSettings settings) {
        this.player = player;
        this.chart = chart;
        this.settings = settings;
        this.state = GameState.IDLE;

        // Initialize managers
        this.entityManager = new EntityManager();
        this.world = player.getWorld();

        // 使用配置的游戏位置而不是玩家当前位置
        double[] gameLocation = instance.getConfigManager().getGameLocation();
        this.centerX = gameLocation[0];
        this.centerY = gameLocation[1];
        this.centerZ = gameLocation[2];

        this.judgmentManager = new JudgmentManager(centerX, centerZ);
        this.scoreManager = new ScoreManager(chart.getTotalNotes());
    }

    /**
     * 开始游戏会话
     */
    public void start() {
        if (state != GameState.IDLE) {
            return;
        }

        state = GameState.LOADING;

        instance.getLogger().info("=== 游戏会话开始 ===");
        instance.getLogger().info("谱面: " + chart.getMetadata().getTitle());
        instance.getLogger().info("总音符数: " + chart.getTotalNotes());
        instance.getLogger().info("速度: " + settings.getSpeed());

        // 传送玩家到游戏位置
        Location gameLocation = new Location(world, centerX, centerY, centerZ);
        player.teleport(gameLocation);
        instance.getLogger().info("玩家已传送到游戏位置: " + centerX + ", " + centerY + ", " + centerZ);

        // 显示歌曲信息
        String title = "§6§l" + chart.getMetadata().getTitle();
        String subtitle = "§e" + chart.getMetadata().getArtist() + " §7| §e" + chart.getMetadata().getCharter();
        if (settings.isAutoPlay()) subtitle += " §8| §aAutoPlay §8| §a自动演示中";
        player.sendTitle(title, subtitle, 10, 60, 20);

        // 播放音频预览（在标题显示期间）
        String audioKey = chart.getMetadata().getAudio();
        if (audioKey != null && !audioKey.isEmpty()) {
            playingAudioKey = audioKey;
            try {
                player.playSound(player.getLocation(), audioKey, 1.0f, 1.0f);
                instance.getLogger().info("播放音乐预览: " + audioKey);

                // 在标题消失时停止音频（90 ticks = 4.5秒后）
                previewStopTask = PlanetLib.getScheduler().runLater(() -> {
                    player.stopSound(audioKey);
                    instance.getLogger().info("停止音乐预览: " + audioKey);
                }, 90L);
            } catch (Exception e) {
                instance.getLogger().warning("无法播放音乐预览 " + audioKey + ": " + e.getMessage());
            }
        }

        // 延迟5秒后开始游戏
        startGameplayTask = PlanetLib.getScheduler().runLater(this::startGameplay, 100L);
    }

    /**
     * 实际开始游戏玩法（在显示歌曲信息后调用）
     */
    private void startGameplay() {
        // 初始化生成器
        noteSpawner = new NoteSpawner(
            chart,
            entityManager,
            player,
            settings.getSpeed(),
            settings.getHitboxScale(),  // 传递难度对应的碰撞箱缩放
            world,
            centerX,
            centerY,
            centerZ
        );

        easingMotionManager = new EasingMotionManager(entityManager);
        noteSpawner.setEasingMotionManager(easingMotionManager);

        instance.getLogger().info("未生成音符数: " + noteSpawner.getUnspawnedCount());

        // 初始化输入处理器
        inputHandler = new InputHandler(this);
        instance.getServer().getPluginManager().registerEvents(inputHandler, instance);

        // 给玩家雪球以捕捉右键事件
        org.cubeRhythm.input.SnowballManager.giveSnowballs(player);

        // 初始化游戏 HUD
        gameHUD = new GameHUD(player, chart, scoreManager, settings);
        gameHUD.initialize();

        // 启动游戏循环（立即开始，currentTime 从 -1 开始，音符提前1秒渲染）
        startTime = System.currentTimeMillis();
        state = GameState.PLAYING;

        gameLoop = PlanetLib.getScheduler().runTimer(this::tick, 0L, 1L);

        // 延迟1秒播放音乐，与 currentTime=0 时刻对齐
        String audioKey = chart.getMetadata().getAudio();
        if (audioKey != null && !audioKey.isEmpty()) {
            playingAudioKey = audioKey;
            PlanetLib.getScheduler().runLater(() -> {
                try {
                    player.playSound(player.getLocation(), audioKey, 1.0f, 1.0f);
                } catch (Exception e) {
                    instance.getLogger().warning("无法播放音乐 " + audioKey + ": " + e.getMessage());
                }
            }, 20L); // 1秒 = 20 ticks
        }

        player.sendMessage("§a游戏开始！");
    }

    /**
     * 主游戏循环 - 每 tick 运行一次（20 TPS）
     */
    private void tick() {
        if (state != GameState.PLAYING) {
            return;
        }
        globalTick++;

        // 更新当前时间
        // 铺面offset: 铺面相对音频的时间偏移（正值=铺面延后，负值=铺面提前）
        // 玩家offset: 玩家个人时间偏移（负值=延后音符，正值=提前音符）
        double chartOffset = chart.getMetadata().getOffset() / 1000.0;
        double playerOffset = settings.getOffset() / 1000.0;
        double realTime = (System.currentTimeMillis() - startTime) / 1000.0;
        currentTime = realTime - chartOffset + playerOffset;

        // 生成音符
        noteSpawner.update(currentTime);

        // 更新 easing_motion
        if (easingMotionManager != null) {
            easingMotionManager.tick();
            // 对刚激活 v 缓动（startTime == globalTick）的音符记录当前距离
            for (org.cubeRhythm.note.NoteEntity e : entityManager.getAllEntities()) {
                if (e.getEasingType() != null && e.getEasingStartTime() != null
                        && e.getEasingStartTime() == globalTick && e.getEasingStartDistance() == 0) {
                    double d = settings.getSpeed() * 20 * (e.getTime() + 1.0 - currentTime) + 4;
                    e.setEasingStartDistance(d);
                }
            }
        }

        // 更新所有音符位置（音符时间+1秒用于提前渲染，配合音乐延迟1秒播放）
        for (NoteEntity entity : entityManager.getAllEntities()) {
            double noteTime = entity.getTime();
            double distance;

            // v 缓动：用积分计算当前距离
            if (entity.getEasingType() != null && entity.getEasingStartTime() != null) {
                double tElapsed = globalTick - entity.getEasingStartTime();
                double distanceTraveled = org.cubeRhythm.note.EasingMotionManager.integralSpeed(
                    entity.getEasingType(), entity.getEasingLambda(), tElapsed);
                distance = entity.getEasingStartDistance() - distanceTraveled;
            } else {
                distance = settings.getSpeed() * 20 * (noteTime + 1.0 - currentTime) + 3;
            }

            NoteRenderer.updateNote(
                    entity,
                    world,
                    centerX,
                    centerY,
                    centerZ,
                    settings.getSpeed(),
                    distance,
                    chart.getMetadata().getBpm()
            );

            // 自动判定逻辑（DRAG、HOLD 和 FLICK 音符）+ 自动演奏
            if (!entity.isHit()) {
                // 判定条件：音符到达判定线附近
                // 参考Skript: z location <= 4 + speed
                // 调整：提前2格判定，修正滞后问题
                if(entity.getType() == NoteType.HOLD && (entity.getFace() == Face.A || entity.getFace() == Face.D)) distance -= entity.getHoldScaleZ();
                boolean inJudgmentZone = distance <= (3.0 + settings.getSpeed() + 2.0);

                if (inJudgmentZone) {
                    // 自动演奏模式：自动完美判定所有音符（包括TAP和DOUBLE）
                    if (settings.isAutoPlay()) {
                        handleAutoPlay(entity);
                        continue; // 跳过正常判定逻辑
                    }

                    // 正常游戏模式下，只有DRAG、HOLD、FLICK自动判定
                    // TAP和DOUBLE需要玩家点击
                    if (entity.getType() == org.cubeRhythm.note.NoteType.DRAG) {
                        // DRAG: 检查准心是否指向音符（检查所有组成部分）
                        boolean isLookingAt = org.cubeRhythm.input.ViewDirectionHelper.isLookingAt(
                                player,
                                entity,
                                50.0
                        );

                        if (isLookingAt) {
                            int timingOffset = judgmentManager.calculateTimingOffset(entity, settings.getSpeed());
                                org.cubeRhythm.judgment.JudgmentResult judgment = judgmentManager.judge(timingOffset);

                                // 如果不在判定窗口内，不处理
                                if (judgment == null) {
                                    // 提前超过 80ms，不处理
                                    continue;
                                }

                                if (judgment != org.cubeRhythm.judgment.JudgmentResult.MISS) {
                                    // DRAG 音符只有 EXACT 判定，没有 JUST
                                    if (judgment == org.cubeRhythm.judgment.JudgmentResult.JUST) {
                                        judgment = org.cubeRhythm.judgment.JudgmentResult.EXACT;
                                    }

                                    entity.setHit(true);
                                    scoreManager.recordJudgment(judgment, timingOffset);

                                    // 播放打击音效
                                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.5f, 1.5f);

                                    // 在状态栏显示判定结果
                                    player.sendActionBar(judgment.getDisplayText());

                                    // 显示判定文本
                                    if (inputHandler != null) {
                                        inputHandler.showJudgmentText(entity, judgment, timingOffset);
                                    }

                                    // 清理音符实体
                                    entity.cleanup();
                                    entityManager.unregisterEntity(entity.getLinkUUID());
                                }
                            }
                    } else if (entity.getType() == org.cubeRhythm.note.NoteType.HOLD) {
                        // HOLD: 检查是否有按键按下（通过检查玩家是否在最近有输入）
                        if (inputHandler != null && inputHandler.isAnyKeyPressed()) {
                            int timingOffset = judgmentManager.calculateTimingOffset(entity, settings.getSpeed());
                            org.cubeRhythm.judgment.JudgmentResult judgment = judgmentManager.judge(timingOffset);

                            // 如果不在判定窗口内，不处理
                            if (judgment == null) {
                                continue;
                            }

                            if (judgment != org.cubeRhythm.judgment.JudgmentResult.MISS) {
                                // HOLD 音符只有 EXACT 判定，没有 JUST
                                if (judgment == org.cubeRhythm.judgment.JudgmentResult.JUST) {
                                    judgment = org.cubeRhythm.judgment.JudgmentResult.EXACT;
                                }

                                entity.setHit(true);
                                scoreManager.recordJudgment(judgment, timingOffset);

                                // HOLD 音符不播放打击音效

                                // 在状态栏显示判定结果
                                player.sendActionBar(judgment.getDisplayText());

                                // HOLD 音符不显示判定文本（只在状态栏显示）

                                // 清理音符实体
                                entity.cleanup();
                                entityManager.unregisterEntity(entity.getLinkUUID());
                            }
                        }
                    } else if (entity.getType() == org.cubeRhythm.note.NoteType.FLICK) {
                        // FLICK: 从到达判定面前200ms到其后200ms内，每tick检查角度是否在范围内
                        // timingOffset < 0 表示音符已经过了判定线（负值越大，过的时间越久）
                        // timingOffset = 0 表示音符在判定线上
                        // timingOffset > 0 表示音符还没到判定线
                        String targetDirection = entity.getTurn();
                        if (targetDirection != null) {
                            int timingOffset = judgmentManager.calculateTimingOffset(entity, settings.getSpeed());

                            // 从判定线前200ms到其后200ms内检查（timingOffset从+200到-200）
                            if (timingOffset <= 200 && timingOffset >= -200) {
                                float playerYaw = org.cubeRhythm.input.ViewDirectionHelper.getPlayerYaw(player);

                                // 根据判定面和转向方向计算目标 yaw 范围
                                boolean isInRange = checkFlickDirection(entity.getFace(), targetDirection, playerYaw);

                                if (isInRange) {
                                    org.cubeRhythm.judgment.JudgmentResult judgment = judgmentManager.judge(timingOffset);

                                    // 如果不在判定窗口内，不处理
                                    if (judgment == null) {
                                        continue;
                                    }

                                    if (judgment != org.cubeRhythm.judgment.JudgmentResult.MISS) {
                                        // FLICK 音符只有 EXACT 判定，没有 JUST
                                        if (judgment == org.cubeRhythm.judgment.JudgmentResult.JUST) {
                                            judgment = org.cubeRhythm.judgment.JudgmentResult.EXACT;
                                        }

                                        entity.setHit(true);
                                        scoreManager.recordJudgment(judgment, timingOffset);

                                        // 播放打击音效
                                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.5f, 1.5f);

                                        // 在状态栏显示判定结果
                                        player.sendActionBar(judgment.getDisplayText());

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
                    }
                }

                // 自动移除超过判定线太远的音符
                // 参考Skript: z location < 4 - speed*4
                if (distance < (3.0 - settings.getSpeed() * 4.0)) {
                    if (!entity.isHit()) {
                        // Miss
                        scoreManager.recordJudgment(org.cubeRhythm.judgment.JudgmentResult.MISS);
                    }
                    entityManager.unregisterEntity(entity.getLinkUUID());
                }
            }
        }

        // 更新游戏 HUD
        if (gameHUD != null) {
            gameHUD.update();
        }

        // 检查游戏是否完成（移到循环外，确保即使没有音符也能检查）
        // 以时长为准：当前时间超过铺面时长时结束
        double chartDuration = chart.getMetadata().getDuration();
        if (currentTime >= chartDuration) {
            instance.getLogger().info("游戏结束条件触发（时长到达）:");
            instance.getLogger().info("  当前时间: " + currentTime);
            instance.getLogger().info("  铺面时长: " + chartDuration);
            end();
        }
    }

    /**
     * 检查玩家视角是否在 FLICK 音符的目标方向范围内
     * 使用Skript中的精确判定逻辑
     * @param face 音符所在的判定面
     * @param targetDirection 目标方向（"left" 或 "right"）
     * @param playerYaw 玩家当前的 yaw 角度
     * @return 是否在范围内
     */
    private boolean checkFlickDirection(org.cubeRhythm.coordinate.Face face, String targetDirection, float playerYaw) {
        // 标准化玩家 yaw 到 -180 到 180 范围
        playerYaw = normalizeAngle(playerYaw);

        boolean result = false;

        if (face == org.cubeRhythm.coordinate.Face.W) {
            // 面W（前方）
            if (targetDirection.equalsIgnoreCase("left")) {
                // 左转：-135 到 -45
                result = playerYaw >= -135f && playerYaw <= -45f;
            } else {
                // 右转：45 到 135
                result = playerYaw >= 45f && playerYaw <= 135f;
            }
        } else if (face == org.cubeRhythm.coordinate.Face.A) {
            // 面A（左侧）
            if (targetDirection.equalsIgnoreCase("left")) {
                // 左转：135 到 180 或 -180 到 -135
                result = (playerYaw >= 135f && playerYaw <= 180f) ||
                         (playerYaw >= -180f && playerYaw <= -135f);
            } else {
                // 右转：-45 到 45
                result = playerYaw >= -45f && playerYaw <= 45f;
            }
        } else if (face == org.cubeRhythm.coordinate.Face.S) {
            // 面S（后方）
            if (targetDirection.equalsIgnoreCase("left")) {
                // 左转：45 到 135
                result = playerYaw >= 45f && playerYaw <= 135f;
            } else {
                // 右转：-135 到 -45
                result = playerYaw >= -135f && playerYaw <= -45f;
            }
        } else if (face == org.cubeRhythm.coordinate.Face.D) {
            // 面D（右侧）
            if (targetDirection.equalsIgnoreCase("right")) {
                // 右转：135 到 180 或 -180 到 -135
                result = (playerYaw >= 135f && playerYaw <= 180f) ||
                         (playerYaw >= -180f && playerYaw <= -135f);
            } else {
                // 左转：-45 到 45
                result = playerYaw >= -45f && playerYaw <= 45f;
            }
        }

        instance.getLogger().fine(String.format("FLICK判定: face=%s, turn=%s, yaw=%.1f, result=%b",
            face, targetDirection, playerYaw, result));

        return result;
    }

    /**
     * 标准化角度到 -180 到 180 范围
     */
    private float normalizeAngle(float angle) {
        while (angle > 180f) {
            angle -= 360f;
        }
        while (angle < -180f) {
            angle += 360f;
        }
        return angle;
    }

    /**
     * 结束游戏会话
     */
    public void end() {
        if (state == GameState.RESULTS) {
            return;
        }

        state = GameState.RESULTS;

        if (gameLoop != null) {
            gameLoop.cancel();
        }

        // 停止播放音乐
        if (playingAudioKey != null && !playingAudioKey.isEmpty()) {
            try {
                player.stopSound(playingAudioKey);
                instance.getLogger().info("停止音乐: " + playingAudioKey);
            } catch (Exception e) {
                instance.getLogger().warning("停止音乐失败: " + e.getMessage());
            }
        }

        // 清理
        entityManager.cleanupAll();
        if (inputHandler != null) {
            inputHandler.cleanup();
            // 注销事件监听器
            org.bukkit.event.HandlerList.unregisterAll(inputHandler);
        }

        // 清理游戏 HUD
        if (gameHUD != null) {
            gameHUD.cleanup();
            gameHUD = null;
        }

        // 清除雪球
        org.cubeRhythm.input.SnowballManager.removeSnowballs(player);

        // 显示结果（使用新的ResultScreen系统）
        ResultScreen resultScreen = new ResultScreen(player, chart, scoreManager, settings);
        resultScreen.show();
    }

    /**
     * 暂停游戏
     */
    public void pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    /**
     * 恢复游戏
     */
    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    /**
     * 停止并清理
     */
    public void stop() {
        if (previewStopTask != null) {
            previewStopTask.cancel();
        }
        if (startGameplayTask != null) {
            startGameplayTask.cancel();
        }
        if (gameLoop != null) {
            gameLoop.cancel();
        }
        entityManager.cleanupAll();
        if (inputHandler != null) {
            inputHandler.cleanup();
            // 注销事件监听器
            org.bukkit.event.HandlerList.unregisterAll(inputHandler);
        }

        // 清理游戏 HUD
        if (gameHUD != null) {
            gameHUD.cleanup();
            gameHUD = null;
        }

        // 停止播放音乐
        if (playingAudioKey != null && !playingAudioKey.isEmpty()) {
            try {
                player.stopSound(playingAudioKey);
                instance.getLogger().info("停止音乐: " + playingAudioKey);
            } catch (Exception e) {
                instance.getLogger().warning("停止音乐失败: " + e.getMessage());
            }
        }

        state = GameState.IDLE;
    }

    /**
     * 自动演奏：自动完美判定音符
     */
    private void handleAutoPlay(NoteEntity entity) {
        // 计算时间偏移
        int timingOffset = judgmentManager.calculateTimingOffset(entity, settings.getSpeed());

        // HOLD 音符：前端到达判定线时（timingOffset >= 0）立即判定
//        boolean shouldJudge = entity.getType() == org.cubeRhythm.note.NoteType.HOLD
//            ? timingOffset >= 0
//            : Math.abs(timingOffset) <= 80;

        boolean shouldJudge = entity.getType() == NoteType.HOLD || timingOffset <= 0;

        if (shouldJudge) {
            entity.setHit(true);

            // 自动演奏总是EXACT判定
            org.cubeRhythm.judgment.JudgmentResult judgment = org.cubeRhythm.judgment.JudgmentResult.EXACT;
            scoreManager.recordJudgment(judgment);

            // 播放打击音效（除了HOLD音符）
            if (entity.getType() != org.cubeRhythm.note.NoteType.HOLD && settings.isHitSound()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.5f, 1.5f);
            }

            // 在状态栏显示判定结果
            player.sendActionBar(judgment.getDisplayText());

            // 显示光晕特效
            if (inputHandler != null) {
                inputHandler.showJudgmentText(entity, judgment, timingOffset);
            }

            // Flick自动转向：如果启用且是Flick音符，触发平滑转向动画
            if (settings.isAutoFlickRotation() && entity.getType() == org.cubeRhythm.note.NoteType.FLICK) {
                String turnDirection = entity.getTurn();
                if (turnDirection != null) {
                    if (turnDirection.equals("left")) {
                        smoothRotatePlayer(-90.0f, 15); // 左转90度，15 ticks动画
                    } else if (turnDirection.equals("right")) {
                        smoothRotatePlayer(90.0f, 15); // 右转90度，15 ticks动画
                    }
                }
            }

            // 清理音符实体
            entity.cleanup();
            entityManager.unregisterEntity(entity.getLinkUUID());
        }
    }

    /**
     * 平滑旋转玩家视角（ease-out动画：先快后慢）
     * @param deltaYaw 旋转角度（正值=右转，负值=左转）
     * @param duration 动画持续时间（ticks）
     */
    private void smoothRotatePlayer(float deltaYaw, int duration) {
        final float startYaw = player.getLocation().getYaw();
        final float targetYaw = startYaw + deltaYaw;
        final int[] tick = {0};

        WrappedTask[] taskHolder = new WrappedTask[1];
        taskHolder[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;

            // Ease-out cubic: 1 - (1-t)^3
            double progress = (double) tick[0] / duration;
            double easedProgress = 1 - Math.pow(1 - progress, 3);

            float currentYaw = (float) (startYaw + deltaYaw * easedProgress);

            Location loc = player.getLocation();
            loc.setYaw(currentYaw);
            player.teleport(loc);

            if (tick[0] >= duration) {
                // 确保最终角度精确
                Location finalLoc = player.getLocation();
                finalLoc.setYaw(targetYaw);
                player.teleport(finalLoc);
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    public double getSpeed() {
        return settings.getSpeed();
    }
}
