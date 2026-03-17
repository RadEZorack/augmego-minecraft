package com.augmego.avatar.core;

public record AvatarMesh(
    float[] positions,
    float[] normals,
    float[] uvs,
    int[] indices,
    String materialName
) {
    public int vertexCount() {
        return positions.length / 3;
    }

    public boolean hasNormals() {
        return normals != null && normals.length >= vertexCount() * 3;
    }

    public boolean hasUvs() {
        return uvs != null && uvs.length >= vertexCount() * 2;
    }
}
