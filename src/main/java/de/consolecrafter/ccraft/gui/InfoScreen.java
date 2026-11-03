package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.ModInfo;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class InfoScreen extends CCraftScreen {

    public InfoScreen(Screen parent) {
        super("CCraft Info", parent);
    }

    @Override
    protected void init() {
        backButton(width / 2 - 75, height - 30, 150);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        Theme.header(g, font, "Info", cx);

        int panelW = 260;
        int panelH = 118;
        int panelX = cx - panelW / 2;
        int panelY = height / 2 - panelH / 2 + 8;
        Theme.panel(g, panelX, panelY, panelW, panelH);
        Theme.divider(g, panelX + 14, panelY + 14, panelW - 28);

        int y = panelY + 24;
        line(g, "Version", ModInfo.version(), y, Theme.TEXT);
        line(g, "Autor", "ConsoleCrafter", y + 14, Theme.ACCENT);
        line(g, "Mitwirken", "MelonVR53", y + 28, Theme.SUCCESS);
        line(g, "Client", "Minecraft " + ModInfo.minecraftVersion(), y + 42, Theme.TEXT_DIM);
        line(g, "Mod", ModInfo.NAME, y + 56, Theme.ACCENT);
    }

    private void line(GuiGraphicsExtractor g, String label, String value, int y, int color) {
        String prefix = label + ":  ";
        int x = width / 2 - (font.width(prefix) + font.width(value)) / 2;
        g.text(font, prefix, x, y, Theme.TEXT_DIM, true);
        g.text(font, value, x + font.width(prefix), y, color, true);
    }
}
