package de.consolecrafter.ccraft.util;

import de.consolecrafter.ccraft.config.ClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class Theme {

    public static final int RADIUS = 5;

    public static final int BUTTON_NORMAL = 0;
    public static final int BUTTON_HOVER = 1;
    public static final int BUTTON_ACTIVE = 2;
    public static final int BUTTON_DISABLED = 3;

    public static int ACCENT;
    public static int ACCENT_DARK;
    public static int TEXT;
    public static int TEXT_DIM;
    public static int TEXT_FAINT;
    public static final int SUCCESS = 0xFF5AD07A;
    public static final int DANGER = 0xFFFF5555;

    public static int PANEL;
    public static int BORDER;

    private static int btnFill;
    private static int btnFillHover;
    private static int btnFillActive;
    private static int btnFillOff;
    private static int btnBorder;
    private static int btnBorderHover;
    private static int btnBorderActive;
    private static int btnBorderOff;

    private static int bgTop;
    private static int bgBottom;

    static {
        apply();
    }

    private Theme() {}

    public static void apply() {
        ClientConfig config = ClientConfig.get();
        int accent = config.accent & 0xFFFFFF;
        ACCENT = 0xFF000000 | accent;

        if (config.dark) {
            int base = 0x160709;
            bgTop = opaque(lerp(0x2A0A0A, accent, 0.35));
            bgBottom = 0xFF0A0507;
            ACCENT_DARK = opaque(lerp(0x000000, accent, 0.5));
            TEXT = 0xFFFFFFFF;
            TEXT_DIM = 0xFFC9AFAF;
            TEXT_FAINT = 0xFF8A6E6E;
            PANEL = alpha(0xCC, lerp(base, accent, 0.10));
            BORDER = alpha(0x99, ACCENT);
            btnFill = alpha(0xB8, lerp(base, accent, 0.14));
            btnFillHover = alpha(0xD8, lerp(base, accent, 0.22));
            btnFillActive = alpha(0xF0, ACCENT_DARK);
            btnFillOff = 0x80303030;
            btnBorder = alpha(0xFF, lerp(accent, 0x000000, 0.55));
            btnBorderHover = alpha(0xFF, lerp(accent, 0xFFFFFF, 0.10));
            btnBorderActive = alpha(0xFF, lerp(accent, 0xFFFFFF, 0.35));
            btnBorderOff = 0xFF555555;
        } else {
            bgTop = 0xFFF2E9E9;
            bgBottom = 0xFFDED2D2;
            ACCENT_DARK = opaque(lerp(0x000000, accent, 0.55));
            TEXT = 0xFF1A1111;
            TEXT_DIM = 0xFF4A3A3A;
            TEXT_FAINT = 0xFF6E5A5A;
            PANEL = alpha(0xE6, 0xF7F0F0);
            BORDER = alpha(0xBB, ACCENT);
            btnFill = alpha(0xE6, lerp(0xFFFFFF, accent, 0.10));
            btnFillHover = alpha(0xF0, lerp(0xFFFFFF, accent, 0.22));
            btnFillActive = alpha(0xFF, ACCENT);
            btnFillOff = 0x80C0C0C0;
            btnBorder = alpha(0xFF, lerp(accent, 0x000000, 0.25));
            btnBorderHover = alpha(0xFF, lerp(accent, 0x000000, 0.05));
            btnBorderActive = alpha(0xFF, ACCENT_DARK);
            btnBorderOff = 0xFF9A9A9A;
        }
    }

    public static int alpha(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static int lerp(int from, int to, double t) {
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static void background(GuiGraphicsExtractor g, int width, int height) {
        g.fillGradient(0, 0, width, height, bgTop, bgBottom);
        g.fillGradient(0, 0, width, height / 3, 0x33000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0x55000000);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        roundedBox(g, x, y, w, h, PANEL, BORDER);
        g.fill(x + RADIUS, y + 1, x + w - RADIUS, y + 2, 0x22FFFFFF);
    }

    public static void header(GuiGraphicsExtractor g, Font font, String name, int centerX) {
        g.centeredText(font, "§lCCraft", centerX, 8, TEXT);
        g.centeredText(font, "§l" + name, centerX, 22, ACCENT);
    }

    public static void accentBar(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 2, ACCENT);
    }

    public static void divider(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, BORDER);
    }

    public static void footer(GuiGraphicsExtractor g, Font font, String text, int centerX, int y) {
        g.centeredText(font, text, centerX, y, TEXT_FAINT);
    }

    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void roundedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.min(RADIUS, Math.min(w, h) / 2);
        if (r <= 1) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int inset = r - (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(x + inset, y + i, x + r, y + i + 1, color);
            g.fill(x + w - r, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + r, y + h - i, color);
            g.fill(x + w - r, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void roundedBox(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        roundedRect(g, x, y, w, h, border);
        roundedRect(g, x + 1, y + 1, w - 2, h - 2, fill);
    }

    public static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, Component label, int state) {
        boolean dark = ClientConfig.get().dark;
        int fill;
        int border;
        int textColor;
        switch (state) {
            case BUTTON_HOVER -> {
                fill = btnFillHover;
                border = btnBorderHover;
                textColor = dark ? 0xFFFFFFFF : TEXT;
            }
            case BUTTON_ACTIVE -> {
                fill = btnFillActive;
                border = btnBorderActive;
                textColor = 0xFFFFFFFF;
            }
            case BUTTON_DISABLED -> {
                fill = btnFillOff;
                border = btnBorderOff;
                textColor = 0xFF9A9A9A;
            }
            default -> {
                fill = btnFill;
                border = btnBorder;
                textColor = dark ? 0xFFFFFFFF : TEXT;
            }
        }
        roundedBox(g, x, y, w, h, fill, border);
        g.fill(x + RADIUS, y + 1, x + w - RADIUS, y + 2, dark ? 0x1AFFFFFF : 0x22FFFFFF);
        g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, textColor);
    }
}
