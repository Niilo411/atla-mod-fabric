package com.minecraft.atlamod.client;

import com.minecraft.atlamod.network.*;
import com.minecraft.atlamod.platform.IPayloadContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.BiConsumer;

/**
 * The client's half of ModNetworking: what each server-to-client packet DOES once it
 * arrives. The types themselves are registered in common code.
 */
public final class ModNetworkingClient {
    private ModNetworkingClient() {
    }

    private static <T extends CustomPacketPayload> void client(CustomPacketPayload.Type<T> type,
                                                               StreamCodec<? super FriendlyByteBuf, T> codec,
                                                               BiConsumer<T, IPayloadContext> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.accept(payload, IPayloadContext.of(context.player())));
    }

    public static void register() {
        // 7. Sync Stats Packet (Server -> Client)
        client(
                SyncStatsPacket.TYPE,
                SyncStatsPacket.STREAM_CODEC,
                (SyncStatsPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(com.minecraft.atlamod.ModAttachments.BENDING_DATA);
                            data.setXp(payload.xp());
                            data.setLevel(payload.level());
                            data.setCurrentChi(payload.currentChi());

                            // The shrine bonus, which the HUD needs for two separate
                            // things: the maximum the chi bar is drawn against, and the
                            // colour it is drawn in.
                            data.setBonusMaxChi(payload.bonusMaxChi());
                        }
                    });
                }
        );

