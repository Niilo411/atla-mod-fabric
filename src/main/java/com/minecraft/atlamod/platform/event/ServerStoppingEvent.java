package com.minecraft.atlamod.platform.event;

import net.minecraft.server.MinecraftServer;

public record ServerStoppingEvent(MinecraftServer getServer) {}
