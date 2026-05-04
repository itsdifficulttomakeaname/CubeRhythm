package org.cubeRhythm.input;

import cn.jason31416.planetlib.PlanetLib;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.cubeRhythm.manager.OffsetConfig;
import org.cubeRhythm.entity.EntityManager;
import org.cubeRhythm.game.GameSession;
import org.cubeRhythm.input.CancelFlagManager.CancelFlag;
import org.cubeRhythm.judgment.JudgmentManager;
import org.cubeRhythm.judgment.JudgmentResult;
import org.cubeRhythm.note.NoteEntity;
import org.cubeRhythm.note.NoteType;

import java.util.HashMap;
import java.util.Map;

public class InputHandler implements Listener {
    private final CancelFlagManager cancelFlagManager;
    private final GameSession gameSession;
    private final EntityManager entityManager;
    private final JudgmentManager judgmentManager;

    // 用于 FLICK 判定的视线追踪
    private float previousYaw;

    // 用于 DOUBLE 判定的点击追踪
    private final Map<NoteEntity, Long> firstClickTime = new HashMap<>();
    private final Map<NoteEntity, Integer> clickCount = new HashMap<>();
    private final Map<NoteEntity, JudgmentResult> firstClickJudgment = new HashMap<>();

    // 用于 HOLD 判定的按键状态机（300ms窗口）
    private final KeyPressCache keyPressCache = new KeyPressCache();

    public InputHandler(GameSession gameSession) {
        this.gameSession = gameSession;
        this.entityManager = gameSession.getEntityManager();
        this.judgmentManager = gameSession.getJudgmentManager();
        this.cancelFlagManager = new CancelFlagManager();
        this.previousYaw = gameSession.getPlayer().getLocation().getYaw();
    }

    /**
     * 检查是否有按键按下（用于 HOLD 音符判定）
     */
    public boolean isAnyKeyPressed() {
        return keyPressCache.hasKeyPressedRecently(gameSession.getPlayer().getUniqueId());
    }

    /**
     * 更新按键缓存
     */
    private void recordKeyPress() {
        keyPressCache.recordKeyPress(gameSession.getPlayer().getUniqueId());
    }

    /**
     * 备用雪球发射事件处理器
     * 作为 PlayerInteract 的备用方案
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        // 如果是游戏中的玩家发射雪球，取消事件（阻止雪球生成和音效）
        if (player.equals(gameSession.getPlayer())) {
            event.setCancelled(true);

            // 检查是否应该取消（防止与 PlayerInteract 重复处理）
            if (cancelFlagManager.shouldCancel(player, CancelFlag.SHOOT)) {
                return;
            }

            recordKeyPress();  // 记录按键
            handleClickInput(player);

            // 确保玩家手中始终有雪球
            ensureSnowballs(player);

            // 记录右键事件
//            org.cubeRhythm.Main.instance.getLogger().fine("右键事件已处理 (ProjectileLaunch)");
        }
    }

    /**
     * 确保玩家手中有雪球
     */
    private void ensureSnowballs(Player player) {
        if (!org.cubeRhythm.input.SnowballManager.hasSnowballs(player)) {
            org.cubeRhythm.input.SnowballManager.giveSnowballs(player);
        }
    }

