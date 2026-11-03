package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.feature.ResourcePacks;
import de.consolecrafter.ccraft.gui.game.BlockBlastScreen;
import de.consolecrafter.ccraft.gui.game.DoodleJumpScreen;
import de.consolecrafter.ccraft.gui.game.FlappyBirdScreen;
import de.consolecrafter.ccraft.gui.game.PongScreen;
import de.consolecrafter.ccraft.gui.game.SnakeScreen;
import de.consolecrafter.ccraft.gui.game.TetrisScreen;
import de.consolecrafter.ccraft.gui.game.TicTacToeScreen;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class SettingsScreen extends CCraftScreen {

    private static final int BUTTON_W = 170;
    private static final int COL_GAP = 14;
    private static final int ROW_GAP = 8;
    private static final int TOP = 44;
    private static final int HEADER_H = 24;

    private record Item(String label, Runnable action) {}

    private record Group(String header, int color, List<Item> items) {}

    private record HeaderLabel(String text, int color, int y) {}

    private final List<HeaderLabel> headers = new ArrayList<>();
    private int scroll;
    private int maxScroll;

    public SettingsScreen(Screen parent) {
        super("CCraft Einstellungen", parent);
    }

    @Override
    protected void init() {
        int totalW = Math.min(560, width - 40);
        int cols = Math.max(1, (totalW + COL_GAP) / (BUTTON_W + COL_GAP));
        int gridX = width / 2 - (cols * BUTTON_W + (cols - 1) * COL_GAP) / 2;
        int bottom = height - 42;
        int y = TOP - scroll;

        headers.clear();
        for (Group group : groups()) {
            headers.add(new HeaderLabel(group.header(), group.color(), y + HEADER_H - 12));
            y += HEADER_H;
            int col = 0;
            for (Item item : group.items()) {
                if (col == cols) {
                    col = 0;
                    y += BUTTON_HEIGHT + ROW_GAP;
                }
                Button b = button(item.label(), gridX + col * (BUTTON_W + COL_GAP), y, BUTTON_W, BUTTON_HEIGHT, item.action());
                b.visible = y + BUTTON_HEIGHT >= 36 && y <= bottom;
                col++;
            }
            if (col != 0) y += BUTTON_HEIGHT + ROW_GAP;
        }
        maxScroll = Math.max(0, y + scroll - bottom);
        scroll = Math.min(scroll, maxScroll);

        backButton(width / 2 - 75, height - 30, 150);
    }

    private List<Group> groups() {
        List<Item> client = new ArrayList<>();
        client.add(new Item("Module", () -> open(MenuScreen.modules())));
        client.add(new Item("Darstellung", () -> open(new AppearanceScreen(this))));
        client.add(new Item("Schriftart: " + ResourcePacks.fontName(ClientConfig.get().fontChoice), () -> {
            ResourcePacks.cycleFont();
            rebuildWidgets();
        }));

        List<Item> games = new ArrayList<>();
        games.add(new Item("Tetris", () -> open(new TetrisScreen(this))));
        games.add(new Item("Snake", () -> open(new SnakeScreen(this))));
        games.add(new Item("Flappy Bird", () -> open(new FlappyBirdScreen(this))));
        games.add(new Item("Doodle Jump", () -> open(new DoodleJumpScreen(this))));
        games.add(new Item("Block Blast", () -> open(new BlockBlastScreen(this))));
        games.add(new Item("Tic Tac Toe", () -> open(new TicTacToeScreen(this))));
        games.add(new Item("Pong", () -> open(new PongScreen(this))));

        List<Item> tools = new ArrayList<>();
        tools.add(new Item("Chat Logger", () -> open(new ChatLoggerScreen(this))));
        tools.add(new Item("Screenshots", () -> open(new ScreenshotScreen(this))));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            tools.add(new Item("Mod Menu", () -> openReflective("com.terraformersmc.modmenu.gui.ModsScreen")));
        }
        if (FabricLoader.getInstance().isModLoaded("ias")) {
            tools.add(new Item("Account Switcher", () -> openReflective("ru.vidtu.ias.screen.AccountScreen")));
        }
        tools.add(new Item("Info", () -> open(new InfoScreen(this))));

        return List.of(
                new Group("Client", Theme.ACCENT, client),
                new Group("Spiele", Theme.SUCCESS, games),
                new Group("Werkzeuge", Theme.TEXT_DIM, tools));
    }

    private void openReflective(String className) {
        try {
            open((Screen) Class.forName(className).getConstructor(Screen.class).newInstance(this));
        } catch (ReflectiveOperationException | ClassCastException e) {
            CCraftMod.LOGGER.warn("Screen {} could not be opened", className, e);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * 16)));
        rebuildWidgets();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Theme.header(g, font, "CCraft Einstellungen", width / 2);
        for (HeaderLabel header : headers) {
            if (header.y() >= 32 && header.y() <= height - 42) g.centeredText(font, header.text(), width / 2, header.y(), header.color());
        }
    }
}
