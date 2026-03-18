package com.augmego.minecraft.avatar.client;

import com.augmego.minecraft.avatar.AugmegoAvatarMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class AugmegoAvatarClientMod implements ClientModInitializer {
    private static boolean attackPressedLastTick;

    private static final KeyBinding OPEN_BROWSER_KEY = new KeyBinding(
        "key.augmegoavatar.open_browser",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        KeyBinding.Category.MISC
    );

    @Override
    public void onInitializeClient() {
        AugmegoAvatarMod.LOGGER.info("Initializing client avatar renderer");
        WorldRenderEvents.AFTER_ENTITIES.register(WorldAvatarRenderHook::render);
        WorldRenderEvents.AFTER_ENTITIES.register(WorldBrowserPanel::render);
        KeyBindingHelper.registerKeyBinding(OPEN_BROWSER_KEY);
        ClientTickEvents.START_CLIENT_TICK.register(AugmegoAvatarClientMod::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (OPEN_BROWSER_KEY.wasPressed() && client.currentScreen == null) {
            if (client.player != null && client.world != null) {
                WorldBrowserPanel.placeInFrontOfPlayer(client);
            } else {
                BrowserSessionManager.openFullscreen(client);
            }
        }

        boolean attackPressed = client.options.attackKey.isPressed();
        if (attackPressed && !attackPressedLastTick && client.currentScreen == null && WorldBrowserPanel.punchOpensFullscreen(client)) {
            BrowserSessionManager.openFullscreen(client);
        }
        attackPressedLastTick = attackPressed;
    }
}
