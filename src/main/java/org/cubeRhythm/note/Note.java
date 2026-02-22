package org.cubeRhythm.note;

import lombok.Data;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;

import java.util.List;
import java.util.Map;

@Data
public class Note {
    private double time;
    private double appearBefore;
    private NoteType type;
    private Face face;
    private NotePosition position;  // For single-position notes (TAP, HOLD, DRAG, FLICK)
    private List<NotePosition> positions;  // For DOUBLE notes (multiple positions)
    private boolean glowing;
    private String tag;
    private String turn; // For flick notes: "left" or "right"
    private List<Map<String, Object>> actions; // For execution notes: list of action configurations
}
