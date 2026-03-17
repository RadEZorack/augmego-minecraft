package com.augmego.minecraft.avatar.client;

import com.augmego.avatar.core.AvatarModel;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.Vec3d;

public final class WorldAvatarRenderHook {
    private WorldAvatarRenderHook() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || context.matrices() == null || context.consumers() == null) {
            return;
        }

        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            AvatarModel avatarModel = ClientAvatarManager.INSTANCE.getAvatarForPlayer(player.getUuid());
            if (avatarModel == null) {
                continue;
            }

            context.matrices().push();
            context.matrices().translate(
                player.getX() - cameraPos.x,
                player.getY() - cameraPos.y,
                player.getZ() - cameraPos.z
            );
            AvatarRenderer.render(player, avatarModel, context.matrices(), context.consumers(), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            context.matrices().pop();
        }
    }
}
