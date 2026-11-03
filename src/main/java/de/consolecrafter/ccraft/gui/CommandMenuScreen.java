package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.cheats.Option;
import de.consolecrafter.ccraft.commands.CommandHandler;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-Game-Menü (Rechter Shift). Lässt das Spiel im Hintergrund sichtbar,
 * nur leicht abgedunkelt. Kategorien liegen als Sidebar links, der Inhalt
 * der aktiven Kategorie rechts daneben.
 *
 * <p>Die Kategorie {@code Cheats} zeigt alle Cheats direkt in dieser Liste —
 * An/Aus, Tastenbelegung und Optionen. Es gibt bewusst kein Untermenü. Die
 * Optionszeilen eines Cheats sind eingeklappt, solange er nicht aufgeklappt
 * wurde, damit die Liste übersichtlich bleibt.</p>
 */
public class CommandMenuScreen extends Screen {

    private static final String[] CATEGORIES = {"Utility", "Games", "Funny", "Cheats", "Settings"};

    private static final int SIDEBAR_W = 140;
    private static final int CONTENT_GAP = 24;
    private static final int BTN_H = 24;
    private static final int BTN_GAP = 6;
    private static final int TOP = 88;
    private static final int SMALL_W = 20;

    private static String currentCategory = "Utility";
    private static int scrollOffset = 0;

    /** Aufgeklappte Cheats; bleibt über Sitzungen des Menüs erhalten. */
    private static final Set<String> EXPANDED = new HashSet<>();

    private int maxScrollOffset = 0;
    private int contentCount = 0;
    private String notice = "";
    private int noticeTicks = 0;

    /** Cheat, der gerade eine neue Taste erwartet. */
    private Cheat capturing;
    /** Zeilen der Cheat-Übersicht für die Beschreibung beim Überfahren. */
    private final List<CheatRow> cheatRows = new ArrayList<>();
    private TextFieldWidget searchField;
    private String searchQuery = "";

    private record CheatRow(Cheat cheat, int y, int height) {}

    /** Eine Zeile im scrollbaren Inhalt; {@code place} erzeugt ihre Widgets. */
    private record Row(int height, Placer place) {}

    private interface Placer {
        void place(int x, int width, int y);
    }

    public CommandMenuScreen() {
        super(Text.literal("CCraft"));
    }

    /** Öffnet das Menü direkt in der Kategorie {@code Cheats}. */
    public static CommandMenuScreen onCheats() {
        currentCategory = "Cheats";
        scrollOffset = 0;
        return new CommandMenuScreen();
    }

    @Override
    public void tick() {
        super.tick();
        if (noticeTicks > 0 && --noticeTicks == 0) {
            notice = "";
        }
    }

    @Override
    protected void init() {
        super.init();

        int totalW = Math.min(560, this.width - 40);
        int containerX = this.width / 2 - totalW / 2;
        int contentX = containerX + SIDEBAR_W + CONTENT_GAP;
        int contentW = totalW - SIDEBAR_W - CONTENT_GAP;

        addCategoryTabs(containerX);
        addSearchField(contentX, contentW);
        addCategoryContent(containerX, totalW, contentX, contentW);
    }

