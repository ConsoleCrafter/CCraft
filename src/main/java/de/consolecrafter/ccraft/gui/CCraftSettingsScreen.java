package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Übersicht aller CCraft-Funktionen und Spiele.
 * Scrollbares Grid, damit beliebig viele Einträge hineinpassen.
 */
public class CCraftSettingsScreen extends Screen {

    private static final int BTN_W = 170;
    private static final int BTN_H = 20;
    private static final int COL_GAP = 14;
    private static final int ROW_GAP = 8;
    private static final int TOP = 44;
    private static final int HEADER_H = 24;

    private final Screen parent;

    private record HeaderDraw(String text, int color, int y) {}
    private final java.util.List<HeaderDraw> headerDraws = new java.util.ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public CCraftSettingsScreen(Screen parent) {
        super(Text.literal("CCraft Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.width / 2;
        int totalW = Math.min(560, this.width - 40);
        int cols = Math.max(1, (totalW + COL_GAP) / (BTN_W + COL_GAP));
        int gridW = cols * BTN_W + (cols - 1) * COL_GAP;
        int gridX = cx - gridW / 2;

        int contentBottom = this.height - 42;
        int y = TOP - scrollOffset;

        headerDraws.clear();
        for (Group group : groups()) {
            headerDraws.add(new HeaderDraw(group.header(), group.color(), y + HEADER_H - 12));
            y += HEADER_H;
            int col = 0;
            for (Item item : group.items()) {
                if (col >= cols) { col = 0; y += BTN_H + ROW_GAP; }
                int bx = gridX + col * (BTN_W + COL_GAP);
                ButtonWidget button = ButtonWidget.builder(Text.literal(item.label()), b -> item.action().run())
                        .position(bx, y).size(BTN_W, BTN_H).build();
                if (y + BTN_H < 36 || y > contentBottom) button.visible = false;
                addDrawableChild(button);
                col++;
            }
            if (col != 0) y += BTN_H + ROW_GAP;
        }

        maxScroll = Math.max(0, (y + scrollOffset) - contentBottom);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent))
                .position(cx - 75, this.height - 30).size(150, BTN_H).build());
    }

    private record Item(String label, Runnable action) {}
    private record Group(String header, int color, java.util.List<Item> items) {}

    /**
     * Inhalt des Einstellungsmenues.
     *
     * <p>Die Gruppen sind nach Aufgabe sortiert: erst was den Client veraendert,
     * dann die Spiele, zuletzt Werkzeuge. Theme und HUD liegen zusammen unter
     * "Darstellung", weil sie dasselbe Thema behandeln.</p>
     */
    private java.util.List<Group> groups() {
        java.util.List<Group> groups = new java.util.ArrayList<>();

        java.util.List<Item> client = new java.util.ArrayList<>();
        client.add(new Item("Cheats", () -> open(CommandMenuScreen.onCheats())));
        client.add(new Item("Darstellung", () -> open(new AppearanceScreen(this))));
        client.add(new Item("Schriftart: " + CCraftMod.fontName(CraftConfig.fontChoice), () -> {
            CCraftMod.setFontChoice((CraftConfig.fontChoice + 1) % 5);
            this.clearAndInit();
        }));
        groups.add(new Group("Client", Theme.ACCENT, client));

        java.util.List<Item> games = new java.util.ArrayList<>();
        games.add(new Item("Tetris", () -> open(new TetrisScreen(this))));
        games.add(new Item("Snake", () -> open(new SnakeScreen(this))));
        games.add(new Item("Flappy Bird", () -> open(new FlappyBirdScreen(this))));
        games.add(new Item("Doodle Jump", () -> open(new DoodleJumpScreen(this))));
        games.add(new Item("Block Blast", () -> open(new BlockBlastScreen(this))));
        games.add(new Item("Tic Tac Toe", () -> open(new TicTacToeScreen(this))));
        games.add(new Item("Pong", () -> open(new PongScreen(this))));
        groups.add(new Group("Spiele", Theme.SUCCESS, games));

        java.util.List<Item> utility = new java.util.ArrayList<>();
        utility.add(new Item("Chat Logger", () -> open(new ChatLoggerScreen(this))));
        utility.add(new Item("Screenshots", () -> open(new ScreenshotScreen(this))));
        utility.add(new Item("Mod Menu", this::openModMenu));
        utility.add(new Item("Account Switcher", this::openAccountSwitcher));
        utility.add(new Item("Info", () -> open(new InfoScreen(this))));
        groups.add(new Group("Werkzeuge", Theme.TEXT_DIM, utility));

        return groups;
    }

    private void open(Screen screen) { client.setScreen(screen); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scrollOffset -= (int) (verticalAmount * 16);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            this.clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Theme.header(context, textRenderer, "CCraft Einstellungen", this.width / 2);
        for (HeaderDraw header : headerDraws) {
            if (header.y() < 32 || header.y() > this.height - 42) continue;
            context.drawCenteredTextWithShadow(textRenderer, header.text(), this.width / 2, header.y(), header.color());
        }
    }

    @Override
    public void close() { client.setScreen(parent); }

    private void openModMenu() {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            try {
                Class<?> cls = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                client.setScreen((Screen) cls.getConstructor(Screen.class).newInstance(this));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void openAccountSwitcher() {
        if (FabricLoader.getInstance().isModLoaded("ias")) {
            try {
                Class<?> cls = Class.forName("ru.vidtu.ias.screen.AccountScreen");
                client.setScreen((Screen) cls.getConstructor(Screen.class).newInstance(this));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
