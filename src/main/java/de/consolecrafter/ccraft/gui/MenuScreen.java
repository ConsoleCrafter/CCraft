package de.consolecrafter.ccraft.gui;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.command.CommandManager;
import de.consolecrafter.ccraft.gui.game.BlockBlastScreen;
import de.consolecrafter.ccraft.gui.game.DoodleJumpScreen;
import de.consolecrafter.ccraft.gui.game.FlappyBirdScreen;
import de.consolecrafter.ccraft.gui.game.PongScreen;
import de.consolecrafter.ccraft.gui.game.SnakeScreen;
import de.consolecrafter.ccraft.gui.game.TetrisScreen;
import de.consolecrafter.ccraft.gui.game.TicTacToeScreen;
import de.consolecrafter.ccraft.hud.Trains;
import de.consolecrafter.ccraft.input.Keybinds;
import de.consolecrafter.ccraft.module.Module;
import de.consolecrafter.ccraft.module.ModuleManager;
import de.consolecrafter.ccraft.module.Option;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MenuScreen extends CCraftScreen {

    private enum Category { UTILITY("Utility"), GAMES("Games"), FUNNY("Funny"), MODULES("Module"), SETTINGS("Settings");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private static final int SIDEBAR_W = 140;
    private static final int CONTENT_GAP = 24;
    private static final int ROW_H = 24;
    private static final int ROW_GAP = 6;
    private static final int TOP = 88;
    private static final int SMALL_W = 20;

    private static final Set<String> EXPANDED = new HashSet<>();
    private static Category category = Category.UTILITY;
    private static int scroll;
    private static String query = "";

    private record Row(int height, Placer placer) {}

    private record HoverRow(Module module, int y) {}

    private interface Placer {
        void place(int x, int width, int y);
    }

    private final List<HoverRow> hoverRows = new ArrayList<>();
    private Module capturing;
    private int maxScroll;
    private int rowCount;
    private String notice = "";
    private int noticeTicks;

    public MenuScreen() {
        super("CCraft", null);
    }

    public static MenuScreen modules() {
        category = Category.MODULES;
        scroll = 0;
        return new MenuScreen();
    }

    private int totalWidth() {
        return Math.min(560, width - 40);
    }

    private int containerX() {
        return width / 2 - totalWidth() / 2;
    }

    private int contentX() {
        return containerX() + SIDEBAR_W + CONTENT_GAP;
    }

    private int contentWidth() {
        return totalWidth() - SIDEBAR_W - CONTENT_GAP;
    }

    private int contentBottom() {
        return height - 34;
    }

    @Override
    public void tick() {
        if (noticeTicks > 0 && --noticeTicks == 0) notice = "";
    }

    @Override
    protected void init() {
        int y = TOP;
        for (Category c : Category.values()) {
            button(c == category ? "§l" + c.label : c.label, containerX(), y, SIDEBAR_W, ROW_H, () -> {
                category = c;
                scroll = 0;
                capturing = null;
                rebuildWidgets();
            });
            y += ROW_H + ROW_GAP;
        }

        if (category == Category.MODULES) {
            EditBox search = new EditBox(font, contentX(), 62, contentWidth(), 18, Component.literal("Suche"));
            search.setHint(Component.literal("§7Modul suchen…"));
            search.setValue(query);
            search.setResponder(value -> {
                if (value.equals(query)) return;
                query = value;
                scroll = 0;
                rebuildWidgets();
            });
            addRenderableWidget(search);
            setInitialFocus(search);
        }

        List<Row> rows = rows();
        rowCount = rows.size();
        int total = 0;
        for (Row row : rows) total += row.height() + ROW_GAP;
        maxScroll = Math.max(0, total - (contentBottom() - TOP));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        hoverRows.clear();
        y = TOP - scroll;
        for (Row row : rows) {
            if (y + row.height() >= TOP - 1 && y <= contentBottom() + 1) row.placer().place(contentX(), contentWidth(), y);
            y += row.height() + ROW_GAP;
        }
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        switch (category) {
            case GAMES -> {
                rows.add(link("Tetris", () -> open(new TetrisScreen(this))));
                rows.add(link("Snake", () -> open(new SnakeScreen(this))));
                rows.add(link("Flappy Bird", () -> open(new FlappyBirdScreen(this))));
                rows.add(link("Doodle Jump", () -> open(new DoodleJumpScreen(this))));
                rows.add(link("Block Blast", () -> open(new BlockBlastScreen(this))));
                rows.add(link("Tic Tac Toe", () -> open(new TicTacToeScreen(this))));
                rows.add(link("Pong", () -> open(new PongScreen(this))));
            }
            case FUNNY -> {
                for (Trains.Kind kind : Trains.Kind.values()) {
                    rows.add(link(kind.displayName(), () -> runCommand(kind.command())));
                }
            }
            case MODULES -> moduleRows(rows);
            case SETTINGS -> {
                rows.add(link("Darstellung", () -> open(new AppearanceScreen(this))));
                rows.add(link("Screenshots", () -> open(new ScreenshotScreen(this))));
                rows.add(link("CCraft Einstellungen", () -> open(new SettingsScreen(this))));
                rows.add(link("Info", () -> open(new InfoScreen(this))));
            }
            case UTILITY -> rows.add(link("Chat Logger", () -> open(new ChatLoggerScreen(this))));
        }
        return rows;
    }

    private void moduleRows(List<Row> rows) {
        for (Module module : ModuleManager.search(query)) {
            boolean expanded = EXPANDED.contains(module.id);
            rows.add(new Row(ROW_H, (x, w, y) -> {
                hoverRows.add(new HoverRow(module, y));
                int keyW = Math.min(110, w / 4);
                int toggleW = w - SMALL_W * 3 - keyW - 16;
                int cx = x;

                button(expanded ? "▾" : "▸", cx, y, SMALL_W, ROW_H, () -> {
                    if (!EXPANDED.remove(module.id)) EXPANDED.add(module.id);
                    rebuildWidgets();
                });
                cx += SMALL_W + 4;

                String status = module.statusText();
                String label = module.name + ": " + (module.isEnabled() ? "§aAN" : "§cAUS") + (status.isEmpty() ? "" : " §8(" + status + "§8)");
                button(label, cx, y, toggleW, ROW_H, () -> {
                    module.toggle();
                    rebuildWidgets();
                });
                cx += toggleW + 4;

                String keyLabel = capturing == module ? "§eTaste drücken…"
                        : ModuleManager.conflicts(module) ? "§c! " + keyName(module)
                        : "§7Taste: §f" + keyName(module);
                button(keyLabel, cx, y, keyW, ROW_H, () -> {
                    capturing = capturing == module ? null : module;
                    rebuildWidgets();
                });
                cx += keyW + 4;

                button("§7↺", cx, y, SMALL_W, ROW_H, () -> {
                    module.setKey(null);
                    rebuildWidgets();
                });
                cx += SMALL_W + 4;

                button("§7⟲", cx, y, SMALL_W, ROW_H, () -> {
                    module.resetOptions();
                    rebuildWidgets();
                });
            }));

            if (!expanded) continue;
            for (Option option : module.options()) rows.add(new Row(ROW_H, (x, w, y) -> optionRow(option, x + 12, w - 12, y)));
        }
    }

    private void optionRow(Option option, int x, int w, int y) {
        String text = (option.isDefault() ? "§8" : "§6• ") + option.label + ": §f" + option.display();
        switch (option.kind) {
            case NUMBER -> {
                button("−", x, y, SMALL_W, ROW_H, () -> {
                    option.step(-1);
                    rebuildWidgets();
                });
                button(text, x + SMALL_W + 4, y, w - 2 * SMALL_W - 8, ROW_H, () -> {
                    option.step(1);
                    rebuildWidgets();
                });
                button("+", x + w - SMALL_W, y, SMALL_W, ROW_H, () -> {
                    option.step(1);
                    rebuildWidgets();
                });
            }
            case TEXT -> {
                EditBox field = new EditBox(font, x, y, w, ROW_H, Component.literal(option.label));
                field.setMaxLength(48);
                field.setValue(option.asText());
                field.setResponder(option::set);
                addRenderableWidget(field);
            }
            default -> button(text, x, y, w, ROW_H, () -> {
                option.cycle();
                rebuildWidgets();
            });
        }
    }

    private String keyName(Module module) {
        return module.hasKey() ? module.key().getDisplayName().getString() : "—";
    }

    private Row link(String label, Runnable action) {
        return new Row(ROW_H, (x, w, y) -> button(label, x, y, w, ROW_H, action));
    }

    private void runCommand(String name) {
        if (minecraft.player == null) return;
        CommandManager.run(minecraft.player, CommandManager.PREFIX + name);
        notice = "§6" + name + " §7gestartet!";
        noticeTicks = 60;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * (ROW_H + ROW_GAP))));
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing != null) {
            if (event.key() != InputConstants.KEY_ESCAPE) capturing.setKey(InputConstants.getKey(event));
            capturing = null;
            rebuildWidgets();
            return true;
        }
        if (Keybinds.menu.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return capturing != null || super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (capturing != null) {
            capturing = null;
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fillGradient(0, 0, width, height, 0x50000000, 0x80000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = containerX();
        g.text(font, "§lCCraft", x, 18, Theme.ACCENT, true);
        g.text(font, "§7Client Menu", x + font.width("CCraft") + 8, 18, Theme.TEXT_DIM, true);
        Theme.divider(g, x, 36, totalWidth());
        g.text(font, category.label, contentX(), 46, Theme.ACCENT, true);

        if (rowCount == 0) {
            String empty = category == Category.MODULES ? "§7Kein Modul passt zur Suche." : "§7Noch keine Einträge.";
            g.text(font, empty, contentX(), TOP + 8, Theme.TEXT_FAINT, true);
        }

        if (category == Category.MODULES) {
            int descX = contentX() + font.width(category.label) + 12;
            for (HoverRow row : hoverRows) {
                if (mouseY >= row.y() && mouseY < row.y() + ROW_H) {
                    g.text(font, truncate("§7" + row.module().description, contentX() + contentWidth() - descX), descX, 48, Theme.TEXT_FAINT, true);
                    break;
                }
            }
        }

        if (!notice.isEmpty()) g.centeredText(font, notice, width / 2, height - 34, Theme.ACCENT);

        String hint = capturing != null ? "Taste drücken für " + capturing.name + " — ESC bricht ab"
                : category == Category.MODULES ? "▸ = Optionen · ↺ Taste löschen · ⟲ Optionen zurücksetzen"
                : Keybinds.menu.getTranslatedKeyMessage().getString() + " = schließen";
        Theme.footer(g, font, hint, width / 2, height - 20);
    }

    private String truncate(String text, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        int limit = maxWidth - font.width("…");
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(out.toString() + c) > limit) break;
            out.append(c);
        }
        return out + "…";
    }
}
