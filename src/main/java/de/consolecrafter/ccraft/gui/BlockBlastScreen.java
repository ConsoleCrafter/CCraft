package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Block Blast: 8x8-Raster, zufällige Blöcke per Drag & Drop platzieren.
 * Volle Reihen/Spalten verschwinden und geben Punkte.
 */
public class BlockBlastScreen extends Screen {

    private final Screen parent;
    private BlockBlastGame game;

    public BlockBlastScreen(Screen parent) {
        super(Text.literal("Block Blast"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (game == null) game = new BlockBlastGame();

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> {
            game.saveHighScore();
            client.setScreen(parent);
        }).position(this.width - 110, this.height - 30).size(100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (game != null) game.render(context, mouseX, mouseY);
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "Ziehe Blöcke ins Raster", "Fülle Reihen oder Spalten", "Maus = Platzieren",
            "Game Over: R = Neustart", "ESC = Zurück");
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (game != null && game.mouseDown(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (game != null && game.mouseDrag(click.x(), click.y())) return true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (game != null && game.mouseUp(click.x(), click.y())) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (key == 256) { // ESC
            game.saveHighScore();
            client.setScreen(parent);
            return true;
        }
        if (key == 82 && game != null && game.gameOver) { // R
            game.restart();
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

    // ── Spiel ─────────────────────────────────────────────────────────────────

    private class BlockBlastGame {

        private static final int N = 8;          // Rastergröße
        private static final int MAX_TRIES = 3;

        private final int[][] grid = new int[N][N];

        private final List<int[][]> shapes = new ArrayList<>();
        private final int[] colors = new int[MAX_TRIES];

        private int score = 0;
        private boolean gameOver = false;

        private int dragIndex = -1;
        private float dragX, dragY;
        private float grabX, grabY;

        private static int highScore = 0;
        private static boolean loadedHS = false;
        private static File HS_FILE;

        private final Random rng = new Random();

        BlockBlastGame() {
            loadHighScore();
            restart();
        }

        void restart() {
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++) grid[x][y] = 0;
            score = 0;
            gameOver = false;
            shapes.clear();
            for (int i = 0; i < MAX_TRIES; i++) {
                shapes.add(randomShape());
                colors[i] = randomColor();
            }
            dragIndex = -1;
            if (!anyMovePossible()) gameOver = true;
        }

        private int[][] randomShape() {
            int r = rng.nextInt(13);
            return switch (r) {
                case 0  -> new int[][]{{0, 0}};                          // 1x1
                case 1  -> new int[][]{{0, 0}, {1, 0}};                  // 2x1
                case 2  -> new int[][]{{0, 0}, {0, 1}};                  // 1x2
                case 3  -> new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};  // 2x2
                case 4  -> new int[][]{{0, 0}, {1, 0}, {2, 0}};          // 3x1
                case 5  -> new int[][]{{0, 0}, {0, 1}, {0, 2}};          // 1x3
                case 6  -> new int[][]{{0, 0}, {1, 0}, {2, 0}, {3, 0}};  // 4x1
                case 7  -> new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}};  // 1x4
                case 8  -> new int[][]{{0, 0}, {1, 0}, {0, 1}};          // L
                case 9  -> new int[][]{{0, 0}, {1, 0}, {1, 1}};          // J
                case 10 -> new int[][]{{0, 0}, {1, 0}, {2, 0}, {2, 1}};  // L groß
                case 11 -> new int[][]{{0, 1}, {1, 1}, {2, 1}, {2, 0}};  // J groß
                default -> new int[][]{{0, 0}, {1, 0}, {1, 1}, {2, 1}};  // S
            };
        }

        private int randomColor() {
            int[] palette = {0xFFE74C3C, 0xFF3498DB, 0xFF2ECC71, 0xFFF1C40F, 0xFF9B59B6, 0xFFE67E22};
            return palette[rng.nextInt(palette.length)];
        }

        // ── Layout ────────────────────────────────────────────────────────────

        private int cellSize() {
            int horizontalAvail = width - GameTutorialRenderer.RAIL_WIDTH - GameTutorialRenderer.PLAYFIELD_GAP - 20;
            int trayCellSize = (horizontalAvail - (MAX_TRIES - 1) * 10) / (MAX_TRIES * 5);
            int boardCellSize = (horizontalAvail - 20) / N;
            int verticalCellSize = (height - 120) / N;
            return Math.max(8, Math.min(30,
                    Math.min(trayCellSize, Math.min(boardCellSize, verticalCellSize))));
        }

        private int boardX() { return GameTutorialRenderer.centeredPlayfieldX(width, N * cellSize()); }
        private int boardY() { return height / 2 - (N * cellSize()) / 2 - 20; }

        private int trayY() { return boardY() + N * cellSize() + 16; }

        private int traySlotX(int index) {
            int cs = cellSize();
            int slotW = 5 * cs;
            int totalW = MAX_TRIES * slotW + (MAX_TRIES - 1) * 10;
            int startX = GameTutorialRenderer.centeredPlayfieldX(width, totalW);
            return startX + index * (slotW + 10);
        }

        // ── Interaktion ───────────────────────────────────────────────────────

        boolean mouseDown(double mx, double my, int button) {
            if (button != 0 || gameOver) return false;
            for (int i = 0; i < shapes.size(); i++) {
                if (shapes.get(i) == null) continue;
                int sx = traySlotX(i);
                int sy = trayY();
                int slotW = 5 * cellSize();
                int slotH = 5 * cellSize();
                if (mx >= sx && mx <= sx + slotW && my >= sy && my <= sy + slotH) {
                    dragIndex = i;
                    dragX = (float) mx;
                    dragY = (float) my;
                    grabX = (float) mx;
                    grabY = (float) my;
                    return true;
                }
            }
            return false;
        }

        boolean mouseDrag(double mx, double my) {
            if (dragIndex < 0) return false;
            dragX = (float) mx;
            dragY = (float) my;
            return true;
        }

        boolean mouseUp(double mx, double my) {
            if (dragIndex < 0) return false;

            int[][] shape = shapes.get(dragIndex);
            int cs = cellSize();

            // Cursorposition = linke obere Ecke des Shapes
            int col = Math.round((dragX - boardX()) / (float) cs);
            int row = Math.round((dragY - boardY()) / (float) cs);

            if (canPlace(shape, col, row)) {
                place(shape, col, row, colors[dragIndex]);
                shapes.set(dragIndex, null);
                colors[dragIndex] = 0;
                if (allUsed()) refill();
                if (!anyMovePossible()) gameOver = true;
            }

            dragIndex = -1;
            return true;
        }

        private boolean allUsed() {
            for (int[][] s : shapes) if (s != null) return false;
            return true;
        }

        private void refill() {
            for (int i = 0; i < MAX_TRIES; i++) {
                shapes.set(i, randomShape());
                colors[i] = randomColor();
            }
        }

        // ── Regeln ────────────────────────────────────────────────────────────

        private boolean canPlace(int[][] shape, int col, int row) {
            for (int[] c : shape) {
                int x = col + c[0];
                int y = row + c[1];
                if (x < 0 || x >= N || y < 0 || y >= N) return false;
                if (grid[x][y] != 0) return false;
            }
            return true;
        }

        private void place(int[][] shape, int col, int row, int color) {
            for (int[] c : shape) grid[col + c[0]][row + c[1]] = color;
            score += shape.length;
            clearLines();
            if (score > highScore) highScore = score;
        }

        private void clearLines() {
            int cleared = 0;

            for (int y = 0; y < N; y++) {
                boolean full = true;
                for (int x = 0; x < N; x++) if (grid[x][y] == 0) { full = false; break; }
                if (full) {
                    for (int x = 0; x < N; x++) grid[x][y] = 0;
                    cleared++;
                }
            }
            for (int x = 0; x < N; x++) {
                boolean full = true;
                for (int y = 0; y < N; y++) if (grid[x][y] == 0) { full = false; break; }
                if (full) {
                    for (int y = 0; y < N; y++) grid[x][y] = 0;
                    cleared++;
                }
            }

            if (cleared > 0) {
                score += cleared * 10 * (cleared + 1);
                if (score > highScore) highScore = score;
            }
        }

        private boolean anyMovePossible() {
            for (int i = 0; i < shapes.size(); i++) {
                int[][] shape = shapes.get(i);
                if (shape == null) continue;
                for (int col = 0; col < N; col++) {
                    for (int row = 0; row < N; row++) {
                        if (canPlace(shape, col, row)) return true;
                    }
                }
            }
            return false;
        }

        // ── Rendering ─────────────────────────────────────────────────────────

        void render(DrawContext context, int mouseX, int mouseY) {
            int cs = cellSize();
            int bx = boardX();
            int by = boardY();

            Theme.header(context, textRenderer, "Block Blast", GameTutorialRenderer.playfieldCenterX(width));

            // Raster-Hintergrund
            context.fill(bx - 4, by - 4, bx + N * cs + 4, by + N * cs + 4, 0xCC0C0C12);
            context.fill(bx - 4, by - 4, bx + N * cs + 4, by - 3, Theme.BORDER);

            for (int x = 0; x < N; x++) {
                for (int y = 0; y < N; y++) {
                    int px = bx + x * cs;
                    int py = by + y * cs;
                    int color = grid[x][y];
                    if (color == 0) {
                        context.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, 0x33FFFFFF);
                    } else {
                        context.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, color);
                        context.fill(px + 1, py + 1, px + cs - 1, py + 3, 0x66FFFFFF);
                    }
                }
            }

            // Vorschau beim Ziehen
            if (dragIndex >= 0) {
                int[][] shape = shapes.get(dragIndex);
                int col = Math.round((dragX - bx) / (float) cs);
                int row = Math.round((dragY - by) / (float) cs);

                if (canPlace(shape, col, row)) {
                    for (int[] c : shape) {
                        int px = bx + (col + c[0]) * cs;
                        int py = by + (row + c[1]) * cs;
                        context.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, colors[dragIndex]);
                    }
                }

                // Am Cursor mitziehen
                int shapeMinX = Integer.MAX_VALUE, shapeMinY = Integer.MAX_VALUE;
                for (int[] c : shape) {
                    shapeMinX = Math.min(shapeMinX, c[0]);
                    shapeMinY = Math.min(shapeMinY, c[1]);
                }
                for (int[] c : shape) {
                    int px = (int) (dragX + (c[0] - shapeMinX) * cs);
                    int py = (int) (dragY + (c[1] - shapeMinY) * cs);
                    context.fill(px, py, px + cs, py + cs, colors[dragIndex]);
                }
            }

