package org.cubeRhythm.note;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Transformation;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.entity.DisplayEntityFactory;
import org.cubeRhythm.manager.OffsetConfig;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class NoteRenderer {

    public static void renderNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                   double speed, double distance, float hitboxScale, double bpm) {
        if (entity.getType() == NoteType.FLICK) {
            renderFlickNote(entity, world, centerX, centerY, centerZ, distance, bpm);
            return;
        }
        if (entity.getType() == NoteType.DOUBLE) {
            renderDoubleNote(entity, world, centerX, centerY, centerZ, speed, distance, hitboxScale, bpm);
            return;
        }
        if (entity.getPosition() == null) {
            org.cubeRhythm.Main.instance.getLogger().warning(
                "音符没有 position: type=" + entity.getType() + ", time=" + entity.getTime());
            return;
        }

        OffsetConfig oc = OffsetConfig.get();
        Material material = getMaterialForNoteType(entity.getType());
        float scaleZ = entity.getType() == NoteType.HOLD
            ? (float) ((60.0 / bpm) * speed * oc.holdScaleZFactor)
            : (float) (oc.scaleZFactor * speed);

        if (entity.getType() == NoteType.HOLD) entity.setHoldScaleZ(scaleZ);

        double adjustedX = entity.getPosition().getX() + oc.noteX(entity.getFace());
        double adjustedY = entity.getPosition().getY() + oc.noteY(entity.getFace());

        Location location = CoordinateSystem.transformCoordinates(
            world, entity.getFace(), adjustedX, adjustedY, distance, centerX, centerY, centerZ);

        entity.setBlockDisplay(DisplayEntityFactory.createBlockDisplay(
            world, location, material, 1.0f, 1.0f, scaleZ, 100));

        if (entity.getType() == NoteType.HOLD &&
                (entity.getFace() == Face.A || entity.getFace() == Face.D)) {
            AxisAngle4f rot = new AxisAngle4f(
                (float)(Math.PI / 2) * (entity.getFace() == Face.A ? 1 : -1), 0, 1, 0);
            Transformation t = entity.getBlockDisplay().getTransformation();
            t.getLeftRotation().set(rot);
            entity.getBlockDisplay().setTransformation(t);
        }

        if (entity.getType() != NoteType.HOLD) {
            entity.setInteraction(DisplayEntityFactory.createInteraction(
                world, location, hitboxScale, hitboxScale));
        }

        if (entity.isGlowing()) entity.getBlockDisplay().setGlowing(true);

        if (entity.getType() != NoteType.HOLD) createCursorDisplay(entity, world, centerX, centerY, centerZ);
    }

    private static void renderDoubleNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                          double speed, double distance, float hitboxScale, double bpm) {
        if (entity.getPositions() == null || entity.getPositions().isEmpty()) {
            org.cubeRhythm.Main.instance.getLogger().warning("DOUBLE 音符没有 positions");
            return;
        }

        OffsetConfig oc = OffsetConfig.get();
        Material material = Material.ORANGE_CONCRETE;
        float scaleZ = (float) (oc.scaleZFactor * speed);

        NotePosition firstPos = entity.getPositions().get(0);
        Location location1 = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            firstPos.getX() + oc.noteX(entity.getFace()),
            firstPos.getY() + oc.noteY(entity.getFace()),
            distance, centerX, centerY, centerZ);

        entity.setBlockDisplay(DisplayEntityFactory.createBlockDisplay(
            world, location1, material, 1.0f, 1.0f, scaleZ, 100));
        entity.setInteraction(DisplayEntityFactory.createInteraction(
            world, location1, hitboxScale, hitboxScale));

        if (entity.getPositions().size() > 1) {
            NotePosition secondPos = entity.getPositions().get(1);
            Location location2 = CoordinateSystem.transformCoordinates(
                world, entity.getFace(),
                secondPos.getX() + oc.noteX(entity.getFace()),
                secondPos.getY() + oc.noteY(entity.getFace()),
                distance, centerX, centerY, centerZ);

            org.bukkit.entity.BlockDisplay secondBlock = DisplayEntityFactory.createBlockDisplay(
                world, location2, material, 1.0f, 1.0f, scaleZ, 100);
            entity.getAdditionalBlockDisplays().add(secondBlock);

            org.bukkit.entity.Interaction secondInteraction = DisplayEntityFactory.createInteraction(
                world, location2, hitboxScale, hitboxScale);
            entity.getAdditionalInteractions().add(secondInteraction);
        }

        if (entity.isGlowing()) {
            entity.getBlockDisplay().setGlowing(true);
            for (org.bukkit.entity.BlockDisplay b : entity.getAdditionalBlockDisplays()) b.setGlowing(true);
        }

        for (NotePosition pos : entity.getPositions()) {
            createCursorDisplayAtPosition(entity, world, centerX, centerY, centerZ, pos);
        }

        if (entity.getPositions().size() > 1) {
            NotePosition p1 = entity.getPositions().get(0);
            NotePosition p2 = entity.getPositions().get(1);
            double x1 = p1.getX() + oc.noteX(entity.getFace());
            double y1 = p1.getY() + oc.noteY(entity.getFace());
            double x2 = p2.getX() + oc.noteX(entity.getFace());
            double y2 = p2.getY() + oc.noteY(entity.getFace());
            double mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
            double lineLen = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));

            Location wLoc1 = CoordinateSystem.transformCoordinates(world, entity.getFace(), x1, y1, distance, centerX, centerY, centerZ);
            Location wLoc2 = CoordinateSystem.transformCoordinates(world, entity.getFace(), x2, y2, distance, centerX, centerY, centerZ);
            double dwx = wLoc2.getX() - wLoc1.getX();
            double dwy = wLoc2.getY() - wLoc1.getY();
            double dwz = wLoc2.getZ() - wLoc1.getZ();

            Location midLoc = CoordinateSystem.transformCoordinates(
                world, entity.getFace(), mx, my, distance, centerX, centerY, centerZ);
            midLoc.add(oc.connectLineOffsetX(entity.getFace()), oc.connectLineOffsetY(entity.getFace()), oc.connectLineOffsetZ(entity.getFace()));
            float lw = (float) oc.connectLineWidth;
            float lo = (float) oc.connectLineTransOffset;
            org.bukkit.entity.BlockDisplay line = DisplayEntityFactory.createBlockDisplay(
                world, midLoc, Material.WHITE_CONCRETE, lw, (float) lineLen, lw, 100);
            line.setGlowing(true);

            org.bukkit.util.Transformation lt = line.getTransformation();
            switch (entity.getFace()) {
                case W -> {
                    lt.getLeftRotation().set(new AxisAngle4f((float) Math.atan2(dwx, -dwy), 0, 0, 1));
                    lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
                }
                case A -> {
                    float angle = (float) Math.atan2(dwz, dwy);
                    lt.getLeftRotation().set(angle, 1, 0, 0);
                    lt.getTranslation().set(-lo,
                            (float) (-lineLen / 2 * Math.cos(angle)),
                            (float) (-lo - lineLen / 2 * Math.sin(angle)));
                }
                case S -> {
                    lt.getLeftRotation().set(new AxisAngle4f((float) Math.atan2(dwx, dwy), 0, 0, 1));
                    lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
                }
                case D -> {
                    float angle = (float) -Math.atan2(dwz, dwy);
                    lt.getLeftRotation().set(angle, 1, 0, 0);
//                    lt.getTranslation().set(-lo,
//                            (float) (-lineLen / 2 * Math.cos(angle)),
//                            (float) (-lo - lineLen / 2 * Math.sin(angle)));
                    lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
                }
                default -> {}
            }

