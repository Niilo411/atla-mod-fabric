package com.minecraft.atlamod.client;

import com.minecraft.atlamod.spirit.SpiritWorld;
import com.minecraft.atlamod.spirit.island.IslandStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * A sky that changes with the island you are standing on.
 *
 * SINCE 1.21.11 MOST OF THIS IS DATA. Fog colour, sky colour, fog distance and the rest
 * are "environment attributes" that a BIOME can set, and they blend smoothly from one
 * biome to the next — so each island family's colours now live in its biome JSON under
 * data/atlamod/worldgen/biome, rather than in an effects object asked for every frame the
 * way the NeoForge build had to. That is the change that made the old note "this cannot
 * be data" stop being true.
 *
 * THE SKYBOX IS THE ONE PART THAT IS STILL NOT. Which dome is drawn — the overworld's,
 * the End's starfield, or none at all — is a single field on the DIMENSION type, so this
 * class answers it per island instead (client.mixin.SkyRendererMixin asks it in place of
 * the dimension's own value):
 *
 *   nether islands  -> no dome, as in the Nether, so their own fog is the whole sky
 *   end islands     -> the End's starfield
 *   the wasteland   -> no dome, over near-black fog: a lightless void that never changes
 *                      and cannot flicker, because nothing about it comes from a clock
 *   everything else -> the ordinary overworld sky, including the open void between islands
 *
 * WHAT DID NOT COME ACROSS: the nether islands' flat "nether" block shading. That is the
 * dimension's cardinal-light setting, which is baked into chunk meshes when they are
 * built rather than read per frame — so it could never follow the player per island
 * reliably anyway, and in the NeoForge build only ever applied to chunks that happened to
 * be meshed while standing on one.
 */
public final class SpiritSky {
    private SpiritSky() {
    }

    /**
     * The last answer, and the block it was worked out for.
     *
     * CACHED BECAUSE THIS IS A PER-FRAME PATH. A standing player asks the same question
     * about the same block every frame, and the answer costs a biome lookup and a walk over
     * the island styles. Keyed on the exact block and the level, so walking one block over
     * or stepping through a portal recomputes.
     */
    private static Level cachedLevel;
    private static long cachedPos = Long.MIN_VALUE;
    private static DimensionType.Skybox cached = DimensionType.Skybox.OVERWORLD;

    /** The skybox to draw: the dimension's own everywhere except the Spirit World. */
    public static DimensionType.Skybox skyboxFor(ClientLevel level, DimensionType.Skybox dimensionDefault) {
        if (!SpiritWorld.isSpiritWorld(level)) return dimensionDefault;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return DimensionType.Skybox.OVERWORLD;

        BlockPos pos = minecraft.player.blockPosition();
        long packed = pos.asLong();
        if (level == cachedLevel && packed == cachedPos) return cached;

        cachedLevel = level;
        cachedPos = packed;
        cached = lookUp(level, pos);
        return cached;
    }

    private static DimensionType.Skybox lookUp(Level level, BlockPos pos) {
        IslandStyle style = IslandStyle.forBiome(level.getBiome(pos));

        // Open void between islands, or some biome that is not ours at all.
        if (style == null) return DimensionType.Skybox.OVERWORLD;

        // Keyed on the FAMILY, not the individual biome, so every nether variant gets the
        // nether sky without this having to list them — and a variant added later is
        // covered the moment it names its family.
        return switch (style.family()) {
            case NETHER, WASTELAND -> DimensionType.Skybox.NONE;
            case END -> DimensionType.Skybox.END;
            default -> DimensionType.Skybox.OVERWORLD;
        };
    }
}
