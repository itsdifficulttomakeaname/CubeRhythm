package org.cubeRhythm.judgment;

import org.bukkit.Location;
import org.cubeRhythm.coordinate.CoordinateSystem;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.note.NoteEntity;

public class JudgmentManager {
    private final JudgmentWindow window;
    private final double centerX;
    private final double centerZ;

    public JudgmentManager(double centerX, double centerZ) {
        this.window = new JudgmentWindow();
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    /**
     * 计算时间偏移（毫秒）
     * @param noteEntity 音符实体
     * @param speed 玩家的速度设置
     * @return 时间偏移（毫秒）（负数 = 早，正数 = 晚）
     */
    public int calculateTimingOffset(NoteEntity noteEntity, double speed) {
        // FLICK 音符没有 Interaction，使用 BlockDisplay 的位置
        Location notePos;
        if (noteEntity.getInteraction() != null) {
            notePos = noteEntity.getInteraction().getLocation();
        } else if (noteEntity.getBlockDisplay() != null) {
            notePos = noteEntity.getBlockDisplay().getLocation();
        } else {
            // 如果两者都没有，返回 0（不应该发生）
            return 0;
        }

        Face face = noteEntity.getFace();
        double distance = CoordinateSystem.calculateDistance(notePos, face, centerX, centerZ);

        if (noteEntity.getType() == org.cubeRhythm.note.NoteType.HOLD) {
            distance -= noteEntity.getHoldScaleZ();
        }

        return (int) ((-distance / speed) * 50);
    }

    /**
     * 判定音符击打
     * @param timingOffsetMs 时间偏移（毫秒）
     * @return 判定结果
     */
    public JudgmentResult judge(int timingOffsetMs) {
        return window.judge(timingOffsetMs);
    }

    /**
     * 检查音符是否在判定范围内
     * @param timingOffsetMs 时间偏移（毫秒）
     * @return 如果在判定范围内返回 true
     */
    public boolean isWithinJudgmentRange(int timingOffsetMs) {
        return Math.abs(timingOffsetMs) <= window.getJustWindow();
    }

    /**
     * 判断击打是早还是晚
     * @param timingOffsetMs 时间偏移（毫秒）
     * @return "EARLY" 或 "LATE"
     */
    public String getEarlyLate(int timingOffsetMs) {
        return timingOffsetMs < 0 ? "§eEARLY" : "§eLATE";
    }
}
