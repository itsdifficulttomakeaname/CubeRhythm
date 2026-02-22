package org.cubeRhythm.editor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.cubeRhythm.note.NoteType;

/**
 * 编辑器预览光标 - 实时显示将要放置的音符
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorPreviewCursor {
    private static final double JUDGMENT_DISTANCE = 4.0;

    /**
     * 更新预览光标位置和外观
     */
    public static void updatePreviewCursor(EditorSession session, Player player) {
        // 清理旧的预览光标
        session.cleanupPreviewCursor();

        // 自动检测玩家正在看向哪个判定面
        Face targetFace = EditorFaceDetector.detectFacingFace(player);

        if (targetFace == null) {
            // 没有指向任何判定面，不显示预览
            return;
        }

        // 更新会话的当前面（用于放置音符）
        session.setCurrentFace(targetFace);

        // 计算光标位置 (使用与 Skript 相同的逻辑)
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection();

        double playerZ = playerLocation.getZ();
        double rate = Math.abs(JUDGMENT_DISTANCE - playerZ) / direction.getZ();

        double targetX = direction.getX() * rate + playerLocation.getX();
        double targetY;

        // 如果按住Shift，使用1.4偏移并对齐到网格；否则使用1.6偏移
        if (player.isSneaking()) {
            targetY = direction.getY() * rate + playerLocation.getY() + 1.4;
            targetX = Math.round(targetX * 2) / 2.0;
            targetY = Math.round(targetY * 2) / 2.0;
        } else {
            targetY = direction.getY() * rate + playerLocation.getY() + 1.6;
        }

        // 检查坐标是否在有效范围内
        if (targetX < -3 || targetX > 4 || targetY < -3 || targetY > 4) {
            return;  // 超出范围，不显示预览
        }

        // 创建预览光标
        World world = player.getWorld();
        NoteType type = session.getCurrentNoteType();

        // 使用配置的游戏位置作为中心
        double[] gameLocation = org.cubeRhythm.Main.instance.getConfigManager().getGameLocation();
        double centerX = gameLocation[0];
        double centerY = gameLocation[1];
        double centerZ = gameLocation[2];

        // 根据面分类处理：W/S面不变，A/D面右移1格
        double faceOffsetX = (targetFace == Face.A || targetFace == Face.D) ? 1.0 : 0.0;

        Location location = CoordinateSystem.transformCoordinates(
            world,
            targetFace,
            targetX - 1.0 + 0.5 + faceOffsetX,
            targetY + 1.6 - 0.5,
            JUDGMENT_DISTANCE,
            centerX,
            centerY,
            centerZ
        );

        // 根据音符类型选择材质
        Material material = getMaterialForNoteType(type);

        // 创建预览实体
        BlockDisplay preview;
        if (type == NoteType.FLICK) {
            // FLICK音符在中心，5x5大小
            Location centerLoc = CoordinateSystem.transformCoordinates(
                world, targetFace, 0, 0, JUDGMENT_DISTANCE,
                centerX,
                centerY,
                centerZ
            );
            preview = DisplayEntityFactory.createBlockDisplay(
                world, centerLoc, material, 5.0f, 5.0f, 1.0f, 0
            );
        } else {
            // 普通音符
            float scaleZ = type == NoteType.HOLD ? 3.0f : 1.0f;
            preview = DisplayEntityFactory.createBlockDisplay(
                world, location, material, 1.0f, 1.0f, scaleZ, 0
            );
        }

        // 设置发光效果和颜色（根据自动检测的面）
        preview.setGlowing(true);
        Color glowColor = getGlowColorForFace(targetFace);
        preview.setGlowColorOverride(glowColor);

        // 设置半透明效果（通过调整亮度）
        preview.setBrightness(new org.bukkit.entity.Display.Brightness(10, 10));

        // 保存预览光标
        session.setPreviewCursor(preview);
    }

    /**
     * 根据音符类型获取材质
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

    /**
     * 根据判定面获取发光颜色
     */
    private static Color getGlowColorForFace(Face face) {
        return switch (face) {
            case W -> Color.fromRGB(255, 255, 255);      // 白色
            case A -> Color.fromRGB(255, 235, 42);       // 黄色
            case S -> Color.fromRGB(255, 150, 0);        // 橙色
            case D -> Color.fromRGB(255, 0, 0);          // 红色
        };
    }

    /**
     * 计算光标在判定面上的坐标 (与 Skript 逻辑一致)
     * 返回 [targetX, targetY]
     */
    public static double[] calculateCursorPosition(Player player) {
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection();

        double playerZ = playerLocation.getZ();
        double rate = Math.abs(JUDGMENT_DISTANCE - playerZ) / direction.getZ();

        double targetX = direction.getX() * rate + playerLocation.getX();
        double targetY;

        // 如果按住Shift，使用1.4偏移并对齐到网格；否则使用1.6偏移
        if (player.isSneaking()) {
            targetY = direction.getY() * rate + playerLocation.getY() + 1.4;
            targetX = Math.round(targetX * 2) / 2.0;
            targetY = Math.round(targetY * 2) / 2.0;
        } else {
            targetY = direction.getY() * rate + playerLocation.getY() + 1.6;
        }

        return new double[]{targetX, targetY};
    }

    /**
     * 发送Action Bar信息
     */
    public static void sendActionBarInfo(EditorSession session, Player player, double targetX, double targetY) {
        int beat = session.getCurrentBeat();
        int beatPos = session.getCurrentBeatPosition();
        int stepLength = session.getStepLength();
        double time = session.getCurrentTime();
        int noteCount = session.getNotes().size();

        String noteTypeDisplay = getNoteTypeDisplay(session.getCurrentNoteType(), session.getFlickDirection());

        // 自动检测当前指向的面
        Face currentFace = EditorFaceDetector.detectFacingFace(player);
        String faceDisplay = currentFace != null ? getFaceDisplay(currentFace) : "§7无";

        String glowDisplay = session.isGlowing() ? "§a✓" : "§c✗";

        // 如果在DOUBLE音符放置模式
        String modeDisplay = "";
        if (session.isDoubleNotePlacementMode()) {
            modeDisplay = " §e[DOUBLE 第2个位置]";
        }

        String actionBar = String.format(
            "§6BPM: §f%.0f §b第 §f%d§7小节 §f%d§7/%d拍 §f(%.2fs) §7| §f%.1f, %.1f §7| %s §7| %s §7| §7发光:%s §7| §f共%d音符%s",
            session.getBpm(),
            beat,
            beatPos,
            stepLength,
            time,
            targetX,
            targetY,
            noteTypeDisplay,
            faceDisplay,
            glowDisplay,
            noteCount,
            modeDisplay
        );

        player.sendActionBar(actionBar);
    }

    /**
     * 获取音符类型显示文本
     */
    private static String getNoteTypeDisplay(NoteType type, String flickDirection) {
        return switch (type) {
            case TAP -> "§bTap";
            case DOUBLE -> "§6Double";
            case DRAG -> "§eDrag";
            case HOLD -> "§fHold";
            case FLICK -> flickDirection.equals("left") ? "§d←Flick" : "§cFlick→";
            case EXECUTION -> "§5Execution";
        };
    }

    /**
     * 获取判定面显示文本
     */
    private static String getFaceDisplay(Face face) {
        return switch (face) {
            case W -> "§f前(W)";
            case A -> "§e左(A)";
            case S -> "§6后(S)";
            case D -> "§c右(D)";
        };
    }
}
