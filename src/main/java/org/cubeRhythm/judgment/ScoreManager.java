package org.cubeRhythm.judgment;

import lombok.Data;

@Data
public class ScoreManager {
    private int exactCount = 0;
    private int justCount = 0;
    private int earlyCount = 0;  // Track early hits
    private int lateCount = 0;   // Track late hits
    private int missCount = 0;
    private int combo = 0;
    private int maxCombo = 0;
    private double score = 0;

    private final int totalNotes;
    private final double scorePerNote;

    public ScoreManager(int totalNotes) {
        this.totalNotes = totalNotes;
        this.scorePerNote = totalNotes > 0 ? (1_000_000.0 + totalNotes) / totalNotes : 0;
    }

    /**
     * Record judgment and update score
     * @param result The judgment result
     * @param timingOffset The timing offset in milliseconds (negative = early, positive = late)
     */
    public void recordJudgment(JudgmentResult result, double timingOffset) {
        switch (result) {
            case EXACT -> {
                exactCount++;
                combo++;
                score += scorePerNote * result.getScoreMultiplier();
            }
            case JUST -> {
                justCount++;
                combo++;
                score += scorePerNote * result.getScoreMultiplier();

                // Track early/late for JUST hits
                if (timingOffset < 0) {
                    earlyCount++;
                } else {
                    lateCount++;
                }
            }
            case MISS -> {
                missCount++;
                combo = 0;
            }
        }

        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    /**
     * Record judgment without timing offset (for backward compatibility)
     */
    public void recordJudgment(JudgmentResult result) {
        recordJudgment(result, 0);
    }

    /**
     * 计算准确率百分比
     */
    public double getAccuracy() {
        int totalHits = exactCount + justCount + missCount;
        if (totalHits == 0) return 0.0;

        double weightedHits = exactCount + (justCount * 0.7);
        return (weightedHits / totalHits) * 100.0;
    }

    /**
     * 检查是否为完美分数（全 exact）
     * 在游戏过程中动态检查：只要没有 JUST 和 MISS 判定，就是完美状态
     */
    public boolean isPerfect() {
        return justCount == 0 && missCount == 0;
    }

    /**
     * 检查是否完成了完美游戏（所有音符都已击打且全为 EXACT）
     * 用于结算界面判断
     */
    public boolean isFullPerfect() {
        return exactCount == totalNotes && justCount == 0 && missCount == 0;
    }

    /**
     * 获取总击打数
     */
    public int getTotalHits() {
        return exactCount + justCount + missCount;
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        exactCount = 0;
        justCount = 0;
        earlyCount = 0;
        lateCount = 0;
        missCount = 0;
        combo = 0;
        maxCombo = 0;
        score = 0;
    }
}
