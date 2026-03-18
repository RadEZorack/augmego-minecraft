package com.augmego.avatar.core;

import java.util.List;

public record AvatarModel(
    String sourceName,
    List<AvatarMesh> meshes,
    List<AvatarNode> nodes,
    List<AvatarSkin> skins,
    List<AvatarAnimation> animations
) {
    public AvatarModel {
        meshes = List.copyOf(meshes);
        nodes = List.copyOf(nodes);
        skins = List.copyOf(skins);
        animations = List.copyOf(animations);
    }

    public AvatarAnimation primaryAnimation() {
        return animations.isEmpty() ? null : animations.getFirst();
    }
}
