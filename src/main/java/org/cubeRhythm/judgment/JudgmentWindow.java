package org.cubeRhythm.judgment;

import lombok.Data;

@Data
public class JudgmentWindow {
    private int exactWindow = 80;  // ±80ms
    private int justWindow = 200;  // ±200ms

    public JudgmentResult judge(int timingOffsetMs) {
        return JudgmentResult.fromTimingOffset(timingOffsetMs);
    }
}
