package com.augmego.avatar.core;

import java.util.List;

public record AvatarPose(List<AvatarPoseMesh> meshes) {
    public AvatarPose {
        meshes = List.copyOf(meshes);
    }
}
