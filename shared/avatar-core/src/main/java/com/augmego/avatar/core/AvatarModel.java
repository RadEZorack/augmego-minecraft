package com.augmego.avatar.core;

import java.util.List;

public record AvatarModel(String sourceName, List<AvatarMesh> meshes) {
    public AvatarModel {
        meshes = List.copyOf(meshes);
    }
}
