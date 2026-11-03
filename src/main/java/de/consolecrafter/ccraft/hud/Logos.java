package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class Logos {

    public static final String[] NAMES = {"CCraft", "Classic", "Modern", "Cringe", "OG"};

    private static final Identifier[] TEXTURES = {
            id("ccraft_logo"), id("CLASSIC"), id("MODERN"), id("CRINGE"), id("OG")
    };
    private static final int[] WIDTHS = {1024, 1024, 476, 666, 331};
    private static final int[] HEIGHTS = {522, 522, 101, 375, 77};

    private Logos() {}

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(CCraftMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    public static int index() {
        return Math.max(0, Math.min(TEXTURES.length - 1, ClientConfig.get().logoChoice));
    }

    public static int heightFor(int width) {
        int i = index();
        return Math.max(1, Math.round(width * (HEIGHTS[i] / (float) WIDTHS[i])));
    }

    public static int draw(GuiGraphicsExtractor g, int x, int y, int width) {
        int height = heightFor(width);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURES[index()], x, y, 0.0f, 0.0f, width, height, width, height);
        return height;
    }
}
