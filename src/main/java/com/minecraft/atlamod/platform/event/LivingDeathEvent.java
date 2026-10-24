package com.minecraft.atlamod.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public record LivingDeathEvent(LivingEntity getEntity, DamageSource getSource) {}
