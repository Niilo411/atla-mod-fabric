package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerEvent {
    private PlayerEvent() {}

    public record StartTracking(Player getEntity, Entity getTarget) {}

    public record PlayerLoggedInEvent(Player getEntity) {}

    public record PlayerLoggedOutEvent(Player getEntity) {}

    public record PlayerRespawnEvent(Player getEntity, boolean isEndConquered) {}

    public record PlayerChangedDimensionEvent(Player getEntity) {}

    /** The respawn copy: the dead (or End-exiting) body and the new one. */
    public record Clone(Player getOriginal, Player getEntity, boolean isWasDeath) {}
}