    /**
     * 处理左键点击（PlayerAnimation 事件）
     */
    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!player.equals(gameSession.getPlayer())) {
            return;
        }

        if (cancelFlagManager.shouldCancel(player, CancelFlag.ANIMATION)) {
            return;
        }

        recordKeyPress();  // 记录按键
        handleClickInput(player);
    }

    /**
     * 处理右键点击实体事件
     * 当玩家直接右键点击到音符实体时触发
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.equals(gameSession.getPlayer())) {
            return;
        }

        // 检查是否应该取消（防止重复处理）
        if (cancelFlagManager.shouldCancel(player, CancelFlag.INTERACT_ENTITY)) {
            return;
        }

        // 记录按键状态
        recordKeyPress();

        // 检查点击的是否是音符的Interaction实体
        Entity clickedEntity = event.getRightClicked();
        if (clickedEntity instanceof Interaction interaction) {
            // 查找对应的 NoteEntity
            NoteEntity noteEntity = findNoteEntityByInteraction(interaction);
            if (noteEntity != null && !noteEntity.isHit()) {
                // 计算时间并判定
                int timingOffset = judgmentManager.calculateTimingOffset(noteEntity, gameSession.getSettings().getSpeed());
                JudgmentResult judgment = judgmentManager.judge(timingOffset);

                // 如果不在判定窗口内（提前超过 80ms），不处理
                if (judgment == null) {
//                    org.cubeRhythm.Main.instance.getLogger().info(
//                        String.format("点击过早，不在判定窗口内: type=%s, offset=%dms",
//                            noteEntity.getType(), timingOffset)
//                    );
                    return;
                }

//                org.cubeRhythm.Main.instance.getLogger().info(
//                    String.format("直接点击实体判定: type=%s, offset=%dms, judgment=%s",
//                        noteEntity.getType(), timingOffset, judgment)
//                );

                // 处理特殊音符类型
                if (noteEntity.getType() == NoteType.DOUBLE) {
                    handleDoubleNote(noteEntity, judgment, timingOffset);
                } else if (noteEntity.getType() == NoteType.TAP) {
                    processJudgment(noteEntity, judgment, timingOffset);
                }
            }
        }

        // 确保玩家手中始终有雪球
        ensureSnowballs(player);
    }

    /**
     * 主要右键处理器（PlayerInteract 事件）
     * 使用射线检测判断是否击中音符
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.equals(gameSession.getPlayer())) {
            return;
        }

        // 只处理右键空气或右键方块
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 必须持有雪球（空手右键空气不能被监听）
        if (!org.cubeRhythm.input.SnowballManager.hasSnowballs(player)) {
            return;
        }

        // 取消事件，防止雪球发射
        event.setCancelled(true);

        // 检查是否应该取消（防止重复处理）
        if (cancelFlagManager.shouldCancel(player, CancelFlag.INTERACT)) {
            return;
        }

        // 记录按键状态
        recordKeyPress();

        // 使用射线检测判断是否击中音符
        handleClickInput(player);

        // 确保玩家手中始终有雪球
        ensureSnowballs(player);

        // 记录右键事件
//        org.cubeRhythm.Main.instance.getLogger().info("右键事件已处理 (PlayerInteract)");
    }

    /**
     * 处理点击输入（TAP 和 DOUBLE 音符）
     */
    private void handleClickInput(Player player) {
        // 记录点击事件
//        org.cubeRhythm.Main.instance.getLogger().fine("处理点击输入");

        // 射线检测以找到目标交互实体
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            50.0,
            entity -> entity instanceof Interaction
        );

        if (result == null || result.getHitEntity() == null) {
            // 未击中任何音符 - 这是正常的
//            org.cubeRhythm.Main.instance.getLogger().fine("未击中任何音符");
            return;
        }

        Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof Interaction interaction)) {
            return;
        }

//        org.cubeRhythm.Main.instance.getLogger().fine("击中 Interaction 实体");

        // 查找对应的 NoteEntity
        NoteEntity noteEntity = findNoteEntityByInteraction(interaction);
        if (noteEntity == null) {
//            org.cubeRhythm.Main.instance.getLogger().warning("找到 Interaction 但无法找到对应的 NoteEntity");
            return;
        }

        if (noteEntity.isHit()) {
            // 音符已经被击中，忽略
//            org.cubeRhythm.Main.instance.getLogger().fine("音符已被击中，忽略");
            return;
        }

        // 计算时间并判定
        int timingOffset = judgmentManager.calculateTimingOffset(noteEntity, gameSession.getSettings().getSpeed());
        JudgmentResult judgment = judgmentManager.judge(timingOffset);

        // 如果不在判定窗口内（提前超过 80ms），不处理
        if (judgment == null) {
//            org.cubeRhythm.Main.instance.getLogger().info(
//                String.format("点击过早，不在判定窗口内: type=%s, offset=%dms",
//                    noteEntity.getType(), timingOffset)
//            );
            return;
        }

        // 记录判定信息
