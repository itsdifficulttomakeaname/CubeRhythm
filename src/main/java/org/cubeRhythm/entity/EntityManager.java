package org.cubeRhythm.entity;

import org.cubeRhythm.note.NoteEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityManager {
    private final Map<UUID, NoteEntity> entities = new ConcurrentHashMap<>();
    private final EntityPool entityPool = new EntityPool();

    public EntityPool getEntityPool() {
        return entityPool;
    }

    public void registerEntity(NoteEntity entity) {
        entities.put(entity.getLinkUUID(), entity);
    }

    public void unregisterEntity(UUID uuid) {
        NoteEntity entity = entities.remove(uuid);
        if (entity != null) {
            returnToPool(entity);
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
            returnToPool(entity);
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

    /**
     * 归还音符实体的 Minecraft 实体到对象池，而非直接销毁
     * 减少实体创建/销毁的 GC 开销
     */
    private void returnToPool(NoteEntity entity) {
        // 主 BlockDisplay 归还池
        if (entity.getBlockDisplay() != null && !entity.getBlockDisplay().isDead()) {
            entityPool.returnBlockDisplay(entity.getBlockDisplay());
        }
        // 主 Interaction 归还池
        if (entity.getInteraction() != null && !entity.getInteraction().isDead()) {
            entityPool.returnInteraction(entity.getInteraction());
        }
        // TextDisplay 归还池
        for (var td : entity.getTextDisplays()) {
            if (td != null && !td.isDead()) {
                entityPool.returnTextDisplay(td);
            }
        }
        entity.getTextDisplays().clear();

        // DOUBLE 音符的额外实体
        for (var bd : entity.getAdditionalBlockDisplays()) {
            if (bd != null && !bd.isDead()) {
                entityPool.returnBlockDisplay(bd);
            }
        }
        entity.getAdditionalBlockDisplays().clear();

        for (var ia : entity.getAdditionalInteractions()) {
            if (ia != null && !ia.isDead()) {
                entityPool.returnInteraction(ia);
            }
        }
        entity.getAdditionalInteractions().clear();

        // 连接线
        if (entity.getConnectLine() != null && !entity.getConnectLine().isDead()) {
            entityPool.returnBlockDisplay(entity.getConnectLine());
            entity.setConnectLine(null);
        }
    }

    /**
     * 完全销毁所有实体和池（插件关闭时调用）
     */
    public void shutdown() {
        for (NoteEntity entity : entities.values()) {
            entity.cleanup(); // 直接 remove，不归还池
        }
        entities.clear();
        entityPool.clearAll();
    }
}
