package io.github.niilo411.elementalpaths.network;

import io.github.niilo411.elementalpaths.platform.IPayloadContext;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.BiConsumer;

/**
 * Every packet the mod sends, registered once.
 *
 * Fabric splits this in two where NeoForge had it in one place: the payload TYPES of
 * both directions are declared here in common code (both sides have to agree on what
 * each id decodes to), and the handlers for server-to-client packets live in
 * {@code client.ModNetworkingClient}, since they touch client-only classes.
 *
 * Fabric already runs receivers on the main thread, so the handlers' enqueueWork simply
 * runs — see IPayloadContext.
 */
public class ModNetworking {

    private static <T extends CustomPacketPayload> void server(CustomPacketPayload.Type<T> type,
                                                               StreamCodec<? super FriendlyByteBuf, T> codec,
                                                               BiConsumer<T, IPayloadContext> handler) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.accept(payload, IPayloadContext.of(context.player())));
    }

    private static <T extends CustomPacketPayload> void toClient(CustomPacketPayload.Type<T> type,
                                                                 StreamCodec<? super FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }

    public static void register() {
        // Server -> client: the types only. Their handlers are in ModNetworkingClient.
        toClient(ChannelingArmorPacket.TYPE, ChannelingArmorPacket.STREAM_CODEC);
        toClient(ChargeStatusPacket.TYPE, ChargeStatusPacket.STREAM_CODEC);
        toClient(EarthquakePacket.TYPE, EarthquakePacket.STREAM_CODEC);
        toClient(RootedPacket.TYPE, RootedPacket.STREAM_CODEC);
        toClient(ScreenFlashPacket.TYPE, ScreenFlashPacket.STREAM_CODEC);
        toClient(ServerKindPacket.TYPE, ServerKindPacket.STREAM_CODEC);
        toClient(SettingsFilePacket.TYPE, SettingsFilePacket.STREAM_CODEC);
        toClient(SyncParagonPacket.TYPE, SyncParagonPacket.STREAM_CODEC);
        toClient(SyncChannelingDataPacket.TYPE, SyncChannelingDataPacket.STREAM_CODEC);
        toClient(SyncBloodPacket.TYPE, SyncBloodPacket.STREAM_CODEC);
        toClient(SyncPassivesPacket.TYPE, SyncPassivesPacket.STREAM_CODEC);
        toClient(SyncStatsPacket.TYPE, SyncStatsPacket.STREAM_CODEC);
        toClient(SyncUpgradesPacket.TYPE, SyncUpgradesPacket.STREAM_CODEC);

        // Client -> server.
        // 1. Element Choice Packet (Client -> Server)
        server(
                ElementChoicePacket.TYPE,
                ElementChoicePacket.STREAM_CODEC,
                (ElementChoicePacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(io.github.niilo411.elementalpaths.ModAttachments.CHANNELING_DATA);

                            // Read BEFORE setMainElement, which sets the flag itself —
                            // asking afterwards would always say they had already chosen.
                            boolean firstChoice = !data.hasChosenElement();

                            // ONLY a first choice is honoured. The screen is shown only to
                            // somebody who has not chosen, so this refuses nothing anybody
                            // can legitimately do — what it stops is a hand-sent packet
                            // rewriting a main element that is already set, which now
                            // matters in a way it did not before: no channeling is reachable
                            // from this screen and is meant to be a first-join decision or
                            // a gift from an operator, not something to switch into later.
                            if (!firstChoice) return;

                            // And only a path that actually exists, for the reason
                            // /paths add is gated: an invented one leaves a player holding
                            // something with no tree, no abilities and no emblem.
                            if (!io.github.niilo411.elementalpaths.abilities.ElementPaths.exists(payload.element())) return;

                            data.setMainElement(payload.element());
                            data.setActiveElement(payload.element());
                            if (!data.getUnlockedElements().contains(payload.element())) {
                                data.getUnlockedElements().add(payload.element());
                            }

                            // Picking your element starts you at level 1 rather than 0.
                            // Gated on it being the FIRST choice so re-sending this packet
                            // can't be used to farm levels, and taken as a floor rather
                            // than an assignment so it can never demote anyone.
                            if (firstChoice) {
                                data.setLevel(Math.max(data.getLevel(), 1));
                            }

                            player.setAttached(io.github.niilo411.elementalpaths.ModAttachments.CHANNELING_DATA, data);

                            // The client's own copy of the level has to be told: it set
                            // the element locally for an instant response, but the level
                            // is decided here, and without this the HUD reads "Lvl: 0"
                            // until something else happens to sync stats.
                            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                                io.github.niilo411.elementalpaths.platform.PacketDistributor.sendToPlayer(serverPlayer,
                                        SyncStatsPacket.of(data));
                            }
                        }
                    });
                }
        );

