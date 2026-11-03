package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keystrokes-Overlay im Stil bekannter Mods. Sitzt oben rechts (damit der Chat
 * unten links frei bleibt). Zeigt WASD + Leertaste; LMB/RMB tragen die
 * Klickrate (CPS) direkt als Beschriftung, MMB gibt es nicht.
 *
 * <p>Layout: W über der Mitte der ASD-Reihe, S unter W. Space ist exakt so
 * breit wie A+S+D zusammen, damit alles bündig untereinander steht.
 *
 * <p>Alle Tasten sind über die Bildschirmbreite skaliert in einer festen
 * Einheit — die Breite von S passt damit exakt zur Space-Breite.
 */
public final class KeystrokesRenderer {

    private KeystrokesRenderer() {}

    private static final Deque<Long> LMB_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RMB_CLICKS = new ArrayDeque<>();
    private static boolean prevLmb, prevRmb;

    public static void render(DrawContext ctx) {
        if (!CraftConfig.keystrokes) return;

        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        var tr = client.textRenderer;

        boolean w = down(window, GLFW.GLFW_KEY_W);
        boolean a = down(window, GLFW.GLFW_KEY_A);
        boolean s = down(window, GLFW.GLFW_KEY_S);
        boolean d = down(window, GLFW.GLFW_KEY_D);
        boolean space = down(window, GLFW.GLFW_KEY_SPACE);
        boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        long now = System.currentTimeMillis();
        if (lmb && !prevLmb) LMB_CLICKS.addLast(now);
        if (rmb && !prevRmb) RMB_CLICKS.addLast(now);
        prevLmb = lmb;
        prevRmb = rmb;
        prune(LMB_CLICKS, now);
        prune(RMB_CLICKS, now);

        int key = keySize();
        int gap = gap();
        int mouseH = key;
        boolean showMouse = CraftConfig.keystrokesShowMouse;
        boolean showSpace = CraftConfig.keystrokesShowSpace;

        // Layout: Breite = 3 Tasten (A,S,D) + 2 Lücken. W sitzt mittig über den
        // ersten beiden Spalten, die untere Reihe darunter.
        int gridW = overlayWidth();
        int keyH = key;

        int baseX = positionX(ctx.getScaledWindowWidth());
        int baseY = positionY(ctx.getScaledWindowHeight());

        // W mittig über der ASD-Reihe.
        drawKey(ctx, tr, baseX + key + gap, baseY, key, keyH, "W", w);
        int rowY = baseY + keyH + gap;
        drawKey(ctx, tr, baseX, rowY, key, keyH, "A", a);
        drawKey(ctx, tr, baseX + key + gap, rowY, key, keyH, "S", s);
        drawKey(ctx, tr, baseX + (key + gap) * 2, rowY, key, keyH, "D", d);

        int cy = rowY + keyH + gap;
        if (showMouse) {
            int half = (gridW - gap) / 2;
            drawMouseKey(ctx, tr, baseX, cy, half, mouseH, "LMB", cpsLabel(LMB_CLICKS, now), lmb);
            drawMouseKey(ctx, tr, baseX + half + gap, cy, half, mouseH, "RMB", cpsLabel(RMB_CLICKS, now), rmb);
            cy += mouseH + gap;
        }
        if (showSpace) {
            drawKey(ctx, tr, baseX, cy, gridW, key, "Space", space);
        }
    }

    public static int overlayWidth() {
        int key = keySize();
        return key * 3 + gap() * 2;
    }

    public static int overlayHeight() {
        int key = keySize();
        int gap = gap();
        // Reihen: W, ASD und je optional Maus/Space. Zwischen allen Reihen liegt
        // eine Luecke — vorher wurde am Ende eine Luecke zu viel abgezogen,
        // wodurch das Overlay 4px zu tief gezogen und unten abgeschnitten wurde.
        int height = key * 2 + gap;
        if (CraftConfig.keystrokesShowMouse) height += key + gap;
        if (CraftConfig.keystrokesShowSpace) height += key + gap;
        return height;
    }

    public static int positionX(int screenWidth) {
        return Math.round(Math.max(0, screenWidth - overlayWidth()) * CraftConfig.keystrokesX);
    }

