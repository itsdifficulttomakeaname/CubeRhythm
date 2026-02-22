package org.cubeRhythm.entity;

import org.cubeRhythm.note.NoteEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityManager {
    private final Map<UUID, NoteEntity> entities = new ConcurrentHashMap<>();

    public void registerEntity(NoteEntity entity) {
        entities.put(entity.getLinkUUID(), entity);
    }

    public void unregisterEntity(UUID uuid) {
        NoteEntity entity = entities.remove(uuid);
        if (entity != null) {
            entity.cleanup();
        }
    }

    public NoteEntity getEntity(UUID uuid) {
        return entities.get(uuid);
    }

    public Collection<NoteEntity> getAllEntities() {
        return entities.values();
    }

    public int getEntityCount() {
        return entities.size();
    }

    public void cleanupAll() {
        for (NoteEntity entity : entities.values()) {
            entity.cleanup();
        }
        entities.clear();
    }

    public List<NoteEntity> getEntitiesByType(org.cubeRhythm.note.NoteType type) {
        List<NoteEntity> result = new ArrayList<>();
        for (NoteEntity entity : entities.values()) {
            if (entity.getType() == type) {
                result.add(entity);
            }
        }
        return result;
    }
}