// 2. Switch Element Packet (Client -> Server)
        server(
                SwitchElementPacket.TYPE,
                SwitchElementPacket.STREAM_CODEC,
                (SwitchElementPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(io.github.niilo411.elementalpaths.ModAttachments.CHANNELING_DATA);
                            data.setActiveElement(payload.newElement());
                            player.setAttached(io.github.niilo411.elementalpaths.ModAttachments.CHANNELING_DATA, data);
                        }
                    });
                });

                // --- EQUIP ABILITY PACKET (Client -> Server) ---
        server(
                        io.github.niilo411.elementalpaths.network.EquipAbilityPacket.TYPE,
                        io.github.niilo411.elementalpaths.network.EquipAbilityPacket.STREAM_CODEC,
                        io.github.niilo411.elementalpaths.network.EquipAbilityPacket::handle
                );

        // 3. Meditate Packet (Client -> Server)
        server(
                MeditatePacket.TYPE,
                MeditatePacket.STREAM_CODEC,
                (MeditatePacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            var data = player.getAttachedOrCreate(io.github.niilo411.elementalpaths.ModAttachments.CHANNELING_DATA);
                            data.setMeditating(payload.isStarting());
                            if (!payload.isStarting()) {
                                data.setMeditateTickTimer(0);
                            }
                            // deleted: registrar.playToServer(...) — was invalid at runtime and crashed the game
                        }
                    });
                }
        );

        // 4. Use Ability Packet (Client -> Server)
        server(
                UseAbilityPacket.TYPE,
                UseAbilityPacket.STREAM_CODEC,
                UseAbilityPacket::handle
        );

        // 5. Unlock Ability Packet (Client -> Server)
        server(
                UnlockAbilityPacket.TYPE,
                UnlockAbilityPacket.STREAM_CODEC,
                UnlockAbilityPacket::handle
        );

        server(
                LeftClickTriggerPacket.TYPE,
                LeftClickTriggerPacket.STREAM_CODEC,
                LeftClickTriggerPacket::handle
        );

        // 6. Ability Hold Packet (Client -> Server) — for channeled abilities like Fire Breath
        server(
                AbilityHoldPacket.TYPE,
                AbilityHoldPacket.STREAM_CODEC,
                AbilityHoldPacket::handle
        );

        // --- EQUIP PASSIVE (Client -> Server) ---
        server(
                EquipPassivePacket.TYPE,
                EquipPassivePacket.STREAM_CODEC,
                EquipPassivePacket::handle
        );

        // --- BUY UPGRADE (Client -> Server) ---
        server(
                BuyUpgradePacket.TYPE,
                BuyUpgradePacket.STREAM_CODEC,
                BuyUpgradePacket::handle
        );

        // --- UPDATE SETTINGS (Client -> Server) : an op editing a dedicated server ---
        server(
                UpdateSettingsPacket.TYPE,
                UpdateSettingsPacket.STREAM_CODEC,
                (UpdateSettingsPacket payload, IPayloadContext context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) return;

                        // Checked again here whatever the menu showed — the client is only
                        // ever asking. See SettingsAccess for who qualifies.
                        if (!io.github.niilo411.elementalpaths.SettingsAccess.canEdit(player)) return;

                        io.github.niilo411.elementalpaths.SettingsAccess.apply(payload.values());
                        io.github.niilo411.elementalpaths.PathsConfig.SPEC.save();
                        io.github.niilo411.elementalpaths.PathsConfig.refresh();
                        io.github.niilo411.elementalpaths.SettingsAccess.broadcast(player.level().getServer());
                    });
                }
        );
    }
}
