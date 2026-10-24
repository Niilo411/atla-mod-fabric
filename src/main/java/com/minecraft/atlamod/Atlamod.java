package com.minecraft.atlamod;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class Atlamod implements ModInitializer {
    public static final String MODID = "atlamod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    // Since 1.21.2 an item or block has to know its own registry key BEFORE it is
    // constructed — the Properties carry it — so registration builds the key first and
    // hands the Properties in, rather than the old DeferredRegister supplier.
    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory,
                               BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        return item(name, factory, new Item.Properties());
    }

    // ------------------------------------------------------------------
    // SPIRIT ORE, ITS SHARD, AND THE ARMOR MADE FROM IT
    //
    // These are the mod's FIRST REAL BLOCK AND ITEM TEXTURES, under
    // assets/atlamod/textures. They were produced once, offline, by recolouring vanilla's
    // emerald ore, amethyst shard and chainmail armor — a selective hue replacement that
    // moves only the coloured pixels and leaves grey stone and black outlines
    // byte-identical. NO TINTING AT RUNTIME — see SpiritTextures.
    // ------------------------------------------------------------------

    /**
     * Spirit ore. Spirit World only, laid by the island feature rather than by an ore
     * placement — see SpiritOre for why the veins are a pure function of position.
     *
     * Emerald's hardness and blast resistance, and it needs a real pickaxe: the tags in
     * data/minecraft/tags/block put it in mineable/pickaxe and needs_iron_tool.
     */
    public static final Block SPIRIT_ORE = block("spirit_ore", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WARPED_NYLIUM)
            .strength(3.0F, 3.0F)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE));

    public static final Item SPIRIT_ORE_ITEM = item("spirit_ore",
            p -> new BlockItem(SPIRIT_ORE, p), new Item.Properties().useBlockDescriptionPrefix());

    /** What a spirit ore block drops, and what the armor is made of. */
    public static final Item SPIRIT_SHARD = item("spirit_shard", Item::new);

    // ------------------------------------------------------------------
    // SPIRIT ARMOR
    //
    // Iron's protection, diamond's durability — see SpiritArmor for why.
    //
    // Since 1.21.2 an armor material is a plain record rather than a registry entry, and
    // what a suit LOOKS like is an "equipment asset" (assets/atlamod/equipment/spirit.json)
    // naming the sheets. Ours are still recoloured CHAINMAIL — see SpiritTextures — and
    // the repair ingredient is a tag holding the shard, so an anvil still takes the
    // material the armor is made of.
    // ------------------------------------------------------------------
    public static final ResourceKey<EquipmentAsset> SPIRIT_ARMOR_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("spirit"));

    public static final TagKey<Item> REPAIRS_SPIRIT_ARMOR =
            TagKey.create(Registries.ITEM, id("repairs_spirit_armor"));

    /** Diamond's own durability factor, so each piece lasts exactly as long as diamond's. */
    private static final int SPIRIT_DURABILITY = 33;

    public static final ArmorMaterial SPIRIT_ARMOR_MATERIAL = new ArmorMaterial(
            SPIRIT_DURABILITY,
            Map.of(ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2,
                    ArmorType.BODY, 5),
            9,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            0.0F,
            0.0F,
            REPAIRS_SPIRIT_ARMOR,
            SPIRIT_ARMOR_ASSET);

    private static Item spiritArmor(String name, ArmorType type) {
        return item(name, Item::new, new Item.Properties().humanoidArmor(SPIRIT_ARMOR_MATERIAL, type));
    }

    public static final Item SPIRIT_HELMET = spiritArmor("spirit_helmet", ArmorType.HELMET);
    public static final Item SPIRIT_CHESTPLATE = spiritArmor("spirit_chestplate", ArmorType.CHESTPLATE);
    public static final Item SPIRIT_LEGGINGS = spiritArmor("spirit_leggings", ArmorType.LEGGINGS);
    public static final Item SPIRIT_BOOTS = spiritArmor("spirit_boots", ArmorType.BOOTS);

    // Bloodbending's key. Bought from a village cleric, read once, spent.
    public static final Item BLOOD_SCROLL = item("blood_scroll", BloodScrollItem::new);

    // Lavabending's lava: never flows, never spreads, and always taken back. It is
    // UNBREAKABLE for the same reason BENDING_METAL is — every ability that places it
    // is borrowing it. No BlockItem: nothing should ever hold one.
    //
    // noCollision is what makes it behave like lava rather than like a glowing wall:
    // things fall into it rather than standing on it, and nothing suffocates in it,
    // since vanilla only smothers something inside a block that blocks motion.
    public static final Block BENDING_LAVA = block("bending_lava", BendingLavaBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .noCollision()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 15)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable());

    // Lavabending's key. Bought from a village shepherd, read once, spent.
    public static final Item LAVA_SCROLL = item("lava_scroll", LavaScrollItem::new);

    // Combustionbending's key. Bought from a village armorer, read once, spent.
    public static final Item COMBUSTION_SCROLL = item("combustion_scroll", CombustionScrollItem::new);

    // Metalbending's blocks: unbreakable, and always taken back. No BlockItem —
    // nothing should ever hold one.
    public static final Block BENDING_METAL = block("bending_metal", BendingMetalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noLootTable());

    // Metalbending's key. Bought from a village mason, read once, spent.
    public static final Item METAL_SCROLL = item("metal_scroll", MetalScrollItem::new);

    // Soundbending's key. Bought from a village fletcher, read once, spent.
    public static final Item SOUND_SCROLL = item("sound_scroll", SoundScrollItem::new);

    // Icebending's key. Bought from a village fisherman, read once, spent.
    public static final Item ICE_SCROLL = item("ice_scroll", IceScrollItem::new);

    // Lightningbending's key. Bought from a village weaponsmith, read once, spent.
    public static final Item LIGHTNING_SCROLL = item("lightning_scroll", LightningScrollItem::new);

    // Gravitybending's key. Bought from a village cartographer, read once, spent.
    public static final Item GRAVITY_SCROLL = item("gravity_scroll", GravityScrollItem::new);

    // Waterbending's fuel away from open water. See WaterCanteenItem / WaterSupply.
    public static final Item WATER_CANTEEN = item("water_canteen", WaterCanteenItem::new);

    // Ability-placed fire that vanilla can't provide: stackable, and blue on demand.
    // No BlockItem — nothing should ever hold one.
    public static final Block BENDING_FIRE = block("bending_fire", BendingFireBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .replaceable()
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable());

    // The lit spirit portal. Unbreakable and uncollectable for the same reason vanilla's
    // nether portal is: it is placed by the world, never held. No BlockItem.
    //
    // Blue WITHOUT a texture of ours — our model wears vanilla's block/nether_portal and
    // marks its faces with a tint index, which SpiritPortalColors answers. See
    // SpiritPortalBlock.
    public static final Block SPIRIT_PORTAL = block("spirit_portal",
            com.minecraft.atlamod.spirit.SpiritPortalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollision()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());

    // ------------------------------------------------------------------
    // THE AVATAR TAB
    //
    // Every item the mod adds, in one place, wearing the Spirit Helmet as its icon.
    //
    // A NEW ITEM HAS TO BE ADDED HERE BY HAND — displayItems is a list, not a sweep of
    // the registry. It is kept a list on purpose: a sweep would also pick up whatever
    // block items a later change registers purely for internal use.
    //
    // The same items are ALSO still offered in the vanilla tabs where their kind lives
    // (see addCreative), so somebody who looks for armor under Combat still finds it.
    // ------------------------------------------------------------------
    public static final CreativeModeTab AVATAR_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            id("avatar_tab"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.atlamod"))
                    .icon(() -> SPIRIT_HELMET.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Spirit World spoils.
                        output.accept(SPIRIT_ORE_ITEM);
                        output.accept(SPIRIT_SHARD);
                        output.accept(SPIRIT_HELMET);
                        output.accept(SPIRIT_CHESTPLATE);
                        output.accept(SPIRIT_LEGGINGS);
                        output.accept(SPIRIT_BOOTS);

                        // Waterbending supply.
                        output.accept(WATER_CANTEEN);

                        // Sub-element scrolls, in the order the sub-elements were added.
                        output.accept(LIGHTNING_SCROLL);
                        output.accept(ICE_SCROLL);
                        output.accept(SOUND_SCROLL);
                        output.accept(METAL_SCROLL);
                        output.accept(COMBUSTION_SCROLL);
                        output.accept(BLOOD_SCROLL);
                        output.accept(LAVA_SCROLL);
                        output.accept(GRAVITY_SCROLL);
                    }).build());

    @Override
    public void onInitialize() {
        ModAttachments.register();
        ModEffects.register();
        ModEntities.register();
        com.minecraft.atlamod.spirit.island.ModFeatures.register();
        com.minecraft.atlamod.spirit.SpiritBiomes.register();
        com.minecraft.atlamod.spirit.structure.ModStructures.register();

        // Populate the ability registry once, before any packet can dispatch a cast.
        com.minecraft.atlamod.abilities.AbilityRegistry.bootstrap();

        com.minecraft.atlamod.network.ModNetworking.register();

        // Everything the player is allowed to change. SERVER, so it belongs to a world
        // rather than to an installation and reaches every client — see AtlaConfig.
        AtlaConfig.register();

        // Every NeoForge event the mod listened to, wired to its Fabric equivalent.
        com.minecraft.atlamod.platform.ServerHooks.register();

        addCreative();
    }

    // The mod's items in the vanilla tabs, alongside the Avatar Tab that holds them all.
    // Both are meant to be earned in survival — the canteen crafted, the scroll bought —
    // but having to remember an item id to test either is needless friction.
    private static void addCreative() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS)
                .register(entries -> entries.accept(SPIRIT_ORE_ITEM));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(SPIRIT_SHARD));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(SPIRIT_HELMET);
            entries.accept(SPIRIT_CHESTPLATE);
            entries.accept(SPIRIT_LEGGINGS);
            entries.accept(SPIRIT_BOOTS);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(WATER_CANTEEN);
            entries.accept(LIGHTNING_SCROLL);
            entries.accept(ICE_SCROLL);
            entries.accept(SOUND_SCROLL);
            entries.accept(METAL_SCROLL);
            entries.accept(COMBUSTION_SCROLL);
            entries.accept(BLOOD_SCROLL);
            entries.accept(LAVA_SCROLL);
            entries.accept(GRAVITY_SCROLL);
        });
    }
}
