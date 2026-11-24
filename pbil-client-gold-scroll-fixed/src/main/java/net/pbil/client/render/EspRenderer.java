package net.pbil.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.pbil.client.module.ModuleManager;

/**
 * Zeichnet Umrisse fuer Block ESP und Storage ESP.
 * Angepasst an die Fabric Rendering API für Minecraft 1.21.11.
 */
public final class EspRenderer {
    private EspRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(EspRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        boolean block = ModuleManager.blockEsp.isEnabled() && !ModuleManager.BLOCK_ESP_HITS.isEmpty();
        boolean storage = ModuleManager.storageEsp.isEnabled() && !ModuleManager.STORAGE_HITS.isEmpty();
        if (!block && !storage) return;

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cam = ctx.camera().getPos();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        if (block) {
            for (BlockPos pos : ModuleManager.BLOCK_ESP_HITS) {
                VertexRendering.drawBox(matrices, lines, new Box(pos), 0.3f, 0.8f, 1.0f, 1.0f);
            }
        }
        if (storage) {
            for (ModuleManager.StorageHit hit : ModuleManager.STORAGE_HITS) {
                VertexRendering.drawBox(matrices, lines, new Box(hit.pos()), hit.r(), hit.g(), hit.b(), 1.0f);
            }
        }

        matrices.pop();
    }
}
