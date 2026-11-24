package net.pbil.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.pbil.client.module.Category;
import net.pbil.client.module.Module;
import net.pbil.client.module.ModuleManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modul-Menue im Stil des Screenshots. Eingaben werden ueber GLFW abgefragt,
 * damit der Code nicht an den Eingabe-Signaturen einzelner Minecraft-Versionen haengt.
 *
 * Linksklick: Modul an/aus   |   Mittelklick: Taste zuweisen (Esc = loeschen)
 * Scrollrad ueber einem Modul: Wert aendern   |   Tippen: Module suchen
 */
public class ClickGui extends Screen {
    private static final int PANEL_W = 150;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 19;
    private static final int GAP = 8;

    // PBIL Gold theme
    private static final int COL_PANEL = 0xF0101013;
    private static final int COL_HEADER = 0xFF191713;
    private static final int COL_ROW = 0xE51B1A18;
    private static final int COL_ROW_HOVER = 0xFF302719;
    private static final int COL_ON = 0xFF76500B;
    private static final int COL_GOLD = 0xFFFFC629;
    private static final int COL_GOLD_DARK = 0xFFB77A0A;
    private static final int COL_TEXT = 0xFFFFF8E8;
    private static final int COL_DIM = 0xFFAAA49A;

    private final Map<Integer, Boolean> lastKeys = new HashMap<>();
    private final int[] scrollOffsets = new int[Category.values().length];
    private boolean lastLeft, lastMiddle;
    private Module binding = null;
    private String query = "";
    private int mouseX, mouseY;

    public ClickGui() {
        super(Text.literal("Pbil Client"));
    }

