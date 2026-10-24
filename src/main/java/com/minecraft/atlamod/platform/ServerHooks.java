package com.minecraft.atlamod.platform;

import com.minecraft.atlamod.Atlamod;
import com.minecraft.atlamod.BendingData;
import com.minecraft.atlamod.ModAttachments;
import com.minecraft.atlamod.ServerEvents;
import com.minecraft.atlamod.platform.event.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * Every NeoForge event this mod listened to, delivered on Fabric.
 *
 * The handlers themselves did not move — they are still the ServerEvents methods,
 * taking the same-shaped event objects (see platform.event) — so this class is only
 * wiring: a Fabric callback where one exists, and a mixin (see the mixin package)
 * calling one of the static entry points below where one does not.
 */
public final class ServerHooks {
    private ServerHooks() {
    }

    /** Runs after Fabric's own listeners in the same event — see the respawn note below. */
    private static final Identifier LATE = Atlamod.id("late");

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                ServerEvents.onServerTick(new ServerTickEvent.Post(server)));

        ServerWorldEvents.UNLOAD.register((server, level) ->
                ServerEvents.onLevelUnload(new LevelEvent.Unload(level)));

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                ServerEvents.onServerStopping(new ServerStoppingEvent(server)));

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                ServerEvents.onRegisterCommands(new RegisterCommandsEvent(dispatcher, buildContext, selection)));

        registerTrades();

        EntityTrackingEvents.START_TRACKING.register((target, watcher) ->
                ServerEvents.onStartTracking(new PlayerEvent.StartTracking(watcher, target)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerEvents.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(handler.player)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerEvents.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));

        // Damage that actually landed. Fabric reports a blocked (shielded) hit here too,
        // so it is filtered out: NeoForge's Post only ever saw damage that got through.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked) ServerEvents.onDamageLanded(new LivingDamageEvent.Post(entity, source, damageTaken));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
                ServerEvents.onPlayerDeath(new LivingDeathEvent(entity, source)));

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            ServerEvents.onBlockBreak(event);
            return !event.isCanceled();
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            PlayerInteractEvent.RightClickBlock event =
                    new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos());
            ServerEvents.onRightClickBlock(event);
            return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                ServerEvents.onPlayerChangeDimension(new PlayerEvent.PlayerChangedDimensionEvent(player)));

        // RESPAWN. Fabric carries attachments across in its OWN AFTER_RESPAWN listener,
        // and it hands over the old body's object BY REFERENCE — transient flags and all.
        // NeoForge instead gave the new body a codec round-trip of the old data, so every
        // "is X running" flag came back false, and this mod depends on that (a rocket lit
        // at the moment of death must not still be lit after it).
        //
        // So this runs in a LATE phase, after Fabric's copy, and first replaces whatever
        // Fabric put there with a round-trip of its own — the same thing NeoForge did —
        // before the mod's clone handler copies its fields by hand, and then the respawn
        // handler, in the order NeoForge fired them.
        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, LATE);
        ServerPlayerEvents.AFTER_RESPAWN.register(LATE, (oldPlayer, newPlayer, alive) -> {
            newPlayer.setAttached(ModAttachments.BENDING_DATA, roundTrip(
                    oldPlayer.getAttachedOrCreate(ModAttachments.BENDING_DATA)));
            ServerEvents.onPlayerClone(new PlayerEvent.Clone(oldPlayer, newPlayer, !alive));
            ServerEvents.onPlayerRespawn(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive));
        });
    }

    /** A fresh BendingData holding exactly what would have been saved — no transient state. */
    private static BendingData roundTrip(BendingData data) {
        return BendingData.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .flatMap(tag -> BendingData.CODEC.parse(NbtOps.INSTANCE, tag))
                .resultOrPartial(error -> Atlamod.LOGGER.error("Could not copy bending data on respawn: {}", error))
                .orElseGet(BendingData::new);
    }

    // ------------------------------------------------------------------
    // Villager trades
    // ------------------------------------------------------------------

    /** The eight professions that sell a scroll. */
    private static final List<ResourceKey<VillagerProfession>> TRADING_PROFESSIONS = List.of(
            VillagerProfession.WEAPONSMITH, VillagerProfession.FISHERMAN, VillagerProfession.CLERIC,
            VillagerProfession.SHEPHERD, VillagerProfession.ARMORER, VillagerProfession.MASON,
            VillagerProfession.FLETCHER, VillagerProfession.CARTOGRAPHER);

    private static final List<Consumer<VillagerTradesEvent>> TRADE_HANDLERS = List.of(
            ServerEvents::onVillagerTrades, ServerEvents::onFishermanTrades, ServerEvents::onClericTrades,
            ServerEvents::onShepherdTrades, ServerEvents::onArmorerTrades, ServerEvents::onMasonTrades,
            ServerEvents::onFletcherTrades, ServerEvents::onCartographerTrades);

    /**
     * Each profession's book is built once, by running every trade handler over it exactly
     * as NeoForge's VillagerTradesEvent did, and whatever they added to a level is handed
     * to Fabric for that level.
     */
    private static void registerTrades() {
        for (ResourceKey<VillagerProfession> profession : TRADING_PROFESSIONS) {
            VillagerTradesEvent event = new VillagerTradesEvent(profession);
            for (Consumer<VillagerTradesEvent> handler : TRADE_HANDLERS) handler.accept(event);

            for (int level = 1; level <= 5; level++) {
                var added = event.getTrades().get(level);
                if (added.isEmpty()) continue;
                TradeOfferHelper.registerVillagerOffers(profession, level, factories -> factories.addAll(added));
            }
        }
    }

    // ------------------------------------------------------------------
    // Entry points for the mixins
    // ------------------------------------------------------------------

    /** @return the amount to go on with, or null when the damage is cancelled outright. */
    public static Float incomingDamage(LivingEntity entity, DamageSource source, float amount) {
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(entity, source, amount);
        ServerEvents.onIncomingDamage(event);
        return event.isCanceled() ? null : event.getAmount();
    }

    public static boolean fallCancelled(LivingEntity entity, double distance) {
        LivingFallEvent event = new LivingFallEvent(entity, distance);
        ServerEvents.onLivingFall(event);
        return event.isCanceled();
    }

    public static boolean dismountCancelled(Entity rider, Entity vehicle) {
        EntityMountEvent event = new EntityMountEvent(rider, vehicle, true);
        ServerEvents.onDismount(event);
        return event.isCanceled();
    }

    public static boolean travelCancelled(ServerPlayer player, ResourceKey<Level> destination) {
        EntityTravelToDimensionEvent event = new EntityTravelToDimensionEvent(player, destination);
        ServerEvents.onTravelToDimension(event);
        return event.isCanceled();
    }

    public static void playerTick(Player player) {
        ServerEvents.onPlayerTick(new PlayerTickEvent.Post(player));
    }

    public static void entityTick(Entity entity) {
        ServerEvents.onEntityTick(new EntityTickEvent.Post(entity));
    }
}
