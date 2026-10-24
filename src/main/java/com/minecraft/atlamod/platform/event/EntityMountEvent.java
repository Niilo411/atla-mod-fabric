package com.minecraft.atlamod.platform.event;

import net.minecraft.world.entity.Entity;

public final class EntityMountEvent extends CancellableEvent {
    private final Entity mounting;
    private final Entity mounted;
    private final boolean dismounting;

    public EntityMountEvent(Entity mounting, Entity mounted, boolean dismounting) {
        this.mounting = mounting;
        this.mounted = mounted;
        this.dismounting = dismounting;
    }

    public Entity getEntityMounting() { return mounting; }
    public Entity getEntityBeingMounted() { return mounted; }
    public boolean isDismounting() { return dismounting; }
    public boolean isMounting() { return !dismounting; }
}
