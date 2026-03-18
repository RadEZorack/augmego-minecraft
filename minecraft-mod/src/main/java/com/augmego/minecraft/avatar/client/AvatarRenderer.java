package com.augmego.minecraft.avatar.client;

import com.augmego.avatar.core.AvatarAnimator;
import com.augmego.avatar.core.AvatarMesh;
import com.augmego.avatar.core.AvatarModel;
import com.augmego.avatar.core.AvatarPose;
import com.augmego.avatar.core.AvatarPoseMesh;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class AvatarRenderer {
    private static final float MODEL_SCALE = 1.0F;
    private static final float MODEL_Y_OFFSET = 0.0F;
    private static final boolean DEBUG_ENABLE_SKINNING = true;
    static final Identifier WHITE_TEXTURE = Identifier.of("minecraft", "textures/misc/white.png");

    private AvatarRenderer() {
    }

    public static void render(
        AbstractClientPlayerEntity player,
        AvatarModel avatarModel,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int packedLight
    ) {
        matrices.push();
        matrices.translate(0.0D, MODEL_Y_OFFSET, 0.0D);
        // matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - player.bodyYaw));
        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        AvatarPose pose = AvatarAnimator.sample(avatarModel, player.age / 20.0F, DEBUG_ENABLE_SKINNING);

        for (AvatarPoseMesh poseMesh : pose.meshes()) {
            AvatarMesh mesh = poseMesh.sourceMesh();
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                RenderLayers.entityCutoutNoCull(AvatarTextureCache.getTextureId(mesh.texture()))
            );
            renderMesh(poseMesh, vertexConsumer, positionMatrix, entry, packedLight);
        }

        matrices.pop();
    }

    private static void renderMesh(
        AvatarPoseMesh poseMesh,
        VertexConsumer vertexConsumer,
        Matrix4f positionMatrix,
        MatrixStack.Entry matrixEntry,
        int packedLight
    ) {
        AvatarMesh mesh = poseMesh.sourceMesh();
        int[] indices = mesh.indices();
        for (int indexOffset = 0; indexOffset + 2 < indices.length; indexOffset += 3) {
            emitVertex(poseMesh, mesh, indices[indexOffset], vertexConsumer, positionMatrix, matrixEntry, packedLight);
            emitVertex(poseMesh, mesh, indices[indexOffset + 1], vertexConsumer, positionMatrix, matrixEntry, packedLight);
            emitVertex(poseMesh, mesh, indices[indexOffset + 2], vertexConsumer, positionMatrix, matrixEntry, packedLight);
            emitVertex(poseMesh, mesh, indices[indexOffset + 2], vertexConsumer, positionMatrix, matrixEntry, packedLight);
        }
    }

    private static void emitVertex(
        AvatarPoseMesh poseMesh,
        AvatarMesh mesh,
        int index,
        VertexConsumer vertexConsumer,
        Matrix4f positionMatrix,
        MatrixStack.Entry matrixEntry,
        int packedLight
    ) {
        int positionOffset = index * 3;
        int uvOffset = index * 2;

        float x = poseMesh.positions()[positionOffset];
        float y = poseMesh.positions()[positionOffset + 1];
        float z = poseMesh.positions()[positionOffset + 2];

        float nx = 0.0F;
        float ny = 1.0F;
        float nz = 0.0F;
        if (poseMesh.normals() != null) {
            nx = poseMesh.normals()[positionOffset];
            ny = poseMesh.normals()[positionOffset + 1];
            nz = poseMesh.normals()[positionOffset + 2];
        }

        float u = 0.0F;
        float v = 0.0F;
        if (mesh.hasUvs()) {
            u = mesh.uvs()[uvOffset];
            v = mesh.uvs()[uvOffset + 1];
        }

        vertexConsumer
            .vertex(positionMatrix, x, y, z)
            .color(0xFFFFFFFF)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(Math.max(packedLight, LightmapTextureManager.MAX_LIGHT_COORDINATE))
            .normal(matrixEntry, nx, ny, nz);
    }
}
