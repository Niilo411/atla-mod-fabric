package com.minecraft.atlamod.platform.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Fired BEFORE an entity leaves for another dimension, while it is still in the old one. */
public final class EntityTravelToDimensionEvent extends CancellableEvent {
    private final Entity entity;
    private final ResourceKey<Level> dimension;

    public EntityTravelToDimensionEvent(Entity entity, ResourceKey<Level> dimension) {
        this.entity = entity;
        this.dimension = dimension;
    }

    public Entity getEntity() { return entity; }
    public ResourceKey<Level> getDimension() { return dimension; }
}
