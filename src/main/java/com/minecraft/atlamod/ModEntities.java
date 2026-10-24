package com.minecraft.atlamod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> BENDING_SEAT_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Atlamod.id("bending_seat"));

    /**
     * The invisible seat Air scooter, Water Surf and Earth trap put a rider on. Never
     * saved (see BendingSeat#shouldBeSaved), never summonable, and updated every tick so
     * the client sees the ride move smoothly rather than in the default jumps.
     */
    public static final EntityType<BendingSeat> BENDING_SEAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, BENDING_SEAT_KEY,
            EntityType.Builder.<BendingSeat>of(BendingSeat::new, MobCategory.MISC)
                    .sized(BendingSeat.SIZE.width(), BendingSeat.SIZE.height())
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSummon()
                    .fireImmune()
                    .build(BENDING_SEAT_KEY));

    private ModEntities() {
    }

    /** Touching the class is what registers it. */
    public static void register() {
    }
}
