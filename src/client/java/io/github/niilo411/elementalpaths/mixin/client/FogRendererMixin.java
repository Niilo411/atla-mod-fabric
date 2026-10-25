package io.github.niilo411.elementalpaths.mixin.client;

import io.github.niilo411.elementalpaths.client.WorldEventSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    /**
     * The world events' fog tint, applied to the final fog colour exactly where NeoForge's
     * ComputeFogColor event used to fire. See WorldEventSky#tintFog.
     */
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void elemental_paths$worldEventFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                       float darkenAmount, CallbackInfoReturnable<Vector4f> cir) {
        WorldEventSky.tintFog(cir.getReturnValue());
    }
}