            // Tray mit den 3 Blöcken
            for (int i = 0; i < MAX_TRIES; i++) {
                int[][] shape = shapes.get(i);
                if (shape == null) continue;
                int sx = traySlotX(i);
                int sy = trayY();
                int slotW = 5 * cs;
                int slotH = 5 * cs;
                context.fill(sx, sy, sx + slotW, sy + slotH, 0x33000000);

                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                for (int[] c : shape) {
                    minX = Math.min(minX, c[0]);
                    minY = Math.min(minY, c[1]);
                }
                int shapeW = 0, shapeH = 0;
                for (int[] c : shape) {
                    shapeW = Math.max(shapeW, c[0] - minX + 1);
                    shapeH = Math.max(shapeH, c[1] - minY + 1);
                }
                int offX = sx + (slotW - shapeW * cs) / 2;
                int offY = sy + (slotH - shapeH * cs) / 2;
                for (int[] c : shape) {
                    int px = offX + (c[0] - minX) * cs;
                    int py = offY + (c[1] - minY) * cs;
                    context.fill(px, py, px + cs - 2, py + cs - 2, colors[i]);
                }
            }

            // Punkte
            context.drawCenteredTextWithShadow(textRenderer,
                    "§ePunkte: §f" + score + "   §7Highscore: §f" + highScore,
                    GameTutorialRenderer.playfieldCenterX(width), by - 22, Theme.TEXT);

