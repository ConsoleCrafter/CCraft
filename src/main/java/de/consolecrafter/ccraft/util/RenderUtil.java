package de.consolecrafter.ccraft.util;

import net.minecraft.client.gui.DrawContext;

public final class RenderUtil {
    private RenderUtil() {}

    /**
     * Einheitlicher Screen-Hintergrund: dunkler Rot-Schwarz-Verlauf
     * mit dezenten Vignette-Bändern oben und unten.
     */
    public static void drawBackground(DrawContext context, int width, int height) {
        Theme.background(context, width, height);
    }
}
