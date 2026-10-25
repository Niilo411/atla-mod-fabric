package io.github.niilo411.elementalpaths.client;

import io.github.niilo411.elementalpaths.ElementalPaths;
import io.github.niilo411.elementalpaths.events.WorldEvents;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * What the three world events look like.
 *
 * EVERYTHING IN THE SKY IS DRAWN RATHER THAN TINTED. The comet is ADDED where there was
 * nothing, the blood moon is vanilla's moon arriving red, and the eclipse takes the sun
 * back out of the sky. Only the overall colour of the light is still fog.
 *
 * WHERE IT HOOKS IN (client.mixin.SkyRendererMixin): since 1.21.5 the sky is a render
 * pass of vanilla's own, drawn through render pipelines rather than the old immediate
 * GL state, and there is no "after the sky" event any more. So the moon's colour is
 * changed where vanilla hands it to the shader, and everything else here is drawn at the
 * end of vanilla's sun-moon-and-stars pass — the same moment the NeoForge build's
 * AFTER_SKY stage gave it, with the sky dome down and nothing solid laid over it yet.
 * That pass only runs under an ordinary overworld-style sky, so the Nether, the End and
 * the Spirit Realm's starless islands are excluded for free.
 *
 * THE ONE FACT THE WHOLE FILE TURNS ON: vanilla's sun and moon textures are 100% opaque
 * — measured, not assumed. Their shape lives entirely in their BRIGHTNESS, and it is the
 * additive blend that makes their dark parts invisible. So ordinary alpha blending cannot
 * recolour them: painting a red moon over the white one with alpha paints a red SQUARE,
 * and the first hollow sun was a hard black rectangle hanging in the sky. Anything that
 * changes one of them has to work in brightness too.
 *
 * THE BLOOD MOON IS NOW ONE PASS, not two. The NeoForge build subtracted vanilla's white
 * moon and added a red one back over it. Since 1.21.5 the colour vanilla draws the moon
 * with is a value it passes to the shader, so it can simply be red from the start: the
 * additive blend multiplies the texture by it, which is exactly what subtracting white and
 * adding red produced — craters and phase intact.
 *
 * THE ECLIPSE IS STILL A SUBTRACTION, and has to be: taking the sun out SUBTRACTIVELY also
 * darkens the sky behind it, which is what leaves a black disc where the sun was rather
 * than a sun that merely faded into an ordinary blue sky. That needs a blend vanilla has
 * no pipeline for, so this registers one (SUBTRACT_CELESTIAL).
 *
 * WHAT IS STILL FOG is the colour of the light itself: the night going red under a blood
 * moon, and the world going dark under an eclipse. TINTED, NEVER REPLACED — every colour
 * is blended TOWARDS what vanilla already chose, so weather, biome, time of day and depth
 * all still show through.
 */
public final class WorldEventSky {

    /** How far the fog is dragged towards each event's colour, at its strongest. */
    private static final float BLOOD_MOON_STRENGTH = 0.75F;
    private static final float HOLLOW_SUN_STRENGTH = 0.90F;

    /** How strong the screen wash is at its deepest. Deliberately slight. */
    private static final float SCREEN_TINT = 0.16F;

    private static final Identifier COMET = Identifier.fromNamespaceAndPath(
            ElementalPaths.MODID, "textures/environment/ember_comet.png");

    /**
     * Where the comet hangs, as two rotations away from straight overhead.
     *
     * FIXED, NOT FOLLOWING THE SUN, which is the whole reason it is two constants rather
     * than a function of the time. The comet is meant to be up for the entire day and
     * night, so anything derived from the clock would have it rise and set like everything
     * else up there. It simply sits in one part of the sky and stays.
     *
     * TILT is degrees away from the zenith — 0 would be directly overhead, 90 on the
     * horizon — so 52 puts it high enough to be seen without craning and low enough to
     * meet the eye. COMPASS swings it around so it does not share the sun's arc.
     */
    private static final float COMET_TILT = 52.0F;
    private static final float COMET_COMPASS = -40.0F;

    /**
     * Half the quad's width. The sun's is 30.
     *
     * Most of the sheet is tail and empty space, so the bright head lands at a few pixels
     * across. Raising this grows the whole streak, head included, rather than just making
     * it brighter.
     */
    private static final float COMET_SIZE = 24.0F;

    /** How far up the sky dome everything is drawn. Vanilla's own figure for sun and moon. */
    private static final float SKY_DISTANCE = 100.0F;

