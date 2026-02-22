package org.cubeRhythm.note;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoteEntity extends Note {
    private BlockDisplay blockDisplay;
    private Interaction interaction;
    private List<TextDisplay> textDisplays = new ArrayList<>();
    private List<BlockDisplay> additionalBlockDisplays = new ArrayList<>();  // For DOUBLE notes
    private List<Interaction> additionalInteractions = new ArrayList<>();  // For DOUBLE notes (second hitbox)

    private long spawnTime;
    private Double hitTime;
    private UUID linkUUID;
    private boolean forceExact;
    private boolean isHit;
    private int hitCount = 0;  // For DOUBLE notes: track how many times hit

    // 用于 hold 音符
    private int holdingCounter;

    // 用于 double 音符
    private NoteEntity linkedNote;

    public NoteEntity(Note note) {
        this.setTime(note.getTime());
        this.setAppearBefore(note.getAppearBefore());
        this.setType(note.getType());
        this.setFace(note.getFace());
        this.setPosition(note.getPosition());
        this.setPositions(note.getPositions());  // For DOUBLE notes
        this.setGlowing(note.isGlowing());
        this.setTag(note.getTag());
        this.setTurn(note.getTurn());
        this.linkUUID = UUID.randomUUID();
    }

    public void cleanup() {
        if (blockDisplay != null && !blockDisplay.isDead()) {
            blockDisplay.remove();
        }
        if (interaction != null && !interaction.isDead()) {
            interaction.remove();
        }
        for (TextDisplay textDisplay : textDisplays) {
            if (textDisplay != null && !textDisplay.isDead()) {
                textDisplay.remove();
            }
        }
        textDisplays.clear();

        // 清理额外的 BlockDisplay（用于 DOUBLE 音符）
        for (BlockDisplay blockDisplay : additionalBlockDisplays) {
            if (blockDisplay != null && !blockDisplay.isDead()) {
                blockDisplay.remove();
            }
        }
        additionalBlockDisplays.clear();

        // 清理额外的 Interaction（用于 DOUBLE 音符）
        for (Interaction interaction : additionalInteractions) {
            if (interaction != null && !interaction.isDead()) {
                interaction.remove();
            }
        }
        additionalInteractions.clear();
    }
}
