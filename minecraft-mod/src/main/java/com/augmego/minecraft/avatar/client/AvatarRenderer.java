package com.augmego.minecraft.avatar.client;

import com.augmego.avatar.core.AvatarMesh;
import com.augmego.avatar.core.AvatarModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class AvatarRenderer {
    private static final float MODEL_SCALE = 0.9F;
    private static final float MODEL_Y_OFFSET = -1.5F;
    private static final Identifier WHITE_TEXTURE = Identifier.of("minecraft", "textures/misc/white.png");

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
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - player.bodyYaw));
        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.entityCutoutNoCull(WHITE_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();

        for (AvatarMesh mesh : avatarModel.meshes()) {
            renderMesh(mesh, vertexConsumer, positionMatrix, entry, packedLight);
        }

        matrices.pop();
    }

    private static void renderMesh(
        AvatarMesh mesh,
        VertexConsumer vertexConsumer,
        Matrix4f positionMatrix,
        MatrixStack.Entry matrixEntry,
        int packedLight
    ) {
        int[] indices = mesh.indices();
        for (int index : indices) {
            int positionOffset = index * 3;
            int uvOffset = index * 2;

            float x = mesh.positions()[positionOffset];
            float y = mesh.positions()[positionOffset + 1];
            float z = mesh.positions()[positionOffset + 2];

            float nx = 0.0F;
            float ny = 1.0F;
            float nz = 0.0F;
            if (mesh.hasNormals()) {
                nx = mesh.normals()[positionOffset];
                ny = mesh.normals()[positionOffset + 1];
                nz = mesh.normals()[positionOffset + 2];
            }

            float u = 0.0F;
            float v = 0.0F;
            if (mesh.hasUvs()) {
                u = mesh.uvs()[uvOffset];
                v = mesh.uvs()[uvOffset + 1];
            }

            float brightness = MathHelper.clamp((ny * 0.5F) + 0.5F, 0.35F, 1.0F);
            int color = ColorHelper.getArgb(255, (int) (255 * brightness), (int) (210 * brightness), (int) (190 * brightness));

            vertexConsumer
                .vertex(positionMatrix, x, y, z)
                .color(color)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(Math.max(packedLight, LightmapTextureManager.MAX_LIGHT_COORDINATE))
                .normal(matrixEntry, nx, ny, nz);
        }
    }
}
