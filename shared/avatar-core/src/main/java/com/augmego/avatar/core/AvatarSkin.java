package com.augmego.avatar.core;

public record AvatarSkin(int[] joints, float[] inverseBindMatrices) {
    public AvatarSkin {
        joints = joints.clone();
        inverseBindMatrices = inverseBindMatrices.clone();
    }

    public int jointCount() {
        return joints.length;
    }
}
