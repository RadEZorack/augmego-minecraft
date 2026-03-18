package com.augmego.avatar.core;

public record AvatarMesh(
    float[] positions,
    float[] normals,
    float[] uvs,
    int[] indices,
    int nodeIndex,
    int skinIndex,
    int[] jointIndices,
    float[] jointWeights,
    String materialName,
    AvatarTexture texture
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

    public boolean hasSkinning() {
        return jointIndices != null &&
            jointWeights != null &&
            jointIndices.length >= vertexCount() * 4 &&
            jointWeights.length >= vertexCount() * 4;
    }
}
