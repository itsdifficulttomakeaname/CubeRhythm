package org.cubeRhythm.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cubeRhythm.Main;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.chart.ChartLoader;
import org.cubeRhythm.chart.ChartMetadata;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.Note;
import org.cubeRhythm.note.NoteType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 编辑器文件工具 - 处理谱面的保存和加载
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorFileUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CHARTS_DIR = new File(Main.instance.getDataFolder(), "charts");

    /**
     * 检查谱面ID是否已存在
     */
    public static boolean chartExists(String chartId) {
        File file = new File(CHARTS_DIR, chartId + ".json");
        return file.exists();
    }

    /**
     * 生成唯一的谱面ID
     */
    public static String generateUniqueChartId(String baseName) {
        String chartId = baseName;
        int counter = 1;
        while (chartExists(chartId)) {
            chartId = baseName + "_" + counter;
            counter++;
        }
        return chartId;
    }

    /**
     * 保存编辑器会话到文件
     */
    public static boolean saveSession(EditorSession session) {
        if (session.getChartId() == null || session.getChartId().isEmpty()) {
            Main.instance.getLogger().warning("无法保存：未设置谱面ID");
            return false;
        }

        try {
            // 确保 charts 文件夹存在
            if (!CHARTS_DIR.exists()) {
                CHARTS_DIR.mkdirs();
            }

            File file = new File(CHARTS_DIR, session.getChartId() + ".json");
            session.setChartFile(file);

            // 构建 JSON 结构
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("version", "1.0.0");

            // 元数据
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", session.getChartId());
            metadata.put("title", session.getChartId());  // 默认使用ID作为标题
            metadata.put("artist", "Unknown");
            metadata.put("charter", session.getPlayer().getName());
            metadata.put("bpm", session.getBpm());
            metadata.put("offset", (int) (session.getPreTime() * 1000));
            metadata.put("duration", calculateDuration(session));

            // 难度信息
            Map<String, Object> difficulty = new LinkedHashMap<>();
            difficulty.put("name", "Normal");
            difficulty.put("level", 1);
            difficulty.put("color", "#FFFFFF");
            metadata.put("difficulty", difficulty);

            root.put("metadata", metadata);

            // 音符列表
            List<Map<String, Object>> notesList = new ArrayList<>();
            for (EditorNote note : session.getSortedNotes()) {
                Map<String, Object> noteData = convertNoteToJson(note);
                if (noteData != null) {
                    notesList.add(noteData);
                }
            }
            root.put("notes", notesList);

            // 写入文件
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            }

            Main.instance.getLogger().info("谱面已保存: " + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            Main.instance.getLogger().severe("保存谱面失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从文件加载谱面到编辑器
     */
    public static boolean loadChartToEditor(EditorSession session, String chartId) {
        try {
            // 构建谱面文件路径
            File chartFile = new File(CHARTS_DIR, chartId + ".json");
            if (!chartFile.exists()) {
                Main.instance.getLogger().warning("谱面文件不存在: " + chartFile.getAbsolutePath());
                return false;
            }

            // 使用 ChartLoader 加载谱面
            Chart chart = ChartLoader.loadChart(chartFile);

            // 清空当前编辑器
            session.clearAllNotes();

            // 设置元数据
            ChartMetadata meta = chart.getMetadata();
            session.setChartId(chartId);
            session.setBpm(meta.getBpm());
            session.setPreTime(meta.getOffset() / 1000.0);

            // 转换音符
            for (Note note : chart.getNotes()) {
                EditorNote editorNote = convertNoteToEditor(note);
                if (editorNote != null) {
                    session.addNote(editorNote);
                }
            }

            Main.instance.getLogger().info("谱面已加载到编辑器: " + chartId + " (" + session.getNotes().size() + " 个音符)");

            // 渲染可见音符
            EditorNoteRenderer.renderVisibleNotes(session, session.getPlayer());

            return true;

        } catch (IOException e) {
            Main.instance.getLogger().severe("加载谱面文件失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Main.instance.getLogger().severe("加载谱面到编辑器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将 EditorNote 转换为 JSON 格式
     */
    private static Map<String, Object> convertNoteToJson(EditorNote note) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 基本信息
        data.put("time", Math.round(note.getTime() * 100.0) / 100.0);  // 保留2位小数

        // 根据类型处理
        switch (note.getType()) {
            case TAP:
                data.put("type", "tap");
                data.put("face", note.getFace().name().toLowerCase());
                if (note.getPosition() != null) {
                    data.put("position", positionToMap(note.getPosition()));
                } else {
                    Main.instance.getLogger().warning("TAP音符缺少position: time=" + note.getTime());
                    return null;
                }
                data.put("glowing", note.isGlowing());
                data.put("tag", note.getTag() != null ? note.getTag() : "");
                break;

            case HOLD:
                data.put("type", "hold");
                data.put("face", note.getFace().name().toLowerCase());
                if (note.getPosition() != null) {
                    data.put("position", positionToMap(note.getPosition()));
                } else {
                    Main.instance.getLogger().warning("HOLD音符缺少position: time=" + note.getTime());
                    return null;
                }
                data.put("glowing", note.isGlowing());
                data.put("tag", note.getTag() != null ? note.getTag() : "");
                break;

            case DRAG:
                data.put("type", "drag");
                data.put("face", note.getFace().name().toLowerCase());
                if (note.getPosition() != null) {
                    data.put("position", positionToMap(note.getPosition()));
                } else {
                    Main.instance.getLogger().warning("DRAG音符缺少position: time=" + note.getTime());
                    return null;
                }
                data.put("glowing", note.isGlowing());
                data.put("tag", note.getTag() != null ? note.getTag() : "");
                break;

            case FLICK:
                data.put("type", "flick");
                data.put("face", note.getFace().name().toLowerCase());
                data.put("turn", note.getTurn() != null ? note.getTurn() : "left");
                data.put("glowing", note.isGlowing());
                data.put("tag", note.getTag() != null ? note.getTag() : "");
                break;

            case DOUBLE:
                data.put("type", "double");
                data.put("face", note.getFace().name().toLowerCase());
                if (note.getPositions() != null && !note.getPositions().isEmpty()) {
                    List<Map<String, Object>> positions = new ArrayList<>();
                    for (NotePosition pos : note.getPositions()) {
                        if (pos != null) {
                            positions.add(positionToMap(pos));
                        }
                    }
                    data.put("positions", positions);
                } else {
                    Main.instance.getLogger().warning("DOUBLE音符缺少positions: time=" + note.getTime());
                    return null;
                }
                data.put("glowing", note.isGlowing());
                data.put("tag", note.getTag() != null ? note.getTag() : "");
                break;

            case EXECUTION:
                data.put("type", "execution");
                data.put("section", note.getSection() != null ? note.getSection() : "");
                break;

            default:
                return null;
        }

        return data;
    }

    /**
     * 将 Note 转换为 EditorNote
     */
    private static EditorNote convertNoteToEditor(Note note) {
        if (note == null) {
            Main.instance.getLogger().warning("尝试转换null的Note");
            return null;
        }

        EditorNote editorNote = new EditorNote();
        editorNote.setType(note.getType());
        editorNote.setTime(note.getTime());
        editorNote.setFace(note.getFace());
        editorNote.setPosition(note.getPosition());
        editorNote.setPositions(note.getPositions() != null ? note.getPositions() : new ArrayList<>());
        editorNote.setGlowing(note.isGlowing());
        editorNote.setTag(note.getTag() != null ? note.getTag() : "");
        editorNote.setTurn(note.getTurn());
        return editorNote;
    }

    /**
     * 将 NotePosition 转换为 Map
     */
    private static Map<String, Object> positionToMap(NotePosition pos) {
        if (pos == null) {
            Main.instance.getLogger().warning("尝试转换null的NotePosition");
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("x", 0);
            map.put("y", 0);
            return map;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", pos.getX());
        map.put("y", pos.getY());
        return map;
    }

    /**
     * 计算谱面时长
     */
    private static int calculateDuration(EditorSession session) {
        if (session.getNotes().isEmpty()) {
            return 60;  // 默认60秒
        }

        double maxTime = 0;
        for (EditorNote note : session.getNotes().values()) {
            if (note.getTime() > maxTime) {
                maxTime = note.getTime();
            }
        }

        return (int) Math.ceil(maxTime) + 5;  // 最后一个音符时间 + 5秒
    }
}
