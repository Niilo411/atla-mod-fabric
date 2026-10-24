package com.minecraft.atlamod;

import com.minecraft.atlamod.avatar.AvatarState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Fabric's Data Attachment API, standing in for NeoForge's. Read with
 * {@code getAttachedOrCreate}, which is exactly what NeoForge's {@code getData} did:
 * an attachment that was never set is created from the initializer on first touch.
 *
 * Both are mutated IN PLACE all over the mod and never re-set, which is safe here as
 * well: an entity serialises whatever object it is holding when it is saved, and
 * Fabric's world attachment store reports itself dirty whenever it holds any
 * persistent attachment at all.
 */
public class ModAttachments {
    public static final AttachmentType<BendingData> BENDING_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Atlamod.MODID, "bending_data"),
            builder -> builder
                    .initializer(BendingData::new)
                    .persistent(BendingData.CODEC)
                    .copyOnDeath());

    /**
     * The Avatar cycle. Held by the OVERWORLD, not by a player — whose turn it is
     * and who currently holds the title are facts about the world, and have to
     * outlive any one player being logged in.
     *
     * No copyOnDeath here: levels do not die.
     */
    public static final AttachmentType<AvatarState> AVATAR_STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Atlamod.MODID, "avatar_state"),
            builder -> builder
                    .initializer(AvatarState::new)
                    .persistent(AvatarState.CODEC));

    /** Touching the class is what registers both. */
    public static void register() {
    }
}
