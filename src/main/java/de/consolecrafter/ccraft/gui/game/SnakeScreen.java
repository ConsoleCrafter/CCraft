package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public final class SnakeScreen extends GameScreen {

    private static final int GW = 20;
    private static final int GH = 20;

    private final Random random = new Random();
    private final Deque<int[]> snake = new ArrayDeque<>();

    private int[] food;
    private int dx;
    private int dy;
    private int nextDx;
    private int nextDy;
    private int score;
    private int level;
    private int ticksPerMove;
    private int tickCounter;
    private boolean gameOver;
    private boolean paused;

    public SnakeScreen(Screen parent) {
        super("Snake", "snake", parent);
        restart();
    }

    @Override
    protected String[] instructions() {
        return new String[]{"W / ↑ = Hoch", "S / ↓ = Runter", "A / ← = Links", "D / → = Rechts",
                "P = Pause", "Game Over: Leertaste = Neustart", "ESC = Zurück"};
    }

    @Override
    protected int score() {
        return score;
    }

    private void restart() {
        saveScore();
        snake.clear();
        for (int i = 0; i < 3; i++) snake.addFirst(new int[]{GW / 2 + i, GH / 2});
        dx = nextDx = 1;
        dy = nextDy = 0;
        score = 0;
        level = 1;
        ticksPerMove = 8;
        gameOver = false;
        paused = false;
        spawnFood();
    }

    private void spawnFood() {
        if (snake.size() >= GW * GH) {
            food = null;
            gameOver = true;
            return;
        }
        do {
            food = new int[]{random.nextInt(GW), random.nextInt(GH)};
        } while (occupied(food[0], food[1]));
    }

    private boolean occupied(int x, int y) {
        for (int[] segment : snake) {
            if (segment[0] == x && segment[1] == y) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        if (paused || gameOver || ++tickCounter < ticksPerMove) return;
        tickCounter = 0;
        dx = nextDx;
        dy = nextDy;

        int[] head = snake.peekFirst();
        int nx = (head[0] + dx + GW) % GW;
        int ny = (head[1] + dy + GH) % GH;
        if (occupied(nx, ny)) {
            gameOver = true;
            return;
        }
        snake.addFirst(new int[]{nx, ny});
        if (food != null && nx == food[0] && ny == food[1]) {
            score++;
            int newLevel = 1 + score / 5;
            if (newLevel > level) {
                level = newLevel;
                ticksPerMove = Math.max(2, 8 - (level - 1));
            }
            spawnFood();
        } else {
            snake.removeLast();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (gameOver) {
            if (key == InputConstants.KEY_SPACE) {
                restart();
                return true;
            }
            return super.keyPressed(event);
        }
        if (key == InputConstants.KEY_P) {
            paused = !paused;
            return true;
        }
        if (paused) return super.keyPressed(event);
        switch (key) {
            case InputConstants.KEY_W, InputConstants.KEY_UP -> turn(0, -1);
            case InputConstants.KEY_S, InputConstants.KEY_DOWN -> turn(0, 1);
            case InputConstants.KEY_A, InputConstants.KEY_LEFT -> turn(-1, 0);
            case InputConstants.KEY_D, InputConstants.KEY_RIGHT -> turn(1, 0);
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    private void turn(int x, int y) {
        if (x != -dx || y != -dy) {
            nextDx = x;
            nextDy = y;
        }
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int cs = Math.max(4, Math.min((width - 40 - RAIL_WIDTH - PLAYFIELD_GAP) / GW, (height - 40) / GH));
        int bw = GW * cs;
        int bh = GH * cs;
        int sx = centeredPlayfieldX(bw);
        int sy = (height - bh) / 2;

        g.fill(sx, sy, sx + bw, sy + bh, 0xFF111111);
        for (int x = 0; x <= GW; x++) g.fill(sx + x * cs, sy, sx + x * cs + 1, sy + bh, 0xFF222222);
        for (int y = 0; y <= GH; y++) g.fill(sx, sy + y * cs, sx + bw, sy + y * cs + 1, 0xFF222222);

        boolean head = true;
        for (int[] segment : snake) {
            int x = sx + segment[0] * cs + 1;
            int y = sy + segment[1] * cs + 1;
            g.fill(x, y, x + cs - 1, y + cs - 1, head ? 0xFF00FF44 : 0xFF008822);
            head = false;
        }
        if (food != null) {
            g.fill(sx + food[0] * cs + 2, sy + food[1] * cs + 2, sx + food[0] * cs + cs - 2, sy + food[1] * cs + cs - 2, 0xFFFF3333);
        }

        int ix = sx + bw + 12;
        int iy = sy + 4;
        int step = font.lineHeight * 2 + 11;
        stat(g, "Highscore", String.valueOf(highscore()), ix, iy, 0xFFFFFF00);
        stat(g, "Score", String.valueOf(score), ix, iy + step, 0xFFFF8800);
        stat(g, "Level", String.valueOf(level), ix, iy + step * 2, 0xFF00AAFF);
        stat(g, "Länge", String.valueOf(snake.size()), ix, iy + step * 3, 0xFFAA00FF);

        if (paused) overlay(g, sx, sy, bw, bh, 0xA0, "§fPAUSIERT");
        else if (gameOver) overlay(g, sx, sy, bw, bh, 0xA0, "§cGAME OVER", "§7Leertaste = Neustart", "§eScore: §f" + score);
    }
}