    /** Kategorien als vertikale Sidebar links. */
    private void addCategoryTabs(int containerX) {
        int y = TOP;
        for (String category : CATEGORIES) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(category),
                    b -> {
                        currentCategory = category;
                        scrollOffset = 0;
                        capturing = null;
                        this.clearAndInit();
                    })
                    .position(containerX, y).size(SIDEBAR_W, BTN_H)
                    .build());
            y += BTN_H + BTN_GAP;
        }
    }

    /** Suchfeld über der Cheat-Liste. */
    private void addSearchField(int contentX, int contentW) {
        if (!"Cheats".equals(currentCategory)) return;
        searchField = new TextFieldWidget(textRenderer, contentX, 62, contentW, 18, Text.literal("Suche"));
        searchField.setPlaceholder(Text.literal("§7Cheat suchen…"));
        searchField.setText(searchQuery);
        searchField.setChangedListener(value -> {
            searchQuery = value;
            scrollOffset = 0;
            this.clearAndInit();
        });
        addDrawableChild(searchField);
        this.setInitialFocus(searchField);
    }

    private void addCategoryContent(int containerX, int totalW, int contentX, int contentW) {
        List<Row> rows = new ArrayList<>();
        switch (currentCategory) {
            case "Games" -> {
                rows.add(link("Tetris", () -> client.setScreen(new TetrisScreen(this))));
                rows.add(link("Snake", () -> client.setScreen(new SnakeScreen(this))));
                rows.add(link("Flappy Bird", () -> client.setScreen(new FlappyBirdScreen(this))));
                rows.add(link("Doodle Jump", () -> client.setScreen(new DoodleJumpScreen(this))));
                rows.add(link("Block Blast", () -> client.setScreen(new BlockBlastScreen(this))));
                rows.add(link("Tic Tac Toe", () -> client.setScreen(new TicTacToeScreen(this))));
                rows.add(link("Pong", () -> client.setScreen(new PongScreen(this))));
            }
            case "Settings" -> {
                rows.add(link("Darstellung", () -> client.setScreen(new AppearanceScreen(this))));
                rows.add(link("Screenshots", () -> client.setScreen(new ScreenshotScreen(this))));
                rows.add(link("CCraft Einstellungen", () -> client.setScreen(new CCraftSettingsScreen(this))));
                rows.add(link("Info", () -> client.setScreen(new InfoScreen(this))));
            }
            case "Funny" -> {
                rows.add(command("SidTrain", "sidtrain"));
                rows.add(command("DevinTrain", "devintrain"));
                rows.add(command("MelonTrain", "melontrain"));
                rows.add(command("DanielTrain", "danieltrain"));
            }
            case "Cheats" -> addCheatRows(rows, contentW);
            default -> rows.add(link("Chat Logger", () -> client.setScreen(new ChatLoggerScreen(this))));
        }

        contentCount = rows.size();

        int totalHeight = 0;
        for (Row row : rows) totalHeight += row.height() + BTN_GAP;
        int visibleHeight = contentBottom() - TOP;
        maxScrollOffset = Math.max(0, totalHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

        cheatRows.clear();
        int y = TOP - scrollOffset;
        for (Row row : rows) {
            if (y + row.height() >= TOP - 1 && y <= contentBottom() + 1) {
                row.place().place(contentX, contentW, y);
            }
            y += row.height() + BTN_GAP;
        }
    }

    /** Alle Cheats mit Schalter, Tastenbelegung und (aufgeklappt) Optionen. */
    private void addCheatRows(List<Row> rows, int contentW) {
        for (Cheat cheat : CheatManager.search(searchQuery)) {
            boolean expanded = EXPANDED.contains(cheat.id);
            boolean conflict = CheatManager.hasKeybindConflict(cheat);

            rows.add(new Row(BTN_H, (x, w, y) -> {
                cheatRows.add(new CheatRow(cheat, y, BTN_H));
                int resetW = SMALL_W;
                int optW = SMALL_W;
                int keyW = Math.min(110, w / 4);
                int arrowW = SMALL_W;
                int gaps = 4 * 4;
                int toggleW = w - arrowW - keyW - resetW - optW - gaps;

                // Auf-/Einklappen
                addDrawableChild(ButtonWidget.builder(
                                Text.literal(expanded ? "▾" : "▸"),
                                b -> toggleExpanded(cheat))
                        .position(x, y).size(arrowW, BTN_H).build());

                // An/Aus
                String status = cheat.statusText();
                String label = cheat.name + ": " + (cheat.isEnabled() ? "§aAN" : "§cAUS");
                if (!status.isEmpty()) label += " §8(" + status + "§8)";
                addDrawableChild(ButtonWidget.builder(
                                Text.literal(label),
                                b -> { cheat.toggle(); this.clearAndInit(); })
                        .position(x + arrowW + 4, y).size(toggleW, BTN_H).build());

                // Taste — bei Konflikt rot
                boolean waiting = capturing == cheat;
                String keyLabel = waiting ? "§eTaste drücken…"
                        : (conflict ? "§c! " + boundKeyName(cheat)
                                    : "§7Taste: §f" + boundKeyName(cheat));
                addDrawableChild(ButtonWidget.builder(
                                Text.literal(keyLabel),
                                b -> { capturing = waiting ? null : cheat; this.clearAndInit(); })
                        .position(x + arrowW + toggleW + 8, y).size(keyW, BTN_H).build());

                // Taste zurücksetzen
                addDrawableChild(ButtonWidget.builder(
                                Text.literal("§7↺"),
                                b -> { CheatManager.resetKeybind(cheat); this.clearAndInit(); })
                        .position(x + arrowW + toggleW + keyW + 12, y).size(resetW, BTN_H).build());

                // Optionen zurücksetzen
                addDrawableChild(ButtonWidget.builder(
                                Text.literal("§7⟲"),
                                b -> { cheat.resetOptions(); this.clearAndInit(); })
                        .position(x + arrowW + toggleW + keyW + resetW + 16, y).size(optW, BTN_H).build());
            }));

            if (!expanded) continue;

            for (Option option : cheat.options()) {
                rows.add(new Row(BTN_H, (x, w, y) -> {
                    int ix = x + 12;
                    int iw = w - 12;
                    String marker = option.isDefault() ? "§8" : "§6• ";
                    if (option.kind == Option.Kind.NUMBER) {
                        addDrawableChild(ButtonWidget.builder(Text.literal("−"),
                                        b -> { option.step(-1); this.clearAndInit(); })
                                .position(ix, y).size(SMALL_W, BTN_H).build());
                        addDrawableChild(ButtonWidget.builder(
                                        Text.literal(marker + option.label + ": §f" + option.display()),
                                        b -> { option.step(1); this.clearAndInit(); })
                                .position(ix + SMALL_W + 4, y).size(iw - 2 * SMALL_W - 8, BTN_H).build());
                        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                                        b -> { option.step(1); this.clearAndInit(); })
                                .position(ix + iw - SMALL_W, y).size(SMALL_W, BTN_H).build());
                    } else if (option.kind == Option.Kind.TEXT) {
                        TextFieldWidget field = new TextFieldWidget(textRenderer,
                                ix, y, iw, BTN_H, Text.literal(option.label));
                        field.setMaxLength(48);
                        field.setText(option.asText());
                        field.setChangedListener(value -> option.set(value));
                        addDrawableChild(field);
                    } else {
                        addDrawableChild(ButtonWidget.builder(
                                        Text.literal(marker + option.label + ": §f" + option.display()),
                                        b -> { option.cycle(); this.clearAndInit(); })
                                .position(ix, y).size(iw, BTN_H).build());
                    }
                }));
            }
        }
    }

    private void toggleExpanded(Cheat cheat) {
        if (!EXPANDED.remove(cheat.id)) EXPANDED.add(cheat.id);
        this.clearAndInit();
    }

    /** Anzeigename der belegten Taste eines Cheats. */
    private String boundKeyName(Cheat cheat) {
        if (cheat.keybind() == null || cheat.keybind().isUnbound()) return "—";
        return cheat.keybind().getBoundKeyLocalizedText().getString();
    }

    private Row link(String label, Runnable action) {
        return new Row(BTN_H, (x, w, y) -> addDrawableChild(ButtonWidget.builder(
                Text.literal(label), b -> action.run())
                .position(x, y).size(w, BTN_H).build()));
    }

    private Row command(String label, String command) {
        return link(label, () -> runCommand(command));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScrollOffset > 0) {
            scrollOffset -= (int) (verticalAmount * (BTN_H + BTN_GAP));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            this.clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        // Belegung: die nächste Taste (oder ESC zum Abbrechen) wird übernommen.
        if (capturing != null) {
            if (keyInput.key() != GLFW.GLFW_KEY_ESCAPE) {
                CheatManager.setKeybind(capturing, InputUtil.fromKeyCode(keyInput));
            }
            capturing = null;
            this.clearAndInit();
            return true;
        }
        // Rechter Shift schließt das Menü, genau wie er es öffnet.
        if (keyInput.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            CCraftMod.closeCommandMenu();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput charInput) {
        // Waehrend des Belegens darf kein Zeichen im Suchfeld landen.
        if (capturing != null) return true;
        return super.charTyped(charInput);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (capturing != null) {
            CheatManager.setKeybind(capturing, InputUtil.Type.MOUSE.createFromCode(click.button()));
            capturing = null;
            this.clearAndInit();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Transluzente Abdunklung statt opakem Verlauf -> das Spiel bleibt sichtbar.
        context.fillGradient(0, 0, this.width, this.height, 0x50000000, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int totalW = Math.min(560, this.width - 40);
        int containerX = this.width / 2 - totalW / 2;

        context.drawText(textRenderer, "§lCCraft", containerX, 18, Theme.ACCENT, true);
        context.drawText(textRenderer, "§7Client Menu",
                containerX + textRenderer.getWidth("CCraft") + 8, 18, Theme.TEXT_DIM, true);
        Theme.divider(context, containerX, 36, totalW);

        int contentX = containerX + SIDEBAR_W + CONTENT_GAP;
        int contentW = totalW - SIDEBAR_W - CONTENT_GAP;
        context.drawText(textRenderer, currentCategory, contentX, 46, Theme.ACCENT, true);

        if (contentCount == 0) {
            String empty = "Cheats".equals(currentCategory)
                    ? "§7Kein Cheat passt zur Suche."
                    : "§7Noch keine Einträge — hier kommen bald neue hin!";
            context.drawText(textRenderer, empty, contentX, TOP + 8, Theme.TEXT_FAINT, true);
        }

        // Beschreibung des Cheats unter dem Mauszeiger — rechts neben dem Titel,
        // damit sie nicht mit der Fußzeile kollidiert.
        if ("Cheats".equals(currentCategory)) {
            int titleEnd = contentX + textRenderer.getWidth(currentCategory) + 12;
            for (CheatRow row : cheatRows) {
                if (mouseY >= row.y() && mouseY < row.y() + row.height()) {
                    String desc = truncate("§7" + row.cheat().description, contentX + contentW - titleEnd);
                    context.drawText(textRenderer, desc, titleEnd, 48, Theme.TEXT_FAINT, true);
                    break;
                }
            }
        }

        if (!notice.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, notice, this.width / 2, this.height - 34, Theme.ACCENT);
        }

        String hint;
        if (capturing != null) {
            hint = "Taste drücken für " + capturing.name + " — ESC bricht ab";
        } else if ("Cheats".equals(currentCategory)) {
            hint = "▸ = Optionen · ↺ Taste · ⟲ Optionen";
        } else {
            hint = "Rechter Shift = schließen";
        }
        Theme.footer(context, textRenderer, hint, this.width / 2, this.height - 20);
    }

    private int contentBottom() {
        return this.height - 34;
    }

    /** Kürzt Text mit Auslassungszeichen, wenn er breiter als {@code maxWidth} ist. */
    private String truncate(String text, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        int limit = maxWidth - textRenderer.getWidth(ellipsis);
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (textRenderer.getWidth(out.toString() + c) > limit) break;
            out.append(c);
        }
        return out + ellipsis;
    }

    private void runCommand(String name) {
        if (client.player == null) return;
        CommandHandler.handleCommand(client.player, "#" + name);
        notice = "§6" + name + " §7gestartet!";
        noticeTicks = 60;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        // Auch ESC muss das Menü-Flag zurücksetzen, sonst braucht der nächste
        // Rechts-Shift-Druck zwei Versuche.
        CCraftMod.closeCommandMenu();
    }
}
