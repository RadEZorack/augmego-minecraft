package com.augmego.avatar.core;

public record AvatarAnimationChannel(
    int nodeIndex,
    String path,
    float[] keyframeTimes,
    float[] values,
    int components,
    String interpolation
) {
    public AvatarAnimationChannel {
        keyframeTimes = keyframeTimes.clone();
        values = values.clone();
    }
}