    public static int positionY(int screenHeight) {
        return Math.round(Math.max(0, screenHeight - overlayHeight()) * CraftConfig.keystrokesY);
    }

    public static void renderPreview(DrawContext ctx, int x, int y) {
        var renderer = MinecraftClient.getInstance().textRenderer;
        int key = keySize();
        int gap = gap();
        int width = overlayWidth();
        int rowY = y + key + gap;
        drawKey(ctx, renderer, x + key + gap, y, key, key, "W", false);
        drawKey(ctx, renderer, x, rowY, key, key, "A", false);
        drawKey(ctx, renderer, x + key + gap, rowY, key, key, "S", false);
        drawKey(ctx, renderer, x + (key + gap) * 2, rowY, key, key, "D", false);

        int nextY = rowY + key + gap;
        if (CraftConfig.keystrokesShowMouse) {
            int half = (width - gap) / 2;
            drawMouseKey(ctx, renderer, x, nextY, half, key, "LMB", "0", false);
            drawMouseKey(ctx, renderer, x + half + gap, nextY, half, key, "RMB", "0", false);
            nextY += key + gap;
        }
        if (CraftConfig.keystrokesShowSpace) {
            drawKey(ctx, renderer, x, nextY, width, key, "Space", false);
        }
    }

    private static int keySize() {
        return Math.max(18, CraftConfig.keystrokesSize);
    }

    private static int gap() {
        return 4;
    }

    private static boolean down(long window, int glfwKey) {
        return GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;
    }

    private static void prune(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) deque.removeFirst();
    }

    /** CPS-Zahl (Klicks der letzten Sekunde) als Beschriftung der Maustaste. */
    private static String cpsLabel(Deque<Long> deque, long now) {
        prune(deque, now);
        return String.valueOf(deque.size());
    }

    private static void drawKey(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                int x, int y, int w, int h, String text, boolean pressed) {
        drawKeyFace(ctx, x, y, w, h, pressed);
        ctx.drawCenteredTextWithShadow(tr, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private static void drawMouseKey(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                     int x, int y, int w, int h, String button, String cps, boolean pressed) {
        drawKeyFace(ctx, x, y, w, h, pressed);
        ctx.drawCenteredTextWithShadow(tr, button, x + w / 2, y + 3, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, cps, x + w / 2, y + h / 2 + 1, 0xFFFFFFFF);
    }

    private static void drawKeyFace(DrawContext ctx, int x, int y, int w, int h, boolean pressed) {
        if (CraftConfig.keystrokesStyle == 1) {
            int fill = pressed ? 0xAA9A9A9A : 0x66505050;
            int edge = pressed ? 0xBBFFFFFF : 0x66777777;
            ctx.fill(x, y, x + w, y + h, fill);
            ctx.fill(x, y, x + w, y + 1, edge);
            ctx.fill(x, y + h - 1, x + w, y + h, edge);
            ctx.fill(x, y, x + 1, y + h, edge);
            ctx.fill(x + w - 1, y, x + w, y + h, edge);
            return;
        }

        if (CraftConfig.keystrokesStyle == 2) {
            int depth = Math.max(3, Math.min(7, w / 6));
            int face = pressed ? Theme.ACCENT : 0xFF262020;
            ctx.fill(x + depth, y + depth, x + w + depth, y + h + depth, 0xFF101010);
            ctx.fill(x, y, x + w, y + h, face);
            ctx.fill(x, y, x + w, y + 2, pressed ? 0xFFFFFFFF : Theme.ACCENT);
            ctx.fill(x, y, x + 2, y + h, Theme.ACCENT);
            return;
        }

        int depth = Math.max(3, Math.min(7, w / 6));
        int face = pressed ? 0xFF858585 : 0xFF666666;
        ctx.fill(x + depth, y + depth, x + w + depth, y + h + depth, 0xFF303030);
        ctx.fill(x, y, x + w, y + h, face);
        ctx.fill(x, y, x + w, y + 2, 0xFF9A9A9A);
        ctx.fill(x, y, x + 2, y + h, 0xFF8C8C8C);
    }
}
