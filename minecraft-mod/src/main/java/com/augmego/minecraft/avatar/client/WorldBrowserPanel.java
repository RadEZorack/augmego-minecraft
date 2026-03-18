package com.augmego.minecraft.avatar.client;

import com.cinemamod.mcef.MCEFBrowser;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class WorldBrowserPanel {
    private static final double PANEL_DISTANCE = 3.0D;
    private static final double PANEL_HEIGHT_OFFSET = 0.15D;
    private static final float PANEL_WIDTH = 1.6F;
    private static final float PANEL_HEIGHT = 0.9F;
    private static final double INTERACTION_DISTANCE = 6.0D;

    private static boolean active;
    private static Vec3d center = Vec3d.ZERO;
    private static float yawDegrees;

    private WorldBrowserPanel() {
    }

    public static void placeInFrontOfPlayer(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }

        Vec3d look = client.player.getRotationVec(1.0F);
        Vec3d horizontalLook = new Vec3d(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSquared() < 1.0E-6D) {
            horizontalLook = new Vec3d(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        center = client.player.getEyePos()
            .add(horizontalLook.multiply(PANEL_DISTANCE))
            .add(0.0D, PANEL_HEIGHT_OFFSET, 0.0D);
        yawDegrees = client.player.getYaw();
        active = true;

        BrowserSessionManager.ensureBrowserSize(1280, 720);
    }

    public static boolean isActive() {
        return active;
    }

    public static void render(WorldRenderContext context) {
        if (!active || context.matrices() == null || context.consumers() == null) {
            return;
        }

        MCEFBrowser browser = BrowserSessionManager.getOrCreateBrowser();
        if (browser == null || !browser.isTextureReady() || browser.getTextureIdentifier() == null) {
            return;
        }

        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();

        matrices.push();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yawDegrees));

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        VertexConsumer consumer = consumers.getBuffer(RenderLayers.entityCutoutNoCull(browser.getTextureIdentifier()));

        emitFrontFace(consumer, positionMatrix, entry);
        emitBackFace(consumer, positionMatrix, entry);
        matrices.pop();
    }

    public static boolean punchOpensFullscreen(MinecraftClient client) {
        if (!active || client == null || client.player == null || client.world == null) {
            return false;
        }

        Vec3d origin = client.player.getEyePos();
        Vec3d direction = client.player.getRotationVec(1.0F).normalize();
        Vec3d panelNormal = getPanelNormal();
        double denominator = direction.dotProduct(panelNormal);
        if (Math.abs(denominator) < 1.0E-6D) {
            return false;
        }

        double distance = center.subtract(origin).dotProduct(panelNormal) / denominator;
        if (distance < 0.0D || distance > INTERACTION_DISTANCE) {
            return false;
        }

        Vec3d hitPos = origin.add(direction.multiply(distance));
        Vec3d local = hitPos.subtract(center);
        Vec3d right = getPanelRight();
        double halfWidth = PANEL_WIDTH * 0.5D;
        double halfHeight = PANEL_HEIGHT * 0.5D;

        double x = local.dotProduct(right);
        double y = local.y;
        return Math.abs(x) <= halfWidth && Math.abs(y) <= halfHeight;
    }

    private static Vec3d getPanelNormal() {
        double yawRadians = Math.toRadians(yawDegrees);
        return new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
    }

    private static Vec3d getPanelRight() {
        Vec3d normal = getPanelNormal();
        return new Vec3d(normal.z, 0.0D, -normal.x);
    }

    private static void emitFrontFace(VertexConsumer consumer, Matrix4f positionMatrix, MatrixStack.Entry entry) {
        float halfWidth = PANEL_WIDTH * 0.5F;
        float halfHeight = PANEL_HEIGHT * 0.5F;

        consumer.vertex(positionMatrix, -halfWidth, halfHeight, 0.0F)
            .color(0xFFFFFFFF)
            .texture(0.0F, 0.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, 1.0F);
        consumer.vertex(positionMatrix, -halfWidth, -halfHeight, 0.0F)
            .color(0xFFFFFFFF)
            .texture(0.0F, 1.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, 1.0F);
        consumer.vertex(positionMatrix, halfWidth, -halfHeight, 0.0F)
            .color(0xFFFFFFFF)
            .texture(1.0F, 1.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, 1.0F);
        consumer.vertex(positionMatrix, halfWidth, halfHeight, 0.0F)
            .color(0xFFFFFFFF)
            .texture(1.0F, 0.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, 1.0F);
    }

    private static void emitBackFace(VertexConsumer consumer, Matrix4f positionMatrix, MatrixStack.Entry entry) {
        float halfWidth = PANEL_WIDTH * 0.5F;
        float halfHeight = PANEL_HEIGHT * 0.5F;

        consumer.vertex(positionMatrix, halfWidth, halfHeight, -0.001F)
            .color(0xFFFFFFFF)
            .texture(0.0F, 0.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, -1.0F);
        consumer.vertex(positionMatrix, halfWidth, -halfHeight, -0.001F)
            .color(0xFFFFFFFF)
            .texture(0.0F, 1.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, -1.0F);
        consumer.vertex(positionMatrix, -halfWidth, -halfHeight, -0.001F)
            .color(0xFFFFFFFF)
            .texture(1.0F, 1.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, -1.0F);
        consumer.vertex(positionMatrix, -halfWidth, halfHeight, -0.001F)
            .color(0xFFFFFFFF)
            .texture(1.0F, 0.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, 0.0F, 0.0F, -1.0F);
    }
}