//        org.cubeRhythm.Main.instance.getLogger().info(
//            String.format("音符判定: type=%s, offset=%dms, judgment=%s",
//                noteEntity.getType(), timingOffset, judgment)
//        );

        // 处理特殊音符类型
        if (noteEntity.getType() == NoteType.DOUBLE) {
            handleDoubleNote(noteEntity, judgment, timingOffset);
        } else if (noteEntity.getType() == NoteType.TAP) {
            // TAP 音符正常处理
            processJudgment(noteEntity, judgment, timingOffset);
        }
        // DRAG、HOLD 和 FLICK 音符不应该通过点击判定，它们有自动判定逻辑
    }

    /**
     * 处理 DOUBLE 音符的双击逻辑
     */
    private void handleDoubleNote(NoteEntity noteEntity, JudgmentResult judgment, int timingOffset) {
        int currentCount = clickCount.getOrDefault(noteEntity, 0) + 1;
        clickCount.put(noteEntity, currentCount);

        if (currentCount == 1) {
            // 第一次点击
            firstClickTime.put(noteEntity, System.currentTimeMillis());
            firstClickJudgment.put(noteEntity, judgment);

            // 启动 0.5 秒定时器
            PlanetLib.getScheduler().runLater(() -> {
                if (clickCount.getOrDefault(noteEntity, 0) == 1 && !noteEntity.isHit()) {
                    // 超时，只点击了一次，判为 MISS
                    processJudgment(noteEntity, JudgmentResult.MISS, 0);
                    clickCount.remove(noteEntity);
                    firstClickTime.remove(noteEntity);
                    firstClickJudgment.remove(noteEntity);
                }
            }, 10L);  // 0.5 秒 = 10 ticks

        } else if (currentCount == 2) {
            // 第二次点击
            long elapsed = System.currentTimeMillis() - firstClickTime.get(noteEntity);
            if (elapsed <= 500) {
                // 在时间内，取两次判定中较差的结果
                // 如果任一为 JUST，则整体为 JUST
                JudgmentResult firstJudgment = firstClickJudgment.get(noteEntity);
                JudgmentResult finalJudgment;

                if (firstJudgment == JudgmentResult.MISS || judgment == JudgmentResult.MISS) {
                    finalJudgment = JudgmentResult.MISS;
                } else if (firstJudgment == JudgmentResult.JUST || judgment == JudgmentResult.JUST) {
                    finalJudgment = JudgmentResult.JUST;
                } else {
                    finalJudgment = JudgmentResult.EXACT;
                }

                processJudgment(noteEntity, finalJudgment, timingOffset);
            } else {
                // 超时
                processJudgment(noteEntity, JudgmentResult.MISS, 0);
            }

            clickCount.remove(noteEntity);
            firstClickTime.remove(noteEntity);
            firstClickJudgment.remove(noteEntity);
        }
    }

    /**
     * 处理判定结果并显示效果
     */
    private void processJudgment(NoteEntity noteEntity, JudgmentResult judgment, int timingOffset) {
        noteEntity.setHit(true);
        gameSession.getScoreManager().recordJudgment(judgment, timingOffset);

        // 播放击中音效（HOLD 音符除外）
        Player player = gameSession.getPlayer();
        if (judgment != JudgmentResult.MISS && noteEntity.getType() != NoteType.HOLD) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.5f, 1.5f);
        }

        // 在状态栏显示判定结果
        String actionBarText = judgment.getDisplayText();
        if (judgment == JudgmentResult.JUST) {
            actionBarText += " " + judgmentManager.getEarlyLate(timingOffset);
        }
        player.sendActionBar(actionBarText);

        // 显示判定文本
        showJudgmentText(noteEntity, judgment, timingOffset);

        // 清理音符实体
        noteEntity.cleanup();
        entityManager.unregisterEntity(noteEntity.getLinkUUID());
    }

    /**
     * 显示判定文本（公共方法，供 GameSession 调用）
     */
    public void showJudgmentText(NoteEntity noteEntity, JudgmentResult judgment, int timingOffset) {
        Player player = gameSession.getPlayer();
        if (judgment == JudgmentResult.MISS) {
            player.sendTitle("", "§4Miss", 2, 10, 6);
            return;
        }

        if (noteEntity.getType() == NoteType.HOLD) return;
        if (noteEntity.getBlockDisplay() == null) return;

        double cx = gameSession.getCenterX(), cy = gameSession.getCenterY(), cz = gameSession.getCenterZ();
        String color = judgment == JudgmentResult.EXACT ? "§b" : "§e";
        String label = (judgment == JudgmentResult.EXACT ? color + "Exact" : color + "Just");
        if (judgment == JudgmentResult.JUST) label += " " + judgmentManager.getEarlyLate(timingOffset);

        float yaw = switch (noteEntity.getFace()) {
            case W -> 180f; case A -> 90f; case S -> 0f; case D -> 270f;
        };

        OffsetConfig oc = OffsetConfig.get();
        if (noteEntity.getType() == NoteType.DOUBLE && noteEntity.getPositions() != null) {
            for (org.cubeRhythm.coordinate.NotePosition pos : noteEntity.getPositions()) {
                Location l = CoordinateSystem.transformCoordinates(
                    noteEntity.getBlockDisplay().getWorld(), noteEntity.getFace(),
                    pos.getX() + 0.0, pos.getY() + oc.cursorY(noteEntity.getFace()),
                    oc.cursorZ(noteEntity.getFace()), cx, cy, cz);
                l.setYaw(yaw);
                spawnHitEffect(l, color, label, judgment);
            }
        } else {
            double nx = noteEntity.getPosition() != null ? noteEntity.getPosition().getX() + 0.0 : 0;
            double ny = noteEntity.getPosition() != null ? noteEntity.getPosition().getY() + oc.cursorY(noteEntity.getFace()) : oc.cursorY(noteEntity.getFace());
            Location loc = CoordinateSystem.transformCoordinates(
                noteEntity.getBlockDisplay().getWorld(), noteEntity.getFace(), nx, ny,
                oc.cursorZ(noteEntity.getFace()), cx, cy, cz);
            loc.setYaw(yaw);
            spawnHitEffect(loc, color, label, judgment);
        }
    }

    private void spawnHitEffect(Location loc, String color, String label, JudgmentResult judgment) {
        OffsetConfig oc = OffsetConfig.get();
        float initScale = (float) oc.hitEffectScale;
        float txf = (float) oc.hitEffectTransXFactor;
        float tyf = (float) oc.hitEffectTransYFactor;

        TextDisplay textEnt = DisplayEntityFactory.createTextDisplay(loc.getWorld(), loc, label, 0);
        textEnt.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        textEnt.setBackgroundColor(org.bukkit.Color.fromARGB(0));
        textEnt.setShadowed(false);
        org.bukkit.util.Transformation t1 = textEnt.getTransformation();
        t1.getScale().set(initScale, initScale, initScale);
        t1.getTranslation().set(-initScale * txf, -initScale * tyf, 0);
        textEnt.setTransformation(t1);
        textEnt.setTextOpacity((byte) 255);

        TextDisplay glowEnt = DisplayEntityFactory.createTextDisplay(loc.getWorld(), loc, color + "█", 0);
        glowEnt.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        glowEnt.setBackgroundColor(org.bukkit.Color.fromARGB(0));
        glowEnt.setShadowed(false);
        org.bukkit.util.Transformation tg = glowEnt.getTransformation();
        tg.getScale().set(initScale, initScale, initScale);
        tg.getTranslation().set(-initScale * txf, -initScale * tyf, 0);
        glowEnt.setTransformation(tg);
        glowEnt.setTextOpacity((byte) 63);
        float decayFactor = (float) (judgment == JudgmentResult.EXACT ? oc.hitEffectDecayExact : oc.hitEffectDecayJust);

        final float[] scale1 = {initScale}, scale2 = {initScale};
        final int[] alpha = {255}, tick = {0};
        cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask[] th = new cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask[1];
        th[0] = PlanetLib.getScheduler().runTimer(() -> {
            tick[0]++;
            alpha[0] = Math.max(0, alpha[0] - (int) oc.hitEffectAlphaDecay);
            scale1[0] = Math.max((float) oc.hitEffectScaleMin, scale1[0] * (float) oc.hitEffectScaleDecay);
            org.bukkit.util.Transformation tr1 = textEnt.getTransformation();
            tr1.getScale().set(scale1[0], scale1[0], scale1[0]);
            tr1.getTranslation().set(-scale1[0] * txf, -scale1[0] * tyf, 0);
            textEnt.setTransformation(tr1);
            textEnt.setTextOpacity((byte) alpha[0]);
            scale2[0] *= decayFactor;
            float gs = (float) oc.hitEffectGlowBase - scale2[0];
            org.bukkit.util.Transformation tr2 = glowEnt.getTransformation();
            tr2.getScale().set(gs, gs, gs);
            tr2.getTranslation().set(-gs * txf, -gs * tyf, 0);
            glowEnt.setTransformation(tr2);
            glowEnt.setTextOpacity((byte) (alpha[0] > (int) oc.hitEffectGlowAlphaThreshold ? alpha[0] / 4 : (int) oc.hitEffectGlowAlphaMin));
            if (tick[0] >= oc.hitEffectDuration) { th[0].cancel(); textEnt.remove(); glowEnt.remove(); }
        }, 0L, 1L);
    }

    private NoteEntity findNoteEntityByInteraction(Interaction interaction) {
        for (NoteEntity entity : entityManager.getAllEntities()) {
            if (entity.getInteraction() != null &&
                entity.getInteraction().getUniqueId().equals(interaction.getUniqueId())) {
                return entity;
            }
            // 检查额外的 Interaction（DOUBLE 音符）
            for (Interaction additionalInteraction : entity.getAdditionalInteractions()) {
                if (additionalInteraction != null &&
                    additionalInteraction.getUniqueId().equals(interaction.getUniqueId())) {
                    return entity;
                }
            }
        }
        return null;
    }

    public void cleanup() {
        cancelFlagManager.clearAll();
        clickCount.clear();
        firstClickTime.clear();
        firstClickJudgment.clear();
        keyPressCache.clearAll();
    }
}
