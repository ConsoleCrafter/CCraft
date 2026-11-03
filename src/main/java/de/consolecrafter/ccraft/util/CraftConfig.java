package de.consolecrafter.ccraft.util;

import de.consolecrafter.ccraft.CCraftMod;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistente Design- und HUD-Einstellungen.
 * Gespeichert in {@code config/ccraft-design.properties}.
 */
public final class CraftConfig {

    private CraftConfig() {}

    // ── Design ───────────────────────────────────────────────────────────────
    /** Basis-Helligkeit: dunkles Theme (true) oder helles Theme (false). */
    public static boolean dark = true;
    /** Akzentfarbe als 0xRRGGBB. */
    public static int accent = 0xFF4D4D;
    /** Name der zuletzt gewählten Palette (nur Anzeige). */
    public static String preset = "CCraft";
    /** Fullbright im Spiel aktivieren. */
    public static boolean fullbright = false;
    /** Globale Schrift: 0 Vanilla, 1 CCraft ASCII, 2 Helltown, 3 Impact, 4 Lazyarie. */
    public static int fontChoice = 0;

    // ── HUD ──────────────────────────────────────────────────────────────────
    /** Anzahl der HUD-Stile; hudStyle laeuft von 0 bis {@code HUD_STYLE_COUNT - 1}. */
    public static final int HUD_STYLE_COUNT = 6;
    /** Anzahl der waehlbaren Logos (0 = CCraft-Standard). */
    public static final int LOGO_COUNT = 5;
    /** HUD-Stil: 0 Classic, 1 Kompakt, 2 Boxed, 3 Akzent, 4 Minimal, 5 Neon. */
    public static int hudStyle = 0;
    /** Gewaehlte Logo-Variante fuer HUD und Hauptmenue. */
    public static int logoChoice = 0;
    /** HUD-Textfarbe als 0xAARRGGBB (Alpha muss gesetzt sein!). */
    public static int hudText = 0xFFFFFFFF;
    /** HUD-Textschatten an/aus. */
    public static boolean hudShadow = true;
    /** Keystrokes-Overlay (WASD/Maus/Space) an/aus. */
    public static boolean keystrokes = false;
    /** Kantenlänge einer Keystrokes-Taste in px. */
    public static int keystrokesSize = 24;
    /** Keystrokes-Design: 0 = 3D, 1 = Transparent, 2 = Akzent. */
    public static int keystrokesStyle = 0;
    /** Mausboxen (LMB/RMB mit CPS) im Keystrokes-Overlay anzeigen. */
    public static boolean keystrokesShowMouse = true;
    /** Leertaste im Keystrokes-Overlay anzeigen. */
    public static boolean keystrokesShowSpace = true;
    /** Horizontale Position des Keystrokes-Overlays (0..1). */
    public static float keystrokesX = 1.0f;
    /** Vertikale Position des Keystrokes-Overlays (0..1). */
    public static float keystrokesY = 0.0f;

    /** HUD-Textfarbe mit garantiertem Alpha (verhindert unsichtbaren Text). */
    public static int hudColor() {
        return (hudText >>> 24) == 0 ? (hudText | 0xFF000000) : hudText;
    }

    // HUD-Position ist fest (oben links unter dem Logo) — keine Verschiebung.
    // Reihenfolge: XYZ, IGN, FPS, IP, TPS, Software, Ping, Memory, Uhr, Online.
    public static boolean[] hudLines = { true, true, true, true, true, true, true, true, true, true };

    // ── Chat-Logger ──────────────────────────────────────────────────────────
    /** Ausgabe-Quelle aktiv: 0 = Alle, 1 = Nur Chat, 2 = Nur System. */
    public static int chatFilter = 0;
    public static boolean chatTimestamps = true;

    // ── Login ────────────────────────────────────────────────────────────────
    /** Bereits angemeldeter Name (bleibt erhalten), leer = nicht merken. */
    public static String savedUser = "";
    /** Speichern des Logins merken. */
    public static boolean saveLogin = true;

    private static Path file;

    public static Path file() {
        if (file == null) {
            file = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("ccraft-design.properties");
        }
        return file;
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(f)) { p.load(in); }

            // Jede Zuweisung einzeln: ein kaputter Wert (z. B. "accent=xyz") darf
            // nicht den Rest der Datei verschlucken. Vorher lag alles in einem
            // try-Block, sodass ein Fehler alle folgenden Felder auf Default
            // liess und der naechste save() sie so zurueckschrieb.
            dark = readBool(p, "dark", dark);
            accent = readInt(p, "accent", accent);
            preset = p.getProperty("preset", preset);
            fullbright = readBool(p, "fullbright", fullbright);

            String savedFont = p.getProperty("fontChoice");
            if (savedFont != null) {
                fontChoice = clamp(readInt(p, "fontChoice", fontChoice), 0, 4);
            } else {
                fontChoice = readBool(p, "customFont", false) ? 1 : 0;
            }

