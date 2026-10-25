package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.world.level.Level;

public final class LevelEvent {
    private LevelEvent() {}

    public record Unload(Level getLevel) {}
}
