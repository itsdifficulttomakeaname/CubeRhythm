package org.cubeRhythm.note;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.entity.EntityManager;

import java.util.ArrayList;
import java.util.List;

public class NoteSpawner {
    private final Chart chart;
    private final EntityManager entityManager;
    private final List<Note> unspawnedNotes;
    private final List<Note> unexecutedActions;
    private final Player player;
    private final double speed;
    private final float hitboxScale;  // 根据难度的碰撞箱缩放
    private final double bpm;  // BPM用于计算HOLD音符长度
    private final World world;
    private final double centerX;
    private final double centerY;
    private final double centerZ;

    private static final int MAX_ENTITIES = 100;
    private static final double SPAWN_DISTANCE = 50.0;

    public NoteSpawner(Chart chart, EntityManager entityManager, Player player, double speed, float hitboxScale,
                       World world, double centerX, double centerY, double centerZ) {
        this.chart = chart;
        this.entityManager = entityManager;
        this.player = player;
        this.speed = speed;
        this.hitboxScale = hitboxScale;
        this.bpm = chart.getMetadata().getBpm();  // 从谱面元数据获取BPM
        this.world = world;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;

        // 分离普通音符和 EXECUTION 音符
        this.unspawnedNotes = new ArrayList<>();
        this.unexecutedActions = new ArrayList<>();

        for (Note note : chart.getNotes()) {
            if (note.getType() == NoteType.EXECUTION) {
                unexecutedActions.add(note);
            } else {
                unspawnedNotes.add(note);
            }
        }

        org.cubeRhythm.Main.instance.getLogger().info("NoteSpawner 初始化:");
        org.cubeRhythm.Main.instance.getLogger().info("  普通音符: " + unspawnedNotes.size());
        org.cubeRhythm.Main.instance.getLogger().info("  EXECUTION音符: " + unexecutedActions.size());
    }

    /**
     * 更新生成器 - 在适当的时候生成音符和执行动作
     * @param currentTime 当前游戏时间（秒）
     */
    public void update(double currentTime) {
        // 处理 EXECUTION 音符
        List<Note> toExecute = new ArrayList<>();
        for (Note note : unexecutedActions) {
            if (currentTime >= note.getTime()) {
                toExecute.add(note);
            }
        }

        for (Note note : toExecute) {
            ExecutionHandler.executeActions(player, note);
            unexecutedActions.remove(note);
        }

        // 处理普通音符生成
        if (entityManager.getEntityCount() >= MAX_ENTITIES) {
            return;
        }

        List<Note> toSpawn = new ArrayList<>();

        for (Note note : unspawnedNotes) {
            double distance = calculateDistance(note.getTime(), currentTime);

            if (distance < SPAWN_DISTANCE) {
                toSpawn.add(note);

                if (entityManager.getEntityCount() + toSpawn.size() >= MAX_ENTITIES) {
                    break;
                }
            }
        }

        for (Note note : toSpawn) {
            spawnNote(note, currentTime);
            unspawnedNotes.remove(note);
        }
    }

    private void spawnNote(Note note, double currentTime) {
        NoteEntity entity = new NoteEntity(note);
        entity.setSpawnTime(System.currentTimeMillis());

        double distance = calculateDistance(note.getTime(), currentTime);
        NoteRenderer.renderNote(entity, world, centerX, centerY, centerZ, speed, distance, hitboxScale, bpm);

        entityManager.registerEntity(entity);
    }

    private double calculateDistance(double noteTime, double currentTime) {
        return speed * 20 * (noteTime - currentTime) + 4;
    }

    public boolean hasUnspawnedNotes() {
        return !unspawnedNotes.isEmpty() || !unexecutedActions.isEmpty();
    }

    public int getUnspawnedCount() {
        return unspawnedNotes.size() + unexecutedActions.size();
    }
}
