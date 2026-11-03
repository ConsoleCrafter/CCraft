package de.consolecrafter.ccraft.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

public final class ModInfo {

    public static final String NAME = "CCraft";

    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer("ccraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");

    private ModInfo() {}

    public static String version() {
        return VERSION;
    }

    public static String display() {
        return "c" + VERSION;
    }

    public static String minecraftVersion() {
        return SharedConstants.getCurrentVersion().name();
    }
}
