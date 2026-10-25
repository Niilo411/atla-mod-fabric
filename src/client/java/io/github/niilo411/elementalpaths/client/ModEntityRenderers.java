package io.github.niilo411.elementalpaths.client;

import net.minecraft.world.entity.EntityTypes;
import io.github.niilo411.elementalpaths.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;

public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    public static void register() {
        // The seat is never drawn — only its rider is.
        EntityRenderers.register(ModEntities.CHANNELING_SEAT, NoopRenderer::new);

        // The channeling armor suits, on both player models (wide and slim arms).
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (type == EntityTypes.PLAYER && renderer instanceof AvatarRenderer<?> paragon) {
                helper.register(new ChannelingArmorLayer(paragon, context.getModelSet(), paragon.getModel().slim));
            }
        });
    }
}
