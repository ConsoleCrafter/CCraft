package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DoodleJumpScreen extends GameScreen {

    private static final int VW = 200;
    private static final int VH = 300;
    private static final int PW = 16;
    private static final int PH = 16;
    private static final float GRAVITY = 0.35f;
    private static final float JUMP_VELOCITY = -8f;
    private static final float BOOST_VELOCITY = -13f;
    private static final float MOVE_SPEED = 2.5f;
    private static final int PLATFORM_W = 30;
    private static final int PLATFORM_H = 5;
    private static final float PLATFORM_SPEED = 0.8f;

    private enum Type { NORMAL, MOVING, SPRING, CRUMBLING }

    private static final class Platform {
        float x;
        final float y;
        final Type type;
        float direction = 1;
        boolean broken;

        Platform(float x, float y, Type type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    private final Random random = new Random();
    private final List<Platform> platforms = new ArrayList<>();

    private float px;
    private float py;
    private float vx;
    private float vy;
    private float cameraY;
    private int score;
    private boolean left;
    private boolean right;
    private boolean paused;
    private boolean gameOver;
    private boolean started;

    public DoodleJumpScreen(Screen parent) {
        super("Doodle Jump", "doodle", parent);
        restart();
    }

    @Override
    protected String[] instructions() {
        return new String[]{"A / ← = Links", "D / → = Rechts", "P = Pause", "Leertaste = Start / Neustart",
                "ESC = Zurück", "Grün = Normal", "Blau = Bewegend", "Gold = Feder x2", "Rot = Zerbricht"};
    }

    @Override
    protected int score() {
        return score;
    }

    private void restart() {
        saveScore();
        gameOver = false;
        paused = false;
        started = false;
        left = false;
        right = false;
        score = 0;
        cameraY = 0;
        vx = 0;
        platforms.clear();
        Platform start = new Platform(VW / 2f - PLATFORM_W / 2f, VH - 40f, Type.NORMAL);
        platforms.add(start);
        for (float y = VH - 80f; y > -VH; y -= 28 + random.nextInt(20)) spawn(y);
        px = start.x + PLATFORM_W / 2f - PW / 2f;
        py = start.y - PH;
        vy = JUMP_VELOCITY;
    }

    private void spawn(float y) {
        float difficulty = Math.min(1f, score / 80f);
        float roll = random.nextFloat();
        Type type = roll < 0.08f + difficulty * 0.12f ? Type.CRUMBLING
                : roll < 0.18f + difficulty * 0.15f ? Type.MOVING
                : roll < 0.25f ? Type.SPRING
                : Type.NORMAL;
        platforms.add(new Platform(random.nextInt(VW - PLATFORM_W), y, type));
    }

    @Override
    public void tick() {
        if (!started || paused || gameOver) return;

        vx = left ? -MOVE_SPEED : right ? MOVE_SPEED : vx * 0.7f;
        px += vx;
        if (px + PW < 0) px = VW;
        if (px > VW) px = -PW;

        vy += GRAVITY;
        py += vy;

        float line = cameraY + VH / 3f;
        if (py < line) cameraY -= line - py;
        score = Math.max(score, (int) (-cameraY / 10));

        for (Platform p : platforms) {
            if (p.type != Type.MOVING) continue;
            p.x += PLATFORM_SPEED * p.direction;
            if (p.x <= 0 || p.x + PLATFORM_W >= VW) p.direction = -p.direction;
        }

        if (vy > 0) {
            for (Platform p : platforms) {
                boolean overlapX = px + PW > p.x && px < p.x + PLATFORM_W;
                boolean landing = py + PH >= p.y && py + PH <= p.y + PLATFORM_H + vy + 2;
                if (p.broken || !overlapX || !landing) continue;
                py = p.y - PH;
                vy = p.type == Type.SPRING ? BOOST_VELOCITY : JUMP_VELOCITY;
                if (p.type == Type.CRUMBLING) p.broken = true;
                break;
            }
        }

        platforms.removeIf(p -> p.broken || p.y > cameraY + VH + 20);
        float top = platforms.stream().map(p -> p.y).min(Float::compare).orElse(cameraY);
        while (top > cameraY - 20) {
            top -= 28 + random.nextInt(20);
            spawn(top);
        }

        if (py - cameraY > VH + 20) gameOver = true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_A, InputConstants.KEY_LEFT -> {
                left = true;
                started = true;
            }
            case InputConstants.KEY_D, InputConstants.KEY_RIGHT -> {
                right = true;
                started = true;
            }
            case InputConstants.KEY_P -> {
                if (started && !gameOver) paused = !paused;
            }
            case InputConstants.KEY_SPACE -> {
                if (gameOver) restart();
                else started = true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_A, InputConstants.KEY_LEFT -> left = false;
            case InputConstants.KEY_D, InputConstants.KEY_RIGHT -> right = false;
            default -> {
                return super.keyReleased(event);
            }
        }
        return true;
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int leftEdge = RAIL_WIDTH + PLAYFIELD_GAP;
        int availW = width - leftEdge - 20;
        int availH = height - 55;
        float scale = Math.min((float) availW / VW, (float) availH / VH);
        int rw = (int) (VW * scale);
        int rh = (int) (VH * scale);
        int ox = leftEdge + (availW - rw) / 2;
        int oy = 20 + (availH - rh) / 2;

        g.fill(ox, oy, ox + rw, oy + rh, 0xFF0A0A2A);
        for (int i = 0; i < 30; i++) {
            int sx = ox + (i * 37 + 13) % Math.max(1, rw);
            int sy = oy + (i * 53 + 7) % Math.max(1, rh);
            g.fill(sx, sy, sx + 1, sy + 1, 0xAAFFFFFF);
        }
        Theme.outline(g, ox - 1, oy - 1, rw + 2, rh + 2, 0xFF55FFFF);

        g.enableScissor(ox, oy, ox + rw, oy + rh);
        for (Platform p : platforms) {
            float screenY = p.y - cameraY;
            if (screenY < -PLATFORM_H || screenY > VH + PLATFORM_H) continue;
            int x = ox + (int) (p.x * scale);
            int y = oy + (int) (screenY * scale);
            int w = (int) (PLATFORM_W * scale);
            int h = Math.max(3, (int) (PLATFORM_H * scale));
            int[] colors = switch (p.type) {
                case MOVING -> new int[]{0xFF4466FF, 0xFF2244CC};
                case SPRING -> new int[]{0xFFFFAA00, 0xFFCC8800};
                case CRUMBLING -> new int[]{0xFFCC3333, 0xFF881111};
                case NORMAL -> new int[]{0xFF44BB44, 0xFF228822};
            };
            g.fill(x, y, x + w, y + h, colors[0]);
            g.fill(x, y, x + w, y + Math.max(1, h / 3), colors[1]);
            if (p.type == Type.SPRING) {
                int springX = x + w / 2 - 1;
                g.fill(springX, y - (int) (8 * scale), springX + Math.max(1, (int) (3 * scale)), y, 0xFFFFFFFF);
            }
        }

        int sx = ox + (int) (px * scale);
        int sy = oy + (int) ((py - cameraY) * scale);
        int sw = Math.max(8, (int) (PW * scale));
        int sh = Math.max(8, (int) (PH * scale));
        int eye = Math.max(2, sw / 5);
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF44DD44);
        g.fill(sx + sw / 4, sy + sh / 4, sx + sw / 4 + eye, sy + sh / 4 + eye, 0xFF000000);
        g.fill(sx + sw * 3 / 5, sy + sh / 4, sx + sw * 3 / 5 + eye, sy + sh / 4 + eye, 0xFF000000);
        g.fill(sx + sw / 4, sy + sh * 2 / 3, sx + sw * 3 / 4, sy + sh * 2 / 3 + Math.max(1, eye / 2), 0xFF000000);
        g.disableScissor();

        int ix = ox + rw + 8;
        int iy = oy + 4;
        int step = font.lineHeight * 2 + 9;
        stat(g, "Highscore", String.valueOf(highscore()), ix, iy, 0xFFFFFF00);
        stat(g, "Score", String.valueOf(score), ix, iy + step, 0xFFFF8800);
        stat(g, "Höhe", (score / 10) + "m", ix, iy + step * 2, 0xFF00AAFF);

        if (!started) overlay(g, ox, oy, rw, rh, 0x88, "§a§lDoodle Jump", "§7A/D zum Starten");
        if (paused) overlay(g, ox, oy, rw, rh, 0x88, "§e§lPAUSIERT", "§7P zum Fortfahren");
        if (gameOver) overlay(g, ox, oy, rw, rh, 0x88, "§c§lGAME OVER", "§eScore: §f" + score, "§7Leertaste = Neustart");
    }
}
