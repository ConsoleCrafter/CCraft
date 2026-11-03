package de.consolecrafter.ccraft.cheats;

import de.consolecrafter.ccraft.CCraftMod;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistenz aller Cheat-Einstellungen ({@code config/ccraft-cheats.properties}).
 *
 * <p>Werte werden ueber Cheat-Id und Optionsnamen adressiert. Dadurch lassen
 * sich neue Cheats hinzufuegen, ohne diese Klasse anzupassen.</p>
 */
public final class CheatConfig {

    private CheatConfig() {}

    private static final Properties store = new Properties();
    private static Path file;

    public static Path file() {
        if (file == null) {
            file = MinecraftClient.getInstance().runDirectory.toPath()
                    .resolve("config").resolve("ccraft-cheats.properties");
        }
        return file;
    }

    private static String propKey(String id, String key) {
        return id + "." + key;
    }

    // ── Lesen ────────────────────────────────────────────────────────────────
    // Alle Getter sind reine Lesevorgaenge: ein fehlender Schluessel wird NICHT
    // in den Speicher geschrieben. Sonst sammelt die Datei beim ersten Menue-
    // Oeffnen alle Optionen an und "nie gesetzt" ist nicht mehr von "bewusst auf
    // Default gesetzt" zu unterscheiden.
    public static boolean getBoolean(String id, String key, boolean def) {
        String value = getRaw(id, key);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    public static int getInt(String id, String key, int def) {
        String value = getRaw(id, key);
        if (value == null) return def;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int getInt(String id, String key, int def, int min, int max) {
        return Math.max(min, Math.min(max, getInt(id, key, def)));
    }

    public static double getDouble(String id, String key, double def) {
        String value = getRaw(id, key);
        if (value == null) return def;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static double getDouble(String id, String key, double def, double min, double max) {
        return Math.max(min, Math.min(max, getDouble(id, key, def)));
    }

    /** Roher gespeicherter Wert (auch leer oder unbekannt), sonst {@code null}. */
    public static String getRaw(String id, String key) {
        return store.getProperty(propKey(id, key));
    }

    /** Ob fuer diesen Schluessel ueberhaupt ein Wert gespeichert ist. */
    public static boolean has(String id, String key) {
        return store.containsKey(propKey(id, key));
    }

    /** Liefert {@code def}, wenn der gespeicherte Wert keine der erlaubten Optionen ist. */
    public static String getChoice(String id, String key, String def, String... allowed) {
        String value = getRaw(id, key);
        if (value == null) return def;
        for (String option : allowed) {
            if (option.equals(value)) return value;
        }
        return def;
    }

    // ── Schreiben ────────────────────────────────────────────────────────────
    public static void set(String id, String key, Object value) {
        store.setProperty(propKey(id, key), String.valueOf(value));
        save();
    }

    // ── Speicher ─────────────────────────────────────────────────────────────
    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            loaded.load(in);
        } catch (Exception e) {
            // Properties.load() bricht beim ersten kaputten Eintrag ab (z. B.
            // fehlerhaftes Unicode-Escape), behaelt aber alles davor. Diese
            // Teildaten sind besser als gar nichts — deshalb wird unten
            // trotzdem uebernommen statt den ganzen Ladevorgang zu verwerfen.
            CCraftMod.LOGGER.warn("ccraft-cheats.properties ist teilweise beschaedigt; "
                    + "die lesbaren Eintraege werden uebernommen.", e);
        }
        store.putAll(loaded);
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (OutputStream out = Files.newOutputStream(f)) {
                store.store(out, "CCraft Cheats");
            }
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("ccraft-cheats.properties konnte nicht gespeichert werden.", e);
        }
    }
}
