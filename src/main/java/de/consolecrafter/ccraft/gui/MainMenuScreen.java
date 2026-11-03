package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.hud.Logos;
import de.consolecrafter.ccraft.util.ModInfo;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;

public final class MainMenuScreen extends Screen {

    private static final int LOGO_WIDTH = 150;
    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 20;
    private static final int GAP = 6;

    public MainMenuScreen() {
        super(Component.literal("CCraft"));
    }

    @Override
    protected void init() {
        int x = width / 2 - BUTTON_W / 2;
        int logoBottom = 20 + Logos.heightFor(LOGO_WIDTH);
        int total = 5 * BUTTON_H + 4 * GAP;
        int y = Math.max(logoBottom + 10, logoBottom + (height - logoBottom - 40 - total) / 2);

        add(x, y, 0, "Singleplayer §7(" + worldCount() + " Welten)", () -> minecraft.gui.setScreen(new SelectWorldScreen(this)));
        add(x, y, 1, "Multiplayer §7(" + serverCount() + " Server)", () -> minecraft.gui.setScreen(
                minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
        add(x, y, 2, "Optionen", () -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options)));
        add(x, y, 3, "CCraft", () -> minecraft.gui.setScreen(new SettingsScreen(this)));
        add(x, y, 4, "Spiel beenden", minecraft::stop);
    }

    private void add(int x, int y, int index, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y + index * (BUTTON_H + GAP), BUTTON_W, BUTTON_H)
                .build());
    }

    private int worldCount() {
        try {
            return minecraft.getLevelSource().findLevelCandidates().levels().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int serverCount() {
        ServerList list = new ServerList(minecraft);
        list.load();
        return list.size();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int logoH = Logos.draw(g, cx - LOGO_WIDTH / 2, 20, LOGO_WIDTH);
        int lineY = 28 + logoH;
        g.fill(cx - 90, lineY, cx + 90, lineY + 1, Theme.BORDER);
        g.fill(cx - 30, lineY + 1, cx + 30, lineY + 2, Theme.ACCENT);

        g.text(font, "Minecraft " + ModInfo.minecraftVersion(), 4, height - 12, Theme.TEXT_FAINT, true);
        String info = "CCraft " + ModInfo.display() + "  |  " + FabricLoader.getInstance().getAllMods().size() + " Mods geladen";
        g.text(font, info, width - font.width(info) - 4, height - 12, Theme.ACCENT, true);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
