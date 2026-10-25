package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.world.entity.player.Player;

public final class PlayerTickEvent {
    private PlayerTickEvent() {}

    /** Fired at the end of Player#tick, on BOTH sides, exactly like NeoForge. */
    public record Post(Player getEntity) {}
}
