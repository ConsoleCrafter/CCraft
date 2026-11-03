package de.consolecrafter.ccraft.gui;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.hud.InfoHud;
import de.consolecrafter.ccraft.hud.KeystrokesHud;
import de.consolecrafter.ccraft.hud.Logos;
import de.consolecrafter.ccraft.util.Theme;
import de.consolecrafter.ccraft.util.Themes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Random;

public final class AppearanceScreen extends CCraftScreen {

    private static final int GAP = 6;
    private static final int ROW_W = 360;
    private static final int COL_W = 175;
    private static final int COL_GAP = 10;
    private static final int SECTION_GAP = 16;
    private static final int LABEL_GAP = 12;
    private static final int PRESET_COLS = 4;
    private static final int CONTENT_TOP = 74;
    private static final int TAB_Y = 40;
    private static final int TAB_W = 110;
    private static final int TAB_H = 18;

    private static final int[] TEXT_COLORS = {0xFFFFFF, 0x000000, 0xFF8080, 0x80FF80, 0x80C0FF, 0xFFFF80};
    private static final int[] KEY_SIZES = {20, 24, 28, 32, 40};
    private static final String[] HUD_STYLES = {"Classic", "Kompakt", "Boxed", "Akzent", "Minimal", "Neon"};
    private static final String[] KEYSTROKE_STYLES = {"3D", "Transparent", "Akzent"};

    private static int tab;

    private final Random random = new Random();
    private int scroll;
    private int maxScroll;
    private int left;
    private int cx;

    private int darkY;
    private int presetY;
    private int rgbY;
    private int randomY;
    private int previewY;
    private int linesY;
    private int styleY;
    private int textY;
    private int logoY;
    private int keystrokesY;

    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    public AppearanceScreen(Screen parent) {
        super("Darstellung", parent);
    }

    @Override
    protected void init() {
        cx = width / 2;
        left = cx - ROW_W / 2;
        layout();

        int tabX = cx - TAB_W - GAP / 2;
        button(tab == 0 ? "§lTheme" : "Theme", tabX, TAB_Y, TAB_W, TAB_H, () -> switchTab(0));
        button(tab == 1 ? "§lHUD" : "HUD", tabX + TAB_W + GAP, TAB_Y, TAB_W, TAB_H, () -> switchTab(1));

        if (tab == 0) initTheme(); else initHud();
        backButton(cx - 75, height - 30, 150);
    }

