package io.github.niilo411.elementalpaths.mixin.client;

import io.github.niilo411.elementalpaths.client.ClientEvents;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    public ClientInput input;

    /**
     * NeoForge's MovementInputUpdateEvent: straight after vanilla reads the movement keys
     * and before anything uses them. See ClientEvents#onMovementInput.
     */
    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void elemental_paths$movementInput(CallbackInfo ci) {
        ClientEvents.onMovementInput((LocalPlayer) (Object) this, input);
    }
}
