package com.augmego.avatar.core;

import java.util.List;

public record AvatarAnimation(String name, float durationSeconds, List<AvatarAnimationChannel> channels) {
    public AvatarAnimation {
        channels = List.copyOf(channels);
    }
}
