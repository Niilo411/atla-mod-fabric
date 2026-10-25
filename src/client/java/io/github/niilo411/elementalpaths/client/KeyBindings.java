package io.github.niilo411.elementalpaths.client;

import io.github.niilo411.elementalpaths.ElementalPaths;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {
    }

    /**
     * The mod's own section of the controls screen. Since 1.21.9 a category is a
     * registered object rather than a translation key, and its title is looked up as
     * key.category.elemental_paths.channeling.
     */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ElementalPaths.id("channeling"));

    public static final KeyMapping MEDITATE = new KeyMapping("key.elemental_paths.meditate", GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping ABILITY_1 = new KeyMapping("key.elemental_paths.ability1", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping ABILITY_2 = new KeyMapping("key.elemental_paths.ability2", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping ABILITY_3 = new KeyMapping("key.elemental_paths.ability3", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping ABILITY_4 = new KeyMapping("key.elemental_paths.ability4", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping UPGRADE_MENU = new KeyMapping("key.elemental_paths.upgrade_menu", GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping SWITCH_ELEMENT = new KeyMapping("key.elemental_paths.switch_element", GLFW.GLFW_KEY_Y, CATEGORY);

    // NeoForge's KeyConflictContext.IN_GAME needs no stand-in: vanilla only updates a
    // key mapping's state while no screen is open, so none of these fire in a menu.
    public static void register() {
        KeyBindingHelper.registerKeyBinding(MEDITATE);
        KeyBindingHelper.registerKeyBinding(ABILITY_1);
        KeyBindingHelper.registerKeyBinding(ABILITY_2);
        KeyBindingHelper.registerKeyBinding(ABILITY_3);
        KeyBindingHelper.registerKeyBinding(ABILITY_4);
        KeyBindingHelper.registerKeyBinding(UPGRADE_MENU);
        KeyBindingHelper.registerKeyBinding(SWITCH_ELEMENT);
    }
}
