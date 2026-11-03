package de.consolecrafter.ccraft.util;

/**
 * Fertige Design-Presets. Ein Preset setzt einen Namen und eine Akzentfarbe;
 * die Grundfarben (dunkel/hell) kommen aus {@link CraftConfig#dark}.
 */
public final class Themes {

    private Themes() {}

    public record Preset(String name, int accent) {}

    public static final Preset[] PRESETS = {
            new Preset("CCraft",   0xFF4D4D),
            new Preset("Karmesin", 0xD7263D),
            new Preset("Ozean",    0x3FA7D6),
            new Preset("Smaragd",  0x2ECC71),
            new Preset("Amethyst", 0x9B59B6),
            new Preset("Gold",     0xF1C40F),
            new Preset("Sunset",   0xFF7F50),
            new Preset("Monochrom",0xDDDDDD),
    };

    public static void apply(Preset preset) {
        CraftConfig.accent = 0xFF000000 | preset.accent();
        CraftConfig.preset = preset.name();
        CraftConfig.save();
    }
}
