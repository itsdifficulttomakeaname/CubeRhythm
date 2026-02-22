package org.cubeRhythm.chart;

import cn.jason31416.planetlib.util.MapTree;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.Note;
import org.cubeRhythm.note.NoteType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartLoader {

    public static Chart loadChart(File file) throws IOException {
        String jsonContent = Files.readString(file.toPath());
        MapTree tree = MapTree.fromJson(jsonContent);

        org.cubeRhythm.Main.instance.getLogger().info("开始加载谱面: " + file.getName());

        Chart chart = new Chart();
        chart.setVersion(tree.getString("version"));

        // Load metadata
        MapTree metadataTree = tree.getSection("metadata");
        ChartMetadata metadata = new ChartMetadata();
        metadata.setId(metadataTree.getString("id"));
        metadata.setTitle(metadataTree.getString("title"));
        metadata.setArtist(metadataTree.getString("artist"));
        metadata.setCharter(metadataTree.getString("charter"));
        metadata.setAudio(metadataTree.getString("audio"));
        metadata.setDuration(metadataTree.getInt("duration"));
        metadata.setOffset(metadataTree.getInt("offset"));
        metadata.setBpm(metadataTree.getInt("bpm"));

        // Load difficulty
        MapTree difficultyTree = metadataTree.getSection("difficulty");
        ChartMetadata.Difficulty difficulty = new ChartMetadata.Difficulty();
        difficulty.setName(difficultyTree.getString("name"));
        difficulty.setLevel(difficultyTree.getInt("level"));
        difficulty.setColor(difficultyTree.getString("color"));
        metadata.setDifficulty(difficulty);

        chart.setMetadata(metadata);

        org.cubeRhythm.Main.instance.getLogger().info("元数据加载完成: " + metadata.getTitle());

        // Load notes
        List<Note> notes = new ArrayList<>();

        // Get notes as a List (JSON arrays are stored as List in MapTree.data)
        Object notesObj = tree.get("notes");
        if (notesObj instanceof List<?> notesList) {
            org.cubeRhythm.Main.instance.getLogger().info("开始解析 " + notesList.size() + " 个音符");

            for (Object noteObj : notesList) {
                if (!(noteObj instanceof Map)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> noteMap = (Map<String, Object>) noteObj;
                MapTree noteTree = new MapTree(noteMap);

                try {
                    Note note = new Note();

                    String typeStr = noteTree.getString("type").toUpperCase();
                    note.setType(NoteType.valueOf(typeStr));
                    note.setTime(noteTree.getDouble("time"));
                    note.setGlowing(noteTree.getBoolean("glowing", false));
                    note.setTag(noteTree.getString("tag", ""));

                    // Handle face (not present for execution notes)
                    if (noteTree.get("face") != null) {
                        note.setFace(Face.fromString(noteTree.getString("face")));
                    }

                    // Handle position based on note type
                    if (note.getType() == NoteType.DOUBLE) {
                        // For double notes, load all positions
                        Object positionsObj = noteTree.get("positions");
                        if (positionsObj instanceof List<?> positionsList) {
                            List<NotePosition> notePositions = new ArrayList<>();
                            for (Object posObj : positionsList) {
                                if (posObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> posMap = (Map<String, Object>) posObj;
                                    MapTree posTree = new MapTree(posMap);
                                    notePositions.add(new NotePosition(
                                        posTree.getDouble("x"),
                                        posTree.getDouble("y")
                                    ));
                                }
                            }
                            note.setPositions(notePositions);
                            // Also set the first position as the main position for compatibility
                            if (!notePositions.isEmpty()) {
                                note.setPosition(notePositions.get(0));
                            }
                        }
                    } else if (note.getType() == NoteType.FLICK) {
                        // Flick notes have turn direction instead of position
                        // Set default position at center (0, 0) for rendering
                        note.setTurn(noteTree.getString("turn"));
                        note.setPosition(new NotePosition(0, 0));
                    } else if (note.getType() != NoteType.EXECUTION) {
                        // Regular notes (tap, hold, drag) have single position
                        if (noteTree.get("position") != null) {
                            MapTree posTree = noteTree.getSection("position");
                            note.setPosition(new NotePosition(
                                posTree.getDouble("x"),
                                posTree.getDouble("y")
                            ));
                        }
                    }

                    // Handle actions for execution notes
                    if (note.getType() == NoteType.EXECUTION && noteTree.get("actions") != null) {
                        Object actionsObj = noteTree.get("actions");
                        List<Map<String, Object>> actionsList = new ArrayList<>();

                        if (actionsObj instanceof List<?> actionsListRaw) {
                            for (Object actionObj : actionsListRaw) {
                                if (actionObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> actionMap = (Map<String, Object>) actionObj;
                                    actionsList.add(actionMap);
                                }
                            }
                        }

                        note.setActions(actionsList);
                    }

                    notes.add(note);
                } catch (Exception e) {
                    org.cubeRhythm.Main.instance.getLogger().warning("解析音符失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            org.cubeRhythm.Main.instance.getLogger().warning("notes 不是数组类型: " + (notesObj != null ? notesObj.getClass().getName() : "null"));
        }

        org.cubeRhythm.Main.instance.getLogger().info("成功加载 " + notes.size() + " 个音符");

        chart.setNotes(notes);
        return chart;
    }

    /**
     * 手动将 MapTree 转换为 Map<String, Object>
     * @param tree MapTree 对象
     * @return Map<String, Object>
     */
    private static Map<String, Object> mapTreeToMap(MapTree tree) {
        Map<String, Object> map = new HashMap<>();

        for (String key : tree.getKeys()) {
            Object value = tree.get(key);

            // 如果值是 Map，递归转换（处理嵌套结构）
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                map.put(key, nestedMap);
            } else {
                // 直接添加具体值
                map.put(key, value);
            }
        }

        return map;
    }
}
