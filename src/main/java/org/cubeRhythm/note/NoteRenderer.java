package org.cubeRhythm.note;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Transformation;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class NoteRenderer {

    /**
     * 渲染音符实体，使用适当的视觉表现
     */
    public static void renderNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                   double speed, double distance, float hitboxScale, double bpm) {
        // FLICK 音符在判定线中心，不需要位置，也没有碰撞箱，也没有光标
        if (entity.getType() == NoteType.FLICK) {
            renderFlickNote(entity, world, centerX, centerY, centerZ, distance, bpm);
            return;
        }

        // DOUBLE 音符需要渲染多个位置
        if (entity.getType() == NoteType.DOUBLE) {
            renderDoubleNote(entity, world, centerX, centerY, centerZ, speed, distance, hitboxScale, bpm);
            return;
        }

        // 检查 position 是否存在
        if (entity.getPosition() == null) {
            org.cubeRhythm.Main.instance.getLogger().warning(
                "音符没有 position: type=" + entity.getType() + ", time=" + entity.getTime()
            );
            return;
        }

        Material material = getMaterialForNoteType(entity.getType());

        // HOLD 音符沿移动方向拉长，长度由 BPM 和流速决定
        // 使得一拍的 HOLD 能与下一拍首尾相接
        float scaleZ = entity.getType() == NoteType.HOLD
            ? (float) ((60.0 / bpm) * speed)  // 一拍的距离
            : (float) (0.5 * speed);

        // 坐标调整：根据面分类处理
        // W/S面：当前位置正确，不需要额外调整
        // A/D面：需要右移1格
        double faceOffsetX = (entity.getFace() == Face.A || entity.getFace() == Face.D) ? -1.0 : 0.0;
        double adjustedX = entity.getPosition().getX() - 1.0 + 0.5 + faceOffsetX + 1.0; // 新增一个 +1.0
        double adjustedY = entity.getPosition().getY() + 1.6 - 0.5;

        Location location = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            adjustedX,
            adjustedY,
            distance,
            centerX,
            centerY,
            centerZ
        );

        // 创建 BlockDisplay
        entity.setBlockDisplay(DisplayEntityFactory.createBlockDisplay(
            world, location, material, 1.0f, 1.0f, scaleZ, 100
        ));

        // 创建 Interaction 碰撞箱（HOLD 音符不需要碰撞箱，因为只需按键即可）
        if (entity.getType() != NoteType.HOLD) {
            // hitboxScale 已经是最终大小：难度1=2.0, 难度2=1.5, 难度3=1.0
            entity.setInteraction(DisplayEntityFactory.createInteraction(
                world, location, hitboxScale, hitboxScale
            ));
        }

        // 如果需要，应用发光效果
        if (entity.isGlowing()) {
            entity.getBlockDisplay().setGlowing(true);
        }

        // 创建光标 TextDisplay（在判定面上）（HOLD没有光标）
        if(entity.getType() != NoteType.HOLD) createCursorDisplay(entity, world, centerX, centerY, centerZ);
    }

    /**
     * 渲染 DOUBLE 音符（两个位置）
     */
    private static void renderDoubleNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                          double speed, double distance, float hitboxScale, double bpm) {
        if (entity.getPositions() == null || entity.getPositions().isEmpty()) {
            org.cubeRhythm.Main.instance.getLogger().warning("DOUBLE 音符没有 positions");
            return;
        }

        Material material = Material.ORANGE_CONCRETE;
        float scaleZ = (float) (0.5 * speed);
        // hitboxScale 已经是最终大小

        // 渲染第一个位置（使用主 BlockDisplay 和 Interaction）
        // 根据面分类处理：W/S面不变，A/D面右移1格
        double faceOffsetX = (entity.getFace() == Face.A || entity.getFace() == Face.D) ? -1.0 : 0.0;
        NotePosition firstPos = entity.getPositions().get(0);
        Location location1 = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            firstPos.getX() - 1.0 + 0.5 + faceOffsetX    + 1.0,
            firstPos.getY() + 1.6 - 0.5,
            distance, centerX, centerY, centerZ
        );

        entity.setBlockDisplay(DisplayEntityFactory.createBlockDisplay(
            world, location1, material, 1.0f, 1.0f, scaleZ, 100
        ));

        // 创建第一个碰撞箱
        entity.setInteraction(DisplayEntityFactory.createInteraction(
            world, location1, hitboxScale, hitboxScale
        ));

        // 如果有第二个位置，创建额外的 BlockDisplay 和 Interaction
        if (entity.getPositions().size() > 1) {
            NotePosition secondPos = entity.getPositions().get(1);
            Location location2 = CoordinateSystem.transformCoordinates(
                world, entity.getFace(),
                secondPos.getX() - 1.0 + 0.5 + faceOffsetX + 1.0,
                secondPos.getY() + 1.6 - 0.5,
                distance, centerX, centerY, centerZ
            );

            // 创建第二个 BlockDisplay
            org.bukkit.entity.BlockDisplay secondBlock = DisplayEntityFactory.createBlockDisplay(
                world, location2, material, 1.0f, 1.0f, scaleZ, 100
            );
            entity.getAdditionalBlockDisplays().add(secondBlock);

            // 创建第二个 Interaction 碰撞箱
            org.bukkit.entity.Interaction secondInteraction = DisplayEntityFactory.createInteraction(
                world, location2, hitboxScale, hitboxScale
            );
            entity.getAdditionalInteractions().add(secondInteraction);
        }

        if (entity.isGlowing()) {
            entity.getBlockDisplay().setGlowing(true);
            // 第二个方块也发光
            for (org.bukkit.entity.BlockDisplay additionalBlock : entity.getAdditionalBlockDisplays()) {
                additionalBlock.setGlowing(true);
            }
        }

        // 为 DOUBLE 音符的两个位置都创建光标
        // 根据面分类处理：W/S面不变，A/D面右移1格
        for (NotePosition pos : entity.getPositions()) {
            createCursorDisplayAtPosition(entity, world, centerX, centerY, centerZ, pos, faceOffsetX);
        }
    }

    /**
     * 渲染 FLICK 音符（在判定线中心）
     */
    private static void renderFlickNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance, double bpm) {
        // 使用白色染色玻璃（实心方块）而不是玻璃板，这样缩放才正确
        Material material = Material.WHITE_STAINED_GLASS;

        // FLICK 音符在判定线中心 (0, 0)
        Location location = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            0, 0,  // 中心位置
            distance,
            centerX,
            centerY,
            centerZ
        );

        // 创建 BlockDisplay（5x5x1 参考 Skript）
        // 使用 translation 将方块居中，这样方块的中心就在 location 位置
        entity.setBlockDisplay(world.spawn(location, org.bukkit.entity.BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setInterpolationDuration(100);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

            // 缩放到 5x5x1，并通过 translation 将方块居中
            Transformation transformation = new Transformation(
                new Vector3f(-2.5f, -2.5f, -0.5f),  // 平移使方块中心在原点
                new AxisAngle4f(),
                new Vector3f(5.0f, 5.0f, 1.0f),
                new AxisAngle4f()
            );
            display.setTransformation(transformation);
        }));

        // FLICK 音符始终发光
        entity.getBlockDisplay().setGlowing(true);

        // FLICK 音符没有碰撞箱（不创建 Interaction）

        // 在中心添加超大箭头 TextDisplay 指示转向方向（参考 Skript scale 20,20,0）
        String arrow = entity.getTurn() != null && entity.getTurn().equalsIgnoreCase("left") ? "←" : "→";
        String arrowText = "§f" + arrow;  // 白色箭头

        // 箭头和玻璃在同一位置（玻璃已通过 translation 居中）
        Location arrowLocation = location.clone();

        org.bukkit.entity.TextDisplay arrowDisplay = world.spawn(arrowLocation, org.bukkit.entity.TextDisplay.class, entity1 -> {
            entity1.setText(arrowText);
            entity1.setInterpolationDuration(100);
            entity1.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            entity1.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);

            // 超大文本缩放（20x20）
            Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(20.0f, 20.0f, 0.0f),
                new AxisAngle4f()
            );
            entity1.setTransformation(transformation);

            // 完全透明背景
            entity1.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            entity1.setViewRange(100.0f);
        });

        // 将箭头显示添加到音符实体
        entity.getTextDisplays().add(arrowDisplay);
    }

    /**
     * 根据距离更新音符实体的位置和外观
     */
    public static void updateNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                   double speed, double distance, double bpm) {
        // FLICK 音符特殊处理（FLICK 没有 interaction，所以要先检查）
        if (entity.getType() == NoteType.FLICK) {
            if (entity.getBlockDisplay() == null) {
                return;
            }
            updateFlickNote(entity, world, centerX, centerY, centerZ, distance, bpm);
            return;
        }

        // 其他音符需要 BlockDisplay（HOLD 音符不需要 Interaction）
        if (entity.getBlockDisplay() == null) {
            return;
        }

        // 非 HOLD 音符需要 Interaction
        if (entity.getType() != NoteType.HOLD && entity.getInteraction() == null) {
            return;
        }

        // DOUBLE 音符特殊处理
        if (entity.getType() == NoteType.DOUBLE) {
            updateDoubleNote(entity, world, centerX, centerY, centerZ, speed, distance, bpm);
            return;
        }

        // 检查 position 是否存在
        if (entity.getPosition() == null) {
            return;
        }

        // 坐标调整：根据面分类处理
        // W/S面：当前位置正确
        // A/D面：右移1格
        double faceOffsetX = (entity.getFace() == Face.A || entity.getFace() == Face.D) ? -1.0 : 0.0;
        double adjustedX = entity.getPosition().getX() - 1.0 + 0.5 + faceOffsetX + 1.0;
        double adjustedY = entity.getPosition().getY() + 1.6 - 0.5;

        Location location = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            adjustedX,
            adjustedY,
            distance,
            centerX,
            centerY,
            centerZ
        );

        // 更新位置
        entity.getBlockDisplay().teleport(location);

        // HOLD 音符没有 Interaction，跳过
        if (entity.getType() != NoteType.HOLD && entity.getInteraction() != null) {
            entity.getInteraction().teleport(location);
        }

        // 计算渐变缩放效果
        float scale = calculateScale(distance);

        // HOLD 音符沿移动方向拉长，长度由 BPM 和流速决定
        float scaleZ = entity.getType() == NoteType.HOLD
            ? (float) ((60.0 / bpm) * speed)  // 一拍的距离
            : (float) (0.5 * speed);

        // 更新 BlockDisplay 的缩放
        Transformation transformation = new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f(),
            new Vector3f(scale, scale, scaleZ),
            new AxisAngle4f()
        );
        entity.getBlockDisplay().setTransformation(transformation);

        // 更新光标显示
        updateCursorDisplay(entity, world, centerX, centerY, centerZ, distance);
    }

    /**
     * 更新 DOUBLE 音符
     */
    private static void updateDoubleNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                          double speed, double distance, double bpm) {
        if (entity.getPositions() == null || entity.getPositions().isEmpty()) {
            return;
        }

        float scale = calculateScale(distance);
        float scaleZ = (float) (0.5 * speed);

        // 根据面分类处理：W/S面不变，A/D面右移1格
        double faceOffsetX = (entity.getFace() == Face.A || entity.getFace() == Face.D) ? -1.0 : 0.0;

        // 更新第一个位置
        NotePosition firstPos = entity.getPositions().get(0);
        Location location1 = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            firstPos.getX() - 1.0 + 0.5 + faceOffsetX + 1.0,
            firstPos.getY() + 1.6 - 0.5,
            distance, centerX, centerY, centerZ
        );

        entity.getBlockDisplay().teleport(location1);
        entity.getInteraction().teleport(location1);

        Transformation transformation = new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f(),
            new Vector3f(scale, scale, scaleZ),
            new AxisAngle4f()
        );
        entity.getBlockDisplay().setTransformation(transformation);

        // 更新第二个位置（如果存在）
        if (entity.getPositions().size() > 1 &&
            !entity.getAdditionalBlockDisplays().isEmpty() &&
            !entity.getAdditionalInteractions().isEmpty()) {

            NotePosition secondPos = entity.getPositions().get(1);
            Location location2 = CoordinateSystem.transformCoordinates(
                world, entity.getFace(),
                secondPos.getX() - 1.0 + 0.5 + faceOffsetX + 1.0,
                secondPos.getY() + 1.6 - 0.5,
                distance, centerX, centerY, centerZ
            );

            // 更新第二个 BlockDisplay
            org.bukkit.entity.BlockDisplay secondBlock = entity.getAdditionalBlockDisplays().get(0);
            secondBlock.teleport(location2);
            secondBlock.setTransformation(transformation);

            // 更新第二个 Interaction
            org.bukkit.entity.Interaction secondInteraction = entity.getAdditionalInteractions().get(0);
            secondInteraction.teleport(location2);
        }

        // 更新 DOUBLE 音符的所有光标显示
        updateDoubleCursorDisplays(entity, world, centerX, centerY, centerZ, distance);
    }

    /**
     * 更新 FLICK 音符
     */
    private static void updateFlickNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance, double bpm) {
        Location location = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            0, 0,  // 中心位置
            distance,
            centerX,
            centerY,
            centerZ
        );

        // 更新 BlockDisplay 位置
        entity.getBlockDisplay().teleport(location);

        // 保持 FLICK 音符的固定大小 5x5x1，并通过 translation 居中
        Transformation transformation = new Transformation(
            new Vector3f(-2.5f, -1f, -0.5f),  // 平移使方块中心在原点
            new AxisAngle4f(),
            new Vector3f(5.0f, 5.0f, 1.0f),
            new AxisAngle4f()
        );
        entity.getBlockDisplay().setTransformation(transformation);

        // 更新箭头位置（和玻璃在同一位置，因为玻璃已居中）
        if (!entity.getTextDisplays().isEmpty()) {
            org.bukkit.entity.TextDisplay arrowDisplay = entity.getTextDisplays().get(0);
            arrowDisplay.teleport(location.clone().add(0,-1.5,0));
        }
    }

    /**
     * 根据距离计算音符的缩放比例
     * 距离越远，音符越小；到达判定线时为正常大小
     *
     * @param distance 距离判定线的距离
     * @return 缩放比例 (0.2 到 1.0)
     */
    private static float calculateScale(double distance) {
        // 定义缩放范围
        final double START_DISTANCE = 50.0;  // 音符生成时的距离
        final double END_DISTANCE = 4.0;     // 判定线的距离
        final float MIN_SCALE = 0.2f;        // 最小缩放（远处）
        final float MAX_SCALE = 1.0f;        // 最大缩放（判定线）

        // 限制距离范围
        if (distance >= START_DISTANCE) {
            return MIN_SCALE;
        }
        if (distance <= END_DISTANCE) {
            return MAX_SCALE;
        }

        // 线性插值计算缩放
        double progress = (START_DISTANCE - distance) / (START_DISTANCE - END_DISTANCE);
        return (float) (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * progress);
    }

    /**
     * 更新光标 TextDisplay 的位置、大小和透明度
     */
    private static void updateCursorDisplay(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance) {
        // 检查是否有光标（第一个 TextDisplay 是光标）
        if (entity.getTextDisplays().isEmpty()) {
            return;
        }

        org.bukkit.entity.TextDisplay cursorDisplay = entity.getTextDisplays().get(0);
        if (cursorDisplay == null || cursorDisplay.isDead()) {
            return;
        }

        // 只在音符距离判定面较近时显示光标（距离 < 25）
        if (distance >= 25) {
            // 距离太远，隐藏光标
            cursorDisplay.setTextOpacity((byte) 0);
            return;
        }

        // 计算光标的透明度和缩放
        // alpha = 100 - (distance - 4) * 4
        int alpha = (int) (100 - (distance - 4) * 4);
        alpha = Math.max(1, Math.min(100, alpha));  // 限制在 1-100 范围内

        // scale = alpha / 25
        float cursorScale = alpha / 25.0f;

        // 根据音符类型选择颜色
        String color = switch (entity.getType()) {
            case TAP -> "§3";      // 深青色
            case DRAG -> "§e";     // 黄色
            case DOUBLE -> "§6";   // 金色
            default -> "§f";       // 白色
        };

        // 设置文本内容
        String text;
        if (alpha <= 10) {
            text = "§f";  // 透明度很低时只显示白色
        } else {
            text = "§f" + color + "█";  // 显示彩色方块
        }
        cursorDisplay.setText(text);

        // 设置透明度（转换为 0-255 范围，然后乘以2以匹配Skript的效果）
        int opacity = Math.min(255, alpha * 2);
        cursorDisplay.setTextOpacity((byte) opacity);

        // 更新缩放
        Transformation transformation = new Transformation(
            new Vector3f(-cursorScale * 0.015f, -cursorScale * 0.15f, 0),
            new AxisAngle4f(),
            new Vector3f(cursorScale, cursorScale, cursorScale),
            new AxisAngle4f()
        );
        cursorDisplay.setTransformation(transformation);

        // 更新光标位置（始终在判定面上）
        // 注意：实际判定提前2格，所以光标也应该提前显示
        // 根据面分类处理：W/S面不变，A/D面右移1格
        double faceOffsetX = (entity.getFace() == Face.A || entity.getFace() == Face.D) ? -1.0 : 0.0;
        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            entity.getPosition().getX() - 1.0 + faceOffsetX + 1.0 + 1.0,  // 音符x调整 + 光标对齐
            entity.getPosition().getY() + 1.6,  // 音符y调整 + 光标对齐
            6.0,  // 判定面距离（4 + 2，提前2格）
            centerX + (entity.getFace() == Face.S ? -1 : 1),
            centerY,
            centerZ
        );

        // 设置光标朝向（面向玩家）
        float yaw = switch (entity.getFace()) {
            case W -> 180f;   // 前方面向玩家（180度）
            case A -> 90f;    // 左侧面向玩家（90度）
            case S -> 0f;     // 后方面向玩家（0度）
            case D -> -90f;   // 右侧面向玩家（-90度）
        };
        cursorLocation.setYaw(yaw);

        cursorDisplay.teleport(cursorLocation);
    }

    /**
     * 更新 DOUBLE 音符的所有光标显示
     */
    private static void updateDoubleCursorDisplays(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance) {
        // DOUBLE 音符应该有两个光标
        if (entity.getTextDisplays().size() < 2) {
            return;
        }

        if (entity.getPositions() == null || entity.getPositions().size() < 2) {
            return;
        }

        // 只在音符距离判定面较近时显示光标（距离 < 25）
        if (distance >= 25) {
            // 距离太远，隐藏所有光标
            for (org.bukkit.entity.TextDisplay cursor : entity.getTextDisplays()) {
                if (cursor != null && !cursor.isDead()) {
                    cursor.setTextOpacity((byte) 0);
                }
            }
            return;
        }

        // 计算光标的透明度和缩放
        int alpha = (int) (100 - (distance - 4) * 4);
        alpha = Math.max(1, Math.min(100, alpha));
        float cursorScale = alpha / 25.0f;

        // 根据音符类型选择颜色
        String color = "§6";  // DOUBLE 音符是金色

        // 设置文本内容
        String text;
        if (alpha <= 10) {
            text = "§f";  // 透明度很低时只显示白色
        } else {
            text = "§f" + color + "█";  // 显示彩色方块
        }

        // 设置透明度
        int opacity = Math.min(255, alpha * 2);

        // 更新缩放
        Transformation transformation = new Transformation(
            new Vector3f(-cursorScale * 0.015f, -cursorScale * 0.15f, 0),
            new AxisAngle4f(),
            new Vector3f(cursorScale, cursorScale, cursorScale),
            new AxisAngle4f()
        );

        // 根据面分类处理：W/S面不变，A/D面右移1格
        double faceOffsetX = (entity.getFace() == Face.A || entity.getFace() == Face.D) ? -1.0 : 0.0;

        // 更新每个光标
        for (int i = 0; i < Math.min(2, entity.getTextDisplays().size()); i++) {
            org.bukkit.entity.TextDisplay cursorDisplay = entity.getTextDisplays().get(i);
            if (cursorDisplay == null || cursorDisplay.isDead()) {
                continue;
            }

            NotePosition pos = entity.getPositions().get(i);

            // 更新光标位置
            Location cursorLocation = CoordinateSystem.transformCoordinates(
                world,
                entity.getFace(),
                pos.getX() - 1.0 + faceOffsetX + 1.0 + 1.0,
                pos.getY() + 1.6,
                6.0,  // 判定面距离（4 + 2，提前2格）
                centerX + (entity.getFace() == Face.S ? -1 : 1),
                centerY,
                centerZ
            );

            // 设置光标朝向（面向玩家）
            float yaw = switch (entity.getFace()) {
                case W -> 180f;
                case A -> 90f;
                case S -> 0f;
                case D -> -90f;
            };
            cursorLocation.setYaw(yaw);

            // 应用更新
            cursorDisplay.setText(text);
            cursorDisplay.setTextOpacity((byte) opacity);
            cursorDisplay.setTransformation(transformation);
            cursorDisplay.teleport(cursorLocation);
        }
    }

    /**
     * 创建光标 TextDisplay（显示在判定面上）
     */
    private static void createCursorDisplay(NoteEntity entity, World world, double centerX, double centerY, double centerZ) {
        // 光标位置在实际判定面上（distance = 6，提前2格）
        // 这样光标显示位置与实际判定位置一致
        // 光标偏移以对齐音符中心（音符已经-1+0.5x, +1.6-0.5y）
        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            entity.getPosition().getX() - 1.0    + 1.0    + 1.0,  // 音符x调整后，光标对齐-0.5
            entity.getPosition().getY() + 1.6,  // 音符y调整后，光标对齐+0.5
            6.0,  // 实际判定面距离（4 + 2）
            centerX,
            centerY,
            centerZ
        );

        // 根据判定面设置光标的朝向（yaw），使其面向玩家
        float yaw = switch (entity.getFace()) {
            case W -> 180f;   // 前方面向玩家（180度）
            case A -> 90f;    // 左侧面向玩家（90度）
            case S -> 0f;     // 后方面向玩家（0度）
            case D -> -90f;   // 右侧面向玩家（-90度）
        };
        cursorLocation.setYaw(yaw);

        // 根据音符类型选择颜色
        String color = switch (entity.getType()) {
            case TAP -> "§3";      // 深青色
            case DRAG -> "§e";     // 黄色
            case DOUBLE -> "§6";   // 金色
            default -> "§f";       // 白色
        };

        // 创建光标 TextDisplay
        org.bukkit.entity.TextDisplay cursorDisplay = world.spawn(cursorLocation, org.bukkit.entity.TextDisplay.class, display -> {
            display.setText("§f");  // 初始为白色，后续会更新
            display.setInterpolationDuration(0);  // 不使用插值，立即更新
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);  // 固定朝向，不自动面向玩家

            // 初始缩放为0（会在update中更新）
            Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new AxisAngle4f()
            );
            display.setTransformation(transformation);

            // 完全透明背景
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setViewRange(100.0f);
        });

        // 将光标添加到音符实体的 TextDisplays 列表
        entity.getTextDisplays().add(cursorDisplay);
    }

    /**
     * 为指定位置创建光标 TextDisplay（用于 DOUBLE 音符）
     */
    private static void createCursorDisplayAtPosition(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                                       NotePosition position, double faceOffsetX) {
        // 光标位置在实际判定面上（distance = 6，提前2格）
        // 光标偏移以对齐音符中心（音符已经-1+0.5x, +1.6-0.5y）
        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world,
            entity.getFace(),
            position.getX() - 1.0 + faceOffsetX + 1.0 + 1.0,  // 音符x调整后，光标对齐-0.5，加上面偏移
            position.getY() + 1.6 - 0.5 + 0.5,  // 音符y调整后，光标对齐+0.5
            6.0,  // 实际判定面距离（4 + 2）
            centerX,
            centerY,
            centerZ
        );

        // 根据判定面设置光标的朝向（yaw），使其面向玩家
        float yaw = switch (entity.getFace()) {
            case W -> 180f;   // 前方面向玩家（180度）
            case A -> 90f;    // 左侧面向玩家（90度）
            case S -> 0f;     // 后方面向玩家（0度）
            case D -> -90f;   // 右侧面向玩家（-90度）
        };
        cursorLocation.setYaw(yaw);

        // 根据音符类型选择颜色
        String color = switch (entity.getType()) {
            case TAP -> "§3";      // 深青色
            case DRAG -> "§e";     // 黄色
            case DOUBLE -> "§6";   // 金色
            default -> "§f";       // 白色
        };

        // 创建光标 TextDisplay
        org.bukkit.entity.TextDisplay cursorDisplay = world.spawn(cursorLocation, org.bukkit.entity.TextDisplay.class, display -> {
            display.setText("§f");  // 初始为白色，后续会更新
            display.setInterpolationDuration(0);  // 不使用插值，立即更新
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);  // 固定朝向，不自动面向玩家

            // 初始缩放为0（会在update中更新）
            Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new AxisAngle4f()
            );
            display.setTransformation(transformation);

            // 完全透明背景
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setViewRange(100.0f);
        });

        // 将光标添加到音符实体的 TextDisplays 列表
        entity.getTextDisplays().add(cursorDisplay);
    }

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
