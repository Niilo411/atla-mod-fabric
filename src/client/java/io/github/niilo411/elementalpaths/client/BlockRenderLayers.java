package io.github.niilo411.elementalpaths.client;

import io.github.niilo411.elementalpaths.ElementalPaths;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Which blocks are drawn with see-through pixels.
 *
 * The NeoForge build said this in each model's JSON ("render_type"), which is a NeoForge
 * extension vanilla ignores — without this, the fire would draw as a solid square and the
 * portal as an opaque wall. These are the same layers vanilla uses for its own fire and
 * nether portal, which is what both blocks are drawn as.
 */
public final class BlockRenderLayers {
    private BlockRenderLayers() {
    }

    public static void register() {
        BlockRenderLayerMap.putBlock(ElementalPaths.CHANNELING_FIRE, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ElementalPaths.SPIRIT_PORTAL, ChunkSectionLayer.TRANSLUCENT);
    }
}
