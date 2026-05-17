package org.cubeRhythm.note;

public enum NoteType {
    TAP, DRAG, HOLD, FLICK, DOUBLE, EXECUTION,
    FAKE_TAP, FAKE_DRAG, FAKE_HOLD, FAKE_FLICK, FAKE_DOUBLE;

    public boolean isFake() {
        return this == FAKE_TAP || this == FAKE_DRAG || this == FAKE_HOLD || this == FAKE_FLICK || this == FAKE_DOUBLE;
    }

    /**
     * 是否计入分数（排除 EXECUTION 和所有 fake 类型）
     */
    public boolean isScored() {
        return this != EXECUTION && !isFake();
    }
}
