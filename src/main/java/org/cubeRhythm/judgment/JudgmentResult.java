package org.cubeRhythm.judgment;

import lombok.Getter;

@Getter
public enum JudgmentResult {
    EXACT(1.0, "§b§lEXACT", 80),
    JUST(0.7, "§e§lJUST", 200),
    MISS(0.0, "§c§lMISS", Integer.MAX_VALUE);

    private final double scoreMultiplier;
    private final String displayText;
    private final int windowMs;

    JudgmentResult(double scoreMultiplier, String displayText, int windowMs) {
        this.scoreMultiplier = scoreMultiplier;
        this.displayText = displayText;
        this.windowMs = windowMs;
    }

    public static JudgmentResult fromTimingOffset(int offsetMs) {
        int abs = Math.abs(offsetMs);
        if (abs <= 80) return EXACT;
        if (abs <= 200) return JUST;
        return null; // 超出 ±200ms，不处理（音符继续移动直到自动 MISS）
    }
}
