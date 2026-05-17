package org.cubeRhythm.chart;

import lombok.Data;
import org.cubeRhythm.note.Note;
import org.cubeRhythm.note.event.GroupEvent;

import java.util.ArrayList;
import java.util.List;

@Data
public class Chart {
    private String version;
    private ChartMetadata metadata;
    private List<Note> notes;
    private List<GroupEvent> groupEvents = new ArrayList<>();

    public int getTotalNotes() {
        if (notes == null) return 0;
        return (int) notes.stream().filter(n -> n.getType().isScored()).count();
    }
}
