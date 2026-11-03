package de.consolecrafter.ccraft.module.impl;

import de.consolecrafter.ccraft.module.Module;
import de.consolecrafter.ccraft.module.Option;
import net.minecraft.client.Minecraft;

public final class NameProtect extends Module {

    private final Option alias = text("alias", "Ersatzname", "Spieler");

    public NameProtect() {
        super("nameprotect", "NameProtect", "Ersetzt den eigenen Namen überall in der Anzeige (Anti Stream-Snipe).");
    }

    public String replace(String text) {
        if (!isEnabled() || text == null || text.isEmpty()) return text;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getUser() == null) return text;
        String name = minecraft.getUser().getName();
        String replacement = alias.asText();
        if (name == null || name.isEmpty() || replacement.isEmpty() || name.equals(replacement)) return text;
        return text.contains(name) ? text.replace(name, replacement) : text;
    }
}
