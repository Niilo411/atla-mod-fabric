package com.minecraft.atlamod.mixin.client;

import com.minecraft.atlamod.client.ClientEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * Earthquake's camera shake, applied to the level's projection exactly where vanilla
     * applies its own hurt wobble — so it moves the view of the world and nothing else,
     * leaving the hand and the HUD steady. Stands in for NeoForge's ComputeCameraAngles.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void atlamod$cameraShake(CallbackInfo ci, @Local PoseStack poseStack) {
        // renderLevel takes no arguments since 26.1, so the partial tick is asked for.
        ClientEvents.applyCameraShake(poseStack,
                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
    }
}