    /** Vanilla's sun size, so the eclipse lands exactly on it. */
    private static final float SUN_SIZE = 30.0F;

    /**
     * The blood moon's colour, MULTIPLIED over the moon's own texture.
     *
     * The RED CHANNEL IS LEFT AT FULL, which is what keeps the moon as bright in red as
     * vanilla's is in white. An earlier attempt scaled all three down and the moon came
     * out a dark smudge. Green and blue near zero are what make it red rather than pink.
     */
    private static final float MOON_RED = 1.0F;
    private static final float MOON_GREEN = 0.13F;
    private static final float MOON_BLUE = 0.10F;

    /** How many stars come out during the eclipse. A scattering, not a full night. */
    private static final int ECLIPSE_STARS = 120;

    /** Fixed, so the same stars come out every time. Not vanilla's, so they are not its. */
    private static final long STAR_SEED = 0x5EED57A45L;

    /**
     * Vanilla's celestial shader with the blend turned around: {@code dst = dst * (1 - src)}.
     * Where the texture is bright the sky goes black, where it is black the sky is
     * untouched, and in between it feathers — exactly how to take back out something that
     * was put in additively.
     */
    private static final RenderPipeline SUBTRACT_CELESTIAL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ElementalPaths.id("pipeline/subtract_celestial"))
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withSampler("Sampler0")
                    .withBlend(new BlendFunction(SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_COLOR,
                            SourceFactor.ZERO, DestFactor.ONE))
                    .withDepthWrite(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
                    .build());

    /** Built on first use and kept: the same comet, sun and stars every frame. */
    private static GpuBuffer cometQuad;
    private static GpuBuffer sunQuad;
    private static GpuBuffer starQuads;
    private static int starIndexCount;

    private WorldEventSky() {
    }

    /** Touching the class registers its pipeline; done at client start-up. */
    public static void register() {
    }

    // ==========================================================================
    //  Hooks called from SkyRendererMixin
    // ==========================================================================

    /**
     * The colour vanilla is about to draw the moon with, red under a blood moon.
     * Rain's dimming (the alpha) is left exactly as vanilla set it.
     */
    public static void tintMoon(Vector4f colour) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !WorldEvents.isActive(mc.level, WorldEvents.Event.BLOOD_MOON)) return;

        colour.set(colour.x * MOON_RED, colour.y * MOON_GREEN, colour.z * MOON_BLUE, colour.w);
    }

    /**
     * Draws the comet and the eclipse onto the sky, straight after vanilla's sun, moon and
     * stars.
     *
     * @param sunAngle        vanilla's own celestial rotation for the sun, in radians
     * @param rainBrightness  1 under a clear sky, 0 in a downpour — vanilla's own figure.
     *                        Rain hides the comet exactly as it hides the sun and moon;
     *                        without that it would burn cheerfully through a thunderstorm.
     */
    public static void renderEvents(float sunAngle, float rainBrightness) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Underwater vanilla still draws the sky, but fog swallows it — the NeoForge build
        // drew nothing here, and neither does this.
        if (mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) return;

        float eclipse = WorldEvents.eclipseDepth(mc.level);
        boolean comet = WorldEvents.isActive(mc.level, WorldEvents.Event.EMBER_COMET);
        if (!comet && eclipse <= 0.0F) return;

        if (comet) {
            PoseStack pose = new PoseStack();
            // Tilted away from the zenith FIRST and then swung around the compass — the
            // other way round would swing the tilt itself.
            pose.mulPose(Axis.YP.rotationDegrees(COMET_COMPASS));
            pose.mulPose(Axis.XP.rotationDegrees(COMET_TILT));
            drawComet(pose.last().pose(), rainBrightness);
        }

        if (eclipse > 0.0F) {
            // VANILLA'S OWN CELESTIAL FRAME, the same two rotations its sun is drawn in, so
            // everything here lands on the sun and turns with the day the same way.
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotation(sunAngle));
            Matrix4f celestial = pose.last().pose();

            // STARS FIRST, so taking the sun out also takes out any star behind it rather
            // than leaving it shining through the black disc.
            float depth = eclipse * rainBrightness;
            drawEclipseStars(celestial, depth);
            drawHollowSun(celestial, depth);
        }
    }

    // ==========================================================================
    //  The drawing itself
    // ==========================================================================

    /** The comet: ADDED to the sky with vanilla's sun blend, so it glows rather than covering. */
    private static void drawComet(Matrix4f pose, float alpha) {
        if (cometQuad == null) cometQuad = buildQuad("Paths comet quad", 0.0F, 0.0F, 1.0F, 1.0F);

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(COMET);
        draw("Paths comet", RenderPipelines.CELESTIAL, cometQuad, 6, pose, COMET_SIZE,
                new Vector4f(1.0F, 1.0F, 1.0F, alpha), texture.getTextureView(), texture.getSampler());
    }

    /**
     * The eclipse: the sun's own light taken back out of the sky, in proportion to how far
     * the moon has crossed it. Nothing is added in its place — what is left is the black of
     * the darkened sky, and the faint halo the subtraction leaves reads as corona.
     */
    private static void drawHollowSun(Matrix4f pose, float depth) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.CELESTIALS);
        if (sunQuad == null) {
            TextureAtlasSprite sun = atlas.getSprite(Identifier.withDefaultNamespace("sun"));
            sunQuad = buildQuad("Paths eclipse quad", sun.getU0(), sun.getV0(), sun.getU1(), sun.getV1());
        }

        draw("Paths eclipse", SUBTRACT_CELESTIAL, sunQuad, 6, pose, SUN_SIZE,
                new Vector4f(depth, depth, depth, 1.0F), atlas.getTextureView(), atlas.getSampler());
    }

    /**
     * A scattering of stars, brightening as the eclipse deepens.
     *
     * OURS RATHER THAN VANILLA'S. Vanilla's star brightness is driven by the time of day,
     * which is exactly what has to be ignored here — the whole point is stars at noon —
     * and its field is fifteen hundred strong, a night sky rather than a scattering.
     */
    private static void drawEclipseStars(Matrix4f pose, float brightness) {
        if (starQuads == null) buildStars();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(pose);

        GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(modelView,
                new Vector4f(brightness, brightness, brightness, brightness), new Vector3f(), new Matrix4f());
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

        try (RenderPass pass = openPass("Paths eclipse stars")) {
            pass.setPipeline(RenderPipelines.STARS);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            pass.setVertexBuffer(0, starQuads);
            pass.setIndexBuffer(indices.getBuffer(starIndexCount), indices.type());
            pass.drawIndexed(0, 0, starIndexCount, 1);
        }

        modelView.popMatrix();
    }

    /**
     * One textured quad on the sky dome, drawn the way vanilla draws its sun: a unit quad
     * in the XZ plane, pushed {@link #SKY_DISTANCE} up and scaled to size.
     */
    private static void draw(String label, RenderPipeline pipeline, GpuBuffer quad, int indexCount,
                             Matrix4f pose, float halfSize, Vector4f colour,
                             GpuTextureView texture, GpuSampler sampler) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(pose);
        modelView.translate(0.0F, SKY_DISTANCE, 0.0F);
        modelView.scale(halfSize, 1.0F, halfSize);

        GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, colour, new Vector3f(), new Matrix4f());
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

        try (RenderPass pass = openPass(label)) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            pass.bindTexture("Sampler0", texture, sampler);
            pass.setVertexBuffer(0, quad);
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
            pass.drawIndexed(0, 0, indexCount, 1);
        }

        modelView.popMatrix();
    }

    private static RenderPass openPass(String label) {
        GpuTextureView colour = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
        return RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> label, colour, OptionalInt.empty(), depth, OptionalDouble.empty());
    }

    /** A unit quad facing down onto the viewer, with the given UVs. Vanilla's own winding. */
    private static GpuBuffer buildQuad(String label, float u0, float v0, float u1, float v1) {
        VertexFormat format = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(4 * format.getVertexSize())) {
            BufferBuilder buffer = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, format);
            buffer.addVertex(-1.0F, 0.0F, -1.0F).setUv(u0, v0);
            buffer.addVertex(1.0F, 0.0F, -1.0F).setUv(u1, v0);
            buffer.addVertex(1.0F, 0.0F, 1.0F).setUv(u1, v1);
            buffer.addVertex(-1.0F, 0.0F, 1.0F).setUv(u0, v1);

            try (MeshData mesh = buffer.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    /**
     * Builds the star quads, using vanilla's own construction.
     *
     * Each star is a point on a sphere of radius 100 with a small square built FACING THE
     * ORIGIN — which is what the quaternion does, rotating the square's own normal onto
     * the direction of the star. Without it the squares would all face one way and half of
     * them would be edge-on and invisible.
     *
     * Built ONCE and kept, because it is the same sky every time.
     */
    private static void buildStars() {
        RandomSource random = RandomSource.create(STAR_SEED);
        VertexFormat format = DefaultVertexFormat.POSITION;

        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(ECLIPSE_STARS * 4 * format.getVertexSize())) {
            BufferBuilder buffer = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, format);
            int made = 0;

            while (made < ECLIPSE_STARS) {
                float x = random.nextFloat() * 2.0F - 1.0F;
                float y = random.nextFloat() * 2.0F - 1.0F;
                float z = random.nextFloat() * 2.0F - 1.0F;
                float size = 0.15F + random.nextFloat() * 0.1F;

                // Rejected outside the unit sphere so the directions are evenly spread; a
                // cube taken as-is would crowd the corners. Too near the middle and the
                // normalise is unstable.
                float lengthSq = x * x + y * y + z * z;
                if (lengthSq <= 0.010000001F || lengthSq >= 1.0F) continue;

                Vector3f centre = new Vector3f(x, y, z).normalize(SKY_DISTANCE);
                float spin = (float) (random.nextDouble() * Math.PI * 2.0);
                Quaternionf facing = new Quaternionf()
                        .rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), centre)
                        .rotateZ(spin);

                buffer.addVertex(new Vector3f(size, -size, 0.0F).rotate(facing).add(centre));
                buffer.addVertex(new Vector3f(size, size, 0.0F).rotate(facing).add(centre));
                buffer.addVertex(new Vector3f(-size, size, 0.0F).rotate(facing).add(centre));
                buffer.addVertex(new Vector3f(-size, -size, 0.0F).rotate(facing).add(centre));
                made++;
            }

            try (MeshData mesh = buffer.buildOrThrow()) {
                starIndexCount = mesh.drawState().indexCount();
                starQuads = RenderSystem.getDevice().createBuffer(() -> "Paths eclipse stars",
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    // ==========================================================================
    //  The two events that are about light rather than objects
    // ==========================================================================

    /**
     * The fog, pulled towards whichever event is running (FogRendererMixin).
     *
     * THE COMET IS NOT HERE. It used to drag the fog orange for a whole day, which washed
     * the world out without putting a comet anywhere; it is drawn instead. What remains are
     * the two events that really are about the light itself.
     */
    public static void tintFog(Vector4f fog) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Level level = mc.level;

        if (WorldEvents.isActive(level, WorldEvents.Event.BLOOD_MOON)) {
            blend(fog, 0.42F, 0.02F, 0.03F, BLOOD_MOON_STRENGTH);
        }

        // Scaled by how far through the crossing we are, so the sun dims and comes back
        // rather than switching. At the middle of the six minutes it is total.
        float eclipse = WorldEvents.eclipseDepth(level);
        if (eclipse > 0.0F) {
            blend(fog, 0.02F, 0.02F, 0.04F, HOLLOW_SUN_STRENGTH * eclipse);
        }
    }

    /** Moves the fog a fraction of the way towards a colour, leaving the rest as it was. */
    private static void blend(Vector4f fog, float red, float green, float blue, float amount) {
        fog.x += (red - fog.x) * amount;
        fog.y += (green - fog.y) * amount;
        fog.z += (blue - fog.z) * amount;
    }

    /**
     * A wash of colour over the whole screen while an event runs.
     *
     * The fog only shows at distance, so indoors and underground an event would otherwise
     * be invisible — and the eclipse in particular is something you should notice from
     * inside a house. Kept slight: this sits over everything including the inventory.
     *
     * The comet has none. It is a thing in the sky rather than a change in the light, and
     * a permanent orange wash for a whole day is exactly what it was asked not to be.
     */
    public static void renderScreenTint(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Level level = mc.level;
        int colour = 0;

        if (WorldEvents.isActive(level, WorldEvents.Event.BLOOD_MOON)) {
            colour = tint(0xFF0A05, SCREEN_TINT);
        }

        float eclipse = WorldEvents.eclipseDepth(level);
        if (eclipse > 0.0F) {
            // Far heavier than the blood moon's, because "the sun went out" is the event.
            colour = tint(0x000008, 0.55F * eclipse);
        }

        if (colour == 0) return;

        graphics.fill(0, 0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(), colour);
    }

    /** An RGB with an alpha put on it. */
    private static int tint(int rgb, float alpha) {
        int a = Math.round(Math.min(1.0F, Math.max(0.0F, alpha)) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
