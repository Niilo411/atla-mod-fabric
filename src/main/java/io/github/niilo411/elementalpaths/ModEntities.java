package io.github.niilo411.elementalpaths;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> CHANNELING_SEAT_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, ElementalPaths.id("channeling_seat"));

    /**
     * The invisible seat Gale Ride, Water Surf and Earth trap put a rider on. Never
     * saved (see ChannelingSeat#shouldBeSaved), never summonable, and updated every tick so
     * the client sees the ride move smoothly rather than in the default jumps.
     */
    public static final EntityType<ChannelingSeat> CHANNELING_SEAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, CHANNELING_SEAT_KEY,
            EntityType.Builder.<ChannelingSeat>of(ChannelingSeat::new, MobCategory.MISC)
                    .sized(ChannelingSeat.SIZE.width(), ChannelingSeat.SIZE.height())
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSummon()
                    .fireImmune()
                    .build(CHANNELING_SEAT_KEY));

    private ModEntities() {
    }

    /** Touching the class is what registers it. */
    public static void register() {
    }
}
