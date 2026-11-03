package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class SnakeScreen extends Screen {

    private final Screen parent;
    private SnakeGame game;

    public SnakeScreen(Screen parent) {
        super(Text.literal("Snake"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (game == null) game = new SnakeGame();

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
        Theme.header(context, textRenderer, "Snake", GameTutorialRenderer.playfieldCenterX(this.width));
        if (game != null) game.render(context);
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "W / ↑ = Hoch", "S / ↓ = Runter", "A / ← = Links", "D / → = Rechts",
            "P = Pause", "Game Over: Leertaste = Neustart", "ESC = Zurück");
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (game != null && game.handleKey(key)) return true;
        if (key == 256) { // ESC
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

    // ── Snake Game ────────────────────────────────────────────────────────────

    private class SnakeGame {

        private static final int GW = 20, GH = 20; // Grid-Größe
        private static int highScore = 0;
        private static boolean loadedHS = false;
        private static File HS_FILE;

        private final Deque<int[]> snake = new ArrayDeque<>();
        private int[] food;
        private int dx = 1, dy = 0;   // Richtung
        private int ndx = 1, ndy = 0; // gepufferte Richtung
        private int score = 0;
        private int level = 1;
        private boolean gameOver = false;
        private boolean paused   = false;

        // Tick-Steuerung (in Minecraft-Ticks, 20/s)
        private int tickCounter = 0;
        private int ticksPerMove = 8; // je kleiner desto schneller

        private final Random rng = new Random();

        public SnakeGame() {
            loadHighScore();
            restart();
        }

        private void restart() {
            snake.clear();
            snake.addFirst(new int[]{GW / 2,     GH / 2});
            snake.addFirst(new int[]{GW / 2 + 1, GH / 2});
            snake.addFirst(new int[]{GW / 2 + 2, GH / 2});
            dx = 1; dy = 0; ndx = 1; ndy = 0;
            score = 0; level = 1; ticksPerMove = 8;
            gameOver = false; paused = false;
            spawnFood();
        }

        private void spawnFood() {
            // Kein freies Feld mehr? (Grid voll) -> Spiel gewonnen, nicht endlos suchen.
            if (snake.size() >= GW * GH) {
                food = null;
                gameOver = true;
                if (score > highScore) highScore = score;
                return;
            }
            do {
                food = new int[]{rng.nextInt(GW), rng.nextInt(GH)};
            } while (occupies(food[0], food[1]));
        }

        private boolean occupies(int x, int y) {
            for (int[] s : snake) if (s[0] == x && s[1] == y) return true;
            return false;
        }

        public void update() {
            if (paused || gameOver) return;
            tickCounter++;
            if (tickCounter < ticksPerMove) return;
            tickCounter = 0;

            // Richtung übernehmen
            dx = ndx; dy = ndy;

            int[] head = snake.peekFirst();
            int nx = (head[0] + dx + GW) % GW;
            int ny = (head[1] + dy + GH) % GH;

            // Kollision mit sich selbst
            if (occupies(nx, ny)) {
                gameOver = true;
                if (score > highScore) highScore = score;
                return;
            }

            snake.addFirst(new int[]{nx, ny});

            if (food != null && nx == food[0] && ny == food[1]) {
                score++;
                checkLevel();
                spawnFood();
            } else {
                snake.removeLast();
            }
        }

        private void checkLevel() {
            int newLevel = 1 + score / 5;
            if (newLevel > level) {
                level = newLevel;
                ticksPerMove = Math.max(2, 8 - (level - 1));
            }
        }

        public void render(DrawContext context) {
            // Spielfeld berechnen
            int margin = 40;
                int cs = Math.min((width - margin - GameTutorialRenderer.RAIL_WIDTH - GameTutorialRenderer.PLAYFIELD_GAP) / GW,
                    (height - margin) / GH);
            if (cs < 4) cs = 4;
            int bw = GW * cs, bh = GH * cs;
            int sx = GameTutorialRenderer.centeredPlayfieldX(width, bw);
            int sy = (height - bh) / 2;

            // Hintergrund
            context.fill(sx, sy, sx + bw, sy + bh, 0xFF111111);

            // Gitter (dezent)
            for (int x = 0; x <= GW; x++)
                context.fill(sx + x * cs, sy, sx + x * cs + 1, sy + bh, 0xFF222222);
            for (int y = 0; y <= GH; y++)
                context.fill(sx, sy + y * cs, sx + bw, sy + y * cs + 1, 0xFF222222);

            // Schlange
            boolean isHead = true;
            for (int[] seg : snake) {
                int px = sx + seg[0] * cs + 1;
                int py = sy + seg[1] * cs + 1;
                int color = isHead ? 0xFF00FF44 : 0xFF008822;
                context.fill(px, py, px + cs - 1, py + cs - 1, color);
                isHead = false;
            }

            // Futter (rot, leicht kleiner) — nur wenn vorhanden.
            if (food != null) {
                int fp = 2;
                context.fill(
                    sx + food[0] * cs + fp,
                    sy + food[1] * cs + fp,
                    sx + food[0] * cs + cs - fp,
                    sy + food[1] * cs + cs - fp,
                    0xFFFF3333
                );
            }

            // Info-Panel rechts
            int ix = sx + bw + 12;
            int iy = sy + 4;
            int lh = textRenderer.fontHeight + 4;

            context.drawText(textRenderer, "Highscore", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(highScore), ix, iy, 0xFFFFFF00, true);
            iy += lh + 6;

            context.drawText(textRenderer, "Score", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(score), ix, iy, 0xFFFF8800, true);
            iy += lh + 6;

            context.drawText(textRenderer, "Level", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(level), ix, iy, 0xFF00AAFF, true);
            iy += lh + 6;

            context.drawText(textRenderer, "Länge", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(snake.size()), ix, iy, 0xFFAA00FF, true);

            // Overlays
            if (paused) {
                context.fill(sx, sy, sx + bw, sy + bh, 0xA0000000);
                drawCentered(context, "PAUSIERT", 0xFFFFFFFF, sx, sy, bw, bh);
            } else if (gameOver) {
                context.fill(sx, sy, sx + bw, sy + bh, 0xA0000000);
                int midX = sx + bw / 2;
                int midY = sy + bh / 2;
                context.drawCenteredTextWithShadow(textRenderer, "§cGAME OVER", midX, midY - 10, 0xFFFF3333);
                context.drawCenteredTextWithShadow(textRenderer, "§7Leertaste = Neustart", midX, midY + 6, 0xFFAAAAAA);
                context.drawCenteredTextWithShadow(textRenderer, "§eScore: §f" + score, midX, midY + 22, 0xFFFFFFFF);
            }
        }

        private void drawCentered(DrawContext ctx, String msg, int color, int sx, int sy, int bw, int bh) {
            ctx.drawCenteredTextWithShadow(textRenderer, msg, sx + bw / 2, sy + bh / 2, color);
        }

        public boolean handleKey(int key) {
            if (gameOver) {
                if (key == 32) { restart(); return true; }
                return false;
            }
            if (paused) {
                if (key == 80) { paused = false; return true; }
                return false;
            }
            switch (key) {
                case 87, 265 -> { if (dy != 1)  { ndx =  0; ndy = -1; } return true; } // W / ↑
                case 83, 264 -> { if (dy != -1) { ndx =  0; ndy =  1; } return true; } // S / ↓
                case 65, 263 -> { if (dx != 1)  { ndx = -1; ndy =  0; } return true; } // A / ←
                case 68, 262 -> { if (dx != -1) { ndx =  1; ndy =  0; } return true; } // D / →
                case 80      -> { paused = true; return true; }                          // P
            }
            return false;
        }

        // ── Highscore persistieren ────────────────────────────────────────────

        private void loadHighScore() {
            if (loadedHS) return;
            loadedHS = true;
            HS_FILE = new File(MinecraftClient.getInstance().runDirectory, "snakeHighscore.txt");
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
