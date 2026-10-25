package io.github.niilo411.elementalpaths.mixin;

import io.github.niilo411.elementalpaths.platform.ServerHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    /** NeoForge's EntityTickEvent.Post, for an entity ticked on its own... */
    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void elemental_paths$entityTick(Entity entity, CallbackInfo ci) {
        ServerHooks.entityTick(entity);
    }

    /** ...and for one ticked as a passenger, which is how a trapped or orbiting mob ticks. */
    @Inject(method = "tickPassenger", at = @At("TAIL"))
    private void elemental_paths$passengerTick(Entity vehicle, Entity passenger, CallbackInfo ci) {
        ServerHooks.entityTick(passenger);
    }
}
