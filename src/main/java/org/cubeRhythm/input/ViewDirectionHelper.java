package org.cubeRhythm.input;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.cubeRhythm.note.NoteEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * 视线方向检测辅助类
 * 用于 DRAG 和 FLICK 音符的判定
 */
public class ViewDirectionHelper {

    /**
     * 检查玩家视线是否指向指定音符实体
     * 用于 DRAG 音符判定
     *
     * 只要射线击中音符的任何组成部分（主体、交互实体、额外显示等），都算指向该音符
     *
     * @param player 玩家
     * @param noteEntity 目标音符实体
     * @param maxDistance 最大检测距离
     * @return 是否指向目标
     */
    public static boolean isLookingAt(Player player, NoteEntity noteEntity, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // 收集音符的所有实体组件
        Set<Entity> noteEntities = new HashSet<>();

        if (noteEntity.getBlockDisplay() != null) {
            noteEntities.add(noteEntity.getBlockDisplay());
        }
        if (noteEntity.getInteraction() != null) {
            noteEntities.add(noteEntity.getInteraction());
        }
        if (noteEntity.getAdditionalBlockDisplays() != null) {
            noteEntities.addAll(noteEntity.getAdditionalBlockDisplays());
        }
        if (noteEntity.getAdditionalInteractions() != null) {
            noteEntities.addAll(noteEntity.getAdditionalInteractions());
        }

        // 射线检测，检查是否击中音符的任何组成部分
        RayTraceResult result = player.getWorld().rayTraceEntities(
            eyeLocation,
            direction,
            maxDistance,
                noteEntities::contains
        );

        return result != null && result.getHitEntity() != null;
    }

    /**
     * 检查玩家视线是否指向指定实体（旧版本，保留用于兼容）
     *
     * @param player 玩家
     * @param entity 目标实体
     * @param maxDistance 最大检测距离
     * @return 是否指向目标
     */
    public static boolean isLookingAt(Player player, Entity entity, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        RayTraceResult result = player.getWorld().rayTraceEntities(
            eyeLocation,
            direction,
            maxDistance,
            e -> e.equals(entity)
        );

        return result != null && result.getHitEntity() != null;
    }

    /**
     * 检查玩家视线旋转方向是否在指定角度范围内
     * 用于 FLICK 音符判定
     *
     * @param player 玩家
     * @param previousYaw 之前的 yaw 角度
     * @param currentYaw 当前的 yaw 角度
     * @param targetDirection 目标方向 ("left" 或 "right")
     * @return 是否在正确的旋转方向
     */
    public static boolean isRotatingInDirection(Player player, float previousYaw, float currentYaw, String targetDirection) {
        // 计算 yaw 变化（处理角度环绕）
        float yawDelta = normalizeAngle(currentYaw - previousYaw);

        // 检查旋转角度是否在 45° 到 135° 范围内
        float absYawDelta = Math.abs(yawDelta);
        if (absYawDelta < 45.0f || absYawDelta > 135.0f) {
            return false;
        }

        // 检查旋转方向是否正确
        if (targetDirection.equalsIgnoreCase("left")) {
            // 左转：yaw 增加（正值）
            return yawDelta > 0;
        } else if (targetDirection.equalsIgnoreCase("right")) {
            // 右转：yaw 减少（负值）
            return yawDelta < 0;
        }

        return false;
    }

    /**
     * 标准化角度到 -180 到 180 范围
     */
    private static float normalizeAngle(float angle) {
        while (angle > 180.0f) {
            angle -= 360.0f;
        }
        while (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    /**
     * 获取玩家当前的 yaw 角度
     */
    public static float getPlayerYaw(Player player) {
        return player.getLocation().getYaw();
    }
}
