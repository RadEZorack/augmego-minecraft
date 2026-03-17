package com.augmego.minecraft.avatar.client;

import com.augmego.avatar.core.AvatarLoadException;
import com.augmego.avatar.core.AvatarLoader;
import com.augmego.avatar.core.AvatarModel;
import com.augmego.minecraft.avatar.AugmegoAvatarMod;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class ClientAvatarManager {
    public static final ClientAvatarManager INSTANCE = new ClientAvatarManager();
    static final UUID TEST_PLAYER_UUID = UUID.fromString("8cbe60f8-6eb5-4fbe-a8f1-87f74cb44699");
    static final Identifier TEST_AVATAR_ID = Identifier.of(AugmegoAvatarMod.MOD_ID, "avatars/test-avatar.glb");

    private final AvatarLoader avatarLoader = new AvatarLoader();
    private AvatarModel avatarModel;
    private boolean attemptedLoad;

    private ClientAvatarManager() {
    }

    public void preload() {
        getTestAvatar();
    }

    public AvatarModel getAvatarForPlayer(UUID playerUuid) {
        if (!TEST_PLAYER_UUID.equals(playerUuid)) {
            return null;
        }
        return getTestAvatar();
    }

    private AvatarModel getTestAvatar() {
        if (this.attemptedLoad) {
            return this.avatarModel;
        }

        this.attemptedLoad = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }

        try {
            Resource resource = client.getResourceManager().getResource(TEST_AVATAR_ID).orElse(null);
            if (resource == null) {
                AugmegoAvatarMod.LOGGER.error("Missing test avatar asset {}", TEST_AVATAR_ID);
                return null;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                this.avatarModel = this.avatarLoader.loadGlb(inputStream, TEST_AVATAR_ID.toString());
                AugmegoAvatarMod.LOGGER.info("Loaded test avatar with {} mesh(es)", this.avatarModel.meshes().size());
            }
        } catch (AvatarLoadException | IOException exception) {
            AugmegoAvatarMod.LOGGER.error("Failed to load test avatar {}", TEST_AVATAR_ID, exception);
        }

        return this.avatarModel;
    }
}
