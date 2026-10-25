package com.minecraft.atlamod.client;

import com.minecraft.atlamod.Atlamod;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;

import java.util.List;

/**
 * What makes the spirit portal BLUE without a texture of our own.
 *
 * The portal's model is vanilla's nether portal geometry wearing vanilla's own
 * block/nether_portal texture, with one thing added: its faces carry "tintindex": 0.
 * A tint index is the game asking "what colour should this face be multiplied by?",
 * and this handler is the answer. It is the same mechanism grass, leaves and water use
 * to be different colours in different biomes off one grey texture.
 *
 * So the whole recolour is two small JSON models plus the constant below, and the mod
 * ships no portal art at all — the pixels come from the player's own copy of the game.
 *
 * Note the texture is purple to begin with, so the tint MULTIPLIES rather than replaces:
 * the red channel has to be cut hard to get blue out of it, which is why the value is
 * not simply 0x0000FF. Killing red entirely would make the portal's swirls black where
 * the texture is reddest, so a little is left in.
 */
public final class SpiritPortalColors {

    /** Multiplied onto the purple nether portal texture to land on blue. */
    private static final int BLUE = 0xFF3060FF;

    private SpiritPortalColors() {
    }

    public static void register() {
        // Since 26.1 a block's tints are a list of SOURCES, one per tint index, and their
        // colours are ARGB, so BLUE carries a full alpha byte.
        BlockColorRegistry.register(List.of(BlockTintSources.constant(BLUE)), Atlamod.SPIRIT_PORTAL);
    }
}
