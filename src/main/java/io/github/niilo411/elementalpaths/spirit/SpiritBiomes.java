package io.github.niilo411.elementalpaths.spirit;

import io.github.niilo411.elementalpaths.ElementalPaths;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class SpiritBiomes {
    public static final ResourceKey<Biome> VOID = ResourceKey.create(Registries.BIOME,
            ElementalPaths.id("spirit_void"));

    private SpiritBiomes() {
    }

    /** The Spirit Realm's biome source codec, which the dimension JSON names as "elemental_paths:spirit". */
    public static void register() {
        Registry.register(BuiltInRegistries.BIOME_SOURCE, ElementalPaths.id("spirit"), SpiritBiomeSource.CODEC);
    }
}
