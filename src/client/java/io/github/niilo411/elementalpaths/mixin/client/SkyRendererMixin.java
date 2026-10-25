package io.github.niilo411.elementalpaths.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.niilo411.elementalpaths.client.SpiritSky;
import io.github.niilo411.elementalpaths.client.WorldEventSky;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    /** The Spirit Realm's skybox follows the island underfoot. See SpiritSky. */
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;skybox()Lnet/minecraft/world/level/dimension/DimensionType$Skybox;"))
    private DimensionType.Skybox elemental_paths$islandSkybox(DimensionType.Skybox original,
                                                      ClientLevel level, float partialTick, Camera camera,
                                                      SkyRenderState state) {
        return SpiritSky.skyboxFor(level, original);
    }

    /** The blood moon: vanilla's moon, handed to the shader red. See WorldEventSky. */
    @ModifyArg(method = "renderMoon", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private Vector4fc elemental_paths$bloodMoon(Vector4fc colour) {
        Vector4f tinted = new Vector4f(colour);
        WorldEventSky.tintMoon(tinted);
        return tinted;
    }

    /** The comet and the eclipse, drawn once vanilla's sun, moon and stars are down. */
    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void elemental_paths$worldEvents(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
                                     MoonPhase moonPhase, float rainBrightness, float starBrightness,
                                     CallbackInfo ci) {
        WorldEventSky.renderEvents(sunAngle, rainBrightness);
    }
}
