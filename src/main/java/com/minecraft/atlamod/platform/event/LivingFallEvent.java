package com.minecraft.atlamod.platform.event;

import net.minecraft.world.entity.LivingEntity;

/** Fired as a fall is about to be billed. Cancelling takes the damage, the thud and the dust. */
public final class LivingFallEvent extends CancellableEvent {
    private final LivingEntity entity;
    private final double distance;

    public LivingFallEvent(LivingEntity entity, double distance) {
        this.entity = entity;
        this.distance = distance;
    }

    public LivingEntity getEntity() { return entity; }
    public double getDistance() { return distance; }
}
