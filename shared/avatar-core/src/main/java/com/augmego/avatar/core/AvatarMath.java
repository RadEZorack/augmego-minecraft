package com.augmego.avatar.core;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class AvatarMath {
    private AvatarMath() {
    }

    static float[] identityMatrix() {
        float[] matrix = new float[16];
        matrix[0] = 1.0F;
        matrix[5] = 1.0F;
        matrix[10] = 1.0F;
        matrix[15] = 1.0F;
        return matrix;
    }

    static void compose(float[] translation, float[] rotation, float[] scale, float[] out) {
        Matrix4f matrix = new Matrix4f()
            .translationRotateScale(
                translation[0],
                translation[1],
                translation[2],
                rotation[0],
                rotation[1],
                rotation[2],
                rotation[3],
                scale[0],
                scale[1],
                scale[2]
            );
        matrix.get(out);
    }

    static void multiply(float[] left, float[] right, float[] out) {
        Matrix4f result = new Matrix4f().set(left).mul(new Matrix4f().set(right));
        result.get(out);
    }

    static boolean invertAffine(float[] matrix, float[] out) {
        Matrix4f result = new Matrix4f().set(matrix).invertAffine();
        result.get(out);
        return true;
    }

    static void transformPosition(float[] matrix, float x, float y, float z, float[] out) {
        Vector3f vector = new Vector3f();
        new Matrix4f().set(matrix).transformPosition(x, y, z, vector);
        out[0] = vector.x;
        out[1] = vector.y;
        out[2] = vector.z;
    }

    static void transformDirection(float[] matrix, float x, float y, float z, float[] out) {
        Vector3f vector = new Vector3f();
        new Matrix4f().set(matrix).transformDirection(x, y, z, vector);
        out[0] = vector.x;
        out[1] = vector.y;
        out[2] = vector.z;
        normalize3(out);
    }

    static void normalize3(float[] vector) {
        float length = (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        if (length > 0.00001F) {
            vector[0] /= length;
            vector[1] /= length;
            vector[2] /= length;
        }
    }

    static void normalizeQuaternion(float[] quaternion) {
        Quaternionf normalized = new Quaternionf(quaternion[0], quaternion[1], quaternion[2], quaternion[3]).normalize();
        quaternion[0] = normalized.x;
        quaternion[1] = normalized.y;
        quaternion[2] = normalized.z;
        quaternion[3] = normalized.w;
    }
}
