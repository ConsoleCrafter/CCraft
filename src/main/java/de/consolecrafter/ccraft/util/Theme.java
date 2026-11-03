package de.consolecrafter.ccraft.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Zentrale Design-Definition für alle CCraft-Screens.
 * Rot-schwarze Marke, abgerundete transluzente Buttons/Panels.
 */
public final class Theme {
    private Theme() {}

    // ── Farbpalette (dynamisch, siehe CraftConfig) ───────────────────────────
    public static int ACCENT       = 0xFFFF4D4D; // CCraft-Akzent
    public static int ACCENT_DARK  = 0xFFB22222;
    public static int BRAND        = 0xFFFF4D4D;
    public static int TEXT         = 0xFFFFFFFF;
    public static int TEXT_DIM     = 0xFFC9AFAF;
    public static int TEXT_FAINT   = 0xFF8A6E6E;
    public static int SUCCESS      = 0xFF5AD07A;
    public static int DANGER       = 0xFFFF5555;

    // Panels (transluzent)
    public static int PANEL        = 0xCC160709;
    public static int PANEL_HEADER = 0xE01E0A0C;
    public static int BORDER       = 0x99FF4D4D;

    // Buttons (transluzent)
    public static int BTN_FILL        = 0xB82A0E0E;
    public static int BTN_FILL_HOVER  = 0xD83A1414;
    public static int BTN_FILL_ACTIVE = 0xF0A81818;
    public static int BTN_FILL_OFF    = 0x80303030;
    public static int BTN_BORDER        = 0xFF7A1F1F;
    public static int BTN_BORDER_HOVER  = 0xFFE23A3A;
    public static int BTN_BORDER_ACTIVE = 0xFFFF6666;
    public static int BTN_BORDER_OFF    = 0xFF555555;

    // Hintergrund-Verlauf
    public static int BG_TOP = 0xFF2A0A0A;
    public static int BG_BOT = 0xFF0A0507;

    public static final int RADIUS = 5;

    private static int tint(double amount, int base) {
        return 0xFF000000 | scale(CraftConfig.accent & 0xFFFFFF, amount, base);
    }

