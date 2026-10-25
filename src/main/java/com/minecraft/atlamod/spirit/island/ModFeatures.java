package com.minecraft.atlamod.spirit.island;

import com.minecraft.atlamod.Atlamod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModFeatures {
    /**
     * The Spirit World's ground: every floating island is laid by this one feature.
     *
     * What is registered is the CODEC, in FEATURE_TYPE — since 26.1 the feature object
     * itself only exists once worldgen/feature/spirit_island.json has been read through it.
     */
    static {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, Atlamod.id("spirit_island"), SpiritIslandFeature.CODEC);
    }

    private ModFeatures() {
    }

    /** Touching the class is what registers it. */
    public static void register() {
    }
}