    private void layout() {
        int presetRows = (Themes.PRESETS.length + PRESET_COLS - 1) / PRESET_COLS;
        darkY = CONTENT_TOP;
        presetY = darkY + BUTTON_HEIGHT + SECTION_GAP + LABEL_GAP;
        rgbY = presetY + presetRows * (BUTTON_HEIGHT + GAP) + SECTION_GAP + LABEL_GAP;
        randomY = rgbY + 3 * (BUTTON_HEIGHT + GAP) + SECTION_GAP;
        previewY = randomY + BUTTON_HEIGHT + SECTION_GAP + LABEL_GAP;
        int themeBottom = previewY + BUTTON_HEIGHT;

        linesY = CONTENT_TOP + LABEL_GAP;
        styleY = linesY + 5 * (BUTTON_HEIGHT + GAP) + SECTION_GAP + LABEL_GAP;
        textY = styleY + BUTTON_HEIGHT + SECTION_GAP + LABEL_GAP;
        logoY = textY + BUTTON_HEIGHT + SECTION_GAP + LABEL_GAP;
        keystrokesY = logoY + BUTTON_HEIGHT + SECTION_GAP + LABEL_GAP;
        int hudBottom = keystrokesY + 3 * (BUTTON_HEIGHT + GAP);

        int content = (tab == 0 ? themeBottom : hudBottom) - CONTENT_TOP;
        maxScroll = Math.max(0, content - (height - 44 - CONTENT_TOP));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void switchTab(int index) {
        if (tab == index) return;
        tab = index;
        scroll = 0;
        rebuildWidgets();
    }

    private void changed() {
        ClientConfig.save();
        rebuildWidgets();
    }

    private void initTheme() {
        int o = -scroll;
        button("Dark", left, darkY + o, COL_W, BUTTON_HEIGHT, () -> setDark(true));
        button("Light", left + COL_W + COL_GAP, darkY + o, COL_W, BUTTON_HEIGHT, () -> setDark(false));

        int presetW = (ROW_W - (PRESET_COLS - 1) * GAP) / PRESET_COLS;
        for (int i = 0; i < Themes.PRESETS.length; i++) {
            Themes.Preset preset = Themes.PRESETS[i];
            button(preset.name(), left + (i % PRESET_COLS) * (presetW + GAP), presetY + (i / PRESET_COLS) * (BUTTON_HEIGHT + GAP) + o,
                    presetW, BUTTON_HEIGHT, () -> {
                        Themes.apply(preset);
                        rebuildWidgets();
                    });
        }

        for (int i = 0; i < 3; i++) {
            int shift = 16 - i * 8;
            int y = rgbY + i * (BUTTON_HEIGHT + GAP) + o;
            button("-", left + 30, y, 20, BUTTON_HEIGHT, () -> adjust(shift, -15));
            button("+", left + ROW_W - 30, y, 20, BUTTON_HEIGHT, () -> adjust(shift, 15));
        }

        button("Zufallsfarbe", left, randomY + o, ROW_W, BUTTON_HEIGHT, () -> {
            Themes.applyCustom(random.nextInt(0x1000000));
            rebuildWidgets();
        });
    }

    private void setDark(boolean dark) {
        ClientConfig.get().dark = dark;
        Theme.apply();
        changed();
    }

    private void adjust(int shift, int delta) {
        int accent = ClientConfig.get().accent;
        int channel = Math.max(0, Math.min(255, ((accent >> shift) & 0xFF) + delta));
        Themes.applyCustom((accent & ~(0xFF << shift)) | (channel << shift));
    }

    private void initHud() {
        ClientConfig config = ClientConfig.get();
        int o = -scroll;
        for (int i = 0; i < InfoHud.LINE_NAMES.length; i++) {
            int index = i;
            String label = (config.hudLines[i] ? "§a✔ " : "§c✘ ") + InfoHud.LINE_NAMES[i];
            button(label, left + (i % 2) * (COL_W + COL_GAP), linesY + (i / 2) * (BUTTON_HEIGHT + GAP) + o, COL_W, BUTTON_HEIGHT, () -> {
                config.hudLines[index] = !config.hudLines[index];
                changed();
            });
        }

        button("Stil: " + HUD_STYLES[config.hudStyle], left, styleY + o, COL_W, BUTTON_HEIGHT, () -> {
            config.hudStyle = (config.hudStyle + 1) % ClientConfig.HUD_STYLE_COUNT;
            changed();
        });
        button("Textfarbe", left + COL_W + COL_GAP, styleY + o, COL_W, BUTTON_HEIGHT, () -> {
            int current = config.hudText & 0xFFFFFF;
            int next = 0;
            for (int i = 0; i < TEXT_COLORS.length; i++) {
                if (TEXT_COLORS[i] == current) next = (i + 1) % TEXT_COLORS.length;
            }
            config.hudText = 0xFF000000 | TEXT_COLORS[next];
            changed();
        });

        button("Textschatten: " + onOff(config.hudShadow), left, textY + o, ROW_W, BUTTON_HEIGHT, () -> {
            config.hudShadow = !config.hudShadow;
            changed();
        });
        button("Logo: " + Logos.NAMES[Logos.index()], left, logoY + o, ROW_W, BUTTON_HEIGHT, () -> {
            config.logoChoice = (Logos.index() + 1) % ClientConfig.LOGO_COUNT;
            changed();
        });

        int y = keystrokesY + o;
        button("Keystrokes: " + onOff(config.keystrokes), left, y, ROW_W, BUTTON_HEIGHT, () -> {
            config.keystrokes = !config.keystrokes;
            changed();
        });
        y += BUTTON_HEIGHT + GAP;
        button("Größe: " + config.keystrokesSize, left, y, COL_W, BUTTON_HEIGHT, () -> {
            int next = 0;
            for (int i = 0; i < KEY_SIZES.length; i++) {
                if (KEY_SIZES[i] == config.keystrokesSize) next = (i + 1) % KEY_SIZES.length;
            }
            config.keystrokesSize = KEY_SIZES[next];
            changed();
        });
        button("Stil: " + KEYSTROKE_STYLES[config.keystrokesStyle], left + COL_W + COL_GAP, y, COL_W, BUTTON_HEIGHT, () -> {
            config.keystrokesStyle = (config.keystrokesStyle + 1) % KEYSTROKE_STYLES.length;
            changed();
        });
        y += BUTTON_HEIGHT + GAP;
        button("Maus: " + onOff(config.keystrokesShowMouse), left, y, COL_W, BUTTON_HEIGHT, () -> {
            config.keystrokesShowMouse = !config.keystrokesShowMouse;
            changed();
        });
        button("Leertaste: " + onOff(config.keystrokesShowSpace), left + COL_W + COL_GAP, y, COL_W, BUTTON_HEIGHT, () -> {
            config.keystrokesShowSpace = !config.keystrokesShowSpace;
            changed();
        });
    }

    private static String onOff(boolean value) {
        return value ? "an" : "aus";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Theme.header(g, font, "Darstellung", cx);
        Theme.accentBar(g, cx - TAB_W - GAP / 2, TAB_Y + TAB_H + 2, 2 * TAB_W + GAP);
        if (tab == 0) extractTheme(g); else extractHud(g);
        Theme.footer(g, font, maxScroll > 0 ? "Mausrad = scrollen" : "ESC = zurück", cx, height - 18);
    }

    private void extractTheme(GuiGraphicsExtractor g) {
        ClientConfig config = ClientConfig.get();
        int o = -scroll;
        label(g, "Helligkeit: " + (config.dark ? "Dark" : "Light"), darkY + o);
        label(g, "Farb-Preset: " + config.preset, presetY + o);
        label(g, "Eigene Akzentfarbe", rgbY + o);
        String[] names = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            int value = (config.accent >> (16 - i * 8)) & 0xFF;
            g.text(font, "§7" + names[i] + ": §f" + value, left + 60, rgbY + i * (BUTTON_HEIGHT + GAP) + o + 6, Theme.TEXT_DIM, true);
        }
        Theme.roundedBox(g, left, previewY + o, ROW_W, BUTTON_HEIGHT, 0xFF000000 | config.accent, 0xFF000000);
        g.centeredText(font, "Akzent-Vorschau", cx, previewY + o + 6, 0xFFFFFFFF);
    }

