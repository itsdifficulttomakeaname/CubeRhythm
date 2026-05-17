package org.cubeRhythm.chart;

import cn.jason31416.planetlib.util.MapTree;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.Note;
import org.cubeRhythm.note.NoteType;
import org.cubeRhythm.note.event.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

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

                    // 多标签支持：兼容 "tag": "xxx" 和 "tags": ["a", "b"]
                    Object tagsObj = noteTree.get("tags");
                    if (tagsObj instanceof List<?> tagsList) {
                        Set<String> tagSet = new HashSet<>();
                        for (Object t : tagsList) {
                            if (t instanceof String s && !s.isEmpty()) tagSet.add(s);
                        }
                        if (!tagSet.isEmpty()) note.setTags(tagSet);
                    } else {
                        // 兼容旧格式 "tag": "xxx"
                        String singleTag = noteTree.getString("tag", "");
                        if (!singleTag.isEmpty()) {
                            note.setTag(singleTag);
                        }
                    }

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

                    // Handle inline events (新事件系统)
                    if (noteTree.get("events") != null) {
                        Object eventsObj = noteTree.get("events");
                        if (eventsObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> eventsMap = (Map<String, Object>) eventsObj;
                            EventTrack track = parseEventTrack(eventsMap, note.getTime(), true);
                            if (!track.isEmpty()) {
                                note.setEvents(track);
                            }
                        }
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

        // 解析 groupEvents（新事件系统）
        Object groupEventsObj = tree.get("groupEvents");
        if (groupEventsObj instanceof List<?> groupEventsList) {
            List<GroupEvent> groupEvents = new ArrayList<>();
            for (Object geObj : groupEventsList) {
                if (geObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> geMap = (Map<String, Object>) geObj;
                    GroupEvent ge = parseGroupEvent(geMap);
                    if (ge != null) {
                        groupEvents.add(ge);
                    }
                }
            }
            chart.setGroupEvents(groupEvents);
            if (!groupEvents.isEmpty()) {
                org.cubeRhythm.Main.instance.getLogger().info("加载 " + groupEvents.size() + " 个群组事件");
            }
        }

        return chart;
    }

    // ── 事件系统解析辅助方法 ──────────────────────────────────────────

    /**
     * 解析群组事件
     */
    private static GroupEvent parseGroupEvent(Map<String, Object> map) {
        GroupEvent ge = new GroupEvent();

        // 解析 selector
        Object selectorObj = map.get("selector");
        if (selectorObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> selectorMap = (Map<String, Object>) selectorObj;
            ge.setSelector(parseSelector(selectorMap));
        } else {
            // 无选择器则命中所有音符
            ge.setSelector(new Selector());
        }

        // 解析 events
        Object eventsObj = map.get("events");
        if (eventsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventsMap = (Map<String, Object>) eventsObj;
            ge.setEvents(parseEventTrack(eventsMap, 0, false));
        } else {
            return null; // 无事件数据则跳过
        }

        return ge;
    }

    /**
     * 解析选择器
     */
    private static Selector parseSelector(Map<String, Object> map) {
        Selector selector = new Selector();

        // face
        Object faceObj = map.get("face");
        if (faceObj != null) {
            Set<Face> faces = new HashSet<>();
            if (faceObj instanceof String s) {
                Face f = Face.fromString(s);
                if (f != null) faces.add(f);
            } else if (faceObj instanceof List<?> faceList) {
                for (Object f : faceList) {
                    if (f instanceof String s) {
                        Face face = Face.fromString(s);
                        if (face != null) faces.add(face);
                    }
                }
            }
            if (!faces.isEmpty()) selector.setFaces(faces);
        }

        // type
        Object typeObj = map.get("type");
        if (typeObj != null) {
            Set<NoteType> types = new HashSet<>();
            if (typeObj instanceof String s) {
                try { types.add(NoteType.valueOf(s.toUpperCase())); } catch (Exception ignored) {}
            } else if (typeObj instanceof List<?> typeList) {
                for (Object t : typeList) {
                    if (t instanceof String s) {
                        try { types.add(NoteType.valueOf(s.toUpperCase())); } catch (Exception ignored) {}
                    }
                }
            }
            if (!types.isEmpty()) selector.setTypes(types);
        }

        // tag
        Object tagObj = map.get("tag");
        if (tagObj != null) {
            Set<String> tags = new HashSet<>();
            if (tagObj instanceof String s) {
                tags.add(s);
            } else if (tagObj instanceof List<?> tagList) {
                for (Object t : tagList) {
                    if (t instanceof String s) tags.add(s);
                }
            }
            if (!tags.isEmpty()) selector.setTags(tags);
        }

        // timeRange
        Object timeRangeObj = map.get("timeRange");
        if (timeRangeObj instanceof List<?> rangeList && rangeList.size() == 2) {
            try {
                double start = ((Number) rangeList.get(0)).doubleValue();
                double end = ((Number) rangeList.get(1)).doubleValue();
                selector.setTimeRange(new double[]{start, end});
            } catch (Exception ignored) {}
        }

        return selector;
    }

    /**
     * 解析事件轨道
     * @param eventsMap 通道名 → 关键帧数组 的 Map
     * @param noteTime 音符时间（用于 rtime → 绝对秒转换）
     * @param isRelative 是否使用 rtime（note.events 为 true，groupEvents 为 false）
     */
    private static EventTrack parseEventTrack(Map<String, Object> eventsMap, double noteTime, boolean isRelative) {
        EventTrack track = new EventTrack();

        for (Map.Entry<String, Object> entry : eventsMap.entrySet()) {
            String channelName = entry.getKey();
            Object kfsObj = entry.getValue();

            if (!(kfsObj instanceof List<?> kfsList)) continue;

            // "scale" 语法糖：同时设置 SCALE_X/Y/Z
            if (channelName.equalsIgnoreCase("scale")) {
                List<Keyframe> keyframes = parseKeyframes(kfsList, noteTime, isRelative);
                if (!keyframes.isEmpty()) {
                    track.setChannel(Channel.SCALE_X, keyframes);
                    track.setChannel(Channel.SCALE_Y, new ArrayList<>(keyframes));
                    track.setChannel(Channel.SCALE_Z, new ArrayList<>(keyframes));
                }
                continue;
            }

            Channel channel = Channel.fromString(channelName);
            if (channel == null) continue;

            List<Keyframe> keyframes = parseKeyframes(kfsList, noteTime, isRelative);
            if (!keyframes.isEmpty()) {
                track.setChannel(channel, keyframes);
            }
        }

        return track;
    }

    /**
     * 解析关键帧数组
     */
    private static List<Keyframe> parseKeyframes(List<?> kfsList, double noteTime, boolean isRelative) {
        List<Keyframe> keyframes = new ArrayList<>();

        for (Object kfObj : kfsList) {
            if (!(kfObj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> kfMap = (Map<String, Object>) kfObj;

            Keyframe kf = new Keyframe();

            // 时间解析
            if (kfMap.containsKey("rtime")) {
                double rtime = ((Number) kfMap.get("rtime")).doubleValue();
                kf.setTime(noteTime + rtime);
            } else if (kfMap.containsKey("time")) {
                kf.setTime(((Number) kfMap.get("time")).doubleValue());
            } else {
                continue; // 无时间字段则跳过
            }

            // value
            if (kfMap.containsKey("value")) {
                Object valObj = kfMap.get("value");
                if (valObj instanceof Number n) {
                    kf.setValue(n.doubleValue());
                } else if (valObj instanceof String s) {
                    // 材质通道：value 存材质名的 hash，easing 字段存材质名
                    kf.setValue(s.hashCode());
                    kf.setEasing(s);
                    keyframes.add(kf);
                    continue;
                }
            }

            // easing
            if (kfMap.containsKey("easing")) {
                kf.setEasing((String) kfMap.get("easing"));
            }

            keyframes.add(kf);
        }

        // 确保按时间排序
        keyframes.sort(Comparator.comparingDouble(Keyframe::getTime));
        return keyframes;
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
