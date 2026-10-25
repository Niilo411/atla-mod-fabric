package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.world.entity.Entity;

public final class EntityTickEvent {
    private EntityTickEvent() {}

    public record Post(Entity getEntity) {}
}
