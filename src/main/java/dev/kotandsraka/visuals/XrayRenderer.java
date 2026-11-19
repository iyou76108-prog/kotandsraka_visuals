package dev.kotandsraka.visuals;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Accent-colored translucent boxes rendered with depth testing disabled.
 * Built against the Minecraft 1.21.11 Fabric world-render API.
 */
public final class XrayRenderer {
    private static final RenderPipeline XRAY_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("kotandsraka_visuals", "pipeline/xray_filled_box"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final org.joml.Matrix4f TEXTURE_MATRIX = new org.joml.Matrix4f();

    private static BufferBuilder builder;
    private static MappableRingBuffer vertexBuffer;
    private static boolean failed;

    private XrayRenderer() {}

    public static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(XrayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (failed) return;

        Minecraft mc = Minecraft.getInstance();
        VisualConfig cfg = VisualConfig.get();
        if (mc.level == null || mc.player == null || (!cfg.playerGlow && !cfg.chestGlow)) return;
        if (context.matrices() == null || context.worldState() == null
            || context.worldState().cameraRenderState == null
            || context.worldState().cameraRenderState.pos == null) return;

        try {
            PoseStack matrices = context.matrices();
            Vec3 camera = context.worldState().cameraRenderState.pos;
            matrices.pushPose();
            try {
                matrices.translate(-camera.x, -camera.y, -camera.z);
                if (cfg.playerGlow) renderPlayers(mc, matrices);
                if (cfg.chestGlow) renderChests(mc, matrices, camera);
            } finally {
                matrices.popPose();
            }

            drawIfNeeded(mc);
        } catch (Throwable error) {
            if (builder != null) {
                try {
                    MeshData partial = builder.build();
                    if (partial != null) partial.close();
                } catch (Throwable ignored) {
                }
                builder = null;
            }
            failed = true;
            VisualConfig current = VisualConfig.get();
            current.playerGlow = false;
            current.chestGlow = false;
            VisualConfig.save();
            System.err.println("[KotCode] X-ray renderer disabled after a render error: " + error);
        }
    }

    private static BufferBuilder getBuilder() {
        if (builder == null) {
            builder = new BufferBuilder(ALLOCATOR, XRAY_FILLED.getVertexFormatMode(), XRAY_FILLED.getVertexFormat());
        }
        return builder;
    }

    private static void renderPlayers(Minecraft mc, PoseStack matrices) {
        int color = VisualConfig.accentColor();
        float red = ((color >> 16) & 255) / 255.0f;
        float green = ((color >> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        BufferBuilder out = getBuilder();
        Matrix4fc matrix = matrices.last().pose();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == mc.player || !player.isAlive()) continue;
            if (player.distanceToSqr(mc.player) > 64.0 * 64.0) continue;
            AABB box = player.getBoundingBox().inflate(0.035, 0.025, 0.035);
            addFilledBox(out, matrix,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                red, green, blue, 0.20f);
        }
    }

    private static void renderChests(Minecraft mc, PoseStack matrices, Vec3 camera) {
        int color = VisualConfig.accentColor();
        float red = ((color >> 16) & 255) / 255.0f;
        float green = ((color >> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        BufferBuilder out = getBuilder();
        Matrix4fc matrix = matrices.last().pose();

        int centerChunkX = mc.player.getBlockX() >> 4;
        int centerChunkZ = mc.player.getBlockZ() >> 4;
        // Query already-loaded chunks only: false prevents the X-ray pass from loading terrain.
        for (int chunkX = centerChunkX - 4; chunkX <= centerChunkX + 4; chunkX++) {
            for (int chunkZ = centerChunkZ - 4; chunkZ <= centerChunkZ + 4; chunkZ++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity.isRemoved()) continue;
                    BlockPos pos = blockEntity.getBlockPos();
                    var state = mc.level.getBlockState(pos);
                    if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST)
                        && !state.is(Blocks.ENDER_CHEST)) continue;

                    double dx = pos.getX() + 0.5 - camera.x;
                    double dy = pos.getY() + 0.5 - camera.y;
                    double dz = pos.getZ() + 0.5 - camera.z;
                    if (dx * dx + dy * dy + dz * dz > 64.0 * 64.0) continue;

                    float minX = pos.getX() + 0.035f;
                    float minY = pos.getY() + 0.035f;
                    float minZ = pos.getZ() + 0.035f;
                    float maxX = pos.getX() + 0.965f;
                    float maxY = pos.getY() + (state.is(Blocks.ENDER_CHEST) ? 0.90f : 0.88f);
                    float maxZ = pos.getZ() + 0.965f;
                    addFilledBox(out, matrix, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, 0.30f);
                }
            }
        }
    }

    private static void addFilledBox(BufferBuilder out, Matrix4fc matrix,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     float red, float green, float blue, float alpha) {
        // Front
        vertex(out, matrix, minX, minY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, minX, maxY, maxZ, red, green, blue, alpha);
        // Back
        vertex(out, matrix, maxX, minY, minZ, red, green, blue, alpha);
        vertex(out, matrix, minX, minY, minZ, red, green, blue, alpha);
        vertex(out, matrix, minX, maxY, minZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, maxY, minZ, red, green, blue, alpha);
        // Left
        vertex(out, matrix, minX, minY, minZ, red, green, blue, alpha);
        vertex(out, matrix, minX, minY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, minX, maxY, minZ, red, green, blue, alpha);
        // Right
        vertex(out, matrix, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, minY, minZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
        // Top
        vertex(out, matrix, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(out, matrix, minX, maxY, minZ, red, green, blue, alpha);
        // Bottom
        vertex(out, matrix, minX, minY, minZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, minY, minZ, red, green, blue, alpha);
        vertex(out, matrix, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(out, matrix, minX, minY, maxZ, red, green, blue, alpha);
    }

    private static void vertex(BufferBuilder out, Matrix4fc matrix,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        out.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void drawIfNeeded(Minecraft client) {
        if (builder == null) return;
        MeshData mesh = builder.build();
        builder = null;
        if (mesh == null) return;
        try {
            MeshData.DrawState drawState = mesh.drawState();
            VertexFormat format = drawState.format();
            int vertexBytes = drawState.vertexCount() * format.getVertexSize();

            if (vertexBuffer == null || vertexBuffer.size() < vertexBytes) {
                if (vertexBuffer != null) vertexBuffer.close();
                vertexBuffer = new MappableRingBuffer(
                    () -> "KotCode X-ray vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    vertexBytes
                );
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBuffer.MappedView mapped = encoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
                MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
            }

            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (XRAY_FILLED.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
                mesh.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
                indexBuffer = XRAY_FILLED.getVertexFormat().uploadImmediateIndexBuffer(mesh.indexBuffer());
                indexType = mesh.drawState().indexType();
            } else {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(XRAY_FILLED.getVertexFormatMode());
                indexBuffer = sequential.getBuffer(drawState.indexCount());
                indexType = sequential.type();
            }

            var dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX
            );

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "KotCode X-ray glow",
                client.getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty()
            )) {
                pass.setPipeline(XRAY_FILLED);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setVertexBuffer(0, vertexBuffer.currentBuffer());
                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(0, 0, drawState.indexCount(), 1);
            }

            vertexBuffer.rotate();
        } finally {
            mesh.close();
        }
    }
}
