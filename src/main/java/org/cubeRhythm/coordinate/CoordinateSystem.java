package org.cubeRhythm.coordinate;

import org.bukkit.Location;
import org.bukkit.World;

public class CoordinateSystem {

    /**
     * 将局部面坐标转换为世界坐标
     * @param world 世界
     * @param face 判定面
     * @param x 局部X坐标
     * @param y 局部Y坐标
     * @param z 距离判定线的距离
     * @param centerX 世界中心X
     * @param centerY 世界中心Y
     * @param centerZ 世界中心Z
     * @return 带有正确旋转的转换后位置
     */
    public static Location transformCoordinates(World world, Face face, double x, double y, double z,
                                                 double centerX, double centerY, double centerZ) {
        // 应用坐标修正：先向右移1单位，再整体向右移1单位（总共向右2单位）
        return switch (face) {
            case W -> new Location(world, centerX - x, centerY + y, centerZ + z, 0, 0);
            case A -> new Location(world, centerX + z, centerY + y, centerZ + x, 90, 0);
            case S -> new Location(world, centerX + x, centerY + y, centerZ - z, 180, 0);
            case D -> new Location(world, centerX - z, centerY + y, centerZ - x, 270, 0);
        };
    }

    /**
     * 根据面计算距离判定线的距离
     * @param notePos 音符在世界中的位置
     * @param face 判定面
     * @param centerX 世界中心X
     * @param centerZ 世界中心Z
     * @return 距离判定线的距离（4格）
     */
    public static double calculateDistance(Location notePos, Face face, double centerX, double centerZ) {
        return switch (face) {
            case W -> notePos.getZ() - (centerZ + 3);
            case A -> notePos.getX() - (centerX + 3);
            case S -> (centerZ - 3) - notePos.getZ();
            case D -> (centerX - 3) - notePos.getX();
        };
    }
}