//            if (entity.getFace() == Face.W || entity.getFace() == Face.S) {
//                float angle = (float) Math.atan2(dwx, dwy);
//                lt.getLeftRotation().set(new AxisAngle4f(angle, 0, 0, 1));
//                lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
//            } else {
//                // 由于A/D面垂直于X方向，而生成的连接线默认指向X方向，所以先旋转90°
//                float angle = (float) Math.atan2(dwz, dwy);
//                Quaternionf rotY = new Quaternionf().rotateY((float) Math.PI / 2);
//                Quaternionf rotX = new Quaternionf().rotateX(angle);
//                rotX.mul(rotY, new Quaternionf());
//                lt.getLeftRotation().set(new AxisAngle4f(rotX));
////                lt.getLeftRotation().set(new AxisAngle4f(angle, 1, 0, 0));
//                lt.getTranslation().set(-lo,
//                    (float) (-lineLen / 2 * Math.cos(angle)),
//                    (float) (-lo - lineLen / 2 * Math.sin(angle)));
//            }
            line.setTransformation(lt);
            entity.setConnectLine(line);
        }
    }

    private static void renderFlickNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance, double bpm) {
        OffsetConfig oc = OffsetConfig.get();
        Material material = Material.WHITE_STAINED_GLASS;

        Location location = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            oc.flickX(entity.getFace()), oc.flickY,
            distance, centerX, centerY, centerZ);

        float bs = (float) oc.flickBlockSize;
        float bd = (float) oc.flickBlockDepth;
        float cxy = (float) oc.flickCenterXY;
        float cz  = (float) oc.flickCenterZ;

        entity.setBlockDisplay(world.spawn(location, org.bukkit.entity.BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setInterpolationDuration(100);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setTransformation(new Transformation(
                new Vector3f(cxy, cxy, cz),
                new AxisAngle4f(),
                new Vector3f(bs, bs, bd),
                new AxisAngle4f()));
        }));
        entity.getBlockDisplay().setGlowing(true);

        String arrow = entity.getTurn() != null && entity.getTurn().equalsIgnoreCase("left") ? "←" : "→";
        float as = (float) oc.flickArrowScale;

        org.bukkit.entity.TextDisplay arrowDisplay = world.spawn(location, org.bukkit.entity.TextDisplay.class, e -> {
            e.setText("§f" + arrow);
            e.setInterpolationDuration(100);
            e.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            e.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            e.setTransformation(new Transformation(
                new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(as, as, 0.0f), new AxisAngle4f()));
            e.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            e.setViewRange(100.0f);
        });
        entity.getTextDisplays().add(arrowDisplay);
    }

    public static void updateNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                   double speed, double distance, double bpm) {
        if (entity.getType() == NoteType.FLICK) {
            if (entity.getBlockDisplay() == null) return;
            updateFlickNote(entity, world, centerX, centerY, centerZ, distance, speed, bpm);
            return;
        }
        if (entity.getBlockDisplay() == null) return;
        if (entity.getType() != NoteType.HOLD && entity.getInteraction() == null) return;
        if (entity.getType() == NoteType.DOUBLE) {
            updateDoubleNote(entity, world, centerX, centerY, centerZ, speed, distance, bpm);
            return;
        }
        if (entity.getPosition() == null) return;

        OffsetConfig oc = OffsetConfig.get();
        double adjustedX = entity.getPosition().getX() + oc.noteX(entity.getFace()) + entity.getXOffset();
        double adjustedY = entity.getPosition().getY() + oc.noteY(entity.getFace()) + entity.getYOffset();

        Location location = CoordinateSystem.transformCoordinates(
            world, entity.getFace(), adjustedX, adjustedY, distance, centerX, centerY, centerZ);
        Location nextLocation = CoordinateSystem.transformCoordinates(
            world, entity.getFace(), adjustedX, adjustedY, distance - speed, centerX, centerY, centerZ);

        entity.getBlockDisplay().teleport(location);
        if (entity.getType() != NoteType.HOLD && entity.getInteraction() != null)
            entity.getInteraction().teleport(location);

        float scale = calculateScale(distance);
        float scaleZ = entity.getType() == NoteType.HOLD
            ? (float) ((60.0 / bpm) * speed * oc.holdScaleZFactor)
            : (float) (oc.scaleZFactor * speed);

        float ndx = (float)(nextLocation.getX() - location.getX());
        float ndy = (float)(nextLocation.getY() - location.getY());
        float ndz = (float)(nextLocation.getZ() - location.getZ());
        boolean isHoldAD = entity.getType() == NoteType.HOLD &&
            (entity.getFace() == Face.A || entity.getFace() == Face.D);
        // A/D面移动方向是X轴，绕Y轴旋转90度使scaleZ对齐X轴
        // A面：X+方向延伸（远离判定线），旋转-90度；D面：X-方向，旋转+90度
//        AxisAngle4f rot = isHoldAD
//            ? new AxisAngle4f((float)(Math.PI / 2) * (entity.getFace() == Face.A ? -1 : 1), 0, 1, 0)
//            : new AxisAngle4f();
        AxisAngle4f rot = new AxisAngle4f();
        Transformation transformation = new Transformation(
            new Vector3f(ndx, ndy, ndz),
            rot,
            new Vector3f(scale, scale, scaleZ),
            new AxisAngle4f());
        entity.getBlockDisplay().setInterpolationDelay(0);
        entity.getBlockDisplay().setInterpolationDuration(1);
        entity.getBlockDisplay().setTransformation(transformation);

        updateCursorDisplay(entity, world, centerX, centerY, centerZ, distance);
    }

    private static void updateDoubleNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                          double speed, double distance, double bpm) {
        if (entity.getPositions() == null || entity.getPositions().isEmpty()) return;

        OffsetConfig oc = OffsetConfig.get();
        float scale = calculateScale(distance);
        float scaleZ = (float) (oc.scaleZFactor * speed);

        NotePosition firstPos = entity.getPositions().get(0);
        double fx = firstPos.getX() + oc.noteX(entity.getFace());
        double fy = firstPos.getY() + oc.noteY(entity.getFace());
        Location location1 = CoordinateSystem.transformCoordinates(world, entity.getFace(), fx, fy, distance, centerX, centerY, centerZ);
        Location nextLoc1  = CoordinateSystem.transformCoordinates(world, entity.getFace(), fx, fy, distance - speed, centerX, centerY, centerZ);

        entity.getBlockDisplay().teleport(location1);
        entity.getInteraction().teleport(location1);

        Transformation t1 = new Transformation(
            new Vector3f((float)(nextLoc1.getX()-location1.getX()), (float)(nextLoc1.getY()-location1.getY()), (float)(nextLoc1.getZ()-location1.getZ())),
            new AxisAngle4f(), new Vector3f(scale, scale, scaleZ), new AxisAngle4f());
        entity.getBlockDisplay().setInterpolationDelay(0);
        entity.getBlockDisplay().setInterpolationDuration(1);
        entity.getBlockDisplay().setTransformation(t1);

        if (entity.getPositions().size() > 1 &&
            !entity.getAdditionalBlockDisplays().isEmpty() &&
            !entity.getAdditionalInteractions().isEmpty()) {

            NotePosition secondPos = entity.getPositions().get(1);
            double sx = secondPos.getX() + oc.noteX(entity.getFace());
            double sy = secondPos.getY() + oc.noteY(entity.getFace());
            Location location2 = CoordinateSystem.transformCoordinates(world, entity.getFace(), sx, sy, distance, centerX, centerY, centerZ);
            Location nextLoc2  = CoordinateSystem.transformCoordinates(world, entity.getFace(), sx, sy, distance - speed, centerX, centerY, centerZ);

            org.bukkit.entity.BlockDisplay secondBlock = entity.getAdditionalBlockDisplays().get(0);
            secondBlock.teleport(location2);
            Transformation t2 = new Transformation(
                new Vector3f((float)(nextLoc2.getX()-location2.getX()), (float)(nextLoc2.getY()-location2.getY()), (float)(nextLoc2.getZ()-location2.getZ())),
                new AxisAngle4f(), new Vector3f(scale, scale, scaleZ), new AxisAngle4f());
            secondBlock.setInterpolationDelay(0);
            secondBlock.setInterpolationDuration(1);
            secondBlock.setTransformation(t2);

            entity.getAdditionalInteractions().get(0).teleport(location2);
        }

        updateDoubleCursorDisplays(entity, world, centerX, centerY, centerZ, distance);

        if (entity.getConnectLine() != null && !entity.getConnectLine().isDead() && entity.getPositions().size() > 1) {
            NotePosition p1 = entity.getPositions().get(0);
            NotePosition p2 = entity.getPositions().get(1);
            double x1 = p1.getX() + oc.noteX(entity.getFace());
            double y1 = p1.getY() + oc.noteY(entity.getFace());
            double x2 = p2.getX() + oc.noteX(entity.getFace());
            double y2 = p2.getY() + oc.noteY(entity.getFace());
            double mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
            double lineLen = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));

            Location wLoc1 = CoordinateSystem.transformCoordinates(world, entity.getFace(), x1, y1, distance, centerX, centerY, centerZ);
            Location wLoc2 = CoordinateSystem.transformCoordinates(world, entity.getFace(), x2, y2, distance, centerX, centerY, centerZ);
            double dwx = wLoc2.getX() - wLoc1.getX();
            double dwy = wLoc2.getY() - wLoc1.getY();
            double dwz = wLoc2.getZ() - wLoc1.getZ();

            Location midLoc = CoordinateSystem.transformCoordinates(world, entity.getFace(), mx, my, distance, centerX, centerY, centerZ);
            midLoc.add(oc.connectLineOffsetX(entity.getFace()), oc.connectLineOffsetY(entity.getFace()), oc.connectLineOffsetZ(entity.getFace()));
            entity.getConnectLine().teleport(midLoc);

            float lo = (float) oc.connectLineTransOffset;
            Transformation lt = entity.getConnectLine().getTransformation();
            switch (entity.getFace()) {
                case W -> {
                    lt.getLeftRotation().set(new AxisAngle4f((float) Math.atan2(dwx, -dwy), 0, 0, 1));
                    lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
                }
                case A -> {
                    float angle = (float) Math.atan2(dwz, dwy);
                    lt.getLeftRotation().set(angle, 1, 0, 0);
                    lt.getTranslation().set(-lo,
                            (float) (-lineLen / 2 * Math.cos(angle)),
                            (float) (-lo - lineLen / 2 * Math.sin(angle)));
                }
                case S -> {
                    lt.getLeftRotation().set(new AxisAngle4f((float) Math.atan2(dwx, dwy), 0, 0, 1));
                    lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
                }
                case D -> {
                    float angle = (float) -Math.atan2(dwz, dwy);
                    lt.getLeftRotation().set(angle, 1, 0, 0);
//                    lt.getTranslation().set(-lo,
//                            (float) (-lineLen / 2 * Math.cos(angle)),
//                            (float) (-lo - lineLen / 2 * Math.sin(angle)));
                    lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
                }
                default -> {}
            }
