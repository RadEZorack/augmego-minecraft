package com.augmego.avatar.core;

import java.util.ArrayList;
import java.util.List;

public final class AvatarAnimator {
    private AvatarAnimator() {
    }

    public static AvatarPose sample(AvatarModel avatarModel, float timeSeconds) {
        return sample(avatarModel, timeSeconds, true);
    }

    public static AvatarPose sample(AvatarModel avatarModel, float timeSeconds, boolean enableSkinning) {
        int nodeCount = avatarModel.nodes().size();
        float[][] translations = new float[nodeCount][3];
        float[][] rotations = new float[nodeCount][4];
        float[][] scales = new float[nodeCount][3];

        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            AvatarNode node = avatarModel.nodes().get(nodeIndex);
            System.arraycopy(node.translation(), 0, translations[nodeIndex], 0, 3);
            System.arraycopy(node.rotation(), 0, rotations[nodeIndex], 0, 4);
            System.arraycopy(node.scale(), 0, scales[nodeIndex], 0, 3);
        }

        applyAnimation(avatarModel.primaryAnimation(), timeSeconds, translations, rotations, scales);

        float[][] localMatrices = new float[nodeCount][16];
        float[][] globalMatrices = new float[nodeCount][16];
        boolean[] computedGlobals = new boolean[nodeCount];
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            AvatarMath.compose(translations[nodeIndex], rotations[nodeIndex], scales[nodeIndex], localMatrices[nodeIndex]);
        }
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            computeGlobalMatrix(avatarModel, nodeIndex, localMatrices, globalMatrices, computedGlobals);
        }

        List<AvatarPoseMesh> poseMeshes = new ArrayList<>(avatarModel.meshes().size());
        for (AvatarMesh mesh : avatarModel.meshes()) {
            poseMeshes.add(sampleMesh(avatarModel, mesh, globalMatrices, enableSkinning));
        }
        return new AvatarPose(poseMeshes);
    }

    private static void computeGlobalMatrix(
        AvatarModel avatarModel,
        int nodeIndex,
        float[][] localMatrices,
        float[][] globalMatrices,
        boolean[] computedGlobals
    ) {
        if (computedGlobals[nodeIndex]) {
            return;
        }

        int parentIndex = avatarModel.nodes().get(nodeIndex).parentIndex();
        if (parentIndex >= 0) {
            computeGlobalMatrix(avatarModel, parentIndex, localMatrices, globalMatrices, computedGlobals);
            AvatarMath.multiply(globalMatrices[parentIndex], localMatrices[nodeIndex], globalMatrices[nodeIndex]);
        } else {
            System.arraycopy(localMatrices[nodeIndex], 0, globalMatrices[nodeIndex], 0, 16);
        }
        computedGlobals[nodeIndex] = true;
    }

    private static void applyAnimation(
        AvatarAnimation animation,
        float timeSeconds,
        float[][] translations,
        float[][] rotations,
        float[][] scales
    ) {
        if (animation == null || animation.durationSeconds() <= 0.0F) {
            return;
        }

        float loopedTime = timeSeconds % animation.durationSeconds();
        for (AvatarAnimationChannel channel : animation.channels()) {
            float[] sampled = sampleChannel(channel, loopedTime);
            if (sampled == null) {
                continue;
            }
            switch (channel.path()) {
                case "translation" -> System.arraycopy(sampled, 0, translations[channel.nodeIndex()], 0, 3);
                case "rotation" -> {
                    System.arraycopy(sampled, 0, rotations[channel.nodeIndex()], 0, 4);
                    AvatarMath.normalizeQuaternion(rotations[channel.nodeIndex()]);
                }
                case "scale" -> System.arraycopy(sampled, 0, scales[channel.nodeIndex()], 0, 3);
                default -> {
                }
            }
        }
    }

    private static AvatarPoseMesh sampleMesh(AvatarModel avatarModel, AvatarMesh mesh, float[][] globalMatrices, boolean enableSkinning) {
        float[] positions = new float[mesh.positions().length];
        float[] normals = mesh.hasNormals() ? new float[mesh.normals().length] : null;
        float[] meshTransform = mesh.nodeIndex() >= 0 ? globalMatrices[mesh.nodeIndex()] : AvatarMath.identityMatrix();

        float[] inverseMeshTransform = AvatarMath.identityMatrix();
        if (!AvatarMath.invertAffine(meshTransform, inverseMeshTransform)) {
            inverseMeshTransform = AvatarMath.identityMatrix();
        }

        AvatarSkin skin = mesh.skinIndex() >= 0 && mesh.skinIndex() < avatarModel.skins().size()
            ? avatarModel.skins().get(mesh.skinIndex())
            : null;

        float[][] jointMatrices = null;
        if (enableSkinning && skin != null) {
            jointMatrices = new float[skin.jointCount()][16];
            float[] inverseBind = new float[16];
            float[] jointToMesh = new float[16];
            for (int jointIndex = 0; jointIndex < skin.jointCount(); jointIndex++) {
                System.arraycopy(skin.inverseBindMatrices(), jointIndex * 16, inverseBind, 0, 16);
                AvatarMath.multiply(globalMatrices[skin.joints()[jointIndex]], inverseBind, jointToMesh);
                AvatarMath.multiply(inverseMeshTransform, jointToMesh, jointMatrices[jointIndex]);
            }
        }

        float[] tempPosition = new float[3];
        float[] tempNormal = new float[3];
        float[] blendedPosition = new float[3];
        float[] blendedNormal = new float[3];

        for (int vertexIndex = 0; vertexIndex < mesh.vertexCount(); vertexIndex++) {
            int positionOffset = vertexIndex * 3;
            float x = mesh.positions()[positionOffset];
            float y = mesh.positions()[positionOffset + 1];
            float z = mesh.positions()[positionOffset + 2];

            if (jointMatrices != null && mesh.hasSkinning()) {
                blendedPosition[0] = 0.0F;
                blendedPosition[1] = 0.0F;
                blendedPosition[2] = 0.0F;
                blendedNormal[0] = 0.0F;
                blendedNormal[1] = 0.0F;
                blendedNormal[2] = 0.0F;

                for (int influenceIndex = 0; influenceIndex < 4; influenceIndex++) {
                    int skinOffset = vertexIndex * 4 + influenceIndex;
                    float weight = mesh.jointWeights()[skinOffset];
                    if (weight <= 0.00001F) {
                        continue;
                    }

                    int jointIndex = mesh.jointIndices()[skinOffset];
                    if (jointIndex < 0 || jointIndex >= jointMatrices.length) {
                        continue;
                    }

                    AvatarMath.transformPosition(jointMatrices[jointIndex], x, y, z, tempPosition);
                    blendedPosition[0] += tempPosition[0] * weight;
                    blendedPosition[1] += tempPosition[1] * weight;
                    blendedPosition[2] += tempPosition[2] * weight;

                    if (normals != null) {
                        AvatarMath.transformDirection(
                            jointMatrices[jointIndex],
                            mesh.normals()[positionOffset],
                            mesh.normals()[positionOffset + 1],
                            mesh.normals()[positionOffset + 2],
                            tempNormal
                        );
                        blendedNormal[0] += tempNormal[0] * weight;
                        blendedNormal[1] += tempNormal[1] * weight;
                        blendedNormal[2] += tempNormal[2] * weight;
                    }
                }

                positions[positionOffset] = blendedPosition[0];
                positions[positionOffset + 1] = blendedPosition[1];
                positions[positionOffset + 2] = blendedPosition[2];
                AvatarMath.transformPosition(
                    meshTransform,
                    positions[positionOffset],
                    positions[positionOffset + 1],
                    positions[positionOffset + 2],
                    tempPosition
                );
                positions[positionOffset] = tempPosition[0];
                positions[positionOffset + 1] = tempPosition[1];
                positions[positionOffset + 2] = tempPosition[2];

                if (normals != null) {
                    AvatarMath.normalize3(blendedNormal);
                    AvatarMath.transformDirection(meshTransform, blendedNormal[0], blendedNormal[1], blendedNormal[2], tempNormal);
                    normals[positionOffset] = tempNormal[0];
                    normals[positionOffset + 1] = tempNormal[1];
                    normals[positionOffset + 2] = tempNormal[2];
                }
            } else {
                AvatarMath.transformPosition(meshTransform, x, y, z, tempPosition);
                positions[positionOffset] = tempPosition[0];
                positions[positionOffset + 1] = tempPosition[1];
                positions[positionOffset + 2] = tempPosition[2];

                if (normals != null) {
                    AvatarMath.transformDirection(
                        meshTransform,
                        mesh.normals()[positionOffset],
                        mesh.normals()[positionOffset + 1],
                        mesh.normals()[positionOffset + 2],
                        tempNormal
                    );
                    normals[positionOffset] = tempNormal[0];
                    normals[positionOffset + 1] = tempNormal[1];
                    normals[positionOffset + 2] = tempNormal[2];
                }
            }
        }

        return new AvatarPoseMesh(mesh, positions, normals);
    }

    private static float[] sampleChannel(AvatarAnimationChannel channel, float timeSeconds) {
        float[] times = channel.keyframeTimes();
        if (times.length == 0) {
            return null;
        }
        if (times.length == 1 || timeSeconds <= times[0]) {
            return readKeyframe(channel, 0);
        }
        if (timeSeconds >= times[times.length - 1]) {
            return readKeyframe(channel, times.length - 1);
        }

        int nextIndex = 1;
        while (nextIndex < times.length && times[nextIndex] < timeSeconds) {
            nextIndex++;
        }
        int previousIndex = nextIndex - 1;

        if ("STEP".equals(channel.interpolation())) {
            return readKeyframe(channel, previousIndex);
        }

        float[] start = readKeyframe(channel, previousIndex);
        float[] end = readKeyframe(channel, nextIndex);
        float span = times[nextIndex] - times[previousIndex];
        float alpha = span <= 0.00001F ? 0.0F : (timeSeconds - times[previousIndex]) / span;

        float[] sampled = new float[channel.components()];
        if ("rotation".equals(channel.path())) {
            interpolateQuaternion(start, end, alpha, sampled);
        } else {
            for (int componentIndex = 0; componentIndex < channel.components(); componentIndex++) {
                sampled[componentIndex] = start[componentIndex] + (end[componentIndex] - start[componentIndex]) * alpha;
            }
        }
        return sampled;
    }

    private static float[] readKeyframe(AvatarAnimationChannel channel, int keyframeIndex) {
        float[] keyframe = new float[channel.components()];
        System.arraycopy(channel.values(), keyframeIndex * channel.components(), keyframe, 0, channel.components());
        return keyframe;
    }

    private static void interpolateQuaternion(float[] start, float[] end, float alpha, float[] out) {
        float dot = start[0] * end[0] + start[1] * end[1] + start[2] * end[2] + start[3] * end[3];
        float[] adjustedEnd = end;
        if (dot < 0.0F) {
            adjustedEnd = new float[] {-end[0], -end[1], -end[2], -end[3]};
            dot = -dot;
        }

        if (dot > 0.9995F) {
            for (int componentIndex = 0; componentIndex < 4; componentIndex++) {
                out[componentIndex] = start[componentIndex] + (adjustedEnd[componentIndex] - start[componentIndex]) * alpha;
            }
            AvatarMath.normalizeQuaternion(out);
            return;
        }

        double theta0 = Math.acos(dot);
        double theta = theta0 * alpha;
        double sinTheta = Math.sin(theta);
        double sinTheta0 = Math.sin(theta0);
        float scaleStart = (float) (Math.cos(theta) - dot * sinTheta / sinTheta0);
        float scaleEnd = (float) (sinTheta / sinTheta0);

        for (int componentIndex = 0; componentIndex < 4; componentIndex++) {
            out[componentIndex] = start[componentIndex] * scaleStart + adjustedEnd[componentIndex] * scaleEnd;
        }
        AvatarMath.normalizeQuaternion(out);
    }
}
