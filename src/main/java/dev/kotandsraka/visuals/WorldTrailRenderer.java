package dev.kotandsraka.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public final class WorldTrailRenderer {
    private static final Deque<Vec3> TRAIL = new ArrayDeque<>();

    private WorldTrailRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldTrailRenderer::render);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        VisualConfig c = VisualConfig.get();
        if (mc.player == null || mc.level == null || !c.playerTrail) {
            TRAIL.clear();
            return;
        }

        Vec3 pos = mc.player.position().add(0.0, 0.08, 0.0);
        if (TRAIL.isEmpty() || TRAIL.peekLast().distanceToSqr(pos) > 0.0004) {
            TRAIL.addLast(pos);
        }
        while (TRAIL.size() > Math.max(4, c.trailLength)) {
            TRAIL.removeFirst();
        }
    }

    private static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        VisualConfig c = VisualConfig.get();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.getCameraType().isFirstPerson()) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Vec3 camera = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        VertexConsumer line = consumers.getBuffer(RenderTypes.lines());

        if (c.playerTrail && TRAIL.size() >= 2) {
            matrices.pushPose();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            matrices.last().pose();
            Vec3 previous = null;
            int index = 0;
            for (Vec3 point : TRAIL) {
                if (previous != null) {
                    int color = c.trailRainbow
                        ? rainbowColor((index + mc.player.tickCount) * 0.045f, c.trailAlpha)
                        : 0x66D8FFFF;
                    drawLine(line, matrices.last(), previous, point, color, c.trailWidth);
                }
                previous = point;
                index++;
            }
            matrices.popPose();
        }

        if (c.chinaHat) {
            renderChinaHat(mc, c, camera, matrices, line);
        }
    }

    private static void renderChinaHat(Minecraft mc, VisualConfig c, Vec3 camera,
                                       PoseStack matrices, VertexConsumer line) {
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        double y = mc.player.getY() + mc.player.getBbHeight() + 0.08;
        double radius = Math.max(0.45, mc.player.getBbWidth() * 0.85);
        Vec3 previous = null;
        int segments = 48;

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            Vec3 point = new Vec3(
                mc.player.getX() + Math.cos(angle) * radius,
                y,
                mc.player.getZ() + Math.sin(angle) * radius
            );
            if (previous != null) {
                int color = c.chromaTheme || c.trailRainbow
                    ? rainbowColor((i + mc.player.tickCount) * 0.025f, 210)
                    : 0xD0FFFFFF;
                drawLine(line, matrices.last(), previous, point, color, Math.max(1.5f, c.trailWidth * 0.75f));
            }
            previous = point;
        }

        matrices.popPose();
    }

    private static void drawLine(VertexConsumer consumer, PoseStack.Pose pose,
                                 Vec3 a, Vec3 b, int color, float width) {
        int alpha = (color >>> 24) & 255;
        int red = (color >>> 16) & 255;
        int green = (color >>> 8) & 255;
        int blue = color & 255;

        consumer.addVertex(pose, (float) a.x, (float) a.y, (float) a.z)
            .setColor(red, green, blue, alpha)
            .setLineWidth(width);
        consumer.addVertex(pose, (float) b.x, (float) b.y, (float) b.z)
            .setColor(red, green, blue, alpha)
            .setLineWidth(width);
    }

    private static int rainbowColor(float phase, int alpha) {
        float r = (float) (Math.sin(phase * Math.PI * 2.0) * 0.5 + 0.5);
        float g = (float) (Math.sin((phase + 0.3333) * Math.PI * 2.0) * 0.5 + 0.5);
        float b = (float) (Math.sin((phase + 0.6666) * Math.PI * 2.0) * 0.5 + 0.5);
        return (Math.max(0, Math.min(255, alpha)) << 24)
            | ((int) (r * 255) << 16)
            | ((int) (g * 255) << 8)
            | (int) (b * 255);
    }
}
