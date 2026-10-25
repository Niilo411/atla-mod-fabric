package com.minecraft.atlamod.gametest;

import net.minecraft.world.entity.EntityTypes;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.minecraft.atlamod.AbilityHandler;
import com.minecraft.atlamod.Atlamod;
import com.minecraft.atlamod.BendingData;
import com.minecraft.atlamod.ModAttachments;
import com.minecraft.atlamod.ServerEvents;
import com.minecraft.atlamod.abilities.Ability;
import com.minecraft.atlamod.abilities.AbilityRegistry;
import com.minecraft.atlamod.abilities.ChargedAbility;
import com.minecraft.atlamod.abilities.PassiveAbility;
import com.minecraft.atlamod.abilities.TwoPhaseAbility;
import com.minecraft.atlamod.platform.event.PlayerEvent;
import com.minecraft.atlamod.spirit.island.IslandStyle;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Automated checks run by `gradlew runGametest` on a headless server.
 *
 * They are smoke tests, not balance tests: they prove the mod's data still loads after a
 * Minecraft update, and that every ability can be pressed, held, released, clicked and
 * pressed again without throwing — the kind of failure that would otherwise only surface
 * as a crash the first time somebody happened to use that one ability in game.
 */
public class AtlaGameTests {

    /** Every registry entry the Spirit World depends on is present and parses. */
    @GameTest
    public void spiritWorldDataLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var access = server.registryAccess();

        helper.assertTrue(access.lookupOrThrow(Registries.DIMENSION_TYPE)
                        .containsKey(Atlamod.id("spirit_world")),
                "the spirit_world dimension type is missing");

        var biomes = access.lookupOrThrow(Registries.BIOME);
        for (IslandStyle style : IslandStyle.values()) {
            helper.assertTrue(biomes.containsKey(style.biome()), "biome missing: " + style.biome());
        }
        helper.assertTrue(biomes.containsKey(Atlamod.id("spirit_void")), "spirit_void biome missing");

        var structures = access.lookupOrThrow(Registries.STRUCTURE);
        helper.assertTrue(structures.containsKey(Atlamod.id("spirit_temple")), "spirit_temple structure missing");
        helper.assertTrue(structures.containsKey(Atlamod.id("spirit_shrine")), "spirit_shrine structure missing");

