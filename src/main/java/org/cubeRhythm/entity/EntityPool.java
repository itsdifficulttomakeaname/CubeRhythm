package org.cubeRhythm.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Entity pool for reusing entities to reduce creation/destruction overhead
 * Improves performance by recycling BlockDisplay, Interaction, and TextDisplay entities
 */
public class EntityPool {
    private final Queue<BlockDisplay> blockDisplayPool = new ConcurrentLinkedQueue<>();
    private final Queue<Interaction> interactionPool = new ConcurrentLinkedQueue<>();
    private final Queue<TextDisplay> textDisplayPool = new ConcurrentLinkedQueue<>();

    private static final int MAX_POOL_SIZE = 200;

    /**
     * Get or create a BlockDisplay entity
     */
    public BlockDisplay getBlockDisplay(Location location) {
        BlockDisplay entity = blockDisplayPool.poll();
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
            return entity;
        }
        // If entity from pool is invalid, try to get another one
        if (entity != null && !entity.isValid() && !blockDisplayPool.isEmpty()) {
            return getBlockDisplay(location);
        }
        // Create new entity with default parameters
        return DisplayEntityFactory.createBlockDisplay(
            location.getWorld(),
            location,
            Material.LIGHT_BLUE_CONCRETE,
            1.0f, 1.0f, 1.0f,
            0
        );
    }

    /**
     * Get or create an Interaction entity
     */
    public Interaction getInteraction(Location location, float width, float height) {
        Interaction entity = interactionPool.poll();
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
            entity.setInteractionWidth(width);
            entity.setInteractionHeight(height);
            return entity;
        }
        // If entity from pool is invalid, try to get another one
        if (entity != null && !entity.isValid() && !interactionPool.isEmpty()) {
            return getInteraction(location, width, height);
        }
        return DisplayEntityFactory.createInteraction(location.getWorld(), location, width, height);
    }

    /**
     * Get or create a TextDisplay entity
     */
    public TextDisplay getTextDisplay(Location location) {
        TextDisplay entity = textDisplayPool.poll();
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
            return entity;
        }
        // If entity from pool is invalid, try to get another one
        if (entity != null && !entity.isValid() && !textDisplayPool.isEmpty()) {
            return getTextDisplay(location);
        }
        return DisplayEntityFactory.createTextDisplay(location.getWorld(), location, "", 0);
    }

    /**
     * Return a BlockDisplay to the pool for reuse
     */
    public void returnBlockDisplay(BlockDisplay entity) {
        if (entity != null && entity.isValid() && blockDisplayPool.size() < MAX_POOL_SIZE) {
            // Reset entity state to default
            entity.setBlock(Material.AIR.createBlockData());
            entity.setGlowing(false);

            // Reset transformation to default
            Transformation defaultTransform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(1, 1, 1),
                new AxisAngle4f()
            );
            entity.setTransformation(defaultTransform);

            blockDisplayPool.offer(entity);
        } else if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    /**
     * Return an Interaction to the pool for reuse
     */
    public void returnInteraction(Interaction entity) {
        if (entity != null && entity.isValid() && interactionPool.size() < MAX_POOL_SIZE) {
            interactionPool.offer(entity);
        } else if (entity != null) {
            entity.remove();
        }
    }

    /**
     * Return a TextDisplay to the pool for reuse
     */
    public void returnTextDisplay(TextDisplay entity) {
        if (entity != null && entity.isValid() && textDisplayPool.size() < MAX_POOL_SIZE) {
            // Reset entity state to default
            entity.text(net.kyori.adventure.text.Component.empty());
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

            // Reset transformation to default
            Transformation defaultTransform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(1, 1, 1),
                new AxisAngle4f()
            );
            entity.setTransformation(defaultTransform);

            textDisplayPool.offer(entity);
        } else if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    /**
     * Clear all pools and remove all entities
     */
    public void clearAll() {
        blockDisplayPool.forEach(entity -> {
            if (entity.isValid()) entity.remove();
        });
        interactionPool.forEach(entity -> {
            if (entity.isValid()) entity.remove();
        });
        textDisplayPool.forEach(entity -> {
            if (entity.isValid()) entity.remove();
        });

        blockDisplayPool.clear();
        interactionPool.clear();
        textDisplayPool.clear();
    }

    /**
     * Get pool statistics for monitoring
     */
    public String getPoolStats() {
        return String.format("EntityPool Stats - BlockDisplay: %d, Interaction: %d, TextDisplay: %d",
                blockDisplayPool.size(), interactionPool.size(), textDisplayPool.size());
    }
}
