package org.cubeRhythm.chart;

import lombok.Data;

@Data
public class ChartMetadata {
    private String id;
    private String title;
    private String artist;
    private String charter;
    private Difficulty difficulty;
    private String audio;
    private int duration;
    private int offset;
    private int bpm;

    @Data
    public static class Difficulty {
        private String name;
        private int level;
        private String color;
    }
}
