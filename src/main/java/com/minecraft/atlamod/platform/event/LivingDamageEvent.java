package com.minecraft.atlamod.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class LivingDamageEvent {
    private LivingDamageEvent() {}

    /** Damage that actually got through and was applied. */
    public record Post(LivingEntity getEntity, DamageSource getSource, float getNewDamage) {}
}
