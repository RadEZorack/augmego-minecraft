package com.augmego.minecraft.avatar.client;

import com.augmego.avatar.core.AvatarTexture;
import com.augmego.minecraft.avatar.AugmegoAvatarMod;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class AvatarTextureCache {
    private static final Map<String, Identifier> TEXTURE_IDS = new HashMap<>();

    private AvatarTextureCache() {
    }

    public static Identifier getTextureId(AvatarTexture avatarTexture) {
        if (avatarTexture == null) {
            return AvatarRenderer.WHITE_TEXTURE;
        }

        return TEXTURE_IDS.computeIfAbsent(avatarTexture.key(), key -> registerTexture(key, avatarTexture));
    }

    private static Identifier registerTexture(String key, AvatarTexture avatarTexture) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return AvatarRenderer.WHITE_TEXTURE;
        }

        try {
            NativeImage nativeImage = NativeImage.read(avatarTexture.imageBytes());
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "avatar-" + key, nativeImage);
            Identifier textureId = Identifier.of(AugmegoAvatarMod.MOD_ID, "dynamic/" + sanitizeKey(key));
            client.getTextureManager().registerTexture(textureId, texture);
            return textureId;
        } catch (IOException exception) {
            AugmegoAvatarMod.LOGGER.error("Failed to decode avatar texture {}", key, exception);
            return AvatarRenderer.WHITE_TEXTURE;
        }
    }

    private static String sanitizeKey(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9/_\\-.]+", "_");
    }
}
