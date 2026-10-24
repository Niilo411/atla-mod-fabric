package com.minecraft.atlamod.platform.event;

import net.minecraft.world.level.Level;

public final class LevelEvent {
    private LevelEvent() {}

    public record Unload(Level getLevel) {}
}
