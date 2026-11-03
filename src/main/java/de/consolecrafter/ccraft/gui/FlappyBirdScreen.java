package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappyBirdScreen extends Screen {

    private final Screen parent;
    private FlappyGame game;

    public FlappyBirdScreen(Screen parent) {
        super(Text.literal("Flappy Bird"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (game == null) game = new FlappyGame();

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> {
            game.saveHighScore();
            client.setScreen(parent);
        }).position(this.width - 110, this.height - 30).size(100, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (game != null) game.update();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Theme.header(context, textRenderer, "Flappy Bird", GameTutorialRenderer.playfieldCenterX(this.width));
        if (game != null) game.render(context);
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "Leertaste / W / ↑ = Fliegen", "P = Pause", "Game Over: Fliegen = Neustart", "ESC = Zurück",
            "Weiche den Rohren aus", "Mit jedem Punkt wirst du schneller");
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (game != null && game.handleKey(key)) return true;
        if (key == 256) {
            game.saveHighScore();
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void removed() {
        super.removed();
        if (game != null) game.saveHighScore();
        game = null;
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Flappy Game ───────────────────────────────────────────────────────────

    private class FlappyGame {

        // Virtuelles Spielfeld
        private static final int VW = 300;
        private static final int VH = 240;

        // Vogel
        private float by  = VH / 2f;
        private float vel = 0f;
        private static final float GRAVITY   = 0.38f;
        private static final float FLAP_VEL  = -5.5f;
        private static final int   BIRD_X    = 60;  // feste X-Position
        private static final int   BIRD_SIZE = 12;

        // Rohre
        private static final int PIPE_W    = 24;
        private static final int PIPE_GAP  = 72;
        private static final int PIPE_DIST = 110;
        private float pipeSpeed = 1.8f;

        private final List<int[]> pipes = new ArrayList<>(); // {x, gapY}
        private final Random rng = new Random();

        private boolean started  = false;
        private boolean gameOver = false;
        private boolean paused   = false;
        private int score        = 0;

        private static int     highScore = 0;
        private static boolean loadedHS  = false;
        private static File    HS_FILE;

        public FlappyGame() {
            loadHighScore();
            spawnInitialPipes();
        }

        private void spawnInitialPipes() {
            pipes.clear();
            // Erste Rohre starten INNERHALB des Spielfelds, mit Abstand zum Vogel
            for (int i = 0; i < 3; i++) {
                int x    = VW / 2 + i * PIPE_DIST; // beginnt bei der Hälfte
                int gapY = 40 + rng.nextInt(VH - 80 - PIPE_GAP);
                pipes.add(new int[]{x, gapY});
            }
        }

        public void flap() {
            if (gameOver) { restart(); return; }
            if (!started) started = true;
            if (paused) return;
            vel = FLAP_VEL;
        }

        public void update() {
            if (!started || paused || gameOver) return;

            vel += GRAVITY;
            by  += vel;

            // Boden / Decke
            if (by <= 0 || by + BIRD_SIZE >= VH) {
                triggerGameOver();
                return;
            }

            // Rohre bewegen
            for (int[] pipe : pipes) {
                pipe[0] -= (int) Math.ceil(pipeSpeed);

                // Score: Vogel hat Rohr passiert
                if (pipe[0] + PIPE_W < BIRD_X && pipe[0] + PIPE_W + (int) Math.ceil(pipeSpeed) >= BIRD_X) {
                    score++;
                    if (score > highScore) highScore = score;
                    pipeSpeed = 1.8f + score * 0.07f;
                }
            }

            // Alte Rohre entfernen
            pipes.removeIf(p -> p[0] + PIPE_W < 0);

            // Neue Rohre spawnen — immer innerhalb des Spielfelds
            while (pipes.size() < 3) {
                int lastX = pipes.isEmpty() ? VW : pipes.get(pipes.size() - 1)[0];
                int gapY  = 40 + rng.nextInt(VH - 80 - PIPE_GAP);
                pipes.add(new int[]{lastX + PIPE_DIST, gapY});
            }

            // Kollision
            for (int[] pipe : pipes) {
                if (collidesWithPipe(pipe)) {
                    triggerGameOver();
                    return;
                }
            }
        }

        private boolean collidesWithPipe(int[] pipe) {
            int px   = pipe[0];
            int gapY = pipe[1];
            // Nur Rohre prüfen die sichtbar im Spielfeld sind
            if (px > VW || px + PIPE_W < 0) return false;

            float bLeft  = BIRD_X + 1;
            float bRight = BIRD_X + BIRD_SIZE - 1;
            float bTop   = by + 1;
            float bBot   = by + BIRD_SIZE - 1;

            if (bRight < px || bLeft > px + PIPE_W) return false;
            return bTop < gapY || bBot > gapY + PIPE_GAP;
        }

        private void triggerGameOver() {
            gameOver = true;
            if (score > highScore) highScore = score;
        }

        private void restart() {
            by = VH / 2f; vel = 0f;
            score = 0; pipeSpeed = 1.8f;
            gameOver = false; started = false; paused = false;
            spawnInitialPipes();
        }

        public void render(DrawContext context) {
            // Spielfeld: links Platz für Steuerung lassen
            int leftPad  = GameTutorialRenderer.RAIL_WIDTH + GameTutorialRenderer.PLAYFIELD_GAP;
            int rightPad = 20;
            int topPad   = 20;
            int botPad   = 35;

            int availW = width  - leftPad - rightPad;
            int availH = height - topPad  - botPad;

            float scale = Math.min((float) availW / VW, (float) availH / VH);
            int rw = (int)(VW * scale);
            int rh = (int)(VH * scale);
            int ox = leftPad + (availW - rw) / 2;
            int oy = topPad  + (availH - rh) / 2;

            // Spielfeld-Hintergrund (Himmel)
            context.fill(ox, oy, ox + rw, oy + rh, 0xFF87CEEB);

            // Gitter (dezent, wie Tetris/Snake)
            for (int x = 0; x <= rw; x += (int)(20 * scale))
                context.fill(ox + x, oy, ox + x + 1, oy + rh, 0x11000000);
            for (int y = 0; y <= rh; y += (int)(20 * scale))
                context.fill(ox, oy + y, ox + rw, oy + y + 1, 0x11000000);

            // Boden
            int groundH = Math.max(4, (int)(10 * scale));
            context.fill(ox, oy + rh - groundH, ox + rw, oy + rh, 0xFF5A3E1B);
            context.fill(ox, oy + rh - groundH, ox + rw, oy + rh - groundH + Math.max(2, (int)(3 * scale)), 0xFF7CBA3B);

            // Spielfeld-Rand
            context.fill(ox - 1, oy - 1, ox + rw + 1, oy, 0xFF55FFFF);
            context.fill(ox - 1, oy + rh, ox + rw + 1, oy + rh + 1, 0xFF55FFFF);
            context.fill(ox - 1, oy, ox, oy + rh, 0xFF55FFFF);
            context.fill(ox + rw, oy, ox + rw + 1, oy + rh, 0xFF55FFFF);

            // Rohre — nur innerhalb des Spielfelds zeichnen
            for (int[] pipe : pipes) {
                int px   = ox + (int)(pipe[0] * scale);
                int gapY = pipe[1];
                int pw   = (int)(PIPE_W * scale);
                int topH = (int)(gapY * scale);
                int botY = (int)((gapY + PIPE_GAP) * scale);
                int capH = Math.max(3, (int)(6 * scale));

                // Nur zeichnen wenn im Spielfeld sichtbar
                if (px + pw < ox || px > ox + rw) continue;

                // Oberes Rohr
                context.fill(px, oy, px + pw, oy + topH, 0xFF228B22);
                context.fill(px - 2, oy + topH - capH, px + pw + 2, oy + topH, 0xFF1A6B1A);

                // Unteres Rohr
                context.fill(px, oy + botY, px + pw, oy + rh - groundH, 0xFF228B22);
                context.fill(px - 2, oy + botY, px + pw + 2, oy + botY + capH, 0xFF1A6B1A);
            }

            // Vogel (Creeper)
            int bsx  = ox + (int)(BIRD_X * scale);
            int bsy  = oy + (int)(by * scale);
            int bssz = Math.max(8, (int)(BIRD_SIZE * scale));

            context.fill(bsx, bsy, bsx + bssz, bsy + bssz, 0xFF3CB371);
            int ew = Math.max(2, bssz / 4);
            // Augen
            context.fill(bsx + bssz/4,   bsy + bssz/4,   bsx + bssz/4 + ew,   bsy + bssz/4 + ew,   0xFF000000);
            context.fill(bsx + bssz*2/3, bsy + bssz/4,   bsx + bssz*2/3 + ew, bsy + bssz/4 + ew,   0xFF000000);
            // Mund
            context.fill(bsx + bssz/4,   bsy + bssz*2/3, bsx + bssz*3/4,      bsy + bssz*2/3 + ew, 0xFF000000);

            // Info-Panel rechts vom Spielfeld
            int ix = ox + rw + 8;
            int iy = oy + 4;
            int lh = textRenderer.fontHeight + 4;

            context.drawText(textRenderer, "Highscore", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(highScore), ix, iy, 0xFFFFFF00, true);
            iy += lh + 4;

            context.drawText(textRenderer, "Score", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(score), ix, iy, 0xFFFF8800, true);
            iy += lh + 4;

            context.drawText(textRenderer, "Speed", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.format("%.1f", pipeSpeed), ix, iy, 0xFF00AAFF, true);

            // Overlays
            if (!started && !gameOver) {
                context.fill(ox, oy, ox + rw, oy + rh, 0x88000000);
                context.drawCenteredTextWithShadow(textRenderer, "§e§lFlappy Creeper", ox + rw/2, oy + rh/2 - 16, 0xFFFFFF00);
                context.drawCenteredTextWithShadow(textRenderer, "§7Leertaste / W zum Starten", ox + rw/2, oy + rh/2,  0xFFAAAAAA);
            }

            if (paused) {
                context.fill(ox, oy, ox + rw, oy + rh, 0x88000000);
                context.drawCenteredTextWithShadow(textRenderer, "§e§lPAUSIERT",       ox + rw/2, oy + rh/2 - 8, 0xFFFFFF00);
                context.drawCenteredTextWithShadow(textRenderer, "§7P zum Fortfahren", ox + rw/2, oy + rh/2 + 6, 0xFFAAAAAA);
            }

            if (gameOver) {
                context.fill(ox, oy, ox + rw, oy + rh, 0x88000000);
                context.drawCenteredTextWithShadow(textRenderer, "§c§lGAME OVER",          ox + rw/2, oy + rh/2 - 18, 0xFFFF3333);
                context.drawCenteredTextWithShadow(textRenderer, "§eScore: §f" + score,     ox + rw/2, oy + rh/2 - 4,  0xFFFFFFFF);
                context.drawCenteredTextWithShadow(textRenderer, "§7Leertaste = Neustart",  ox + rw/2, oy + rh/2 + 10, 0xFFAAAAAA);
            }
        }

        public boolean handleKey(int key) {
            switch (key) {
                case 32, 87, 265 -> { flap(); return true; }
                case 80 -> {
                    if (!gameOver && started) paused = !paused;
                    return true;
                }
            }
            return false;
        }

        private void loadHighScore() {
            if (loadedHS) return;
            loadedHS = true;
            HS_FILE = new File(MinecraftClient.getInstance().runDirectory, "flappyHighscore.txt");
            if (!HS_FILE.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(HS_FILE))) {
                String line = br.readLine();
                if (line != null) highScore = Integer.parseInt(line.trim());
            } catch (Exception ignored) {}
        }

        public void saveHighScore() {
            if (score > highScore) highScore = score;
            if (HS_FILE == null) return;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(HS_FILE))) {
                bw.write(String.valueOf(highScore));
            } catch (IOException ignored) {}
        }
    }
}
