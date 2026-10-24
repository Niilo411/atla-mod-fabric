package com.minecraft.atlamod.spirit.structure;

import com.minecraft.atlamod.Atlamod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * TWO registries per structure, and both matter: a StructureType is how the structure
 * is read out of JSON, a StructurePieceType is how its piece is read back out of a
 * SAVE. Register only the first and the world loads once and then fails on reload.
 */
public final class ModStructures {
    public static final StructureType<SpiritTempleStructure> SPIRIT_TEMPLE = Registry.register(
            BuiltInRegistries.STRUCTURE_TYPE, Atlamod.id("spirit_temple"),
            () -> SpiritTempleStructure.CODEC);

    public static final StructurePieceType SPIRIT_TEMPLE_PIECE = Registry.register(
            BuiltInRegistries.STRUCTURE_PIECE, Atlamod.id("spirit_temple"),
            (StructurePieceType.ContextlessType) SpiritTemplePiece::new);

    public static final StructureType<SpiritShrineStructure> SPIRIT_SHRINE = Registry.register(
            BuiltInRegistries.STRUCTURE_TYPE, Atlamod.id("spirit_shrine"),
            () -> SpiritShrineStructure.CODEC);

    public static final StructurePieceType SPIRIT_SHRINE_PIECE = Registry.register(
            BuiltInRegistries.STRUCTURE_PIECE, Atlamod.id("spirit_shrine"),
            (StructurePieceType.ContextlessType) SpiritShrinePiece::new);

    public static final TagKey<Structure> SPIRIT_TEMPLE_TAG =
            TagKey.create(Registries.STRUCTURE, Atlamod.id("spirit_temple"));

    private ModStructures() {
    }

    /** Touching the class is what registers them. */
    public static void register() {
    }
}
