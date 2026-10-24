package com.minecraft.atlamod.platform;

import net.minecraft.world.entity.player.Player;

/**
 * The slice of NeoForge's IPayloadContext the packet handlers use, so every handler
 * kept its shape through the port.
 *
 * Fabric already runs play-phase receivers on the main thread (server thread for C2S,
 * render thread for S2C), so enqueueWork has nothing left to defer and simply runs.
 */
public interface IPayloadContext {
    Player player();

    default void enqueueWork(Runnable work) {
        work.run();
    }

    static IPayloadContext of(Player player) {
        return () -> player;
    }
}
