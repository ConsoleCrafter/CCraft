package de.consolecrafter.ccraft.util;

import de.consolecrafter.ccraft.config.ClientConfig;

public final class Themes {

    public record Preset(String name, int accent) {}

    public static final Preset[] PRESETS = {
            new Preset("CCraft", 0xFF4D4D),
            new Preset("Karmesin", 0xD7263D),
            new Preset("Ozean", 0x3FA7D6),
            new Preset("Smaragd", 0x2ECC71),
            new Preset("Amethyst", 0x9B59B6),
            new Preset("Gold", 0xF1C40F),
            new Preset("Sunset", 0xFF7F50),
            new Preset("Monochrom", 0xDDDDDD),
    };

    private Themes() {}

    public static void apply(Preset preset) {
        ClientConfig config = ClientConfig.get();
        config.accent = preset.accent();
        config.preset = preset.name();
        ClientConfig.save();
        Theme.apply();
    }

    public static void applyCustom(int rgb) {
        ClientConfig config = ClientConfig.get();
        config.accent = rgb & 0xFFFFFF;
        config.preset = "Eigene";
        ClientConfig.save();
        Theme.apply();
    }
}
