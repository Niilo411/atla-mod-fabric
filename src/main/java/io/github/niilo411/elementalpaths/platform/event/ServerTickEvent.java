package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.server.MinecraftServer;

public final class ServerTickEvent {
    private ServerTickEvent() {}

    public record Post(MinecraftServer getServer) {}
}
