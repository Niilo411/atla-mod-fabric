package com.minecraft.atlamod.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.minecraft.atlamod.client.SpiritSky;
import com.minecraft.atlamod.client.WorldEventSky;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    /** The Spirit World's skybox follows the island underfoot. See SpiritSky. */
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;skybox()Lnet/minecraft/world/level/dimension/DimensionType$Skybox;"))
    private DimensionType.Skybox atlamod$islandSkybox(DimensionType.Skybox original,
                                                      ClientLevel level, float partialTick, Camera camera,
                                                      SkyRenderState state) {
        return SpiritSky.skyboxFor(level, original);
    }

    /** The blood moon: vanilla's moon, handed to the shader red. See WorldEventSky. */
    @ModifyArg(method = "renderMoon", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private Vector4f atlamod$bloodMoon(Vector4f colour) {
        Vector4f tinted = new Vector4f(colour);
        WorldEventSky.tintMoon(tinted);
        return tinted;
    }

    /** The comet and the eclipse, drawn once vanilla's sun, moon and stars are down. */
    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void atlamod$worldEvents(RenderPass renderPass, PoseStack poseStack,
                                     float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness,
                                     CallbackInfo ci) {
        WorldEventSky.renderEvents(renderPass, sunAngle, rainBrightness);
    }
}
