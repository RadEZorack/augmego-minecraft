package com.augmego.avatar.core;

public record AvatarNode(
    String name,
    int parentIndex,
    int[] children,
    float[] translation,
    float[] rotation,
    float[] scale
) {
    public AvatarNode {
        children = children.clone();
        translation = translation.clone();
        rotation = rotation.clone();
        scale = scale.clone();
    }
}