            hudStyle = clamp(readInt(p, "hudStyle", hudStyle), 0, HUD_STYLE_COUNT - 1);
            logoChoice = clamp(readInt(p, "logoChoice", logoChoice), 0, LOGO_COUNT - 1);
            hudText = readInt(p, "hudText", hudText);
            if ((hudText >>> 24) == 0) hudText |= 0xFF000000; // alte Configs ohne Alpha reparieren
            hudShadow = readBool(p, "hudShadow", hudShadow);
            keystrokes = readBool(p, "keystrokes", keystrokes);
            keystrokesSize = readInt(p, "keystrokesSize", keystrokesSize);
            keystrokesStyle = clamp(readInt(p, "keystrokesStyle", keystrokesStyle), 0, 2);
            keystrokesShowMouse = readBool(p, "keystrokesShowMouse", keystrokesShowMouse);
            keystrokesShowSpace = readBool(p, "keystrokesShowSpace", keystrokesShowSpace);
            keystrokesX = clampPosition(readFloat(p, "keystrokesX", keystrokesX));
            keystrokesY = clampPosition(readFloat(p, "keystrokesY", keystrokesY));
            chatFilter = readInt(p, "chatFilter", chatFilter);
            chatTimestamps = readBool(p, "chatTimestamps", chatTimestamps);
            savedUser = p.getProperty("savedUser", savedUser);
            saveLogin = readBool(p, "saveLogin", saveLogin);

            // Erst zum Schluss: hudLines ersetzt das ganze Array, ein Fehler darf
            // die bereits gelesenen Felder nicht mehr betreffen.
            String lines = p.getProperty("hudLines");
            if (lines != null) {
                String[] parts = lines.split(",");
                for (int i = 0; i < hudLines.length && i < parts.length; i++) {
                    hudLines[i] = parts[i].trim().equals("1");
                }
            }
        } catch (Exception e) {
            CCraftMod.LOGGER.warn("CCraft-Design-Config konnte nicht gelesen werden; "
                    + "es gelten die Standardwerte.", e);
        }
    }

    private static boolean readBool(Properties p, String key, boolean def) {
        String value = p.getProperty(key);
        return value == null ? def : Boolean.parseBoolean(value.trim());
    }

    private static int readInt(Properties p, String key, int def) {
        String value = p.getProperty(key);
        if (value == null) return def;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            CCraftMod.LOGGER.warn("Ungueltiger Wert fuer '{}': '{}' — Standard {} wird benutzt.",
                    key, value, def);
            return def;
        }
    }

    private static float readFloat(Properties p, String key, float def) {
        String value = p.getProperty(key);
        if (value == null) return def;
        try {
            float parsed = Float.parseFloat(value.trim());
            // "NaN" und "Infinity" parsen erfolgreich, sind aber als Position unbrauchbar.
            return Float.isFinite(parsed) ? parsed : def;
        } catch (NumberFormatException e) {
            CCraftMod.LOGGER.warn("Ungueltiger Wert fuer '{}': '{}' — Standard {} wird benutzt.",
                    key, value, def);
            return def;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Properties p = new Properties();
            p.setProperty("dark", String.valueOf(dark));
            p.setProperty("accent", String.valueOf(accent));
            p.setProperty("preset", preset);
            p.setProperty("fullbright", String.valueOf(fullbright));
            p.setProperty("fontChoice", String.valueOf(fontChoice));
            p.setProperty("hudStyle", String.valueOf(hudStyle));
            p.setProperty("logoChoice", String.valueOf(logoChoice));
            p.setProperty("hudText", String.valueOf(hudText));
            p.setProperty("hudShadow", String.valueOf(hudShadow));
            p.setProperty("keystrokes", String.valueOf(keystrokes));
            p.setProperty("keystrokesSize", String.valueOf(keystrokesSize));
            p.setProperty("keystrokesStyle", String.valueOf(keystrokesStyle));
            p.setProperty("keystrokesShowMouse", String.valueOf(keystrokesShowMouse));
            p.setProperty("keystrokesShowSpace", String.valueOf(keystrokesShowSpace));
            p.setProperty("keystrokesX", String.valueOf(keystrokesX));
            p.setProperty("keystrokesY", String.valueOf(keystrokesY));
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < hudLines.length; i++) {
                if (i > 0) lines.append(',');
                lines.append(hudLines[i] ? '1' : '0');
            }
            p.setProperty("hudLines", lines.toString());
            p.setProperty("chatFilter", String.valueOf(chatFilter));
            p.setProperty("chatTimestamps", String.valueOf(chatTimestamps));
            p.setProperty("savedUser", savedUser);
            p.setProperty("saveLogin", String.valueOf(saveLogin));
            try (OutputStream out = Files.newOutputStream(f)) { p.store(out, "CCraft Design"); }
        } catch (IOException e) {
            // Sichtbar machen: eine stumm fehlschlagende Speicherung laesst den
            // Spieler glauben, seine Einstellungen seien gesichert.
            CCraftMod.LOGGER.warn("CCraft-Design-Config konnte nicht gespeichert werden.", e);
        }
    }

    private static float clampPosition(float position) {
        return Math.max(0.0f, Math.min(1.0f, position));
    }
}
