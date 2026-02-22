package org.cubeRhythm.coordinate;

import lombok.Getter;

@Getter
public enum Face {
    W(0, "white"),
    A(90, "yellow"),
    S(180, "orange"),
    D(270, "red");

    private final float yaw;
    private final String markerColor;

    Face(float yaw, String markerColor) {
        this.yaw = yaw;
        this.markerColor = markerColor;
    }

    public static Face fromString(String face) {
        return switch (face.toLowerCase()) {
            case "w" -> W;
            case "a" -> A;
            case "s" -> S;
            case "d" -> D;
            default -> throw new IllegalArgumentException("Invalid face: " + face);
        };
    }
}
