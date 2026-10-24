package com.minecraft.atlamod.client;

import com.minecraft.atlamod.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;

public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    public static void register() {
        // The seat is never drawn — only its rider is.
        EntityRenderers.register(ModEntities.BENDING_SEAT, NoopRenderer::new);

        // The bending armor suits, on both player models (wide and slim arms).
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (type == EntityType.PLAYER && renderer instanceof AvatarRenderer<?> avatar) {
                helper.register(new BendingArmorLayer(avatar, context.getModelSet(), avatar.getModel().slim));
            }
        });
    }
}
