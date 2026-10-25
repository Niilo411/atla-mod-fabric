package io.github.niilo411.elementalpaths.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.github.niilo411.elementalpaths.platform.ServerHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * NeoForge's LivingIncomingDamageEvent, at the same place it fires: straight after
     * vanilla's own refusals (invulnerable, already dying, fire into fire resistance) and
     * before anything else — blocking, helmets, invulnerability frames, armour — has
     * looked at the figure. Firing any earlier would let things like Flesh shield soak up
     * a blow the victim was never going to take.
     */
    @Inject(method = "hurtServer", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z", ordinal = 0))
    private void elemental_paths$incomingDamage(ServerLevel level, DamageSource source, float damage,
                                        CallbackInfoReturnable<Boolean> cir,
                                        @Local(argsOnly = true) LocalFloatRef amount) {
        Float result = ServerHooks.incomingDamage((LivingEntity) (Object) this, source, amount.get());
        if (result == null) {
            cir.setReturnValue(false);
        } else {
            amount.set(result);
        }
    }

    /** NeoForge's LivingFallEvent: a cancelled fall bills nothing and makes no thud. */
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void elemental_paths$fall(double distance, float multiplier, DamageSource source,
                              CallbackInfoReturnable<Boolean> cir) {
        if (ServerHooks.fallCancelled((LivingEntity) (Object) this, distance)) {
            cir.setReturnValue(false);
        }
    }
}
