package net.pbil.client.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.pbil.client.PbilClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Integer, Boolean> LAST_KEY_STATE = new LinkedHashMap<>();

    // Direkt erreichbare Module
    public static Module fullbright, zoom, esp, itemEsp, blockEsp, storageEsp;
    public static Module customFov, hud, performance, toggleSprint;

    /** Ergebnisse der Block-Scans, werden vom EspRenderer gezeichnet. */
    public static final List<BlockPos> BLOCK_ESP_HITS = new ArrayList<>();
    public static final List<StorageHit> STORAGE_HITS = new ArrayList<>();

    public record StorageHit(BlockPos pos, float r, float g, float b) {}

    private static final Set<Block> BLOCK_ESP_BLOCKS = new HashSet<>(List.of(
            Blocks.BEACON, Blocks.END_PORTAL_FRAME, Blocks.ENCHANTING_TABLE
    ));

    private ModuleManager() {}

    public static List<Module> all() {
        return MODULES;
    }

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    public static void init() {
        // ---------------- Render ----------------
        fullbright = add(new Module("Fullbright", Category.RENDER, "Helle Sicht ueberall (Nachtsicht nur auf dem Client)") {
            @Override public void onTick(MinecraftClient mc) {
                ClientPlayerEntity p = mc.player;
                if (p == null) return;
                StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (cur == null || cur.getDuration() < 220) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false, false));
                }
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        zoom = add(new Module("Zoom", Category.RENDER, "Halte C zum Zoomen, Scrollrad im Menue = Staerke") {}
                .withValue("Faktor", 4.0, 1.5, 10.0, 0.5));

        esp = add(new Module("ESP", Category.RENDER, "Spieler leuchten (Glow-Effekt, nur lokal sichtbar)") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.world == null) return;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof PlayerEntity && e != mc.player) e.setGlowing(true);
                }
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world == null) return;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof PlayerEntity && e != mc.player) e.setGlowing(false);
                }
            }
        });

        itemEsp = add(new Module("Item ESP", Category.RENDER, "Herumliegende Items leuchten") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.world == null) return;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof ItemEntity) e.setGlowing(true);
                }
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world == null) return;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof ItemEntity) e.setGlowing(false);
                }
            }
        });

        blockEsp = add(new Module("Block ESP", Category.RENDER, "Markiert Beacon, Portalrahmen und Zaubertisch im Umkreis") {}
                .withValue("Radius", 24, 8, 48, 4));

        storageEsp = add(new Module("Storage ESP", Category.RENDER, "Markiert Kisten, Fass, Shulker, Endertruhe in geladenen Chunks") {
            @Override public void onDisable() {
                STORAGE_HITS.clear();
            }
        });

        // ---------------- Visuals ----------------
        customFov = add(new Module("Custom FOV", Category.VISUALS, "Eigenes Sichtfeld, Scrollrad im Menue = Wert") {}
                .withValue("FOV", 90, 30, 140, 5));

        // ---------------- Client ----------------
        hud = add(new Module("HUD", Category.CLIENT, "Wasserzeichen und Koordinaten") {});
        performance = add(new Module("Performance", Category.CLIENT, "FPS und Ping anzeigen") {});

        // ---------------- Komfort ----------------
        toggleSprint = add(new Module("Sprint", Category.KOMFORT, "Sprintet automatisch beim Vorwaertslaufen") {
            @Override public void onTick(MinecraftClient mc) {
                ClientPlayerEntity p = mc.player;
                if (p == null) return;
                if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                        && p.getHungerManager().getFoodLevel() > 6) {
                    p.setSprinting(true);
                }
            }
        });

        PbilClient.LOGGER.info("{} Module registriert", MODULES.size());
    }

    private static Module add(Module m) {
        MODULES.add(m);
        return m;
    }

    // ---------------- Tick, Tasten, Scans ----------------

    private static int scanTimer = 0;

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        for (Module m : MODULES) {
            if (m.isEnabled()) m.onTick(mc);
        }

        if (++scanTimer >= 20) {
            scanTimer = 0;
            tickScans(mc);
        }
    }

    private static void tickScans(MinecraftClient mc) {
        BLOCK_ESP_HITS.clear();
        STORAGE_HITS.clear();

        if (blockEsp.isEnabled()) {
            int r = (int) blockEsp.value;
            BlockPos c = mc.player.getBlockPos();
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        pos.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                        if (BLOCK_ESP_BLOCKS.contains(mc.world.getBlockState(pos).getBlock())) {
                            BLOCK_ESP_HITS.add(pos.toImmutable());
                        }
                    }
                }
            }
        }

        if (storageEsp.isEnabled()) {
            int cx = mc.player.getChunkPos().x;
            int cz = mc.player.getChunkPos().z;
            int r = Math.min(8, mc.options.getViewDistance().getValue());
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        StorageHit hit = classify(be);
                        if (hit != null) STORAGE_HITS.add(hit);
                    }
                }
            }
        }
    }

    private static StorageHit classify(BlockEntity be) {
        BlockPos p = be.getPos();
        if (be instanceof EnderChestBlockEntity) return new StorageHit(p, 0.7f, 0.2f, 1.0f);
        if (be instanceof ShulkerBoxBlockEntity) return new StorageHit(p, 1.0f, 0.4f, 0.8f);
        if (be instanceof BarrelBlockEntity) return new StorageHit(p, 1.0f, 0.6f, 0.2f);
        if (be instanceof ChestBlockEntity) return new StorageHit(p, 1.0f, 0.85f, 0.2f);
        return null;
    }

    /** Schaltet Module per eigener Taste um. Wird nur ohne offenen Bildschirm aufgerufen. */
    public static void pollKeys(long window) {
        for (Module m : MODULES) {
            if (m.key <= 0) continue;
            boolean down = GLFW.glfwGetKey(window, m.key) == GLFW.GLFW_PRESS;
            boolean was = LAST_KEY_STATE.getOrDefault(m.key, false);
            if (down && !was) m.toggle();
            LAST_KEY_STATE.put(m.key, down);
        }
    }

    // ---------------- FOV (vom Mixin aufgerufen) ----------------

    public static boolean zoomHeld = false;

    public static float modifyFov(float original) {
        if (customFov == null) return original;
        float fov = original;
        if (customFov.isEnabled()) fov = (float) customFov.value;
        if (zoom.isEnabled() && zoomHeld) fov = fov / (float) zoom.value;
        return fov;
    }

    // ---------------- HUD ----------------

    public static void renderHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || mc.options.hudHidden) return;
        int white = 0xFFFFFFFF;
        int blue = 0xFF4F8CFF;

        if (hud.isEnabled()) {
            ctx.drawText(mc.textRenderer, "Pbil Client", 4, 4, blue, true);
            String xyz = String.format("XYZ %d %d %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            ctx.drawText(mc.textRenderer, xyz, 4, 16, white, true);
        }

        if (performance.isEnabled()) {
            int ping = -1;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            String text = "FPS " + mc.getCurrentFps() + (ping >= 0 ? "  Ping " + ping + " ms" : "");
            int y = hud.isEnabled() ? 28 : 4;
            ctx.drawText(mc.textRenderer, text, 4, y, white, true);
        }
    }
}
