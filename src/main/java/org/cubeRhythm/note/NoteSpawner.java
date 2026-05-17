package org.cubeRhythm.note;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.entity.EntityManager;
import org.cubeRhythm.note.event.EventTrack;
import org.cubeRhythm.note.event.GroupEvent;
import org.cubeRhythm.note.event.Channel;
import org.cubeRhythm.note.event.Keyframe;
import org.cubeRhythm.note.event.TrackEvaluator;

import java.util.ArrayList;
import java.util.List;

public class NoteSpawner {
    private final Chart chart;
    private final EntityManager entityManager;
    private final List<Note> unspawnedNotes;
    private final List<Note> unexecutedActions;
    private final Player player;
    private final double speed;
    private final float hitboxScale;
    private final double bpm;
    private final World world;
    private final double centerX;
    private final double centerY;
    private final double centerZ;

    private final List<GroupEvent> groupEvents;

    private static final int MAX_ENTITIES = 100;
    private static final double SPAWN_DISTANCE = 50.0;

    public NoteSpawner(Chart chart, EntityManager entityManager, Player player, double speed, float hitboxScale,
                       World world, double centerX, double centerY, double centerZ) {
        this.chart = chart;
        this.entityManager = entityManager;
        this.player = player;
        this.speed = speed;
        this.hitboxScale = hitboxScale;
        this.bpm = chart.getMetadata().getBpm();
        this.world = world;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.groupEvents = chart.getGroupEvents() != null ? chart.getGroupEvents() : List.of();

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
        org.cubeRhythm.Main.instance.getLogger().info("  群组事件: " + groupEvents.size());
    }

    public void update(double currentTime) {
        List<Note> toExecute = new ArrayList<>();
        for (Note note : unexecutedActions) {
            if (currentTime >= note.getTime()) {
                toExecute.add(note);
            }
        }

        for (Note note : toExecute) {
            ExecutionHandler.executeActions(player, note, entityManager);
            unexecutedActions.remove(note);
        }

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

        double noteTime = note.getTime();

        // 事件预筛：匹配 groupEvents + inline events，应用时间窗口过滤
        List<EventTrack> matchedTracks = new ArrayList<>();
        for (GroupEvent ge : groupEvents) {
            if (ge.getSelector().matches(note)) {
                EventTrack track = ge.getEvents();
                if (track.getEndTime() <= noteTime) {
                    matchedTracks.add(track);
                }
            }
        }
        if (note.getEvents() != null && !note.getEvents().isEmpty()) {
            EventTrack inlineTrack = note.getEvents();
            if (inlineTrack.getEndTime() <= noteTime) {
                matchedTracks.add(inlineTrack);
            }
        }

        if (!matchedTracks.isEmpty()) {
            entity.setMatchedTracks(matchedTracks);

            // 落点预计算：事件在 noteTime 时刻的 x/y 偏移
            double preX = 0, preY = 0;
            for (EventTrack track : matchedTracks) {
                List<Keyframe> xKfs = track.getChannel(Channel.X);
                if (xKfs != null && !xKfs.isEmpty()) {
                    preX += TrackEvaluator.sample(xKfs, noteTime, null, 0, Channel.X);
                }
                List<Keyframe> yKfs = track.getChannel(Channel.Y);
                if (yKfs != null && !yKfs.isEmpty()) {
                    preY += TrackEvaluator.sample(yKfs, noteTime, null, 0, Channel.Y);
                }
            }
            entity.setCursorOffsetX(preX);
            entity.setCursorOffsetY(preY);
        }

        double distance = calculateDistance(note.getTime(), currentTime);
        NoteRenderer.renderNote(entity, world, centerX, centerY, centerZ, speed, distance, hitboxScale, bpm);

        entityManager.registerEntity(entity);
    }

    private double calculateDistance(double noteTime, double currentTime) {
        return speed * 20 * (noteTime + 1.0 - currentTime) + 4;
    }

    public boolean hasUnspawnedNotes() {
        return !unspawnedNotes.isEmpty() || !unexecutedActions.isEmpty();
    }

    public int getUnspawnedCount() {
        return unspawnedNotes.size() + unexecutedActions.size();
    }
}
