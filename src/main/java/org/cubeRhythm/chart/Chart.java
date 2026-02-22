package org.cubeRhythm.chart;

import lombok.Data;
import org.cubeRhythm.note.Note;

import java.util.List;

@Data
public class Chart {
    private String version;
    private ChartMetadata metadata;
    private List<Note> notes;

    public int getTotalNotes() {
        return notes != null ? notes.size() : 0;
    }
}
