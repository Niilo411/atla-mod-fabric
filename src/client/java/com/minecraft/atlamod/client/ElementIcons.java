package com.minecraft.atlamod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Element emblems for the UI.
 *
 * Only elements listed here have artwork. Anything missing falls back to the old
 * "?" box rather than being blitted anyway, because a texture Minecraft cannot find
 * renders as the black and magenta checkerboard — which looks far more broken than
 * an honest placeholder.
 *
 * Adding an element is a PNG in textures/gui/elements/ plus a line in ICONS.
 */
public final class ElementIcons {

    /**
     * Side length the source PNGs are expected to be. Anything square works as long
     * as this matches, since the icon is scaled to whatever size the caller asks for.
     */
    private static final int SOURCE_SIZE = 256;

    private static final Map<String, Identifier> ICONS = Map.of(
            "fire", Identifier.fromNamespaceAndPath("atlamod", "textures/gui/elements/fire_icon.png"),
            "water", Identifier.fromNamespaceAndPath("atlamod", "textures/gui/elements/water_icon.png"),
            "air", Identifier.fromNamespaceAndPath("atlamod", "textures/gui/elements/air_icon.png"),
            "earth", Identifier.fromNamespaceAndPath("atlamod", "textures/gui/elements/earth_icon.png"),

            // Not a bending art, but it is chosen from the same screen and worn in the
            // same HUD badge, so it needs an emblem for exactly the same reasons. Ours
            // rather than anyone's — see tools/GenChiIcon.java.
            "nobending", Identifier.fromNamespaceAndPath("atlamod", "textures/gui/elements/nobending_icon.png")
    );

    private ElementIcons() {
    }

    /** Whether this element has artwork to draw. */
    public static boolean has(String element) {
        return element != null && ICONS.containsKey(element.toLowerCase());
    }

    /**
     * Draws the element emblem into a square of {@code size} at {@code x, y}.
     *
     * Scaled through the pose stack rather than by the blit arguments: blit's width
     * arguments set the source region as well as the drawn size, so it cannot resize
     * on its own. Scaling here means the PNG can stay whatever resolution it was
     * drawn at instead of having to match every place it appears.
     */
    public static void draw(GuiGraphicsExtractor graphics, String element, int x, int y, int size) {
        Identifier icon = ICONS.get(element.toLowerCase());
        if (icon == null) return;

        float scale = size / (float) SOURCE_SIZE;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0.0F, 0.0F,
                SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE);

        graphics.pose().popMatrix();
    }
}