    @Override
    protected void init() {
        // aktuellen Tastenstatus merken, damit das noch gehaltene P nichts ausloest
        long w = MinecraftClient.getInstance().getWindow().getHandle();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            lastKeys.put(k, GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS);
        }
        lastLeft = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        lastMiddle = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return binding == null;
    }

    private List<Module> modulesOf(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.category != c) continue;
            if (!query.isEmpty() && !m.name.toLowerCase().contains(query.toLowerCase())) continue;
            out.add(m);
        }
        return out;
    }

    private int panelX(int index) {
        int total = Category.values().length * PANEL_W + (Category.values().length - 1) * GAP;
        int start = Math.max(10, (this.width - total) / 2);
        return start + index * (PANEL_W + GAP);
    }

    private int contentTop() { return 66 + HEADER_H; }
    private int contentBottom() { return this.height - 42; }
    private int visibleRows() { return Math.max(1, (contentBottom() - contentTop()) / ROW_H); }
    private int maxScroll(int categoryIndex) {
        return Math.max(0, modulesOf(Category.values()[categoryIndex]).size() - visibleRows());
    }

    /** Liefert das Modul unter dem Mauszeiger oder null; Scrollposition wird beruecksichtigt. */
    private Module moduleAt(int mx, int my) {
        Category[] cats = Category.values();
        if (my < contentTop() || my >= contentBottom()) return null;
        for (int i = 0; i < cats.length; i++) {
            int x = panelX(i);
            if (mx < x || mx >= x + PANEL_W) continue;
            int index = (my - contentTop()) / ROW_H + scrollOffsets[i];
            List<Module> list = modulesOf(cats[i]);
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }
        return null;
    }

    private boolean pressedNow(long w, int key) {
        boolean down = GLFW.glfwGetKey(w, key) == GLFW.GLFW_PRESS;
        boolean was = lastKeys.getOrDefault(key, false);
        lastKeys.put(key, down);
        return down && !was;
    }

    private void pollInput() {
        long w = MinecraftClient.getInstance().getWindow().getHandle();

        // Tastenbelegung
        if (binding != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (pressedNow(w, k)) {
                    binding.key = (k == GLFW.GLFW_KEY_ESCAPE) ? -1 : k;
                    binding = null;
                    net.pbil.client.Config.save();
                    break;
                }
            }
        } else {
            // Suche
            for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
                if (pressedNow(w, k)) query += (char) ('a' + (k - GLFW.GLFW_KEY_A));
            }
            for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
                if (pressedNow(w, k)) query += (char) ('0' + (k - GLFW.GLFW_KEY_0));
            }
            if (pressedNow(w, GLFW.GLFW_KEY_SPACE)) query += " ";
            if (pressedNow(w, GLFW.GLFW_KEY_BACKSPACE) && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
            }
        }

        // Mausklicks
        boolean left = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean middle = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        Module hit = moduleAt(mouseX, mouseY);
        if (hit != null && binding == null) {
            if (left && !lastLeft) hit.toggle();
            if (middle && !lastMiddle) binding = hit;
        }
        lastLeft = left;
        lastMiddle = middle;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        Module m = moduleAt((int) mx, (int) my);
        if (m != null && m.hasValue) {
            m.adjustValue(vertical > 0 ? 1 : -1);
            return true;
        }
        if (my >= contentTop() && my < contentBottom()) {
            for (int i = 0; i < Category.values().length; i++) {
                int x = panelX(i);
                if (mx >= x && mx < x + PANEL_W) {
                    scrollOffsets[i] = Math.max(0, Math.min(maxScroll(i), scrollOffsets[i] + (vertical < 0 ? 2 : -2)));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        this.mouseX = mx;
        this.mouseY = my;
        super.render(ctx, mx, my, delta);
        pollInput();

        Category[] cats = Category.values();
        Module hovered = moduleAt(mx, my);

        // Dunkler Hintergrund und goldene Kopf-/Fußleiste
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        int total = cats.length * PANEL_W + (cats.length - 1) * GAP;
        int left = Math.max(8, (this.width - total) / 2);
        int rightEdge = Math.min(this.width - 8, left + total);
        ctx.fill(left - 2, 12, rightEdge + 2, this.height - 12, COL_PANEL);
        ctx.fill(left - 2, 12, rightEdge + 2, 14, COL_GOLD);
        ctx.fill(left - 2, 56, rightEdge + 2, 58, COL_GOLD_DARK);
        ctx.fill(left - 2, this.height - 34, rightEdge + 2, this.height - 32, COL_GOLD_DARK);

        ctx.drawText(this.textRenderer, "♛  PBIL CLIENT", left + 8, 27, COL_GOLD, true);
        String searchLabel = query.isEmpty() ? "Suche: Module..." : "Suche: " + query;
        int searchX = Math.max(left + 150, rightEdge - this.textRenderer.getWidth(searchLabel) - 12);
        ctx.fill(searchX - 6, 22, rightEdge - 6, 43, COL_HEADER);
        ctx.drawText(this.textRenderer, searchLabel, searchX, 28, COL_DIM, false);

        for (int i = 0; i < cats.length; i++) {
            int x = panelX(i);
            int y = 66;
            ctx.fill(x, y, x + PANEL_W, y + HEADER_H, COL_HEADER);
            ctx.fill(x, y, x + PANEL_W, y + 2, COL_GOLD_DARK);
            ctx.drawText(this.textRenderer, cats[i].label.toUpperCase(), x + 8, y + 7, COL_GOLD, true);
            y += HEADER_H;

            // Jede Kategorie hat ihren eigenen Scrollbereich.
            int clipBottom = contentBottom();
            ctx.enableScissor(x, contentTop(), x + PANEL_W, clipBottom);
            List<Module> categoryModules = modulesOf(cats[i]);
            for (int j = scrollOffsets[i]; j < categoryModules.size(); j++) {
                Module m = categoryModules.get(j);
                int rowY = contentTop() + (j - scrollOffsets[i]) * ROW_H;
                if (rowY >= clipBottom) break;
                int bg = m.isEnabled() ? COL_ON : (m == hovered ? COL_ROW_HOVER : COL_ROW);
                ctx.fill(x, rowY, x + PANEL_W, rowY + ROW_H - 1, bg);
                if (m.isEnabled()) ctx.fill(x, rowY, x + 2, rowY + ROW_H - 1, COL_GOLD);
                ctx.drawText(this.textRenderer, m.name, x + 7, rowY + 5, m.isEnabled() ? COL_GOLD : COL_TEXT, false);

                String right = "";
                if (binding == m) right = "...";
                else if (m.key > 0) right = keyName(m.key);
                else if (m.hasValue) right = trimValue(m.value);
                if (!right.isEmpty()) {
                    int tw = this.textRenderer.getWidth(right);
                    ctx.drawText(this.textRenderer, right, x + PANEL_W - tw - 6, rowY + 5, m.isEnabled() ? COL_GOLD : COL_DIM, false);
                }
            }
            ctx.disableScissor();
            if (maxScroll(i) > 0) {
                String indicator = (scrollOffsets[i] + 1) + "/" + categoryModules.size();
                ctx.drawText(this.textRenderer, indicator, x + PANEL_W - this.textRenderer.getWidth(indicator) - 4,
                        contentBottom() + 3, COL_GOLD_DARK, false);
            }
        }

        String tip;
        if (binding != null) tip = "Taste fuer " + binding.name + " druecken (Esc = loeschen)";
        else if (hovered != null) tip = hovered.name + ": " + hovered.description
                + (hovered.hasValue ? "  [" + hovered.valueName + " " + trimValue(hovered.value) + "]" : "");
        else tip = "LINKSKLICK: AN/AUS   |   MITTELKLICK: TASTE   |   SCROLLRAD: WERT";
        ctx.drawText(this.textRenderer, tip, left + 8, this.height - 25, COL_GOLD, false);
    }

    private static String trimValue(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            default -> "K" + key;
        };
    }
}
