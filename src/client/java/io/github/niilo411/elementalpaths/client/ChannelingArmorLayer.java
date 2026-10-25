package io.github.niilo411.elementalpaths.client;

import io.github.niilo411.elementalpaths.ChannelingArmorSuit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Draws whichever channeling armor suit a player is wearing over the top of them.
 *
 * A LAYER rather than a change of equipment, which is the whole reason the abilities
 * can say "adds armor on top of whatever you have on" and mean it — the channeler's real
 * armor is untouched underneath, and simply not visible while the suit is over it.
 *
 * What to draw is asked of ClientChannelingArmor rather than of the entity's effects,
 * because a client only ever knows its OWN effects: for anyone else, the server has to
 * say so, which is what ChannelingArmorPacket is for.
 *
 * ONE layer serves every suit. The alternative — a layer per suit — would draw two
 * sheets onto the same model whenever a channeler had both up, which z-fights; asking for
 * the top suit once settles that here instead of hoping it never happens.
 *
 * Since 1.21.2 vanilla's armor is a model PER SLOT rather than one outer and one inner
 * model. The old suit was the whole OUTER model drawn at once; the outer parts are now
 * the head, chest and feet models (leggings are the inner one), so drawing those three
 * gives exactly the same suit. Each is submitted with the player's own render state, and
 * vanilla poses it from that state — there is no copying of the parent's pose any more.
 */
public class ChannelingArmorLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final EquipmentSlot[] OUTER_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET
    };

    private final ArmorModelSet<PlayerModel> armor;

    public ChannelingArmorLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models,
                             boolean slim) {
        super(parent);
        this.armor = ArmorModelSet.bake(slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
                models, part -> new PlayerModel(part, slim));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        ChannelingArmorSuit suit = ClientChannelingArmor.top(state.id);
        if (suit == null || state.isInvisible) return;

        for (EquipmentSlot slot : OUTER_SLOTS) {
            collector.submitModel(
                    armor.get(slot),
                    state,
                    poseStack,
                    RenderTypes.armorCutoutNoCull(suit.texture()),
                    packedLight,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                    -1,
                    null,
                    state.outlineColor);
        }
    }
}
