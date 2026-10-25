package com.minecraft.atlamod.client;

import com.minecraft.atlamod.BendingSeat;
import com.minecraft.atlamod.ModAttachments;
import com.minecraft.atlamod.ModEffects;
import com.minecraft.atlamod.network.AbilityHoldPacket;
import com.minecraft.atlamod.network.LeftClickTriggerPacket;
import com.minecraft.atlamod.network.MeditatePacket;
import com.minecraft.atlamod.network.SwitchElementPacket;
import com.minecraft.atlamod.network.UseAbilityPacket;
import com.minecraft.atlamod.platform.ForcedPose;
import com.minecraft.atlamod.spirit.SpiritWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class ClientEvents {

    // Our flag that the Server can now trigger!
    public static boolean needsToOpenMenu = false;

    private static boolean wasLeftClicking = false;
    private static final boolean[] lastAbilityHeld = new boolean[4];

    /** What the server was last told about the meditate key, and when. */
    private static boolean lastMeditateSent = false;
    private static int meditateResendTicks = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onLoggingOut());

        // NO RESPAWN COPY. The NeoForge build carried the client's bending data onto the
        // new LocalPlayer by hand, because NeoForge copies no attachments on the client and
        // the menu and HUD read a blank copy until the server's resync landed. Fabric
        // already does that copy itself on respawn (for any attachment marked
        // copyOnDeath, which BENDING_DATA is), so the gap that handler closed never opens.
    }

    /**
     * Tells the server about the meditate key — but only when there is something to say.
     *
     * THIS USED TO SEND A PACKET EVERY TICK, for every player, whether or not anybody was
     * touching the key: twenty packets a second per player doing nothing but repeating
     * "not meditating". It now sends on a CHANGE, the same shape the ability hold keys
     * already use, plus a refresh once a second while the key is actually held. The
     * refresh is there because the server can clear its own copy without being asked — a
     * respawn or a dimension change builds a fresh BendingData with meditating false —
     * and a player still holding the key should pick back up within a second rather than
     * having to let go and press it again.
     */
    private static void syncMeditate() {
        boolean down = KeyBindings.MEDITATE.isDown();

        if (down != lastMeditateSent || (down && --meditateResendTicks <= 0)) {
            ClientPlayNetworking.send(new MeditatePacket(down));
            lastMeditateSent = down;
            meditateResendTicks = 20;
        }
    }

    /** The four ability KeyMappings, in slot order. Shift selects slots 5 to 8. */
    private static final KeyMapping[] ABILITY_KEYS = {
            KeyBindings.ABILITY_1, KeyBindings.ABILITY_2, KeyBindings.ABILITY_3, KeyBindings.ABILITY_4
    };

    /**
     * Holds the local player flat while they are on a ride that lies down — Water
     * Surf, and nothing else so far.
     *
     * The rider's own client is the one copy of them the server cannot settle. The
     * server's forced pose reaches everybody watching, and the synched swimming flag
     * reaches their RemotePlayer copies, but the LOCAL player recomputes its own pose
     * from scratch every tick in Player#updatePlayerPose and clears the swimming flag
     * in updateSwimming on the way — so from the inside the bender stood bolt upright
     * while everyone else saw them surfing.
     *
     * A forced pose (see ForcedPose) is what ends that argument: updatePlayerPose
     * returns it immediately and looks at nothing else. Set from a POST tick, so it
     * lands after the player's own tick has had its say, and rendering — which happens
     * between ticks — sees ours.
     *
     * Only ever cleared back from SWIMMING, so this cannot stamp on a forced pose set
     * by anything else.
     */
    private static void holdRidingPose(Minecraft mc) {
        if (mc.player == null) return;

        boolean laying = mc.player.getVehicle() instanceof BendingSeat seat && seat.isLaying();

        if (laying) {
            ForcedPose.set(mc.player, Pose.SWIMMING);
        } else if (ForcedPose.get(mc.player) == Pose.SWIMMING) {
            ForcedPose.set(mc.player, null);
        }
    }

    private static void onClientTick(Minecraft mc) {
        // Counted down here rather than in the renderer, which runs once per FRAME and
        // would run the shake down at whatever rate the machine happens to render.
        ClientShake.tick();
        ClientFlash.tick();

        holdRidingPose(mc);

        if (mc.player == null || mc.level == null) return;

        // Safely open the menu if the server flagged it AND no other screen is open.
        if (needsToOpenMenu && mc.gui.screen() == null) {
            mc.gui.setScreen(new ElementSelectionScreen());
            needsToOpenMenu = false;
        }

        // The client's own copy of "passives do not work here". The server sets the
        // same flag from its player tick; this side needs it because the menu and the
        // blood level readout both ask whether a passive is equipped, and a client
        // that thought one was still running would disagree with the server.
        var data = mc.player.getAttachedOrCreate(ModAttachments.BENDING_DATA);
        data.setPassivesSuppressed(SpiritWorld.isSpiritWorld(mc.level));

        // Asked whether or not a screen is open, so letting go of the key with a menu up
        // still reaches the server.
        syncMeditate();

        // Everything below is a key PRESS, and vanilla only registers presses while no
        // screen is open — so there is nothing to read while one is.
        if (mc.gui.screen() != null) return;

        // --- UNIVERSAL LEFT CLICK DETECTOR ---
        boolean isLeftClicking = mc.mouseHandler.isLeftPressed();
        if (isLeftClicking && !wasLeftClicking) {
            ClientPlayNetworking.send(new LeftClickTriggerPacket());
        }
        wasLeftClicking = isLeftClicking;

        boolean isShiftDown = mc.hasShiftDown();

        for (int i = 0; i < ABILITY_KEYS.length; i++) {
            int slot = isShiftDown ? i + 4 : i;

            while (ABILITY_KEYS[i].consumeClick()) {
                ClientPlayNetworking.send(new UseAbilityPacket(slot));
            }

            // Held/channeled abilities (Fire Breath and the rest): sent only on a CHANGE.
            boolean held = ABILITY_KEYS[i].isDown();
            if (held != lastAbilityHeld[i]) {
                ClientPlayNetworking.send(new AbilityHoldPacket(slot, held));
                lastAbilityHeld[i] = held;
            }
        }

        // Meditation and the rooting channels (the shields) hold the player still. The
        // input itself is taken away in onMovementInput; this stops momentum the player
        // was already carrying so they do not slide.
        if (data.isMeditating() || ClientRootState.isRooted()) {
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
        }

        while (KeyBindings.UPGRADE_MENU.consumeClick()) {
            mc.gui.setScreen(new UpgradeMenuScreen());
        }

        while (KeyBindings.SWITCH_ELEMENT.consumeClick()) {
            var unlocked = data.getUnlockedElements();

            if (unlocked.size() > 1) {
                int currentIndex = unlocked.indexOf(data.getActiveElement());
                String nextElement = unlocked.get((currentIndex + 1) % unlocked.size());

                data.setActiveElement(nextElement);
                ClientPlayNetworking.send(new SwitchElementPacket(nextElement));
            }
        }
    }

    /**
     * Rewrites this tick's movement keys, straight after vanilla has read them
     * (LocalPlayerMixin, at the point NeoForge's MovementInputUpdateEvent fired).
     *
     * Movement input exists ONLY on the client — the server sees the resulting motion,
     * not which key produced it — so this is the one place these effects can happen.
     * The effects themselves are held on the entity server-side and vanilla syncs them
     * to the owning player's client, which is why simply asking hasEffect() here is
     * enough and no packet of our own is needed.
     *
     * Since 1.21.2 the keys are an immutable {@link Input} record plus a separate move
     * vector, so each case builds a replacement rather than flipping fields.
     */
    public static void onMovementInput(LocalPlayer player, ClientInput input) {
        Input keys = input.keyPresses;

        // Stunned comes FIRST and returns: there is nothing to reverse once the input
        // has been thrown away, and a victim carrying both effects should be held still
        // rather than held still backwards.
        if (player.hasEffect(ModEffects.STUNNED)) {
            input.keyPresses = Input.EMPTY;
            input.moveVector = Vec2.ZERO;
            return;
        }

        // Meditating and rooted: no walking and no jumping. The server pins the player
        // too; taking the keys away here stops the client trying to walk and being
        // corrected every tick, which would rubber-band rather than hold still.
        var data = player.getAttachedOrCreate(ModAttachments.BENDING_DATA);
        if (data.isMeditating() || ClientRootState.isRooted()) {
            input.keyPresses = new Input(false, false, false, false, false, keys.shift(), false);
            input.moveVector = Vec2.ZERO;
            return;
        }

        // Disorientation (Air pull): every direction is reversed. Both the move vector
        // AND the key booleans are flipped — the vector is what actually moves the
        // player, the booleans are what vanilla reads for things like "is this player
        // trying to sprint forward", so leaving them alone would give a player who
        // walks backwards while still sprinting the way they pressed.
        if (player.hasEffect(ModEffects.DISORIENTATION)) {
            input.keyPresses = new Input(keys.backward(), keys.forward(), keys.right(), keys.left(),
                    keys.jump(), keys.shift(), keys.sprint());
            input.moveVector = input.moveVector.negated();
        }
    }

    /**
     * Whether a sound should be silenced: everything, while the local player is
     * Deafened (SoundManagerMixin).
     *
     * This is the WHOLE of that effect: sound exists only on the client, so the server
     * can no more mute somebody than it can read their movement keys.
     *
     * Deliberately NOT filtered by category. Muting effects but leaving music, or
     * leaving the UI, would be a half-deafness nobody asked for.
     */
    public static boolean isDeafened() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasEffect(ModEffects.DEAFENED);
    }

    /**
     * Forgets per-world client state on the way out of a world.
     *
     * The armor set is keyed on entity id, and ids start again in the next world —
     * without this, whichever entity happened to be handed a matching number would turn
     * up wearing stone.
     */
    private static void onLoggingOut() {
        ClientBendingArmor.clear();
        ClientShake.clear();
        ClientFlash.clear();

        // The next server starts out believing nobody is meditating.
        lastMeditateSent = false;
    }

    /**
     * Shakes the view while an Earthquake is running under this player
     * (GameRendererMixin, applied to the level's projection).
     *
     * The view is the only thing that should move: nudging the player entity instead
     * would fight the server about where they are.
     */
    public static void applyCameraShake(PoseStack poseStack, float partialTick) {
        if (!ClientShake.active()) return;

        poseStack.rotateDegrees(Axis.ZP, ClientShake.offset(2, partialTick));
        poseStack.rotateDegrees(Axis.XP, ClientShake.offset(1, partialTick));
        poseStack.rotateDegrees(Axis.YP, ClientShake.offset(0, partialTick));
    }
}