    private void extractHud(GuiGraphicsExtractor g) {
        ClientConfig config = ClientConfig.get();
        int o = -scroll;
        label(g, "Sichtbare HUD-Zeilen", linesY + o);
        label(g, "Aussehen", styleY + o);
        label(g, "Text", textY + o);
        label(g, "Logo", logoY + o);
        label(g, "Keystrokes (WASD-Overlay)", keystrokesY + o);

        int logoH = Logos.draw(g, 2, 2, 62);
        int y = 2 + logoH + 4;
        int lineHeight = font.lineHeight + 1;
        int visible = 0;
        for (boolean line : config.hudLines) if (line) visible++;
        Theme.panel(g, 0, y - 3, 120, (visible + 1) * lineHeight + 6);
        g.text(font, "CCraft Client", 2, y, Theme.ACCENT, true);
        int cy = y + lineHeight;
        for (int i = 0; i < config.hudLines.length; i++) {
            if (!config.hudLines[i]) continue;
            g.text(font, InfoHud.LINE_NAMES[i] + ": …", 2, cy, config.hudColor(), config.hudShadow);
            cy += lineHeight;
        }

        int kx = KeystrokesHud.x(width);
        int ky = KeystrokesHud.y(height);
        KeystrokesHud.preview(g, font, kx, ky);
        String hint = "Ziehen zum Positionieren";
        g.text(font, hint, Math.max(4, Math.min(kx, width - font.width(hint) - 4)),
                Math.max(TAB_Y + TAB_H + 4, ky - font.lineHeight - 6), Theme.TEXT_DIM, true);
    }

    private void label(GuiGraphicsExtractor g, String text, int y) {
        g.text(font, "§7" + text, left, y - LABEL_GAP + 1, Theme.TEXT_DIM, true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * 16)));
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (tab == 1 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int kx = KeystrokesHud.x(width);
            int ky = KeystrokesHud.y(height);
            if (event.x() >= kx && event.x() <= kx + KeystrokesHud.width() && event.y() >= ky && event.y() <= ky + KeystrokesHud.height()) {
                dragging = true;
                dragOffsetX = event.x() - kx;
                dragOffsetY = event.y() - ky;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        ClientConfig config = ClientConfig.get();
        int maxX = Math.max(0, width - KeystrokesHud.width());
        int maxY = Math.max(0, height - KeystrokesHud.height());
        config.keystrokesX = maxX == 0 ? 0 : (float) Math.max(0, Math.min(1, (event.x() - dragOffsetX) / maxX));
        config.keystrokesY = maxY == 0 ? 0 : (float) Math.max(0, Math.min(1, (event.y() - dragOffsetY) / maxY));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!dragging) return super.mouseReleased(event);
        dragging = false;
        ClientConfig.save();
        return true;
    }
}
