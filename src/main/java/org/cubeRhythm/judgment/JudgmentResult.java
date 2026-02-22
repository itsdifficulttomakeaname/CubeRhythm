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
        // 非对称判定窗口：
        // 判定面前 80ms，判定面后 200ms
        // Early (负值): -80ms 到 0ms
        // Late (正值): 0ms 到 +200ms

        // 判定面前超过 80ms：不在判定范围内
        if (offsetMs < -200) {
            return null;  // 不处理，避免误触
        }

        // EXACT 窗口：-80ms 到 +80ms
        if (offsetMs <= 80) {
            return EXACT;
        }

        // JUST 窗口：+81ms 到 +200ms
        if (offsetMs <= 200) {
            return JUST;
        }

        // 判定面后超过 200ms：MISS
        return MISS;
    }
}
