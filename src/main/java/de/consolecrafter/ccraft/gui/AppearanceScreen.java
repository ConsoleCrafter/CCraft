package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.hud.CustomHud;
import de.consolecrafter.ccraft.hud.KeystrokesRenderer;
import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.Theme;
import de.consolecrafter.ccraft.util.Themes;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Darstellung — Theme und HUD in einem Screen mit zwei Reitern.
 *
 * <p>Vorher gab es dafuer zwei getrennte Screens ("Design" und "HUD Editor"),
 * zwischen denen man hin- und herspringen musste, obwohl beide dasselbe Thema
 * behandeln. Die Reiter trennen die Inhalte weiterhin sauber, teilen aber
 * Kopfzeile, Ränder und Bedienlogik.</p>
 *
 * <p>Alle Aenderungen werden sofort gespeichert und sind im HUD-Vorschaubereich
 * direkt sichtbar ({@link #shouldPause()} ist {@code false}, damit die Welt
 * weiterlaeuft).</p>
 */
public class AppearanceScreen extends Screen {

    private static final int BTN_H = 20;
    private static final int GAP = 6;
    private static final int ROW_W = 360;
    private static final int COL_W = 175;
    private static final int COL_GAP = 10;
    private static final int SECTION_GAP = 16;
    private static final int LABEL_GAP = 12;
    private static final int COLS = 4;
    private static final int CONTENT_TOP = 74;
    private static final int TAB_Y = 40;
    private static final int TAB_W = 110;
    private static final int TAB_H = 18;

    private static final String[] LINE_NAMES =
            {"XYZ", "IGN", "FPS", "IP", "TPS", "Software", "Ping", "Memory", "Uhr", "Online"};
    private static final int[] TEXT_COLORS =
            {0xFFFFFF, 0x000000, 0xFF8080, 0x80FF80, 0x80C0FF, 0xFFFF80};
    private static final int[] KEY_SIZES = {20, 24, 28, 32, 40};
    private static final String[] HUD_STYLE_NAMES =
            {"Classic", "Kompakt", "Boxed", "Akzent", "Minimal", "Neon"};
    private static final String[] LOGO_NAMES =
            {"CCraft", "Classic", "Modern", "Cringe", "OG"};

    private final Screen parent;

    /** 0 = Theme, 1 = HUD. */
    private int tab = 0;
    private int scroll = 0;
    private int maxScroll = 0;

    private final int[] rgb = {255, 77, 77};
    private int left, cx;

    // Layout des Theme-Reiters (ohne Scroll; gezeichnet wird mit Offset).
    private int darkY, presetY, presetRows, rgbY, randomY, previewY, themeBottom;
    // Layout des HUD-Reiters.
    private int linesY, styleY, textColorY, logoY, keystrokesY, ksSizeY, ksMouseY, hudBottom;

    private boolean draggingKeystrokes;
    private double ksDragOffsetX, ksDragOffsetY;

    public AppearanceScreen(Screen parent) {
        super(Text.literal("Darstellung"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        CraftConfig.load();
        rgb[0] = (CraftConfig.accent >> 16) & 0xFF;
        rgb[1] = (CraftConfig.accent >> 8) & 0xFF;
        rgb[2] = CraftConfig.accent & 0xFF;

        cx = this.width / 2;
        left = cx - ROW_W / 2;

        layout();
        if (tab == 0) initTheme(); else initHud();

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent))
                .position(cx - 75, this.height - 30).size(150, BTN_H).build());
    }

    private void layout() {
        presetRows = (Themes.PRESETS.length + COLS - 1) / COLS;

        int y = CONTENT_TOP;
        darkY = y;
        y += BTN_H + SECTION_GAP;

        presetY = y + LABEL_GAP;
        y = presetY + presetRows * (BTN_H + GAP) + SECTION_GAP;

        rgbY = y + LABEL_GAP;
        y = rgbY + 3 * (BTN_H + GAP) + SECTION_GAP;

        randomY = y;
        y += BTN_H + SECTION_GAP;

        previewY = y + LABEL_GAP;
        themeBottom = previewY + BTN_H;

        y = CONTENT_TOP;
        linesY = y + LABEL_GAP;
        y = linesY + 5 * (BTN_H + GAP) + SECTION_GAP;

        styleY = y + LABEL_GAP;
        y = styleY + BTN_H + SECTION_GAP;

        textColorY = y + LABEL_GAP;
        y = textColorY + BTN_H + SECTION_GAP;

        logoY = y + LABEL_GAP;
        y = logoY + BTN_H + SECTION_GAP;

        keystrokesY = y + LABEL_GAP;
        y = keystrokesY + BTN_H + GAP;
        ksSizeY = y;
        y += BTN_H + GAP;
        ksMouseY = y;
        y += BTN_H;
        hudBottom = y;

        int contentHeight = (tab == 0 ? themeBottom : hudBottom) - CONTENT_TOP;
        int viewport = (this.height - 44) - CONTENT_TOP;
        maxScroll = Math.max(0, contentHeight - viewport);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private int offset() {
        return -scroll;
    }

    // ── Reiter ───────────────────────────────────────────────────────────────
    private void initTabs() {
        int tabX = cx - TAB_W - GAP / 2;
        addDrawableChild(ButtonWidget.builder(tabLabel("Theme", 0), b -> switchTab(0))
                .position(tabX, TAB_Y).size(TAB_W, TAB_H).build());
        addDrawableChild(ButtonWidget.builder(tabLabel("HUD", 1), b -> switchTab(1))
                .position(tabX + TAB_W + GAP, TAB_Y).size(TAB_W, TAB_H).build());
    }

    private Text tabLabel(String name, int index) {
        return Text.literal(index == tab ? "§l" + name : name);
    }

    private void switchTab(int index) {
        if (tab == index) return;
        tab = index;
        scroll = 0;
        this.clearAndInit();
    }

    // ── Theme-Reiter ─────────────────────────────────────────────────────────
    private void initTheme() {
        initTabs();
        int o = offset();

        addDrawableChild(ButtonWidget.builder(Text.literal("Dark"), b -> setDark(true))
                .position(left, darkY + o).size(COL_W, BTN_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Light"), b -> setDark(false))
                .position(left + COL_W + COL_GAP, darkY + o).size(COL_W, BTN_H).build());

        int pw = (ROW_W - (COLS - 1) * GAP) / COLS;
        for (int i = 0; i < Themes.PRESETS.length; i++) {
            Themes.Preset preset = Themes.PRESETS[i];
            int px = left + (i % COLS) * (pw + GAP);
            int py = presetY + (i / COLS) * (BTN_H + GAP) + o;
            addDrawableChild(ButtonWidget.builder(Text.literal(preset.name()),
                            b -> { Themes.apply(preset); Theme.apply(); this.clearAndInit(); })
                    .position(px, py).size(pw, BTN_H).build());
        }

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int sy = rgbY + i * (BTN_H + GAP) + o;
            addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> adjust(idx, -15))
                    .position(left + 30, sy).size(20, BTN_H).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> adjust(idx, 15))
                    .position(left + ROW_W - 30, sy).size(20, BTN_H).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Zufallsfarbe"), b -> {
            CraftConfig.accent = (randomChannel() << 16) | (randomChannel() << 8) | randomChannel();
            CraftConfig.preset = "Eigene";
            Theme.apply();
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, randomY + o).size(ROW_W, BTN_H).build());
    }

    private void setDark(boolean dark) {
        CraftConfig.dark = dark;
        Theme.apply();
        CraftConfig.save();
        this.clearAndInit();
    }

    private void adjust(int index, int delta) {
        rgb[index] = Math.max(0, Math.min(255, rgb[index] + delta));
        CraftConfig.accent = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        CraftConfig.preset = "Eigene";
        Theme.apply();
        CraftConfig.save();
    }

    private int randomChannel() {
        return (int) (Math.random() * 256);
    }

    private void renderTheme(DrawContext context) {
        int o = offset();

        label(context, "Helligkeit: " + (CraftConfig.dark ? "Dark" : "Light"), darkY + o);
        label(context, "Farb-Preset: " + CraftConfig.preset, presetY + o);
        label(context, "Eigene Akzentfarbe", rgbY + o);

        int[] channels = {(CraftConfig.accent >> 16) & 0xFF, (CraftConfig.accent >> 8) & 0xFF, CraftConfig.accent & 0xFF};
        String[] names = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            int sy = rgbY + i * (BTN_H + GAP) + o;
            context.drawText(textRenderer, "§7" + names[i] + ": §f" + channels[i],
                    left + 60, sy + 6, Theme.TEXT_DIM, true);
        }

        Theme.roundedBox(context, left, previewY + o, ROW_W, BTN_H, CraftConfig.accent, 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer, "Akzent-Vorschau", cx, previewY + o + 6, 0xFFFFFFFF);
    }

    // ── HUD-Reiter ───────────────────────────────────────────────────────────
    private void initHud() {
        initTabs();
        int o = offset();

        for (int i = 0; i < LINE_NAMES.length; i++) {
            final int idx = i;
            int bx = left + (i % 2) * (COL_W + COL_GAP);
            int by = linesY + (i / 2) * (BTN_H + GAP) + o;
            addDrawableChild(ButtonWidget.builder(lineLabel(i), b -> {
                CraftConfig.hudLines[idx] = !CraftConfig.hudLines[idx];
                CraftConfig.save();
                this.clearAndInit();
            }).position(bx, by).size(COL_W, BTN_H).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Stil: " + styleName()), b -> {
            CraftConfig.hudStyle = (CraftConfig.hudStyle + 1) % CraftConfig.HUD_STYLE_COUNT;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, styleY + o).size(COL_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Textfarbe"), b -> {
            int current = CraftConfig.hudText & 0xFFFFFF;
            int next = 0;
            for (int i = 0; i < TEXT_COLORS.length; i++) {
                if ((TEXT_COLORS[i] & 0xFFFFFF) == current) { next = (i + 1) % TEXT_COLORS.length; break; }
            }
            CraftConfig.hudText = 0xFF000000 | TEXT_COLORS[next];
            CraftConfig.save();
            this.clearAndInit();
        }).position(left + COL_W + COL_GAP, styleY + o).size(COL_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Logo: " + LOGO_NAMES[logoIndex()]), b -> {
            CraftConfig.logoChoice = (logoIndex() + 1) % CraftConfig.LOGO_COUNT;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, logoY + o).size(ROW_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Textschatten: " + (CraftConfig.hudShadow ? "an" : "aus")), b -> {
            CraftConfig.hudShadow = !CraftConfig.hudShadow;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, textColorY + o).size(ROW_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Keystrokes: " + (CraftConfig.keystrokes ? "an" : "aus")), b -> {
            CraftConfig.keystrokes = !CraftConfig.keystrokes;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, keystrokesY + o).size(ROW_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Größe: " + CraftConfig.keystrokesSize), b -> {
            int next = 0;
            for (int i = 0; i < KEY_SIZES.length; i++) {
                if (KEY_SIZES[i] == CraftConfig.keystrokesSize) { next = (i + 1) % KEY_SIZES.length; break; }
            }
            CraftConfig.keystrokesSize = KEY_SIZES[next];
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, ksSizeY + o).size(COL_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Stil: " + keystrokesStyleName()), b -> {
            CraftConfig.keystrokesStyle = (CraftConfig.keystrokesStyle + 1) % 3;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left + COL_W + COL_GAP, ksSizeY + o).size(COL_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Maus: " + (CraftConfig.keystrokesShowMouse ? "an" : "aus")), b -> {
            CraftConfig.keystrokesShowMouse = !CraftConfig.keystrokesShowMouse;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left, ksMouseY + o).size(COL_W, BTN_H).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Leertaste: " + (CraftConfig.keystrokesShowSpace ? "an" : "aus")), b -> {
            CraftConfig.keystrokesShowSpace = !CraftConfig.keystrokesShowSpace;
            CraftConfig.save();
            this.clearAndInit();
        }).position(left + COL_W + COL_GAP, ksMouseY + o).size(COL_W, BTN_H).build());
    }

    private void renderHud(DrawContext context) {
        int o = offset();

        label(context, "Sichtbare HUD-Zeilen", linesY + o);
        label(context, "Aussehen", styleY + o);
        label(context, "Text", textColorY + o);
        label(context, "Logo", logoY + o);
        label(context, "Keystrokes (WASD-Overlay)", keystrokesY + o);

        renderHudPreview(context);

        // Vorschau an der echten HUD-Position; per Ziehen verschiebbar.
        int keyX = KeystrokesRenderer.positionX(this.width);
        int keyY = KeystrokesRenderer.positionY(this.height);
        KeystrokesRenderer.renderPreview(context, keyX, keyY);

        String hint = "Ziehen zum Positionieren";
        int hintX = Math.max(4, Math.min(keyX, this.width - textRenderer.getWidth(hint) - 4));
        context.drawTextWithShadow(textRenderer, hint,
                hintX, Math.max(TAB_Y + TAB_H + 4, keyY - textRenderer.fontHeight - 6), Theme.TEXT_DIM);
    }

    /**
     * Vorschau der HUD-Infobox an der echten Position (oben links unter dem
     * Logo). Zeigt die eingestellte Textfarbe, die aktiven Zeilen und die
     * gewaehlte Logo-Variante, damit Aenderungen sofort sichtbar sind.
     */
    private void renderHudPreview(DrawContext context) {
        int x = 2;
        int logoH = CustomHud.logoHeightForWidth(62);
        int y = 2 + logoH + 4;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, CustomHud.logo(),
                2, 2, 0f, 0f, 62, logoH, 62, logoH);

        int lineHeight = textRenderer.fontHeight + 1;
        int visible = 0;
        for (int i = 0; i < CraftConfig.hudLines.length; i++) if (CraftConfig.hudLines[i]) visible++;

        Theme.panel(context, x - 2, y - 3, 120, (visible + 1) * lineHeight + 6);
        context.drawText(textRenderer, "CCraft Client", x, y, Theme.ACCENT, true);

        int cy = y + lineHeight;
        for (int i = 0; i < CraftConfig.hudLines.length && i < LINE_NAMES.length; i++) {
            if (!CraftConfig.hudLines[i]) continue;
            context.drawText(textRenderer, LINE_NAMES[i] + ": …", x, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
            cy += lineHeight;
        }
    }

    private Text lineLabel(int index) {
        return Text.literal((CraftConfig.hudLines[index] ? "§a✔ " : "§c✘ ") + LINE_NAMES[index]);
    }

    private String styleName() {
        int index = Math.max(0, Math.min(HUD_STYLE_NAMES.length - 1, CraftConfig.hudStyle));
        return HUD_STYLE_NAMES[index];
    }

    private int logoIndex() {
        return Math.max(0, Math.min(CraftConfig.LOGO_COUNT - 1, CraftConfig.logoChoice));
    }

    private String keystrokesStyleName() {
        return switch (CraftConfig.keystrokesStyle) {
            case 1 -> "Transparent";
            case 2 -> "Akzent";
            default -> "3D";
        };
    }

    private void label(DrawContext context, String text, int y) {
        context.drawText(textRenderer, "§7" + text, left, y - LABEL_GAP + 1, Theme.TEXT_DIM, true);
    }

    // ── Rahmen ───────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Theme.header(context, textRenderer, "Darstellung", cx);
        Theme.accentBar(context, cx - TAB_W - GAP / 2, TAB_Y + TAB_H + 2, 2 * TAB_W + GAP);

        if (tab == 0) renderTheme(context); else renderHud(context);

        String hint = maxScroll > 0 ? "Mausrad = scrollen" : "Rechter Shift = schließen";
        Theme.footer(context, textRenderer, hint, cx, this.height - 18);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 16)));
            this.clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (tab == 1 && click.button() == 0) {
            int keyX = KeystrokesRenderer.positionX(this.width);
            int keyY = KeystrokesRenderer.positionY(this.height);
            if (click.x() >= keyX && click.x() <= keyX + KeystrokesRenderer.overlayWidth()
                    && click.y() >= keyY && click.y() <= keyY + KeystrokesRenderer.overlayHeight()) {
                draggingKeystrokes = true;
                ksDragOffsetX = click.x() - keyX;
                ksDragOffsetY = click.y() - keyY;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingKeystrokes) {
            int maxX = Math.max(0, this.width - KeystrokesRenderer.overlayWidth());
            int maxY = Math.max(0, this.height - KeystrokesRenderer.overlayHeight());
            int x = (int) Math.round(click.x() - ksDragOffsetX);
            int y = (int) Math.round(click.y() - ksDragOffsetY);
            CraftConfig.keystrokesX = maxX == 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, x / (float) maxX));
            CraftConfig.keystrokesY = maxY == 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, y / (float) maxY));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingKeystrokes) {
            draggingKeystrokes = false;
            CraftConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
