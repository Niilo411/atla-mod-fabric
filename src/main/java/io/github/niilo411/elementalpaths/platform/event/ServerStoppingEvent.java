package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.server.MinecraftServer;

public record ServerStoppingEvent(MinecraftServer getServer) {}
