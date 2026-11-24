package net.pbil.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.pbil.client.gui.ClickGui;
import net.pbil.client.module.ModuleManager;
import net.pbil.client.render.EspRenderer;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PbilClient implements ClientModInitializer {
    public static final String NAME = "Pbil Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private boolean menuKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        EspRenderer.register();
        HudRenderCallback.EVENT.register((ctx, tickCounter) ->
                ModuleManager.renderHud(ctx, MinecraftClient.getInstance()));
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        LOGGER.info("{} geladen - Menue mit rechter Shift-Taste", NAME);
    }

    private void onTick(MinecraftClient mc) {
        long window = mc.getWindow().getHandle();

        // Zoom-Taste (C) nur ohne offenen Bildschirm
        ModuleManager.zoomHeld = mc.currentScreen == null
                && GLFW.glfwGetKey(window, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS;

        if (mc.currentScreen == null && mc.player != null) {
            boolean down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !menuKeyWasDown) {
                mc.setScreen(new ClickGui());
            }
            menuKeyWasDown = down;

            ModuleManager.pollKeys(window);
        } else {
            menuKeyWasDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        }

        ModuleManager.tick(mc);
    }
}