            // Game Over
            if (gameOver) {
                context.fill(bx - 4, by - 4, bx + N * cs + 4, by + N * cs + 4, 0xAA000000);
                context.drawCenteredTextWithShadow(textRenderer, "§c§lGAME OVER",
                    GameTutorialRenderer.playfieldCenterX(width), by + N * cs / 2 - 8, Theme.DANGER);
                context.drawCenteredTextWithShadow(textRenderer, "§7R = Neustart",
                    GameTutorialRenderer.playfieldCenterX(width), by + N * cs / 2 + 8, Theme.TEXT_DIM);
            }
        }

        // ── Highscore ─────────────────────────────────────────────────────────

        private void loadHighScore() {
            if (loadedHS) return;
            loadedHS = true;
            HS_FILE = new File(MinecraftClient.getInstance().runDirectory, "blockblastHighscore.txt");
            if (!HS_FILE.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(HS_FILE))) {
                String line = br.readLine();
                if (line != null) highScore = Integer.parseInt(line.trim());
            } catch (Exception ignored) {}
        }

        void saveHighScore() {
            if (score > highScore) highScore = score;
            if (HS_FILE == null) return;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(HS_FILE))) {
                bw.write(String.valueOf(highScore));
            } catch (IOException ignored) {}
        }
    }
}