    private static int scale(int rgb, double f, int base) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int br = (base >> 16) & 0xFF, bg = (base >> 8) & 0xFF, bb = base & 0xFF;
        int nr = Math.min(255, (int) (br + (r - br) * f));
        int ng = Math.min(255, (int) (bg + (g - bg) * f));
        int nb = Math.min(255, (int) (bb + (b - bb) * f));
        return (nr << 16) | (ng << 8) | nb;
    }

    private static int mix(int c1, int c2, double t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Baut die komplette Palette aus {@link CraftConfig}. Muss nach jeder
     * Änderung an Theme/Farbe aufgerufen werden.
     */
    public static void apply() {
        int a = CraftConfig.accent & 0xFFFFFF;   // Akzent-RGB
        ACCENT = 0xFF000000 | a;
        BRAND = ACCENT;

        if (CraftConfig.dark) {
            int dark = 0x160709;
            BG_TOP = tint(0.35, 0x2A0A0A);
            BG_BOT = 0xFF0A0507;
            ACCENT_DARK = 0xFF000000 | scale(a, 0.5, 0);
            TEXT = 0xFFFFFFFF;
            TEXT_DIM = 0xFFC9AFAF;
            TEXT_FAINT = 0xFF8A6E6E;

            PANEL = withAlpha(0xCC, tint(0.10, dark));
            PANEL_HEADER = withAlpha(0xE0, tint(0.16, dark));
            BORDER = withAlpha(0x99, ACCENT);

            BTN_FILL = withAlpha(0xB8, tint(0.14, dark));
            BTN_FILL_HOVER = withAlpha(0xD8, tint(0.22, dark));
            BTN_FILL_ACTIVE = withAlpha(0xF0, ACCENT_DARK);
            BTN_FILL_OFF = 0x80303030;
            BTN_BORDER = withAlpha(0xFF, mix(ACCENT, 0x000000, 0.55));
            BTN_BORDER_HOVER = withAlpha(0xFF, mix(ACCENT, 0xFFFFFF, 0.10));
            BTN_BORDER_ACTIVE = withAlpha(0xFF, mix(ACCENT, 0xFFFFFF, 0.35));
            BTN_BORDER_OFF = 0xFF555555;
        } else {
            BG_TOP = 0xFFF2E9E9;
            BG_BOT = 0xFFDED2D2;
            ACCENT_DARK = 0xFF000000 | scale(a, 0.55, 0);
            TEXT = 0xFF1A1111;
            TEXT_DIM = 0xFF4A3A3A;
            TEXT_FAINT = 0xFF6E5A5A;

            PANEL = withAlpha(0xE6, 0xFFF7F0F0);
            PANEL_HEADER = withAlpha(0xF0, 0xFFFFFFFF);
            BORDER = withAlpha(0xBB, ACCENT);

            BTN_FILL = withAlpha(0xE6, mix(0xFFFFFF, ACCENT, 0.10));
            BTN_FILL_HOVER = withAlpha(0xF0, mix(0xFFFFFF, ACCENT, 0.22));
            BTN_FILL_ACTIVE = withAlpha(0xFF, ACCENT);
            BTN_FILL_OFF = 0x80C0C0C0;
            BTN_BORDER = withAlpha(0xFF, mix(ACCENT, 0x000000, 0.25));
            BTN_BORDER_HOVER = withAlpha(0xFF, mix(ACCENT, 0x000000, 0.05));
            BTN_BORDER_ACTIVE = withAlpha(0xFF, ACCENT_DARK);
            BTN_BORDER_OFF = 0xFF9A9A9A;
        }
    }

    private static int withAlpha(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    // ── Hintergrund ──────────────────────────────────────────────────────────
    /** Screen-Hintergrund: Verlauf + Vignette, Helligkeit aus CraftConfig. */
    public static void background(DrawContext ctx, int width, int height) {
        ctx.fillGradient(0, 0, width, height, BG_TOP, BG_BOT);
        ctx.fillGradient(0, 0, width, height / 3, 0x33000000, 0x00000000);
        ctx.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0x55000000);
    }

    // ── Panel (abgerundet, transluzent) ──────────────────────────────────────
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        roundedBox(ctx, x, y, w, h, PANEL, BORDER);
        ctx.fill(x + RADIUS, y + 1, x + w - RADIUS, y + 2, 0x22FFFFFF);
    }

    // ── Titel mit Akzentlinie ────────────────────────────────────────────────
    public static void title(DrawContext ctx, TextRenderer tr, String title, int centerX, int y) {
        ctx.drawCenteredTextWithShadow(tr, title, centerX, y, ACCENT);
        int w = tr.getWidth(title);
        accentBar(ctx, centerX - w / 2, y + tr.fontHeight + 3, w);
    }

    /**
     * Einheitliche Kopfzeile aller CCraft-Screens: "CCraft" (weiß) über dem
     * Namen des Screens (Akzent). Belegt oben ca. 34px — darunter beginnt der
     * Inhalt.
     */
    public static void header(DrawContext ctx, TextRenderer tr, String name, int centerX) {
        ctx.drawCenteredTextWithShadow(tr, "§lCCraft", centerX, 8, TEXT);
        ctx.drawCenteredTextWithShadow(tr, "§l" + name, centerX, 22, ACCENT);
    }

    public static void accentBar(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 2, ACCENT);
    }

    public static void divider(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, BORDER);
    }

    // ── Fußzeile ─────────────────────────────────────────────────────────────
    public static void footer(DrawContext ctx, TextRenderer tr, String text, int centerX, int y) {
        ctx.drawCenteredTextWithShadow(tr, text, centerX, y, TEXT_FAINT);
    }

    // ── Label/Wert-Zeile ─────────────────────────────────────────────────────
    public static void stat(DrawContext ctx, TextRenderer tr, String label, String value,
                            int x, int y, int valueColor) {
        ctx.drawText(tr, "▶ ", x, y, TEXT_FAINT, true);
        ctx.drawText(tr, label, x + tr.getWidth("▶ "), y, TEXT_DIM, true);
        ctx.drawText(tr, value, x + tr.getWidth("▶ " + label + "  "), y, valueColor, true);
    }

    public static void section(DrawContext ctx, TextRenderer tr, String text, int centerX, int y, int color) {
        ctx.drawCenteredTextWithShadow(tr, text, centerX, y, color);
    }

    // ── Abgerundete Flächen ──────────────────────────────────────────────────
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.min(RADIUS, Math.min(w, h) / 2);
        if (r <= 1) { ctx.fill(x, y, x + w, y + h, color); return; }
        // Zentrale Bänder (voller Durchmesser)
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + w, y + h - r, color);
        // Ecken per Kreisbogen
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int inset = r - (int) Math.round(Math.sqrt((double) (r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + r, y + i + 1, color);
            ctx.fill(x + w - r, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + r, y + h - i, color);
            ctx.fill(x + w - r, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void roundedBox(DrawContext ctx, int x, int y, int w, int h, int fill, int border) {
        roundedRect(ctx, x, y, w, h, border);
        roundedRect(ctx, x + 1, y + 1, w - 2, h - 2, fill);
    }

    // ── Einheitlicher Button-Look ────────────────────────────────────────────
    /** state: 0 = normal, 1 = hover, 2 = aktiv/ausgewählt, 3 = deaktiviert. */
    public static void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                              net.minecraft.text.Text label, int state) {
        int fill, border, textColor;
        switch (state) {
            case 1 -> { fill = BTN_FILL_HOVER;  border = BTN_BORDER_HOVER;  textColor = buttonText(false); }
            case 2 -> { fill = BTN_FILL_ACTIVE; border = BTN_BORDER_ACTIVE; textColor = buttonText(true); }
            case 3 -> { fill = BTN_FILL_OFF;    border = BTN_BORDER_OFF;    textColor = 0xFF9A9A9A; }
            default -> { fill = BTN_FILL;       border = BTN_BORDER;        textColor = buttonText(false); }
        }
        roundedBox(ctx, x, y, w, h, fill, border);
        ctx.fill(x + RADIUS, y + 1, x + w - RADIUS, y + 2, CraftConfig.dark ? 0x1AFFFFFF : 0x22FFFFFF);
        if (state == 2) {
            roundedRect(ctx, x + 3, y + 4, 3, h - 8, CraftConfig.dark ? 0xFFFFD0D0 : 0xFFFFF0F0);
        }
        ctx.drawCenteredTextWithShadow(tr, label, x + w / 2, y + (h - 8) / 2, textColor);
    }

    private static int buttonText(boolean active) {
        if (CraftConfig.dark) return 0xFFFFFFFF;
        return active ? 0xFFFFFFFF : TEXT;
    }
}
