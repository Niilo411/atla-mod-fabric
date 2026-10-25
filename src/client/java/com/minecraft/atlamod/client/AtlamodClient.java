package com.minecraft.atlamod.client;

import com.minecraft.atlamod.Atlamod;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;

public class AtlamodClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindings.register();
        ModNetworkingClient.register();
        ClientEvents.register();
        ModHudOverlay.register();
        ModEntityRenderers.register();
        SpiritPortalColors.register();
        WorldEventSky.register();

        // The mod list's Config button opens OUR settings screen rather than a generic
        // config editor, so there is one settings UI rather than two that could disagree.
        // Forge Config API Port answers Mod Menu for every mod it holds a config for, and
        // this is how it is told which screen ours is.
        ConfigScreenFactoryRegistry.INSTANCE.register(Atlamod.MODID,
                (modId, parent) -> new AtlaSettingsScreen(parent));
    }
}
