package com.augmego.minecraft.avatar.client;

import com.augmego.minecraft.avatar.AugmegoAvatarMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public final class AugmegoAvatarClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AugmegoAvatarMod.LOGGER.info("Initializing client avatar renderer");
        WorldRenderEvents.AFTER_ENTITIES.register(WorldAvatarRenderHook::render);
    }
}
