package com.minecraft.atlamod.platform.event;

import net.minecraft.server.MinecraftServer;

public final class ServerTickEvent {
    private ServerTickEvent() {}

    public record Post(MinecraftServer getServer) {}
}
