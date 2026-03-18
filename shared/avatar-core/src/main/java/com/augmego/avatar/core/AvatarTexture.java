package com.augmego.avatar.core;

import java.util.Arrays;

public record AvatarTexture(String key, byte[] imageBytes, String mimeType) {
    public AvatarTexture {
        imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
    }
}
