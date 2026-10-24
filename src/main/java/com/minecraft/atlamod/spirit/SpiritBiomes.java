package com.minecraft.atlamod.spirit;

import com.minecraft.atlamod.Atlamod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class SpiritBiomes {
    public static final ResourceKey<Biome> VOID = ResourceKey.create(Registries.BIOME,
            Atlamod.id("spirit_void"));

    private SpiritBiomes() {
    }

    /** The Spirit World's biome source codec, which the dimension JSON names as "atlamod:spirit". */
    public static void register() {
        Registry.register(BuiltInRegistries.BIOME_SOURCE, Atlamod.id("spirit"), SpiritBiomeSource.CODEC);
    }
}
