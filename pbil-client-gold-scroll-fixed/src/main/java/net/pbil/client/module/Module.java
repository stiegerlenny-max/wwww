package net.pbil.client.module;

import net.minecraft.client.MinecraftClient;
import net.pbil.client.Config;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;

    private boolean enabled;
    /** GLFW-Taste zum Umschalten, -1 = keine. */
    public int key = -1;

    // Optionaler Zahlenwert (Scrollrad im Menü).
    public boolean hasValue;
    public double value;
    public double min;
    public double max;
    public double step;
    public String valueName = "";

    protected Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    protected Module withValue(String valueName, double value, double min, double max, double step) {
        this.hasValue = true;
        this.valueName = valueName;
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean state) {
        if (state == enabled) return;
        enabled = state;
        if (state) onEnable(); else onDisable();
        Config.save();
    }

    /** Wird nur vom Laden der Config benutzt. */
    public void restoreEnabled(boolean state) {
        enabled = state;
        if (state) onEnable();
    }

    public void adjustValue(int direction) {
        if (!hasValue) return;
        value = Math.max(min, Math.min(max, value + direction * step));
        Config.save();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}
}
