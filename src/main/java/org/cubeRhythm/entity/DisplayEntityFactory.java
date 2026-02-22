package org.cubeRhythm.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

@SuppressWarnings("deprecation")
public class DisplayEntityFactory {

    /**
     * 创建用于音符可视化的 BlockDisplay 实体
     */
    public static BlockDisplay createBlockDisplay(World world, Location location, Material material,
                                                   float scaleX, float scaleY, float scaleZ,
                                                   int interpolationDuration) {
        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(material.createBlockData());
            entity.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(scaleX, scaleY, scaleZ),
                new AxisAngle4f()
            ));
            entity.setInterpolationDuration(interpolationDuration);
            entity.setBrightness(new Display.Brightness(15, 15));
        });
    }

    /**
     * 创建用于碰撞检测的 Interaction 实体
     */
    public static Interaction createInteraction(World world, Location location,
                                                 float width, float height) {
        return world.spawn(location, Interaction.class, entity -> {
            entity.setInteractionWidth(width);
            entity.setInteractionHeight(height);
        });
    }

    /**
     * 创建用于 UI 元素的 TextDisplay 实体
     */
    public static TextDisplay createTextDisplay(World world, Location location, String text,
                                                 int interpolationDuration) {
        return world.spawn(location, TextDisplay.class, entity -> {
            entity.setText(text);
            entity.setInterpolationDuration(interpolationDuration);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setBillboard(Display.Billboard.CENTER);

            // 设置文本缩放，使其可见
            Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(2.0f, 2.0f, 2.0f),  // 放大2倍
                new AxisAngle4f()
            );
            entity.setTransformation(transformation);

            // 设置背景透明度（可选）
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(128, 0, 0, 0));

            // 设置视距
            entity.setViewRange(50.0f);
        });
    }
}
