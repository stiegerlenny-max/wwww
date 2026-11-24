package net.pbil.client;

import net.fabricmc.loader.api.FabricLoader;
import net.pbil.client.module.Module;
import net.pbil.client.module.ModuleManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Speichert Modulstatus, Tasten und Werte in config/pbil-client.properties. */
public final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pbil-client.properties");
    private static boolean loading = false;

    private Config() {}

    public static void load() {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            PbilClient.LOGGER.warn("Config konnte nicht gelesen werden", e);
            return;
        }
        loading = true;
        try {
            for (Module m : ModuleManager.all()) {
                String id = id(m);
                m.key = Integer.parseInt(p.getProperty(id + ".key", "-1"));
                if (m.hasValue) {
                    m.value = Double.parseDouble(p.getProperty(id + ".value", String.valueOf(m.value)));
                }
                m.restoreEnabled(Boolean.parseBoolean(p.getProperty(id + ".enabled", "false")));
            }
        } catch (NumberFormatException e) {
            PbilClient.LOGGER.warn("Config enthält ungültige Werte, benutze Standardwerte", e);
        } finally {
            loading = false;
        }
    }

    public static void save() {
        if (loading) return;
        Properties p = new Properties();
        for (Module m : ModuleManager.all()) {
            String id = id(m);
            p.setProperty(id + ".enabled", String.valueOf(m.isEnabled()));
            p.setProperty(id + ".key", String.valueOf(m.key));
            if (m.hasValue) p.setProperty(id + ".value", String.valueOf(m.value));
        }
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "Pbil Client");
        } catch (IOException e) {
            PbilClient.LOGGER.warn("Config konnte nicht gespeichert werden", e);
        }
    }

    private static String id(Module m) {
        return m.name.toLowerCase().replace(' ', '_');
    }
}
