package com.minecraft.atlamod.platform;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * NeoForge's PacketDistributor, server half, over Fabric networking. Same method names
 * so the fifty-odd call sites did not have to change.
 *
 * Every send checks canSend first: a client without the mod (or one still in the
 * configuration phase) has not registered our channels, and Fabric throws on a send
 * the other side cannot receive.
 */
public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        for (ServerPlayer p : PlayerLookup.tracking(entity)) {
            if (p != entity) sendToPlayer(p, payload);
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        sendToPlayersTrackingEntity(entity, payload);
        if (entity instanceof ServerPlayer self) sendToPlayer(self, payload);
    }

    public static void sendToAllPlayers(MinecraftServer server, CustomPacketPayload payload) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendToPlayer(p, payload);
    }
}
