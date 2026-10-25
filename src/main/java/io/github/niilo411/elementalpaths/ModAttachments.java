package io.github.niilo411.elementalpaths;

import io.github.niilo411.elementalpaths.paragon.ParagonState;
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
    public static final AttachmentType<ChannelingData> CHANNELING_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ElementalPaths.MODID, "channeling_data"),
            builder -> builder
                    .initializer(ChannelingData::new)
                    .persistent(ChannelingData.CODEC)
                    .copyOnDeath());

    /**
     * The Paragon cycle. Held by the OVERWORLD, not by a player — whose turn it is
     * and who currently holds the title are facts about the world, and have to
     * outlive any one player being logged in.
     *
     * No copyOnDeath here: levels do not die.
     */
    public static final AttachmentType<ParagonState> PARAGON_STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ElementalPaths.MODID, "paragon_state"),
            builder -> builder
                    .initializer(ParagonState::new)
                    .persistent(ParagonState.CODEC));

    /** Touching the class is what registers both. */
    public static void register() {
    }
}
