package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class FlappyBirdScreen extends GameScreen {

    private static final int VW = 300;
    private static final int VH = 240;
    private static final float GRAVITY = 0.38f;
    private static final float FLAP_VELOCITY = -5.5f;
    private static final int BIRD_X = 60;
    private static final int BIRD_SIZE = 12;
    private static final int PIPE_W = 24;
    private static final int PIPE_GAP = 72;
    private static final int PIPE_DISTANCE = 110;
    private static final float BASE_SPEED = 1.8f;

    private final Random random = new Random();
    private final List<int[]> pipes = new ArrayList<>();

    private float birdY;
    private float velocity;
    private float pipeSpeed;
    private boolean started;
    private boolean gameOver;
    private boolean paused;
    private int score;

    public FlappyBirdScreen(Screen parent) {
        super("Flappy Bird", "flappy", parent);
        restart();
    }

    @Override
    protected String[] instructions() {
        return new String[]{"Leertaste / W / ↑ = Fliegen", "P = Pause", "Game Over: Fliegen = Neustart",
                "ESC = Zurück", "Weiche den Rohren aus", "Mit jedem Punkt wirst du schneller"};
    }

    @Override
    protected int score() {
        return score;
    }

    private void restart() {
        saveScore();
        birdY = VH / 2f;
        velocity = 0;
        score = 0;
        pipeSpeed = BASE_SPEED;
        started = false;
        gameOver = false;
        paused = false;
        pipes.clear();
        for (int i = 0; i < 3; i++) pipes.add(new int[]{VW / 2 + i * PIPE_DISTANCE, randomGap()});
    }

    private int randomGap() {
        return 40 + random.nextInt(VH - 80 - PIPE_GAP);
    }

    private void flap() {
        if (gameOver) {
            restart();
            return;
        }
        started = true;
        if (!paused) velocity = FLAP_VELOCITY;
    }

    @Override
    public void tick() {
        if (!started || paused || gameOver) return;
        velocity += GRAVITY;
        birdY += velocity;
        if (birdY <= 0 || birdY + BIRD_SIZE >= VH) {
            gameOver = true;
            return;
        }

        int move = (int) Math.ceil(pipeSpeed);
        for (int[] pipe : pipes) {
            pipe[0] -= move;
            if (pipe[0] + PIPE_W < BIRD_X && pipe[0] + PIPE_W + move >= BIRD_X) {
                score++;
                pipeSpeed = BASE_SPEED + score * 0.07f;
            }
        }
        pipes.removeIf(pipe -> pipe[0] + PIPE_W < 0);
        while (pipes.size() < 3) {
            int lastX = pipes.isEmpty() ? VW : pipes.getLast()[0];
            pipes.add(new int[]{lastX + PIPE_DISTANCE, randomGap()});
        }
        for (int[] pipe : pipes) {
            if (hits(pipe)) {
                gameOver = true;
                return;
            }
        }
    }

    private boolean hits(int[] pipe) {
        int x = pipe[0];
        if (x > VW || x + PIPE_W < 0) return false;
        if (BIRD_X + BIRD_SIZE - 1 < x || BIRD_X + 1 > x + PIPE_W) return false;
        return birdY + 1 < pipe[1] || birdY + BIRD_SIZE - 1 > pipe[1] + PIPE_GAP;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_SPACE, InputConstants.KEY_W, InputConstants.KEY_UP -> flap();
            case InputConstants.KEY_P -> {
                if (started && !gameOver) paused = !paused;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int left = RAIL_WIDTH + PLAYFIELD_GAP;
        int availW = width - left - 20;
        int availH = height - 55;
        float scale = Math.min((float) availW / VW, (float) availH / VH);
        int rw = (int) (VW * scale);
        int rh = (int) (VH * scale);
        int ox = left + (availW - rw) / 2;
        int oy = 20 + (availH - rh) / 2;

        g.fill(ox, oy, ox + rw, oy + rh, 0xFF87CEEB);
        int grid = Math.max(1, (int) (20 * scale));
        for (int x = 0; x <= rw; x += grid) g.fill(ox + x, oy, ox + x + 1, oy + rh, 0x11000000);
        for (int y = 0; y <= rh; y += grid) g.fill(ox, oy + y, ox + rw, oy + y + 1, 0x11000000);

        int groundH = Math.max(4, (int) (10 * scale));
        g.fill(ox, oy + rh - groundH, ox + rw, oy + rh, 0xFF5A3E1B);
        g.fill(ox, oy + rh - groundH, ox + rw, oy + rh - groundH + Math.max(2, (int) (3 * scale)), 0xFF7CBA3B);

        g.enableScissor(ox, oy, ox + rw, oy + rh);
        for (int[] pipe : pipes) {
            int px = ox + (int) (pipe[0] * scale);
            int pw = (int) (PIPE_W * scale);
            int topH = (int) (pipe[1] * scale);
            int bottomY = (int) ((pipe[1] + PIPE_GAP) * scale);
            int capH = Math.max(3, (int) (6 * scale));
            g.fill(px, oy, px + pw, oy + topH, 0xFF228B22);
            g.fill(px - 2, oy + topH - capH, px + pw + 2, oy + topH, 0xFF1A6B1A);
            g.fill(px, oy + bottomY, px + pw, oy + rh - groundH, 0xFF228B22);
            g.fill(px - 2, oy + bottomY, px + pw + 2, oy + bottomY + capH, 0xFF1A6B1A);
        }
        g.disableScissor();
        Theme.outline(g, ox - 1, oy - 1, rw + 2, rh + 2, 0xFF55FFFF);

        int bx = ox + (int) (BIRD_X * scale);
        int by = oy + (int) (birdY * scale);
        int size = Math.max(8, (int) (BIRD_SIZE * scale));
        int eye = Math.max(2, size / 4);
        g.fill(bx, by, bx + size, by + size, 0xFF3CB371);
        g.fill(bx + size / 4, by + size / 4, bx + size / 4 + eye, by + size / 4 + eye, 0xFF000000);
        g.fill(bx + size * 2 / 3, by + size / 4, bx + size * 2 / 3 + eye, by + size / 4 + eye, 0xFF000000);
        g.fill(bx + size / 4, by + size * 2 / 3, bx + size * 3 / 4, by + size * 2 / 3 + eye, 0xFF000000);

        int ix = ox + rw + 8;
        int iy = oy + 4;
        int step = font.lineHeight * 2 + 9;
        stat(g, "Highscore", String.valueOf(highscore()), ix, iy, 0xFFFFFF00);
        stat(g, "Score", String.valueOf(score), ix, iy + step, 0xFFFF8800);
        stat(g, "Speed", String.format(Locale.ROOT, "%.1f", pipeSpeed), ix, iy + step * 2, 0xFF00AAFF);

        if (!started && !gameOver) overlay(g, ox, oy, rw, rh, 0x88, "§e§lFlappy Creeper", "§7Leertaste / W zum Starten");
        if (paused) overlay(g, ox, oy, rw, rh, 0x88, "§e§lPAUSIERT", "§7P zum Fortfahren");
        if (gameOver) overlay(g, ox, oy, rw, rh, 0x88, "§c§lGAME OVER", "§eScore: §f" + score, "§7Leertaste = Neustart");
    }
}
