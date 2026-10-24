package com.minecraft.atlamod.spirit.island;

import com.minecraft.atlamod.Atlamod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ModFeatures {
    /** The Spirit World's ground: every floating island is laid by this one feature. */
    public static final SpiritIslandFeature SPIRIT_ISLAND = Registry.register(BuiltInRegistries.FEATURE,
            Atlamod.id("spirit_island"), new SpiritIslandFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    /** Touching the class is what registers it. */
    public static void register() {
    }
}
