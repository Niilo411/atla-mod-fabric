package com.minecraft.atlamod.platform.event;

import net.minecraft.world.entity.Entity;

public final class EntityTickEvent {
    private EntityTickEvent() {}

    public record Post(Entity getEntity) {}
}
