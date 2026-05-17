package org.cubeRhythm.note;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.cubeRhythm.Main;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.cubeRhythm.manager.OffsetConfig;
import org.cubeRhythm.note.event.EvalResult;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class NoteRenderer {

    public static void renderNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                   double speed, double distance, float hitboxScale, double bpm) {
        if (entity.getType() == NoteType.FLICK || entity.getType() == NoteType.FAKE_FLICK) {
            renderFlickNote(entity, world, centerX, centerY, centerZ, distance, bpm);
            return;
        }
        if (entity.getType() == NoteType.DOUBLE || entity.getType() == NoteType.FAKE_DOUBLE) {
            renderDoubleNote(entity, world, centerX, centerY, centerZ, speed, distance, hitboxScale, bpm);
            return;
        }
        if (entity.getPosition() == null) {
            Main.instance.getLogger().warning(
                "音符没有 position: type=" + entity.getType() + ", time=" + entity.getTime());
            return;
        }

        OffsetConfig oc = OffsetConfig.get();
        Material material = getMaterialForNoteType(entity.getType());
        boolean isHold = entity.getType() == NoteType.HOLD || entity.getType() == NoteType.FAKE_HOLD;
        float scaleZ = isHold
            ? (float) ((60.0 / bpm) * speed * oc.holdScaleZFactor)
            : (float) (oc.scaleZFactor * speed);

        if (isHold) entity.setHoldScaleZ(scaleZ);

        double adjustedX = entity.getPosition().getX() + oc.noteX(entity.getFace());
        double adjustedY = entity.getPosition().getY() + oc.noteY(entity.getFace());

        Location location = CoordinateSystem.transformCoordinates(
            world, entity.getFace(), adjustedX, adjustedY, distance, centerX, centerY, centerZ);

        entity.setBlockDisplay(DisplayEntityFactory.createBlockDisplay(
            world, location, material, 1.0f, 1.0f, scaleZ, 100));

        if (isHold &&
                (entity.getFace() == Face.A || entity.getFace() == Face.D)) {
            AxisAngle4f rot = new AxisAngle4f(
                (float)(Math.PI / 2) * (entity.getFace() == Face.A ? 1 : -1), 0, 1, 0);
            Transformation t = entity.getBlockDisplay().getTransformation();
            t.getLeftRotation().set(rot);
            entity.getBlockDisplay().setTransformation(t);
        }

        if (entity.getType() != NoteType.HOLD && !entity.getType().isFake()) {
            entity.setInteraction(DisplayEntityFactory.createInteraction(
                world, location, hitboxScale, hitboxScale));
        }

        if (entity.isGlowing()) entity.getBlockDisplay().setGlowing(true);

        if (!isHold && !entity.getType().isFake()) createCursorDisplay(entity, world, centerX, centerY, centerZ);
    }

    private static void renderDoubleNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                          double speed, double distance, float hitboxScale, double bpm) {
        if (entity.getPositions() == null || entity.getPositions().isEmpty()) {
            Main.instance.getLogger().warning("DOUBLE 音符没有 positions");
            return;
        }

        OffsetConfig oc = OffsetConfig.get();
        Material material = entity.getType().isFake() ? getMaterialForNoteType(entity.getType()) : Material.ORANGE_CONCRETE;
        float scaleZ = (float) (oc.scaleZFactor * speed);

        NotePosition firstPos = entity.getPositions().get(0);
        Location location1 = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            firstPos.getX() + oc.noteX(entity.getFace()),
            firstPos.getY() + oc.noteY(entity.getFace()),
            distance, centerX, centerY, centerZ);

        entity.setBlockDisplay(DisplayEntityFactory.createBlockDisplay(
            world, location1, material, 1.0f, 1.0f, scaleZ, 100));
        if (!entity.getType().isFake()) {
            entity.setInteraction(DisplayEntityFactory.createInteraction(
                world, location1, hitboxScale, hitboxScale));
        }

        Location location2 = null;
        if (entity.getPositions().size() > 1) {
            NotePosition secondPos = entity.getPositions().get(1);
            location2 = CoordinateSystem.transformCoordinates(
                world, entity.getFace(),
                secondPos.getX() + oc.noteX(entity.getFace()),
                secondPos.getY() + oc.noteY(entity.getFace()),
                distance, centerX, centerY, centerZ);

            BlockDisplay secondBlock = DisplayEntityFactory.createBlockDisplay(
                world, location2, material, 1.0f, 1.0f, scaleZ, 100);
            entity.getAdditionalBlockDisplays().add(secondBlock);

            if (!entity.getType().isFake()) {
                Interaction secondInteraction = DisplayEntityFactory.createInteraction(
                    world, location2, hitboxScale, hitboxScale);
                entity.getAdditionalInteractions().add(secondInteraction);
            }
        }

        if (entity.isGlowing()) {
            entity.getBlockDisplay().setGlowing(true);
            for (BlockDisplay b : entity.getAdditionalBlockDisplays()) b.setGlowing(true);
        }

        if (entity.getPositions().size() > 1 && location2 != null) {
            renderDoubleLine(entity, world, location1, location2);
        }

        for (NotePosition pos : entity.getPositions()) {
            createCursorDisplayAtPosition(entity, world, centerX, centerY, centerZ, pos);
        }

    }

    /**
     * 构建 Double 连接线的 Transformation。
     * 连线沿 Y 轴延伸（scale Y = lineLen），通过旋转对齐两点方向：
     * W/S 面两点差值在 XY 平面，绕 Z 轴旋转 atan2(-dwx, dwy)；
     * A/D 面两点差值在 YZ 平面，绕 X 轴旋转 atan2(dwz, dwy)。
     * 平移 (-dwx/2-to, -dwy/2, -dwz/2-to) 使连线从 loc1 延伸到 loc2，to 用于宽度居中。
     * @param face 判定面，决定旋转轴方向
     * @param dwx  loc2.x - loc1.x，世界坐标 X 差值
     * @param dwy  loc2.y - loc1.y，世界坐标 Y 差值
     * @param dwz  loc2.z - loc1.z，世界坐标 Z 差值
     * @param lw   连线宽度（connectLineWidth）
     * @param to   宽度居中偏移量（connectLineTransOffset，通常 = lw/2）
     */
    private static Transformation buildLineTransformation(Face face, float dwx, float dwy, float dwz, float lw, float to) {
        float lineLen = (float) Math.sqrt(dwx * dwx + dwy * dwy + dwz * dwz);
        switch (face) {
            case W -> {
                return new Transformation(
                    new Vector3f(-dwx / 2 - to, -dwy / 2, -dwz / 2 - to),
                    new AxisAngle4f((float) Math.atan2(-dwx, dwy), 0, 0, 1),
                    new Vector3f(lw, lineLen, lw), new AxisAngle4f());
            }
            case S -> {
                return new Transformation(
                    new Vector3f(-dwx / 2 - to, -dwy / 2, -dwz / 2 - to),
                    new AxisAngle4f((float) Math.atan2(dwx, dwy), 0, 0, 1),
                    new Vector3f(lw, lineLen, lw), new AxisAngle4f());
            }
            case A -> {
                // A面 yaw=90：局部X=世界+Z，局部Z=世界-X，方向(0,dwy,dwz)在局部空间=(dwz,dwy,0)
                return new Transformation(
                    new Vector3f(-dwx / 2 - to, -dwy / 2, -dwz / 2 - to),
                    new AxisAngle4f((float) Math.atan2(-dwz, dwy), 0, 0, 1),
                    new Vector3f(lw, lineLen, lw), new AxisAngle4f());
            }
            case D -> {
                // D面 yaw=270：局部X=世界-Z，局部Z=世界+X，方向(0,dwy,dwz)在局部空间=(-dwz,dwy,0)
                return new Transformation(
                    new Vector3f(-dwx / 2 - to, -dwy / 2, -dwz / 2 - to),
                    new AxisAngle4f((float) Math.atan2(dwz, dwy), 0, 0, 1),
                    new Vector3f(lw, lineLen, lw), new AxisAngle4f());
            }
        }
        return null;
    }

    private static void renderDoubleLine(NoteEntity entity, World world, Location loc1, Location loc2) {
        OffsetConfig oc = OffsetConfig.get();
        float lw = (float) oc.connectLineWidth;
        float to = (float) oc.connectLineTransOffset;
        float dwx = (float)(loc2.getX() - loc1.getX());
        float dwy = (float)(loc2.getY() - loc1.getY());
        float dwz = (float)(loc2.getZ() - loc1.getZ());
        if (Math.sqrt(dwx * dwx + dwy * dwy + dwz * dwz) < 0.001) return;

        Transformation tf = buildLineTransformation(entity.getFace(), dwx, dwy, dwz, lw, to);
        if (tf == null) return;
        Location mid = loc1.clone().add(
            dwx / 2.0 + oc.connectLineOffsetX(entity.getFace()),
            dwy / 2.0 + oc.connectLineOffsetY(entity.getFace()),
            dwz / 2.0 + oc.connectLineOffsetZ(entity.getFace()));

        entity.setConnectLine(world.spawn(mid, BlockDisplay.class, display -> {
            display.setBlock(Material.WHITE_CONCRETE.createBlockData());
            display.setInterpolationDuration(2);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTransformation(tf);
        }));
    }

    private static void updateDoubleLine(NoteEntity entity, Location loc1, Location loc2) {
        if (entity.getConnectLine() == null || entity.getConnectLine().isDead()) return;
        // 保证 loc1 始终在 loc2 左侧
        boolean swap = switch (entity.getFace()) {
            case W -> loc1.getX() < loc2.getX();
            case A -> loc1.getZ() > loc2.getZ();
            case S -> loc1.getX() > loc2.getX();
            case D -> loc1.getZ() < loc2.getZ();
        };
        if (swap) { Location tmp = loc1; loc1 = loc2; loc2 = tmp; }

        OffsetConfig oc = OffsetConfig.get();
        float lw = (float) oc.connectLineWidth;
        float to = (float) oc.connectLineTransOffset;
        float dwx = (float)(loc2.getX() - loc1.getX());
        float dwy = (float)(loc2.getY() - loc1.getY());
        float dwz = (float)(loc2.getZ() - loc1.getZ());
        if (Math.sqrt(dwx * dwx + dwy * dwy + dwz * dwz) < 0.001) return;

        Transformation tf = buildLineTransformation(entity.getFace(), dwx, dwy, dwz, lw, to);
        if (tf == null) return;
        Location mid = loc1.clone().add(
            dwx / 2.0 + oc.connectLineOffsetX(entity.getFace()),
            dwy / 2.0 + oc.connectLineOffsetY(entity.getFace()),
            dwz / 2.0 + oc.connectLineOffsetZ(entity.getFace()));
        entity.getConnectLine().teleport(mid);
        entity.getConnectLine().setInterpolationDelay(0);
        entity.getConnectLine().setInterpolationDuration(2);
        entity.getConnectLine().setTransformation(tf);
    }

    private static void renderFlickNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance, double bpm) {
        OffsetConfig oc = OffsetConfig.get();
        Material material = entity.getType().isFake() ? getMaterialForNoteType(entity.getType()) : Material.WHITE_STAINED_GLASS;

        Location location = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            oc.flickX(entity.getFace()), oc.flickY,
            distance, centerX, centerY, centerZ);

        float bs = (float) oc.flickBlockSize;
        float bd = (float) oc.flickBlockDepth;
        float cxy = (float) oc.flickCenterXY;
        float cz  = (float) oc.flickCenterZ;

        entity.setBlockDisplay(world.spawn(location, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setInterpolationDuration(100);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTransformation(new Transformation(
                new Vector3f(cxy, cxy, cz),
                new AxisAngle4f(),
                new Vector3f(bs, bs, bd),
                new AxisAngle4f()));
        }));
        entity.getBlockDisplay().setGlowing(true);

        String arrow = entity.getTurn() != null && entity.getTurn().equalsIgnoreCase("left") ? "←" : "→";
        float as = (float) oc.flickArrowScale;

        TextDisplay arrowDisplay = world.spawn(location, TextDisplay.class, e -> {
            e.setText("§f" + arrow);
            e.setInterpolationDuration(100);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setBillboard(Display.Billboard.CENTER);
            e.setTransformation(new Transformation(
                new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(as, as, 0.0f), new AxisAngle4f()));
            e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            e.setViewRange(100.0f);
        });
        entity.getTextDisplays().add(arrowDisplay);
    }

    public static void updateNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                   double speed, double distance, double bpm, @Nullable EvalResult evalResult) {
        if (entity.getType() == NoteType.FLICK || entity.getType() == NoteType.FAKE_FLICK) {
            if (entity.getBlockDisplay() == null) return;
            updateFlickNote(entity, world, centerX, centerY, centerZ, distance, speed, bpm, evalResult);
            return;
        }
        if (entity.getBlockDisplay() == null) return;
        if (entity.getType() != NoteType.HOLD && !entity.getType().isFake() && entity.getInteraction() == null) return;
        if (entity.getType() == NoteType.DOUBLE || entity.getType() == NoteType.FAKE_DOUBLE) {
            updateDoubleNote(entity, world, centerX, centerY, centerZ, speed, distance, bpm, evalResult);
            return;
        }
        if (entity.getPosition() == null) return;

        OffsetConfig oc = OffsetConfig.get();

        double eventX = (evalResult != null) ? evalResult.getX() : 0;
        double eventY = (evalResult != null) ? evalResult.getY() : 0;

        double adjustedX = entity.getPosition().getX() + oc.noteX(entity.getFace()) + eventX;
        double adjustedY = entity.getPosition().getY() + oc.noteY(entity.getFace()) + eventY;

        Location location = CoordinateSystem.transformCoordinates(
            world, entity.getFace(), adjustedX, adjustedY, distance, centerX, centerY, centerZ);
        Location nextLocation = CoordinateSystem.transformCoordinates(
            world, entity.getFace(), adjustedX, adjustedY, distance - speed, centerX, centerY, centerZ);

        entity.getBlockDisplay().teleport(location);
        if (entity.getType() != NoteType.HOLD && entity.getType() != NoteType.FAKE_HOLD && entity.getInteraction() != null)
            entity.getInteraction().teleport(location);

        float scale = 1.0f;
        boolean isHold = entity.getType() == NoteType.HOLD || entity.getType() == NoteType.FAKE_HOLD;
        float scaleZ = isHold
            ? (float) ((60.0 / bpm) * speed * oc.holdScaleZFactor)
            : (float) (oc.scaleZFactor * speed);

        float evScaleX = (evalResult != null) ? (float) evalResult.getScaleX() : 1.0f;
        float evScaleY = (evalResult != null) ? (float) evalResult.getScaleY() : 1.0f;
        float evScaleZ = (evalResult != null) ? (float) evalResult.getScaleZ() : 1.0f;
        float evRotate = (evalResult != null) ? (float) Math.toRadians(evalResult.getRotate()) : 0f;

        float ndx = (float)(nextLocation.getX() - location.getX());
        float ndy = (float)(nextLocation.getY() - location.getY());
        float ndz = (float)(nextLocation.getZ() - location.getZ());

        float finalScaleX = scale * evScaleX;
        float finalScaleY = scale * evScaleY;
        float finalScaleZ = scaleZ * evScaleZ;

        Vector3f translation = new Vector3f(ndx, ndy, ndz);
        AxisAngle4f rot;
        if (evRotate != 0f) {
            rot = new AxisAngle4f(evRotate, ndx, ndy, ndz);
            Vector3f center = new Vector3f(finalScaleX / 2f, finalScaleY / 2f, finalScaleZ / 2f);
            Vector3f rotatedCenter = new Vector3f(center);
            new Quaternionf().set(rot).transform(rotatedCenter);
            translation.add(center).sub(rotatedCenter);
        } else {
            rot = new AxisAngle4f();
        }

        Transformation transformation = new Transformation(
            translation, rot, new Vector3f(finalScaleX, finalScaleY, finalScaleZ), new AxisAngle4f());
        entity.getBlockDisplay().setInterpolationDelay(0);
        entity.getBlockDisplay().setInterpolationDuration(2);
        entity.getBlockDisplay().setTransformation(transformation);

        // alpha
        if (evalResult != null && evalResult.getAlpha() < 1.0) {
            entity.getBlockDisplay().setViewRange((float) (evalResult.getAlpha() * 100.0));
        }

        // color
        if (evalResult != null && evalResult.hasColorOverride() && entity.isGlowing()) {
            entity.getBlockDisplay().setGlowColorOverride(
                org.bukkit.Color.fromARGB(
                    (int) evalResult.getColorA(), (int) evalResult.getColorR(),
                    (int) evalResult.getColorG(), (int) evalResult.getColorB()));
        }

        // material
        if (evalResult != null && evalResult.getMaterial() != null) {
            try {
                Material mat = Material.valueOf(evalResult.getMaterial().toUpperCase());
                entity.getBlockDisplay().setBlock(mat.createBlockData());
            } catch (IllegalArgumentException ignored) {}
        }

        updateCursorDisplay(entity, world, centerX, centerY, centerZ, distance);
    }

    private static void updateDoubleNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                          double speed, double distance, double bpm, @Nullable EvalResult evalResult) {
        if (entity.getPositions() == null || entity.getPositions().isEmpty()) return;

        OffsetConfig oc = OffsetConfig.get();
        float scaleZ = (float) (oc.scaleZFactor * speed);

        double eventX = (evalResult != null) ? evalResult.getX() : 0;
        double eventY = (evalResult != null) ? evalResult.getY() : 0;
        float evScaleX = (evalResult != null) ? (float) evalResult.getScaleX() : 1.0f;
        float evScaleY = (evalResult != null) ? (float) evalResult.getScaleY() : 1.0f;
        float evScaleZ = (evalResult != null) ? (float) evalResult.getScaleZ() : 1.0f;
        float evRotate = (evalResult != null) ? (float) Math.toRadians(evalResult.getRotate()) : 0f;

        float finalScaleX = evScaleX;
        float finalScaleY = evScaleY;
        float finalScaleZ = scaleZ * evScaleZ;

        NotePosition firstPos = entity.getPositions().get(0);
        double fx = firstPos.getX() + oc.noteX(entity.getFace()) + eventX;
        double fy = firstPos.getY() + oc.noteY(entity.getFace()) + eventY;
        Location location1 = CoordinateSystem.transformCoordinates(world, entity.getFace(), fx, fy, distance, centerX, centerY, centerZ);
        Location nextLoc1  = CoordinateSystem.transformCoordinates(world, entity.getFace(), fx, fy, distance - speed, centerX, centerY, centerZ);

        entity.getBlockDisplay().teleport(location1);
        if (entity.getInteraction() != null) entity.getInteraction().teleport(location1);

        float ndx1 = (float)(nextLoc1.getX()-location1.getX());
        float ndy1 = (float)(nextLoc1.getY()-location1.getY());
        float ndz1 = (float)(nextLoc1.getZ()-location1.getZ());

        Vector3f trans1 = new Vector3f(ndx1, ndy1, ndz1);
        AxisAngle4f rot1;
        if (evRotate != 0f) {
            rot1 = new AxisAngle4f(evRotate, ndx1, ndy1, ndz1);
            Vector3f center1 = new Vector3f(finalScaleX / 2f, finalScaleY / 2f, finalScaleZ / 2f);
            Vector3f rc1 = new Vector3f(center1);
            new Quaternionf().set(rot1).transform(rc1);
            trans1.add(center1).sub(rc1);
        } else {
            rot1 = new AxisAngle4f();
        }

        Transformation t1 = new Transformation(trans1, rot1, new Vector3f(finalScaleX, finalScaleY, finalScaleZ), new AxisAngle4f());
        entity.getBlockDisplay().setInterpolationDelay(0);
        entity.getBlockDisplay().setInterpolationDuration(2);
        entity.getBlockDisplay().setTransformation(t1);

        if (entity.getPositions().size() > 1 &&
            !entity.getAdditionalBlockDisplays().isEmpty() &&
            !entity.getAdditionalInteractions().isEmpty()) {

            NotePosition secondPos = entity.getPositions().get(1);
            double sx = secondPos.getX() + oc.noteX(entity.getFace()) + eventX;
            double sy = secondPos.getY() + oc.noteY(entity.getFace()) + eventY;
            Location location2 = CoordinateSystem.transformCoordinates(world, entity.getFace(), sx, sy, distance, centerX, centerY, centerZ);
            Location nextLoc2  = CoordinateSystem.transformCoordinates(world, entity.getFace(), sx, sy, distance - speed, centerX, centerY, centerZ);

            BlockDisplay secondBlock = entity.getAdditionalBlockDisplays().get(0);
            secondBlock.teleport(location2);

            float ndx2 = (float)(nextLoc2.getX()-location2.getX());
            float ndy2 = (float)(nextLoc2.getY()-location2.getY());
            float ndz2 = (float)(nextLoc2.getZ()-location2.getZ());

            Vector3f trans2 = new Vector3f(ndx2, ndy2, ndz2);
            AxisAngle4f rot2;
            if (evRotate != 0f) {
                rot2 = new AxisAngle4f(evRotate, ndx2, ndy2, ndz2);
                Vector3f center2 = new Vector3f(finalScaleX / 2f, finalScaleY / 2f, finalScaleZ / 2f);
                Vector3f rc2 = new Vector3f(center2);
                new Quaternionf().set(rot2).transform(rc2);
                trans2.add(center2).sub(rc2);
            } else {
                rot2 = new AxisAngle4f();
            }

            Transformation t2 = new Transformation(trans2, rot2, new Vector3f(finalScaleX, finalScaleY, finalScaleZ), new AxisAngle4f());
            secondBlock.setInterpolationDelay(0);
            secondBlock.setInterpolationDuration(2);
            secondBlock.setTransformation(t2);

            entity.getAdditionalInteractions().get(0).teleport(location2);
            updateDoubleLine(entity, nextLoc1, nextLoc2);
        }

        if (evalResult != null && evalResult.getAlpha() < 1.0) {
            float viewRange = (float) (evalResult.getAlpha() * 100.0);
            entity.getBlockDisplay().setViewRange(viewRange);
            for (BlockDisplay b : entity.getAdditionalBlockDisplays()) b.setViewRange(viewRange);
        }

        if (evalResult != null && evalResult.hasColorOverride() && entity.isGlowing()) {
            org.bukkit.Color color = org.bukkit.Color.fromARGB(
                (int) evalResult.getColorA(), (int) evalResult.getColorR(),
                (int) evalResult.getColorG(), (int) evalResult.getColorB());
            entity.getBlockDisplay().setGlowColorOverride(color);
            for (BlockDisplay b : entity.getAdditionalBlockDisplays()) b.setGlowColorOverride(color);
        }

        if (evalResult != null && evalResult.getMaterial() != null) {
            try {
                Material mat = Material.valueOf(evalResult.getMaterial().toUpperCase());
                entity.getBlockDisplay().setBlock(mat.createBlockData());
                for (BlockDisplay b : entity.getAdditionalBlockDisplays()) b.setBlock(mat.createBlockData());
            } catch (IllegalArgumentException ignored) {}
        }

        updateDoubleCursorDisplays(entity, world, centerX, centerY, centerZ, distance);

    }

    private static void updateFlickNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance, double speed, double bpm, @Nullable EvalResult evalResult) {
        OffsetConfig oc = OffsetConfig.get();

        double eventX = (evalResult != null) ? evalResult.getX() : 0;
        double eventY = (evalResult != null) ? evalResult.getY() : 0;

        double flickX = oc.flickX(entity.getFace()) + eventX;
        double flickY = oc.flickY + eventY;

        Location location = CoordinateSystem.transformCoordinates(world, entity.getFace(), flickX, flickY, distance, centerX, centerY, centerZ);
        Location nextLocation = CoordinateSystem.transformCoordinates(world, entity.getFace(), flickX, flickY, distance - speed, centerX, centerY, centerZ);

        entity.getBlockDisplay().teleport(location);

        float ndx = (float)(nextLocation.getX() - location.getX());
        float ndy = (float)(nextLocation.getY() - location.getY());
        float ndz = (float)(nextLocation.getZ() - location.getZ());
        float cxy = (float) oc.flickCenterXY;
        float cz  = (float) oc.flickCenterZ;
        float bs  = (float) oc.flickBlockSize;
        float bd  = (float) oc.flickBlockDepth;

        float evScaleX = (evalResult != null) ? (float) evalResult.getScaleX() : 1.0f;
        float evScaleY = (evalResult != null) ? (float) evalResult.getScaleY() : 1.0f;
        float evScaleZ = (evalResult != null) ? (float) evalResult.getScaleZ() : 1.0f;
        float evRotate = (evalResult != null) ? (float) Math.toRadians(evalResult.getRotate()) : 0f;

        float finalScaleX = bs * evScaleX;
        float finalScaleY = bs * evScaleY;
        float finalScaleZ = bd * evScaleZ;

        Vector3f translation = new Vector3f(cxy + ndx, cxy + ndy, cz + ndz);
        AxisAngle4f rot;
        if (evRotate != 0f) {
            rot = new AxisAngle4f(evRotate, ndx, ndy, ndz);
            Vector3f center = new Vector3f(finalScaleX / 2f, finalScaleY / 2f, finalScaleZ / 2f);
            Vector3f rotatedCenter = new Vector3f(center);
            new Quaternionf().set(rot).transform(rotatedCenter);
            translation.add(center).sub(rotatedCenter);
        } else {
            rot = new AxisAngle4f();
        }

        Transformation transformation = new Transformation(
            translation, rot, new Vector3f(finalScaleX, finalScaleY, finalScaleZ), new AxisAngle4f());
        entity.getBlockDisplay().setInterpolationDelay(0);
        entity.getBlockDisplay().setInterpolationDuration(2);
        entity.getBlockDisplay().setTransformation(transformation);

        if (!entity.getTextDisplays().isEmpty()) {
            entity.getTextDisplays().get(0).teleport(location.clone().add(
                oc.flickArrowX(entity.getFace()),
                oc.flickArrowY(entity.getFace()),
                oc.flickArrowZ(entity.getFace())));
        }

        if (evalResult != null && evalResult.getAlpha() < 1.0) {
            float viewRange = (float) (evalResult.getAlpha() * 100.0);
            entity.getBlockDisplay().setViewRange(viewRange);
            for (TextDisplay td : entity.getTextDisplays()) {
                if (td != null && !td.isDead()) td.setViewRange(viewRange);
            }
        }

        if (evalResult != null && evalResult.hasColorOverride() && entity.isGlowing()) {
            entity.getBlockDisplay().setGlowColorOverride(
                org.bukkit.Color.fromARGB(
                    (int) evalResult.getColorA(), (int) evalResult.getColorR(),
                    (int) evalResult.getColorG(), (int) evalResult.getColorB()));
        }

        if (evalResult != null && evalResult.getMaterial() != null) {
            try {
                Material mat = Material.valueOf(evalResult.getMaterial().toUpperCase());
                entity.getBlockDisplay().setBlock(mat.createBlockData());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static float calculateScale(double distance) {
        OffsetConfig oc = OffsetConfig.get();
        if (distance >= oc.scaleStartDist) return (float) oc.scaleMin;
        if (distance <= oc.scaleEndDist)   return (float) oc.scaleMax;
        double progress = (oc.scaleStartDist - distance) / (oc.scaleStartDist - oc.scaleEndDist);
        return (float) (oc.scaleMin + (oc.scaleMax - oc.scaleMin) * progress);
    }

    private static void updateCursorDisplay(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance) {
        if (entity.getTextDisplays().isEmpty()) return;
        TextDisplay cursorDisplay = entity.getTextDisplays().get(0);
        if (cursorDisplay == null || cursorDisplay.isDead()) return;

        OffsetConfig oc = OffsetConfig.get();
        if (distance >= oc.cursorHideDist) {
            cursorDisplay.setTextOpacity((byte) 0);
            return;
        }

        int alpha = (int) (oc.cursorAlphaBase - (distance - oc.cursorAlphaDistRef) * oc.cursorAlphaFactor);
        alpha = Math.max(1, Math.min(100, alpha));
        float cursorScale = (float) (alpha / oc.cursorScaleDivisor);

        String color = switch (entity.getType()) {
            case TAP -> "§3"; case DRAG -> "§e"; case DOUBLE -> "§6"; default -> "§f";
        };
        String text = alpha <= (int) oc.cursorAlphaLow ? "§f" : "§f" + color + "█";
        cursorDisplay.setText(text);

        int opacity = Math.min(255, (int) (alpha * oc.cursorOpacityMult));
        cursorDisplay.setTextOpacity((byte) opacity);

        float cx = (float) oc.cursorTransXFactor;
        float cy = (float) oc.cursorTransYFactor;
        cursorDisplay.setTransformation(new Transformation(
            new Vector3f(-cursorScale * cx, -cursorScale * cy, 0),
            new AxisAngle4f(),
            new Vector3f(cursorScale, cursorScale, cursorScale),
            new AxisAngle4f()));

        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            entity.getPosition().getX() + entity.getCursorOffsetX(),
            entity.getPosition().getY() + oc.cursorY(entity.getFace()) + entity.getCursorOffsetY(),
            oc.cursorZ(entity.getFace()),
            centerX, centerY, centerZ);
        cursorLocation.setYaw(faceYaw(entity.getFace()));
        cursorDisplay.teleport(cursorLocation);
    }

    private static void updateDoubleCursorDisplays(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance) {
        if (entity.getTextDisplays().size() < 2) return;
        if (entity.getPositions() == null || entity.getPositions().size() < 2) return;

        OffsetConfig oc = OffsetConfig.get();
        if (distance >= oc.cursorHideDist) {
            for (TextDisplay cursor : entity.getTextDisplays()) {
                if (cursor != null && !cursor.isDead()) cursor.setTextOpacity((byte) 0);
            }
            return;
        }

        int alpha = (int) (oc.cursorAlphaBase - (distance - oc.cursorAlphaDistRef) * oc.cursorAlphaFactor);
        alpha = Math.max(1, Math.min(100, alpha));
        float cursorScale = (float) (alpha / oc.cursorScaleDivisor);
        String text = alpha <= (int) oc.cursorAlphaLow ? "§f" : "§f§6█";
        int opacity = Math.min(255, (int) (alpha * oc.cursorOpacityMult));

        float cx = (float) oc.cursorTransXFactor;
        float cy = (float) oc.cursorTransYFactor;
        Transformation transformation = new Transformation(
            new Vector3f(-cursorScale * cx, -cursorScale * cy, 0),
            new AxisAngle4f(),
            new Vector3f(cursorScale, cursorScale, cursorScale),
            new AxisAngle4f());

        for (int i = 0; i < Math.min(2, entity.getTextDisplays().size()); i++) {
            TextDisplay cursorDisplay = entity.getTextDisplays().get(i);
            if (cursorDisplay == null || cursorDisplay.isDead()) continue;

            NotePosition pos = entity.getPositions().get(i);
            Location cursorLocation = CoordinateSystem.transformCoordinates(
                world, entity.getFace(),
                pos.getX() + entity.getCursorOffsetX(),
                pos.getY() + oc.cursorY(entity.getFace()) + entity.getCursorOffsetY(),
                oc.cursorZ(entity.getFace()),
                centerX, centerY, centerZ);
            cursorLocation.setYaw(faceYaw(entity.getFace()));

            cursorDisplay.setText(text);
            cursorDisplay.setTextOpacity((byte) opacity);
            cursorDisplay.setTransformation(transformation);
            cursorDisplay.teleport(cursorLocation);
        }
    }

    private static void createCursorDisplay(NoteEntity entity, World world, double centerX, double centerY, double centerZ) {
        OffsetConfig oc = OffsetConfig.get();
        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            entity.getPosition().getX() + entity.getCursorOffsetX(),
            entity.getPosition().getY() + oc.cursorY(entity.getFace()) + entity.getCursorOffsetY(),
            oc.cursorZ(entity.getFace()),
            centerX, centerY, centerZ);
        cursorLocation.setYaw(faceYaw(entity.getFace()));

        TextDisplay cursorDisplay = world.spawn(cursorLocation, TextDisplay.class, display -> {
            display.setText("§f");
            display.setInterpolationDuration(0);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f()));
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setViewRange(100.0f);
        });
        entity.getTextDisplays().add(cursorDisplay);
    }

    private static void createCursorDisplayAtPosition(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                                       NotePosition position) {
        OffsetConfig oc = OffsetConfig.get();
        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            position.getX() + entity.getCursorOffsetX(),
            position.getY() + oc.cursorY(entity.getFace()) + entity.getCursorOffsetY(),
            oc.cursorZ(entity.getFace()),
            centerX, centerY, centerZ);
        cursorLocation.setYaw(faceYaw(entity.getFace()));

        TextDisplay cursorDisplay = world.spawn(cursorLocation, TextDisplay.class, display -> {
            display.setText("§f");
            display.setInterpolationDuration(0);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f()));
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setViewRange(100.0f);
        });
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
            case FAKE_TAP -> Material.LIGHT_BLUE_STAINED_GLASS;
            case FAKE_HOLD -> Material.WHITE_STAINED_GLASS;
            case FAKE_DRAG -> Material.YELLOW_STAINED_GLASS;
            case FAKE_FLICK -> Material.MAGENTA_STAINED_GLASS;
            case FAKE_DOUBLE -> Material.ORANGE_STAINED_GLASS;
        };
    }

    private static float faceYaw(Face face) {
        return switch (face) { case W -> 180f; case A -> 90f; case S -> 0f; case D -> 270f; };
    }
}
