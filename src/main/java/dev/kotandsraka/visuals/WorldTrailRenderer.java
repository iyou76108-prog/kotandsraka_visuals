package dev.kotandsraka.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

public final class WorldTrailRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Deque<Vec3> TRAIL = new ArrayDeque<>();
    private static final Deque<JumpRing> JUMP_RINGS = new ArrayDeque<>();
    private record JumpRing(Vec3 center, int startTick) {}
    private static boolean renderFailed = false;

    private WorldTrailRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldTrailRenderer::render);
    }

    public static void spawnJumpRing(Player player) {
        if (player == null) return;
        double groundY = player.blockPosition().getY();
        Vec3 center = new Vec3(player.getX(), groundY + 0.018, player.getZ());
        if (JUMP_RINGS.size() >= 8) JUMP_RINGS.removeFirst();
        JUMP_RINGS.addLast(new JumpRing(center, player.tickCount));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        VisualConfig c = VisualConfig.get();
        if (mc.player == null || mc.level == null) {
            TRAIL.clear();
            JUMP_RINGS.clear();
            return;
        }

        if (!c.jumpRings) JUMP_RINGS.clear();
        else while (!JUMP_RINGS.isEmpty() && mc.player.tickCount - JUMP_RINGS.peekFirst().startTick() > 36) {
            JUMP_RINGS.removeFirst();
        }

        if (c.playerTrail) {
            Vec3 pos = mc.player.position().add(0.0, 0.08, 0.0);
            if (TRAIL.isEmpty() || TRAIL.peekLast().distanceToSqr(pos) > 0.0004) TRAIL.addLast(pos);
            while (TRAIL.size() > Math.max(4, c.trailLength)) TRAIL.removeFirst();
        } else {
            TRAIL.clear();
        }

    }

    private static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        VisualConfig c = VisualConfig.get();
        if (renderFailed || mc.player == null || mc.level == null) return;

        // Do not touch render state at all when every world renderer is disabled.
        if (!c.playerTrail && !c.chinaHat && !c.targetEsp && !c.hitRange && !c.trajectory
            && !c.jumpRings) return;

        try {
            MultiBufferSource consumers = context.consumers();
            PoseStack matrices = context.matrices();
            if (consumers == null || matrices == null || context.worldState() == null
                || context.worldState().cameraRenderState == null
                || context.worldState().cameraRenderState.pos == null) return;

            Vec3 camera = context.worldState().cameraRenderState.pos;
            VertexConsumer line = consumers.getBuffer(RenderTypes.lines());
            matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = matrices.last();

        if (c.playerTrail && !mc.options.getCameraType().isFirstPerson() && TRAIL.size() >= 2) {
            drawPath(line, pose, TRAIL, c.trailRainbow ? null : 0xAA56D8FF, c.trailWidth, mc.player.tickCount);
        }

        if (c.chinaHat && !mc.options.getCameraType().isFirstPerson()) {
            renderChinaHat(mc, c, pose, line);
        }

        if (c.targetEsp) {
            LivingEntity target = VisualHud.getTarget();
            if (target != null && target.isAlive()) {
                drawCircle(line, pose, new Vec3(target.getX(), target.getY() + 0.06, target.getZ()),
                    Math.max(0.7, target.getBbWidth() * 0.9), 48, VisualConfig.accentColor(), 2.0f);
                drawCircle(line, pose, new Vec3(target.getX(), target.getY() + target.getBbHeight() + 0.04, target.getZ()),
                    Math.max(0.7, target.getBbWidth() * 0.9), 48, VisualConfig.accentColor(), 1.5f);
            }
        }

        if (c.hitRange) {
            drawCircle(line, pose, new Vec3(mc.player.getX(), mc.player.getY() + 0.035, mc.player.getZ()),
                3.0, 64, 0xAA56D8FF, 1.5f);
        }

        if (c.trajectory) renderProjectileTrajectories(mc, pose, line);
        if (c.jumpRings) renderJumpRings(mc, c, pose, line);

            matrices.popPose();
        } catch (RuntimeException exception) {
            // A bad render context must never take down the whole client while joining a world.
            renderFailed = true;
            TRAIL.clear();
            LOGGER.error("KotAndSraka Visuals disabled its world renderer after a render error", exception);
        }
    }

    private static void renderJumpRings(Minecraft mc, VisualConfig cfg,
                                         PoseStack.Pose pose, VertexConsumer line) {
        if (JUMP_RINGS.isEmpty()) return;
        float scaleSetting = Math.max(0.60f, Math.min(1.80f, cfg.jumpRingScale));
        int pattern = Math.floorMod(cfg.jumpRingPattern, 3);

        for (JumpRing ring : JUMP_RINGS) {
            int age = mc.player.tickCount - ring.startTick();
            if (age < 0 || age > 36) continue;
            float progress = age / 36.0f;
            int alpha = Math.max(0, Math.min(235, Math.round(235.0f * (1.0f - progress))));
            int color = withAlpha(VisualConfig.jumpRingColor(), alpha);
            double pulse = 1.0 + Math.sin(age * 0.22) * 0.018;
            double scale = scaleSetting * (0.92 + progress * 0.10) * pulse;
            Vec3 center = ring.center();
            float width = 2.8f - progress * 0.7f;

            // Flat "plate" rim with two inner tracks and small radial marks.
            drawCircle(line, pose, center, 1.04 * scale, 72, color, width + 0.35f);
            drawCircle(line, pose, center.add(0.0, 0.008, 0.0), 0.86 * scale, 72,
                withAlpha(VisualConfig.jumpRingColor(), alpha * 210 / 255), Math.max(1.2f, width * 0.72f));
            drawCircle(line, pose, center.add(0.0, 0.014, 0.0), 0.57 * scale, 56,
                withAlpha(VisualConfig.jumpRingColor(), alpha * 180 / 255), Math.max(1.0f, width * 0.58f));

            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2.0 * i / 24.0;
                Vec3 outer = flatPoint(center, Math.cos(angle) * 0.96 * scale,
                    Math.sin(angle) * 0.96 * scale, 0.010);
                Vec3 inner = flatPoint(center, Math.cos(angle) * 0.88 * scale,
                    Math.sin(angle) * 0.88 * scale, 0.010);
                drawLine(line, pose, outer, inner, color, Math.max(1.0f, width * 0.72f));
            }

            Vec3 symbolCenter = center.add(0.0, 0.022, 0.0);
            switch (pattern) {
                case 1 -> drawStarSymbol(line, pose, symbolCenter, scale * 0.92, color, width);
                case 2 -> drawBananaSymbol(line, pose, symbolCenter, scale * 0.92, color, width);
                default -> drawSkullSymbol(line, pose, symbolCenter, scale * 0.92, color, width);
            }
        }
    }

    private static Vec3 flatPoint(Vec3 center, double x, double z, double yOffset) {
        return new Vec3(center.x + x, center.y + yOffset, center.z + z);
    }

    private static void drawShape(VertexConsumer line, PoseStack.Pose pose, Vec3 center,
                                  double[][] points, double scale, int color, float width, boolean close) {
        if (points.length < 2) return;
        Vec3 previous = flatPoint(center, points[0][0] * scale, points[0][1] * scale, 0.0);
        Vec3 first = previous;
        for (int i = 1; i < points.length; i++) {
            Vec3 next = flatPoint(center, points[i][0] * scale, points[i][1] * scale, 0.0);
            drawLine(line, pose, previous, next, color, width);
            previous = next;
        }
        if (close) drawLine(line, pose, previous, first, color, width);
    }

    private static void drawSkullSymbol(VertexConsumer line, PoseStack.Pose pose,
                                        Vec3 center, double scale, int color, float width) {
        double[][] skull = {
            {-0.22, -0.12}, {-0.25, 0.00}, {-0.22, 0.18}, {-0.14, 0.27},
            {0.00, 0.30}, {0.14, 0.27}, {0.22, 0.18}, {0.25, 0.00},
            {0.22, -0.12}, {0.16, -0.19}, {0.16, -0.31}, {0.07, -0.31},
            {0.07, -0.23}, {-0.07, -0.23}, {-0.07, -0.31}, {-0.16, -0.31},
            {-0.16, -0.19}
        };
        drawShape(line, pose, center, skull, scale, color, width, true);
        drawShape(line, pose, center,
            new double[][]{{-0.17, 0.02}, {-0.08, 0.02}, {-0.08, 0.11}, {-0.17, 0.11}},
            scale, color, width, true);
        drawShape(line, pose, center,
            new double[][]{{0.08, 0.02}, {0.17, 0.02}, {0.17, 0.11}, {0.08, 0.11}},
            scale, color, width, true);
        drawShape(line, pose, center,
            new double[][]{{-0.09, -0.16}, {-0.09, -0.23}},
            scale, color, width, false);
        drawShape(line, pose, center,
            new double[][]{{0.00, -0.16}, {0.00, -0.23}},
            scale, color, width, false);
        drawShape(line, pose, center,
            new double[][]{{0.09, -0.16}, {0.09, -0.23}},
            scale, color, width, false);
    }

    private static void drawStarSymbol(VertexConsumer line, PoseStack.Pose pose,
                                       Vec3 center, double scale, int color, float width) {
        double[][] star = new double[10][2];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * i / 5.0;
            double radius = (i & 1) == 0 ? 0.34 : 0.15;
            star[i][0] = Math.cos(angle) * radius;
            star[i][1] = Math.sin(angle) * radius;
        }
        drawShape(line, pose, center, star, scale, color, width + 0.25f, true);
    }

    private static void drawBananaSymbol(VertexConsumer line, PoseStack.Pose pose,
                                         Vec3 center, double scale, int color, float width) {
        double[][] outer = new double[17][2];
        double[][] inner = new double[17][2];
        for (int i = 0; i <= 16; i++) {
            double x = -0.34 + (0.68 * i / 16.0);
            double t = x / 0.34;
            outer[i][0] = x;
            outer[i][1] = 0.20 * (1.0 - t * t) + 0.035;
            inner[i][0] = x;
            inner[i][1] = 0.075 * (1.0 - t * t) - 0.025;
        }
        drawShape(line, pose, center, outer, scale, color, width + 0.25f, false);
        drawShape(line, pose, center, inner, scale, color, width + 0.25f, false);
        drawShape(line, pose, center,
            new double[][]{{-0.34, 0.035}, {-0.30, -0.02}, {-0.25, -0.035}},
            scale, color, width, false);
        drawShape(line, pose, center,
            new double[][]{{0.34, 0.035}, {0.30, -0.02}, {0.25, -0.035}},
            scale, color, width, false);
    }

    private static void renderChinaHat(Minecraft mc, VisualConfig c, PoseStack.Pose pose, VertexConsumer line) {
        // A shallow conical Chinese-style hat: pointed crown, woven ribs and a wide brim.
        double centerX = mc.player.getX();
        double centerZ = mc.player.getZ();
        double brimY = mc.player.getY() + mc.player.getBbHeight() + 0.08;
        double radius = Math.max(0.62, mc.player.getBbWidth() * 1.28);
        double coneHeight = Math.max(0.30, radius * 0.48);
        Vec3 crown = new Vec3(centerX, brimY + coneHeight, centerZ);
        int segments = 64;
        float lineWidth = Math.max(1.4f, c.trailWidth * 0.62f);
        boolean rainbow = c.chromaTheme || c.trailRainbow;

        // Main brim outline and the narrow inner seam give the hat a visible rim.
        drawCircle(line, pose, new Vec3(centerX, brimY, centerZ), radius,
            segments, rainbow ? rainbowColor(mc.player.tickCount * 0.025f, 235)
                : VisualConfig.accentColor(), lineWidth + 0.35f);
        drawCircle(line, pose, new Vec3(centerX, brimY + 0.012, centerZ), radius * 0.91,
            segments, rainbow ? rainbowColor(mc.player.tickCount * 0.025f + 0.12f, 190)
                : withAlpha(VisualConfig.accentColor(), 185), Math.max(1.0f, lineWidth * 0.72f));

        // Concentric woven bands follow the slope of the cone.
        double[] bands = {0.30, 0.58, 0.82};
        for (int bandIndex = 0; bandIndex < bands.length; bandIndex++) {
            double fraction = bands[bandIndex];
            Vec3 bandCenter = new Vec3(centerX, brimY + coneHeight * (1.0 - fraction), centerZ);
            int bandColor = rainbow
                ? rainbowColor((mc.player.tickCount * 0.025f) + bandIndex * 0.14f, 205)
                : withAlpha(VisualConfig.accentColor(), 205 - bandIndex * 18);
            drawCircle(line, pose, bandCenter, radius * fraction,
                segments, bandColor, Math.max(1.0f, lineWidth * 0.68f));
        }

        // Radial ribs connect the pointed top to the outer edge, like a woven straw hat.
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0 * i / 24.0;
            Vec3 rim = new Vec3(centerX + Math.cos(angle) * radius, brimY,
                centerZ + Math.sin(angle) * radius);
            int ribColor = rainbow
                ? rainbowColor((i / 24.0f) + mc.player.tickCount * 0.025f, 220)
                : withAlpha(VisualConfig.accentColor(), 215);
            drawLine(line, pose, crown, rim, ribColor, Math.max(1.0f, lineWidth * 0.72f));
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static void renderProjectileTrajectories(Minecraft mc, PoseStack.Pose pose, VertexConsumer line) {
        int accent = VisualConfig.accentColor();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile) || projectile.getOwner() != mc.player) continue;

            EntityType<?> type = entity.getType();
            double gravity;
            double drag;
            float width;
            if (type == EntityType.ENDER_PEARL) {
                gravity = 0.03;
                drag = 0.99;
                width = 1.65f;
            } else if (type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW) {
                gravity = 0.05;
                drag = 0.99;
                width = 1.5f;
            } else if (type == EntityType.TRIDENT) {
                gravity = 0.05;
                drag = 0.99;
                width = 1.9f;
            } else if (type == EntityType.WIND_CHARGE) {
                // Wind charges fly with very little vertical drop and lose speed quickly.
                gravity = 0.0;
                drag = 0.95;
                width = 1.8f;
            } else {
                continue;
            }

            renderProjectilePrediction(mc, projectile, pose, line, gravity, drag, width, accent);
        }
    }

    private static void renderProjectilePrediction(Minecraft mc, Projectile projectile,
                                                   PoseStack.Pose pose, VertexConsumer line,
                                                   double gravity, double drag, float width, int accent) {
        Vec3 position = projectile.position();
        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < 0.0001) return;

        List<Vec3> points = new ArrayList<>(96);
        points.add(position);
        BlockPos landingBlock = null;

        // Simulate one tick at a time and raycast every segment against real block collision shapes.
        for (int tick = 0; tick < 120; tick++) {
            Vec3 next = position.add(velocity);
            BlockHitResult hit = mc.level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));

            if (hit.getType() == HitResult.Type.BLOCK) {
                points.add(hit.getLocation());
                landingBlock = hit.getBlockPos();
                break;
            }

            points.add(next);
            position = next;
            velocity = velocity.scale(drag).add(0.0, -gravity, 0.0);
            if (velocity.lengthSqr() < 0.0004) break;
        }

        for (int i = 1; i < points.size(); i++) {
            int alpha = Math.max(55, 225 - i * 2);
            drawLine(line, pose, points.get(i - 1), points.get(i),
                withAlpha(accent, alpha), width);
        }

        if (landingBlock != null) {
            drawBlockMarker(line, pose, landingBlock, accent);
        }
    }

    private static void drawBlockMarker(VertexConsumer line, PoseStack.Pose pose,
                                        BlockPos block, int color) {
        double x0 = block.getX() + 0.015;
        double y0 = block.getY() + 0.015;
        double z0 = block.getZ() + 0.015;
        double x1 = block.getX() + 0.985;
        double y1 = block.getY() + 0.985;
        double z1 = block.getZ() + 0.985;

        Vec3[] corners = {
            new Vec3(x0, y0, z0), new Vec3(x1, y0, z0),
            new Vec3(x0, y1, z0), new Vec3(x1, y1, z0),
            new Vec3(x0, y0, z1), new Vec3(x1, y0, z1),
            new Vec3(x0, y1, z1), new Vec3(x1, y1, z1)
        };
        for (int i = 0; i < corners.length; i++) {
            if ((i & 1) == 0) drawLine(line, pose, corners[i], corners[i | 1], color, 2.4f);
            if ((i & 2) == 0) drawLine(line, pose, corners[i], corners[i | 2], color, 2.4f);
            if ((i & 4) == 0) drawLine(line, pose, corners[i], corners[i | 4], color, 2.4f);
        }
    }

    private static void drawPath(VertexConsumer line, PoseStack.Pose pose, Deque<Vec3> points,
                                 Integer fixedColor, float width, int tick) {
        Vec3 previous = null;
        int index = 0;
        for (Vec3 point : points) {
            if (previous != null) {
                int color = fixedColor == null
                    ? rainbowColor((index + tick) * 0.045f, 210)
                    : fixedColor;
                drawLine(line, pose, previous, point, color, width);
            }
            previous = point;
            index++;
        }
    }

    private static void drawCircle(VertexConsumer line, PoseStack.Pose pose, Vec3 center,
                                   double radius, int segments, int color, float width) {
        Vec3 previous = null;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            Vec3 point = new Vec3(center.x + Math.cos(angle) * radius, center.y,
                center.z + Math.sin(angle) * radius);
            if (previous != null) drawLine(line, pose, previous, point, color, width);
            previous = point;
        }
    }

    private static void drawLine(VertexConsumer consumer, PoseStack.Pose pose,
                                 Vec3 a, Vec3 b, int color, float width) {
        int alpha = (color >>> 24) & 255;
        int red = (color >>> 16) & 255;
        int green = (color >>> 8) & 255;
        int blue = color & 255;
        // The 1.21.11 LINES vertex format requires a normal for every vertex.
        // The lines shader interprets this normal as the direction along the segment.
        float dx = (float) (b.x - a.x);
        float dy = (float) (b.y - a.y);
        float dz = (float) (b.z - a.z);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-6f) return;

        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;
        consumer.addVertex(pose, (float) a.x, (float) a.y, (float) a.z)
            .setColor(red, green, blue, alpha).setNormal(nx, ny, nz).setLineWidth(width);
        consumer.addVertex(pose, (float) b.x, (float) b.y, (float) b.z)
            .setColor(red, green, blue, alpha).setNormal(nx, ny, nz).setLineWidth(width);
    }

    private static int rainbowColor(float phase, int alpha) {
        float r = (float) (Math.sin(phase * Math.PI * 2.0) * 0.5 + 0.5);
        float g = (float) (Math.sin((phase + 0.3333) * Math.PI * 2.0) * 0.5 + 0.5);
        float b = (float) (Math.sin((phase + 0.6666) * Math.PI * 2.0) * 0.5 + 0.5);
        return (Math.max(0, Math.min(255, alpha)) << 24)
            | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
