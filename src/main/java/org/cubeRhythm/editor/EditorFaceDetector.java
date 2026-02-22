package org.cubeRhythm.editor;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.cubeRhythm.coordinate.Face;

/**
 * 编辑器面检测器 - 自动检测玩家视线指向哪个判定面
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorFaceDetector {

    /**
     * 根据玩家视线方向自动检测当前指向的判定面
     *
     * 判定面定义：
     * - Z+ 方向 为 W 判定面 (前)
     * - X+ 方向 为 A 判定面 (左)
     * - Z- 方向 为 S 判定面 (后)
     * - X- 方向 为 D 判定面 (右)
     *
     * @param player 玩家
     * @return 当前指向的判定面，如果没有指向任何面则返回 null
     */
    public static Face detectFacingFace(Player player) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection();

        // 获取玩家中心位置 (假设玩家站在 0,y,0)
        double centerX = 0;
        double centerZ = 0;

        // 计算与各个判定面的交点
        // W 面: z = 4
        // A 面: x = 4
        // S 面: z = -4
        // D 面: x = -4

        double playerX = playerLoc.getX();
        double playerZ = playerLoc.getZ();

        // 计算到达各个面需要的距离
        Double distToW = calculateDistanceToPlane(playerX, playerZ, direction.getX(), direction.getZ(),
                                                   centerX, centerZ + 4, false); // Z = 4
        Double distToA = calculateDistanceToPlane(playerX, playerZ, direction.getX(), direction.getZ(),
                                                   centerX + 4, centerZ, true);  // X = 4
        Double distToS = calculateDistanceToPlane(playerX, playerZ, direction.getX(), direction.getZ(),
                                                   centerX, centerZ - 4, false); // Z = -4
        Double distToD = calculateDistanceToPlane(playerX, playerZ, direction.getX(), direction.getZ(),
                                                   centerX - 4, centerZ, true);  // X = -4

        // 找到最近的面（且距离为正，表示在玩家前方）
        Face closestFace = null;
        double minDist = Double.MAX_VALUE;

        if (distToW != null && distToW > 0 && distToW < minDist) {
            minDist = distToW;
            closestFace = Face.W;
        }
        if (distToA != null && distToA > 0 && distToA < minDist) {
            minDist = distToA;
            closestFace = Face.A;
        }
        if (distToS != null && distToS > 0 && distToS < minDist) {
            minDist = distToS;
            closestFace = Face.S;
        }
        if (distToD != null && distToD > 0 && distToD < minDist) {
            minDist = distToD;
            closestFace = Face.D;
        }

        // 验证交点是否在判定面范围内
        if (closestFace != null) {
            double[] intersection = calculateIntersectionPoint(playerLoc, direction, closestFace);
            if (intersection != null && isPointInFaceRange(intersection[0], intersection[1], closestFace)) {
                return closestFace;
            }
        }

        return null;
    }

    /**
     * 计算到达平面的距离
     * @param isXPlane true 表示 X 平面，false 表示 Z 平面
     */
    private static Double calculateDistanceToPlane(double playerX, double playerZ,
                                                    double dirX, double dirZ,
                                                    double planeX, double planeZ,
                                                    boolean isXPlane) {
        if (isXPlane) {
            // X 平面: x = planeX
            if (Math.abs(dirX) < 0.001) return null; // 平行于平面
            double t = (planeX - playerX) / dirX;
            return t;
        } else {
            // Z 平面: z = planeZ
            if (Math.abs(dirZ) < 0.001) return null; // 平行于平面
            double t = (planeZ - playerZ) / dirZ;
            return t;
        }
    }

    /**
     * 计算视线与判定面的交点坐标
     * @return [x, y, z] 或 null
     */
    private static double[] calculateIntersectionPoint(Location playerLoc, Vector direction, Face face) {
        double playerX = playerLoc.getX();
        double playerY = playerLoc.getY();
        double playerZ = playerLoc.getZ();

        double dirX = direction.getX();
        double dirY = direction.getY();
        double dirZ = direction.getZ();

        double t;

        switch (face) {
            case W: // Z = 4
                if (Math.abs(dirZ) < 0.001) return null;
                t = (4 - playerZ) / dirZ;
                break;
            case A: // X = 4
                if (Math.abs(dirX) < 0.001) return null;
                t = (4 - playerX) / dirX;
                break;
            case S: // Z = -4
                if (Math.abs(dirZ) < 0.001) return null;
                t = (-4 - playerZ) / dirZ;
                break;
            case D: // X = -4
                if (Math.abs(dirX) < 0.001) return null;
                t = (-4 - playerX) / dirX;
                break;
            default:
                return null;
        }

        if (t <= 0) return null; // 在玩家后方

        double x = playerX + dirX * t;
        double y = playerY + dirY * t;
        double z = playerZ + dirZ * t;

        return new double[]{x, y, z};
    }

    /**
     * 检查点是否在判定面范围内
     *
     * 判定面范围：
     * - W 面: x ∈ [-3, 3], y ∈ [-3, 3], z = 4
     * - A 面: x = 4, y ∈ [-3, 3], z ∈ [-3, 3]
     * - S 面: x ∈ [-3, 3], y ∈ [-3, 3], z = -4
     * - D 面: x = -4, y ∈ [-3, 3], z ∈ [-3, 3]
     */
    private static boolean isPointInFaceRange(double x, double y, Face face) {
        // Y 坐标对所有面都是 [-3, 3]
        if (y < -3 || y > 3) return false;

        switch (face) {
            case W: // Z = 4
            case S: // Z = -4
                // X 坐标范围 [-3, 3]
                return x >= -3 && x <= 3;
            case A: // X = 4
            case D: // X = -4
                // Z 坐标范围 [-3, 3]
                // 注意：这里的 x 参数实际上是 z 坐标（因为我们传入的是交点的 x, y）
                // 需要重新计算，这个方法需要修改
                return true; // 暂时返回 true，需要在调用处传入正确的坐标
            default:
                return false;
        }
    }

    /**
     * 检查交点是否在判定面范围内（完整版本）
     */
    public static boolean isIntersectionInFaceRange(double x, double y, double z, Face face) {
        // Y 坐标对所有面都是 [-3, 3]
        if (y < -3 || y > 3) return false;

        switch (face) {
            case W: // Z = 4
                return x >= -3 && x <= 3 && Math.abs(z - 4) < 0.1;
            case A: // X = 4
                return z >= -3 && z <= 3 && Math.abs(x - 4) < 0.1;
            case S: // Z = -4
                return x >= -3 && x <= 3 && Math.abs(z + 4) < 0.1;
            case D: // X = -4
                return z >= -3 && z <= 3 && Math.abs(x + 4) < 0.1;
            default:
                return false;
        }
    }
}