        // The test server builds a flat world and never makes datapack dimensions, so the
        // dimension file itself is decoded here exactly as world creation would decode it.
        var resource = server.getResourceManager()
                .getResource(Atlamod.id("dimension/spirit_world.json"))
                .orElseThrow(() -> new AssertionError("dimension/spirit_world.json not found"));
        try (Reader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), json).getOrThrow(
                    error -> new AssertionError("spirit_world dimension does not parse: " + error));
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        helper.succeed();
    }

    /** Ticks given to an ability after its charge (or once pressed) before it is let go. */
    private static final int HOLD_EXTRA = 30;

    /** Ticks given after release for projectiles, waves and timers to run. */
    private static final int AFTERMATH = 40;

    /**
     * Every registered ability, driven the way the client drives it: the key press sends
     * both a cast and a hold, then the key is held long enough for any charge to complete,
     * then released, then the left mouse button is clicked once per shot (two-phase
     * abilities), then the key is pressed once more (switching toggles back off), and
     * finally the player logs out — which runs every cleanup path the mod has.
     *
     * One fresh player per ability, so no cooldown or half-finished state leaks from one
     * to the next. The server sprints for the duration, so a ten-second charge costs a
     * fraction of a second of real time; a sprint changes how fast ticks come, not what
     * happens in one.
     *
     * THREE SETTINGS, tried in turn. Most abilities need a target in view; waterbending
     * needs water (and is refused without it, quite correctly); a few earth abilities need
     * the ground under the crosshair. An ability that leaves no trace in one setting —
     * spends no chi, earns no xp, starts no cooldown — is tried again in the next, so every
     * one of those code paths actually runs. Whatever is still silent after all three is
     * logged; passives belong there, since they cost nothing by design.
     */
    @GameTest(maxTicks = 60000)
    public void everyAbilitySurvivesAFullCycle(GameTestHelper helper) {
        helper.getLevel().getServer().tickRateManager().requestGameToSprint(60000);
        new Cycle(helper, new ArrayDeque<>(AbilityRegistry.all().values())).next();
    }

    /** Where the player stands and what they are looking at. */
    private enum Setting {
        /** A zombie four blocks ahead, looked at. */
        AT_A_MOB,
        /** Standing in water, a pool ahead, looking along it at a zombie, with a full canteen. */
        IN_WATER,
        /** On stone, looking down at the stone ahead. */
        AT_THE_GROUND
    }

    private static final Setting[] SETTINGS = Setting.values();

    /**
     * Walks the ability list one ability at a time, scheduling each step on the test clock.
     *
     * EVERY STEP IS A NEW LAMBDA, and that is not style. Vanilla keeps scheduled work in a
     * map keyed on the Runnable itself, so scheduling one shared Runnable at twenty
     * different times leaves it scheduled once, at the last of them — the first version
     * of this test ticked each player a single time and passed without testing anything.
     */
    private static final class Cycle {
        private final GameTestHelper helper;
        private Deque<Ability> queue;
        private List<Ability> silent = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();
        private int setting = 0;
        private int cycled = 0;

        Cycle(GameTestHelper helper, Deque<Ability> queue) {
            this.helper = helper;
            this.queue = queue;
        }

        void next() {
            if (queue.isEmpty()) {
                if (setting + 1 < SETTINGS.length && !silent.isEmpty()) {
                    setting++;
                    queue = new ArrayDeque<>(silent);
                    silent = new ArrayList<>();
                } else {
                    finish();
                    return;
                }
            }
            run(queue.poll(), SETTINGS[setting]);
        }

        private void finish() {
            helper.getLevel().getServer().tickRateManager().stopSprinting();
            Atlamod.LOGGER.info("Game test: {} ability runs over {} abilities; {} left no trace in any setting: {}",
                    cycled, AbilityRegistry.all().size(), silent.size(),
                    silent.stream().map(Ability::getName).toList());
            if (failures.isEmpty()) {
                helper.succeed();
            } else {
                helper.fail(failures.size() + " ability step(s) threw:\n" + String.join("\n", failures));
            }
        }

        private void run(Ability ability, Setting where) {
            cycled++;
            String name = ability.getName();

            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            Mob target = prepare(where, player);

            BendingData data = player.getAttachedOrCreate(ModAttachments.BENDING_DATA);
            data.setLevel(20);
            data.setCurrentChi(data.getMaxChi());
            data.unlockAbility(name);
            if (ability instanceof PassiveAbility) {
                // A passive does its work while equipped and ticking.
                data.setAllEquippedPassives(List.of(name, "EMPTY", "EMPTY", "EMPTY"));
            }
            // One tick on the spot first, as a real player would have had: vanilla only
            // works out "standing in water" during a tick, and Water heal asks at the press.
            guard(name, where, "settle", player::doTick);
            data.setCurrentChi(data.getMaxChi());
            int fullChi = data.getCurrentChi();

            int hold = HOLD_EXTRA;
            if (ability instanceof ChargedAbility charged) hold += charged.getChargeTicks();
            int clicks = ability instanceof TwoPhaseAbility twoPhase ? Math.max(1, twoPhase.getShots()) : 1;

            guard(name, where, "press", () -> {
                AbilityHandler.executeAbility(player, data, name);
                AbilityHandler.executeAbilityHold(player, data, name, true);
            });

            long t = 1;
            for (int i = 0; i < hold; i++) helper.runAfterDelay(t++, () -> guard(name, where, "tick", player::doTick));
            helper.runAfterDelay(t++, () -> guard(name, where, "release",
                    () -> AbilityHandler.executeAbilityHold(player, data, name, false)));
            for (int i = 0; i < clicks; i++) {
                helper.runAfterDelay(t++, () -> guard(name, where, "left click",
                        () -> AbilityHandler.executeLeftClickPhase(player, data)));
                helper.runAfterDelay(t++, () -> guard(name, where, "tick", player::doTick));
            }
            for (int i = 0; i < AFTERMATH; i++) helper.runAfterDelay(t++, () -> guard(name, where, "tick", player::doTick));
            helper.runAfterDelay(t++, () -> guard(name, where, "second press", () -> {
                AbilityHandler.executeAbility(player, data, name);
                AbilityHandler.executeAbilityHold(player, data, name, true);
                AbilityHandler.executeAbilityHold(player, data, name, false);
            }));
            for (int i = 0; i < 5; i++) helper.runAfterDelay(t++, () -> guard(name, where, "tick", player::doTick));
            helper.runAfterDelay(t++, () -> {
                guard(name, where, "logout", () ->
                        ServerEvents.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player)));
                player.level().getServer().getPlayerList().remove(player);
                if (target != null) target.discard();

                if (data.getCurrentChi() >= fullChi && data.getXp() == 0 && data.getLevel() == 20
                        && !data.isOnCooldown(ability.getKey())) {
                    silent.add(ability);
                }
                next();
            });
        }

        /** Lays out the setting (blocks are re-set every time, since abilities reshape them). */
        private Mob prepare(Setting where, ServerPlayer player) {
            // A stone floor under the whole working area, whatever the last ability did to it.
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                }
            }

            Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);

            return switch (where) {
                case AT_A_MOB -> {
                    Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 5));
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
                    yield zombie;
                }
                case IN_WATER -> {
                    for (int z = 1; z <= 4; z++) helper.setBlock(new BlockPos(1, 1, z), Blocks.WATER);
                    player.getInventory().add(new ItemStack(Atlamod.WATER_CANTEEN));
                    Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 6));
                    player.lookAt(EntityAnchorArgument.Anchor.EYES,
                            Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 3))));
                    yield zombie;
                }
                case AT_THE_GROUND -> {
                    player.lookAt(EntityAnchorArgument.Anchor.EYES,
                            Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 0, 3))));
                    yield null;
                }
            };
        }

        private void guard(String ability, Setting where, String step, Runnable action) {
            try {
                action.run();
            } catch (Throwable t) {
                Atlamod.LOGGER.error("Game test: {} threw on {} ({})", ability, step, where, t);
                failures.add(ability + " (" + step + ", " + where + "): " + t);
            }
        }
    }
}
