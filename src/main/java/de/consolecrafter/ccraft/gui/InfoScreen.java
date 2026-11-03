package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.ModInfo;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class InfoScreen extends Screen {

    private final Screen parent;

    public InfoScreen(Screen parent) {
        super(Text.literal("CCraft Info"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.width / 2;
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Zurück"),
            button -> client.setScreen(parent)
        ).position(cx - 75, this.height - 30).size(150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        Theme.header(context, textRenderer, "Info", cx);

        int panelW = 260;
        int panelH = 118;
        int panelX = cx - panelW / 2;
        int panelY = this.height / 2 - panelH / 2 + 8;

        Theme.panel(context, panelX, panelY, panelW, panelH);
        Theme.divider(context, panelX + 14, panelY + 14, panelW - 28);

        int y = panelY + 24;
        int lineH = 14;

        info(context, "Version",   ModInfo.version(),    y,          Theme.TEXT);   y += lineH;
        info(context, "Autor",     "ConsoleCrafter",      y,          Theme.ACCENT); y += lineH;
        info(context, "Mitwirken", "MelonVR53",           y,          Theme.SUCCESS); y += lineH;
        info(context, "Client",    "Minecraft 1.21.10",   y,          Theme.TEXT_DIM); y += lineH;
        info(context, "Mod",       ModInfo.NAME,          y,          Theme.BRAND);
    }

    private void info(DrawContext context, String label, String value, int y, int valueColor) {
        int cx = this.width / 2;
        String line = label + ":  ";
        int labelW = textRenderer.getWidth(line);
        int valueW = textRenderer.getWidth(value);
        int totalW = labelW + valueW;
        int startX = cx - totalW / 2;
        context.drawText(textRenderer, line, startX, y, Theme.TEXT_DIM, true);
        context.drawText(textRenderer, value, startX + labelW, y, valueColor, true);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