//            lt.getScale().set((float) oc.connectLineWidth, (float) lineLen, (float) oc.connectLineWidth);
//            if (entity.getFace() == Face.W || entity.getFace() == Face.S) {
//                lt.getLeftRotation().set(new AxisAngle4f((float) Math.atan2(dwx, dwy), 0, 0, 1));
//                lt.getTranslation().set(-lo, (float) (-lineLen / 2), -lo);
//            } else {
//                float angle = (float) Math.atan2(dwz, dwy);
//                lt.getLeftRotation().set(new AxisAngle4f(angle, 1, 0, 0));
//                lt.getTranslation().set(-lo,
//                    (float) (-lineLen / 2 * Math.cos(angle)),
//                    (float) (-lo - lineLen / 2 * Math.sin(angle)));
//            }
            entity.getConnectLine().setTransformation(lt);
        }
    }

    private static void updateFlickNote(NoteEntity entity, World world, double centerX, double centerY, double centerZ, double distance, double speed, double bpm) {
        OffsetConfig oc = OffsetConfig.get();
        Location location = CoordinateSystem.transformCoordinates(world, entity.getFace(), oc.flickX(entity.getFace()), oc.flickY, distance, centerX, centerY, centerZ);
        Location nextLocation = CoordinateSystem.transformCoordinates(world, entity.getFace(), oc.flickX(entity.getFace()), oc.flickY, distance - speed, centerX, centerY, centerZ);

        entity.getBlockDisplay().teleport(location);

        float ndx = (float)(nextLocation.getX() - location.getX());
        float ndy = (float)(nextLocation.getY() - location.getY());
        float ndz = (float)(nextLocation.getZ() - location.getZ());
        float cxy = (float) oc.flickCenterXY;
        float cz  = (float) oc.flickCenterZ;
        float bs  = (float) oc.flickBlockSize;
        float bd  = (float) oc.flickBlockDepth;

        Transformation transformation = new Transformation(
            new Vector3f(cxy + ndx, cxy + ndy, cz + ndz),
            new AxisAngle4f(),
            new Vector3f(bs, bs, bd),
            new AxisAngle4f());
        entity.getBlockDisplay().setInterpolationDelay(0);
        entity.getBlockDisplay().setInterpolationDuration(1);
        entity.getBlockDisplay().setTransformation(transformation);

        if (!entity.getTextDisplays().isEmpty()) {
            entity.getTextDisplays().get(0).teleport(location.clone().add(
                oc.flickArrowX(entity.getFace()),
                oc.flickArrowY(entity.getFace()),
                oc.flickArrowZ(entity.getFace())));
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
        org.bukkit.entity.TextDisplay cursorDisplay = entity.getTextDisplays().get(0);
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
            entity.getPosition().getX() + 0.0,
            entity.getPosition().getY() + oc.cursorY(entity.getFace()),
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
            for (org.bukkit.entity.TextDisplay cursor : entity.getTextDisplays()) {
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
            org.bukkit.entity.TextDisplay cursorDisplay = entity.getTextDisplays().get(i);
            if (cursorDisplay == null || cursorDisplay.isDead()) continue;

            NotePosition pos = entity.getPositions().get(i);
            Location cursorLocation = CoordinateSystem.transformCoordinates(
                world, entity.getFace(),
                pos.getX() + 0.0,
                pos.getY() + oc.cursorY(entity.getFace()),
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
            entity.getPosition().getX() + 0.0,
            entity.getPosition().getY() + oc.cursorY(entity.getFace()),
            oc.cursorZ(entity.getFace()),
            centerX, centerY, centerZ);
        cursorLocation.setYaw(faceYaw(entity.getFace()));

        org.bukkit.entity.TextDisplay cursorDisplay = world.spawn(cursorLocation, org.bukkit.entity.TextDisplay.class, display -> {
            display.setText("§f");
            display.setInterpolationDuration(0);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f()));
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setViewRange(100.0f);
        });
        entity.getTextDisplays().add(cursorDisplay);
    }

    private static void createCursorDisplayAtPosition(NoteEntity entity, World world, double centerX, double centerY, double centerZ,
                                                       NotePosition position) {
        OffsetConfig oc = OffsetConfig.get();
        Location cursorLocation = CoordinateSystem.transformCoordinates(
            world, entity.getFace(),
            position.getX() + 0.0,
            position.getY() + oc.cursorY(entity.getFace()),
            oc.cursorZ(entity.getFace()),
            centerX, centerY, centerZ);
        cursorLocation.setYaw(faceYaw(entity.getFace()));

        org.bukkit.entity.TextDisplay cursorDisplay = world.spawn(cursorLocation, org.bukkit.entity.TextDisplay.class, display -> {
            display.setText("§f");
            display.setInterpolationDuration(0);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f()));
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
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
        };
    }

    private static float faceYaw(Face face) {
        return switch (face) { case W -> 180f; case A -> 90f; case S -> 0f; case D -> 270f; };
    }
}
