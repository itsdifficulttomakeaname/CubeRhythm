package org.cubeRhythm.editor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.cubeRhythm.note.NoteType;

import java.util.ArrayList;
import java.util.List;

/**
 * 编辑器音符渲染器 - 在编辑器中显示音符
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorNoteRenderer {
    private static final double SPAWN_DISTANCE = 50.0;  // 音符生成距离（与游戏一致）
    private static final double JUDGMENT_DISTANCE = 4.0;  // 判定线距离
    private static final double POST_JUDGMENT_TIME = 0.2;  // 判定后显示时间（200ms）

    /**
     * 渲染编辑器中的可见音符
     */
    public static void renderVisibleNotes(EditorSession session, Player player) {
        // 清理旧的显示实体
        clearNoteDisplays(session);

        double currentTime = session.getCurrentTime();
        double speed = session.getSpeed();
        World world = player.getWorld();

        // 使用配置的游戏位置作为中心
        double[] gameLocation = org.cubeRhythm.Main.instance.getConfigManager().getGameLocation();
        double centerX = gameLocation[0];
        double centerY = gameLocation[1];
        double centerZ = gameLocation[2];

        // 计算音符可见时间范围
        // 音符出现时间：distance = 50 = speed * 20 * (noteTime - currentTime) + 4
        // 解得：noteTime - currentTime = (50 - 4) / (speed * 20) = 46 / (speed * 20) = 2.3 / speed
        double spawnTimeBefore = (SPAWN_DISTANCE - JUDGMENT_DISTANCE) / (speed * 20.0);

        org.cubeRhythm.Main.instance.getLogger().info(
                String.format("编辑器渲染: 当前时间=%.2fs, 速度=%.2f, 生成提前时间=%.2fs, 中心位置=(%.1f, %.1f, %.1f), 总音符数=%d",
                        currentTime, speed, spawnTimeBefore, centerX, centerY, centerZ, session.getNotes().size())
        );

        int renderedCount = 0;

        // 渲染当前时间范围内的音符
        for (EditorNote note : session.getNotes().values()) {
            double timeDiff = note.getTime() - currentTime;

            // 只渲染从音符出现到判定后200ms的音符
            // timeDiff <= spawnTimeBefore: 音符已经出现或即将出现
            // timeDiff >= -POST_JUDGMENT_TIME: 音符还未超过判定后200ms
            if (timeDiff <= spawnTimeBefore && timeDiff >= -POST_JUDGMENT_TIME) {
                renderNote(note, world, speed, timeDiff, centerX, centerY, centerZ);
                renderedCount++;
            }
        }

        org.cubeRhythm.Main.instance.getLogger().info("已渲染 " + renderedCount + " 个音符");
    }

    /**
     * 渲染单个音符
     */
    private static void renderNote(EditorNote note, World world, double speed, double timeDiff,
                                    double centerX, double centerY, double centerZ) {
        // 计算音符距离
        double distance = speed * 20 * timeDiff + 4;

        org.cubeRhythm.Main.instance.getLogger().info(
                String.format("渲染音符: type=%s, time=%.2fs, timeDiff=%.2fs, distance=%.2f",
                        note.getType(), note.getTime(), timeDiff, distance)
        );

        // 根据音符类型选择材质
        Material material = getMaterialForNoteType(note.getType());

        // 根据音符类型渲染
        if (note.getType() == NoteType.DOUBLE && note.getPositions() != null && note.getPositions().size() >= 2) {
            // DOUBLE 音符渲染两个位置
            renderDoubleNote(note, world, material, distance, centerX, centerY, centerZ);
        } else if (note.getType() == NoteType.FLICK) {
            // FLICK 音符在中心
            renderFlickNote(note, world, material, distance, centerX, centerY, centerZ);
        } else if (note.getType() == NoteType.EXECUTION) {
            // EXECUTION 音符不渲染
            return;
        } else if (note.getPosition() != null) {
            // 普通音符
            renderSingleNote(note, world, material, distance, centerX, centerY, centerZ);
        }
    }

    /**
     * 渲染单个位置的音符
     */
    private static void renderSingleNote(EditorNote note, World world, Material material, double distance,
                                          double centerX, double centerY, double centerZ) {
        // 根据面分类处理：W/S面不变，A/D面右移1格
        double faceOffsetX = (note.getFace() == Face.A || note.getFace() == Face.D) ? 1.0 : 0.0;

        Location location = CoordinateSystem.transformCoordinates(
                world,
                note.getFace(),
                note.getPosition().getX() - 1.0 + 0.5 + faceOffsetX,
                note.getPosition().getY() + 1.6 - 0.5,
                distance,
                centerX,
                centerY,
                centerZ
        );

        // 创建 BlockDisplay
        float scaleZ = note.getType() == NoteType.HOLD ? 3.0f : 1.0f;
        BlockDisplay display = DisplayEntityFactory.createBlockDisplay(
                world, location, material, 1.0f, 1.0f, scaleZ, 100
        );

        // 设置发光效果
        if (note.isGlowing()) {
            display.setGlowing(true);
        }

        // 保存到音符实体
        note.setBlockDisplay(display);
    }

    /**
     * 渲染 DOUBLE 音符
     */
    private static void renderDoubleNote(EditorNote note, World world, Material material, double distance,
                                          double centerX, double centerY, double centerZ) {
        double faceOffsetX = (note.getFace() == Face.A || note.getFace() == Face.D) ? 1.0 : 0.0;

        // 渲染第一个位置
        NotePosition firstPos = note.getPositions().get(0);
        Location location1 = CoordinateSystem.transformCoordinates(
                world,
                note.getFace(),
                firstPos.getX() - 1.0 + 0.5 + faceOffsetX,
                firstPos.getY() + 1.6 - 0.5,
                distance,
                centerX,
                centerY,
                centerZ
        );

        BlockDisplay display1 = DisplayEntityFactory.createBlockDisplay(
                world, location1, material, 1.0f, 1.0f, 1.0f, 100
        );
        note.setBlockDisplay(display1);

        // 渲染第二个位置
        if (note.getPositions().size() > 1) {
            NotePosition secondPos = note.getPositions().get(1);
            Location location2 = CoordinateSystem.transformCoordinates(
                    world,
                    note.getFace(),
                    secondPos.getX() - 1.0 + 0.5 + faceOffsetX,
                    secondPos.getY() + 1.6 - 0.5,
                    distance,
                    centerX,
                    centerY,
                    centerZ
            );

            BlockDisplay display2 = DisplayEntityFactory.createBlockDisplay(
                    world, location2, material, 1.0f, 1.0f, 1.0f, 100
            );
            note.getAdditionalDisplays().add(display2);
        }

        // 设置发光效果
        if (note.isGlowing()) {
            display1.setGlowing(true);
            for (BlockDisplay display : note.getAdditionalDisplays()) {
                display.setGlowing(true);
            }
        }
    }

    /**
     * 渲染 FLICK 音符（在中心）
     */
    private static void renderFlickNote(EditorNote note, World world, Material material, double distance,
                                         double centerX, double centerY, double centerZ) {
        Location location = CoordinateSystem.transformCoordinates(
                world,
                note.getFace(),
                0, 0,  // 中心位置
                distance,
                centerX,
                centerY,
                centerZ
        );

        BlockDisplay display = DisplayEntityFactory.createBlockDisplay(
                world, location, material, 5.0f, 5.0f, 1.0f, 100
        );

        note.setBlockDisplay(display);
    }

    /**
     * 清理所有音符显示实体
     */
    public static void clearNoteDisplays(EditorSession session) {
        for (EditorNote note : session.getNotes().values()) {
            note.cleanup();
        }
    }

    /**
     * 获取音符类型对应的材质
     */
    private static Material getMaterialForNoteType(NoteType type) {
        return switch (type) {
            case TAP -> Material.LIGHT_BLUE_CONCRETE;
            case HOLD -> Material.WHITE_CONCRETE;
            case DRAG -> Material.YELLOW_CONCRETE;
            case FLICK -> Material.MAGENTA_CONCRETE;
            case DOUBLE -> Material.ORANGE_CONCRETE;
            case EXECUTION -> Material.BARRIER;
        };
    }
}
