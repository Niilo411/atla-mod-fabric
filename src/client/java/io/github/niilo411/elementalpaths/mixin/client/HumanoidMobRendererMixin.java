package io.github.niilo411.elementalpaths.mixin.client;

import io.github.niilo411.elementalpaths.ChannelingSeat;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public abstract class HumanoidMobRendererMixin {
    /**
     * Stands a rider up on a seat that says so (Water Surf, Earth trap). Vanilla draws
     * every passenger sitting; NeoForge asked the vehicle's shouldRiderSit, and this puts
     * that question back. Covers players and humanoid mobs, which both come through here.
     */
    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void elemental_paths$standingRider(LivingEntity entity, HumanoidRenderState state, float partialTick,
                                              ItemModelResolver itemModelResolver, CallbackInfo ci) {
        if (entity.getVehicle() instanceof ChannelingSeat seat && !seat.shouldRiderSit()) {
            state.isPassenger = false;
        }
    }
}
