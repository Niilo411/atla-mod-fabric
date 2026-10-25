package io.github.niilo411.elementalpaths.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.niilo411.elementalpaths.ElementalPaths;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class KeyBindings {
    private KeyBindings() {
    }

    /**
     * The mod's own section of the controls screen. Since 1.21.9 a category is a
     * registered object rather than a translation key, and its title is looked up as
     * key.category.elemental_paths.channeling.
     */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ElementalPaths.id("channeling"));

    public static final KeyMapping MEDITATE = new KeyMapping("key.elemental_paths.meditate", InputConstants.KEY_M, CATEGORY);
    public static final KeyMapping ABILITY_1 = new KeyMapping("key.elemental_paths.ability1", InputConstants.KEY_Z, CATEGORY);
    public static final KeyMapping ABILITY_2 = new KeyMapping("key.elemental_paths.ability2", InputConstants.KEY_X, CATEGORY);
    public static final KeyMapping ABILITY_3 = new KeyMapping("key.elemental_paths.ability3", InputConstants.KEY_C, CATEGORY);
    public static final KeyMapping ABILITY_4 = new KeyMapping("key.elemental_paths.ability4", InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping UPGRADE_MENU = new KeyMapping("key.elemental_paths.upgrade_menu", InputConstants.KEY_N, CATEGORY);
    public static final KeyMapping SWITCH_ELEMENT = new KeyMapping("key.elemental_paths.switch_element", InputConstants.KEY_Y, CATEGORY);

    // NeoForge's KeyConflictContext.IN_GAME needs no stand-in: vanilla only updates a
    // key mapping's state while no screen is open, so none of these fire in a menu.
    public static void register() {
        KeyMappingHelper.registerKeyMapping(MEDITATE);
        KeyMappingHelper.registerKeyMapping(ABILITY_1);
        KeyMappingHelper.registerKeyMapping(ABILITY_2);
        KeyMappingHelper.registerKeyMapping(ABILITY_3);
        KeyMappingHelper.registerKeyMapping(ABILITY_4);
        KeyMappingHelper.registerKeyMapping(UPGRADE_MENU);
        KeyMappingHelper.registerKeyMapping(SWITCH_ELEMENT);
    }
}
