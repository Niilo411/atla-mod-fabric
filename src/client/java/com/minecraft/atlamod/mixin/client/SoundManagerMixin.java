package com.minecraft.atlamod.mixin.client;

import com.minecraft.atlamod.client.ClientEvents;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    /** Deafened: nothing the game tries to play is heard. See ClientEvents#isDeafened. */
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void atlamod$deafen(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (ClientEvents.isDeafened()) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