// 8. Sync Bending Data Packet (Server -> Client)
        client(
                com.minecraft.atlamod.network.SyncBendingDataPacket.TYPE,
                com.minecraft.atlamod.network.SyncBendingDataPacket.STREAM_CODEC,
                (com.minecraft.atlamod.network.SyncBendingDataPacket payload, com.minecraft.atlamod.platform.IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(com.minecraft.atlamod.ModAttachments.BENDING_DATA);
                            data.setMainElement(payload.mainElement());
                            data.setActiveElement(payload.activeElement());

                            data.getUnlockedElements().clear();
                            if (payload.unlockedElements() != null) data.getUnlockedElements().addAll(payload.unlockedElements());

                            data.getUnlockedAbilities().clear();
                            if (payload.unlockedAbilities() != null) data.getUnlockedAbilities().addAll(payload.unlockedAbilities());

                            // Perfect 1:1 overwrite using the titanium vault setter
                            if (payload.equippedAbilities() != null) {
                                data.setAllEquippedAbilities(payload.equippedAbilities());
                            }

                            // Lock it in for the UI
                            player.setAttached(com.minecraft.atlamod.ModAttachments.BENDING_DATA, data);

                            if (!payload.hasChosen()) {
                                com.minecraft.atlamod.client.ClientEvents.needsToOpenMenu = true;
                            }
                        }
                    });
                }
        );

        // --- CHARGE STATUS (Server -> Client) : drives the HUD charge meter ---
        // Top-level inside register(), NOT nested in another handler lambda —
        // nesting it would throw "Cannot register payload after registration phase".
        client(
                ChargeStatusPacket.TYPE,
                ChargeStatusPacket.STREAM_CODEC,
                (ChargeStatusPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> com.minecraft.atlamod.client.ClientChargeState.update(
                            payload.ability(), payload.held(), payload.total(), payload.armed()));
                }
        );

        // --- SYNC PASSIVES (Server -> Client) ---
        client(
                SyncPassivesPacket.TYPE,
                SyncPassivesPacket.STREAM_CODEC,
                (SyncPassivesPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(com.minecraft.atlamod.ModAttachments.BENDING_DATA);
                            data.setAllEquippedPassives(payload.equippedPassives());
                            player.setAttached(com.minecraft.atlamod.ModAttachments.BENDING_DATA, data);
                        }
                    });
                }
        );

        // --- ROOTED (Server -> Client) : abilities that hold the player still ---
        client(
                RootedPacket.TYPE,
                RootedPacket.STREAM_CODEC,
                (RootedPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() ->
                            com.minecraft.atlamod.client.ClientRootState.set(payload.rooted()));
                }
        );

        // --- BENDING ARMOR (Server -> Client) : who is wearing which drawn suit ---
        client(
                BendingArmorPacket.TYPE,
                BendingArmorPacket.STREAM_CODEC,
                (BendingArmorPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        com.minecraft.atlamod.BendingArmorSuit[] suits =
                                com.minecraft.atlamod.BendingArmorSuit.VALUES;
                        // Guarded because the ordinal comes off the wire: a client on a
                        // different version of the mod would otherwise index past the end.
                        if (payload.suit() < 0 || payload.suit() >= suits.length) return;
                        com.minecraft.atlamod.client.ClientBendingArmor.set(
                                payload.entityId(), suits[payload.suit()], payload.active());
                    });
                }
        );

        // --- EARTHQUAKE (Server -> Client) : shake the receiving player's camera ---
        client(
                EarthquakePacket.TYPE,
                EarthquakePacket.STREAM_CODEC,
                (EarthquakePacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() ->
                            com.minecraft.atlamod.client.ClientShake.start(payload.ticks()));
                }
        );

        // --- SCREEN FLASH (Server -> Client) : Lightning stun's white-out ---
        client(
                ScreenFlashPacket.TYPE,
                ScreenFlashPacket.STREAM_CODEC,
                (ScreenFlashPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() ->
                            com.minecraft.atlamod.client.ClientFlash.start(payload.ticks()));
                }
        );

        // --- SYNC BLOOD (Server -> Client) : drives the HUD blood level ---
        client(
                SyncBloodPacket.TYPE,
                SyncBloodPacket.STREAM_CODEC,
                (SyncBloodPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(com.minecraft.atlamod.ModAttachments.BENDING_DATA);
                            data.setBloodXp(payload.bloodXp());
                            data.setBloodLevel(payload.bloodLevel());
                            player.setAttached(com.minecraft.atlamod.ModAttachments.BENDING_DATA, data);
                        }
                    });
                }
        );

        // --- SYNC AVATAR (Server -> Client) : drives the HUD lives counter ---
        // Top-level inside register(), NOT nested in another handler's lambda.
        client(
                SyncAvatarPacket.TYPE,
                SyncAvatarPacket.STREAM_CODEC,
                (SyncAvatarPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(com.minecraft.atlamod.ModAttachments.BENDING_DATA);
                            data.setAvatar(payload.avatar());
                            data.setAvatarLives(payload.lives());
                            player.setAttached(com.minecraft.atlamod.ModAttachments.BENDING_DATA, data);
                        }
                    });
                }
        );

        // --- SYNC UPGRADES (Server -> Client) ---
        client(
                SyncUpgradesPacket.TYPE,
                SyncUpgradesPacket.STREAM_CODEC,
                (SyncUpgradesPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(com.minecraft.atlamod.ModAttachments.BENDING_DATA);
                            data.setAllUnlockedUpgrades(payload.unlockedUpgrades());
                            player.setAttached(com.minecraft.atlamod.ModAttachments.BENDING_DATA, data);
                        }
                    });
                }
        );

        // --- SERVER KIND (Server -> Client) : whether ops may edit settings here ---
        client(
                ServerKindPacket.TYPE,
                ServerKindPacket.STREAM_CODEC,
                (ServerKindPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> com.minecraft.atlamod.client.ClientSettingsAccess
                            .setDedicatedServer(payload.dedicated()));
                }
        );

        // --- SETTINGS FILE (Server -> Client) : the settings as they now are ---
        client(
                SettingsFilePacket.TYPE,
                SettingsFilePacket.STREAM_CODEC,
                (SettingsFilePacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> com.minecraft.atlamod.client.ClientSettingsAccess
                            .receive(payload.fileName(), payload.contents()));
                }
        );
    }
}
