package de.consolecrafter.ccraft.util;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Zentrale Versionsangabe.
 *
 * <p>Die Version wird zur Laufzeit aus den Mod-Metadaten gelesen, die Loom aus
 * {@code gradle.properties} einsetzt. Dadurch kann sie nicht mehr von der
 * Anzeige abweichen — vorher stand im HUD und in InfoScreen eine fest
 * eingetragene Zahl, die beim Versionssprung vergessen wurde.</p>
 */
public final class ModInfo {

    private ModInfo() {}

    public static final String NAME = "CCraft";

    private static String version;

    /** Reine Version ohne Prefix, z. B. {@code "0.5"}. */
    public static String version() {
        if (version == null) {
            version = FabricLoader.getInstance()
                    .getModContainer("ccraft")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("dev");
        }
        return version;
    }

    /** Anzeigeform mit Client-Prefix, z. B. {@code "c0.5"}. */
    public static String display() {
        return "c" + version();
    }
}
