package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.Deque;

public final class KeystrokesHud {

    private static final int GAP = 4;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

    private static boolean wasLeft;
    private static boolean wasRight;

    private KeystrokesHud() {}

    public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        ClientConfig config = ClientConfig.get();
        Minecraft minecraft = Minecraft.getInstance();
        if (!config.keystrokes || minecraft.options.hideGui) return;

        Options options = minecraft.options;
        boolean left = options.keyAttack.isDown();
        boolean right = options.keyUse.isDown();
        long now = System.currentTimeMillis();
        if (left && !wasLeft) LEFT_CLICKS.addLast(now);
        if (right && !wasRight) RIGHT_CLICKS.addLast(now);
        wasLeft = left;
        wasRight = right;
        prune(LEFT_CLICKS, now);
        prune(RIGHT_CLICKS, now);

        draw(g, minecraft.font, x(g.guiWidth()), y(g.guiHeight()),
                options.keyUp.isDown(), options.keyLeft.isDown(), options.keyDown.isDown(), options.keyRight.isDown(),
                options.keyJump.isDown(), left, right, LEFT_CLICKS.size(), RIGHT_CLICKS.size());
    }

    public static void preview(GuiGraphicsExtractor g, Font font, int x, int y) {
        draw(g, font, x, y, false, false, false, false, false, false, false, 0, 0);
    }

    public static int width() {
        return keySize() * 3 + GAP * 2;
    }

    public static int height() {
        ClientConfig config = ClientConfig.get();
        int key = keySize();
        int height = key * 2 + GAP;
        if (config.keystrokesShowMouse) height += key + GAP;
        if (config.keystrokesShowSpace) height += key + GAP;
        return height;
    }

    public static int x(int screenWidth) {
        return Math.round(Math.max(0, screenWidth - width()) * ClientConfig.get().keystrokesX);
    }

    public static int y(int screenHeight) {
        return Math.round(Math.max(0, screenHeight - height()) * ClientConfig.get().keystrokesY);
    }

    private static int keySize() {
        return Math.max(18, ClientConfig.get().keystrokesSize);
    }

    private static void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.removeFirst();
    }

    private static void draw(GuiGraphicsExtractor g, Font font, int x, int y,
                             boolean w, boolean a, boolean s, boolean d, boolean space,
                             boolean lmb, boolean rmb, int leftCps, int rightCps) {
        ClientConfig config = ClientConfig.get();
        int key = keySize();
        int gridW = width();

        key(g, font, x + key + GAP, y, key, key, "W", w);
        int row = y + key + GAP;
        key(g, font, x, row, key, key, "A", a);
        key(g, font, x + key + GAP, row, key, key, "S", s);
        key(g, font, x + (key + GAP) * 2, row, key, key, "D", d);

        int cy = row + key + GAP;
        if (config.keystrokesShowMouse) {
            int half = (gridW - GAP) / 2;
            mouse(g, font, x, cy, half, key, "LMB", leftCps, lmb);
            mouse(g, font, x + half + GAP, cy, half, key, "RMB", rightCps, rmb);
            cy += key + GAP;
        }
        if (config.keystrokesShowSpace) key(g, font, x, cy, gridW, key, "Space", space);
    }

    private static void key(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, boolean pressed) {
        face(g, x, y, w, h, pressed);
        g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private static void mouse(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, int cps, boolean pressed) {
        face(g, x, y, w, h, pressed);
        g.centeredText(font, label, x + w / 2, y + 3, 0xFFFFFFFF);
        g.centeredText(font, String.valueOf(cps), x + w / 2, y + h / 2 + 1, 0xFFFFFFFF);
    }

    private static void face(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean pressed) {
        int depth = Math.max(3, Math.min(7, w / 6));
        switch (ClientConfig.get().keystrokesStyle) {
            case 1 -> {
                g.fill(x, y, x + w, y + h, pressed ? 0xAA9A9A9A : 0x66505050);
                Theme.outline(g, x, y, w, h, pressed ? 0xBBFFFFFF : 0x66777777);
            }
            case 2 -> {
                g.fill(x + depth, y + depth, x + w + depth, y + h + depth, 0xFF101010);
                g.fill(x, y, x + w, y + h, pressed ? Theme.ACCENT : 0xFF262020);
                g.fill(x, y, x + w, y + 2, pressed ? 0xFFFFFFFF : Theme.ACCENT);
                g.fill(x, y, x + 2, y + h, Theme.ACCENT);
            }
            default -> {
                g.fill(x + depth, y + depth, x + w + depth, y + h + depth, 0xFF303030);
                g.fill(x, y, x + w, y + h, pressed ? 0xFF858585 : 0xFF666666);
                g.fill(x, y, x + w, y + 2, 0xFF9A9A9A);
                g.fill(x, y, x + 2, y + h, 0xFF8C8C8C);
            }
        }
    }
}
