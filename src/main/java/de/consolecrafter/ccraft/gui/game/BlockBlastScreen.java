package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Random;

public final class BlockBlastScreen extends GameScreen {

    private static final int N = 8;
    private static final int TRAY = 3;
    private static final int[][][] SHAPES = {
            {{0, 0}},
            {{0, 0}, {1, 0}},
            {{0, 0}, {0, 1}},
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {1, 0}, {2, 0}},
            {{0, 0}, {0, 1}, {0, 2}},
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
            {{0, 0}, {0, 1}, {0, 2}, {0, 3}},
            {{0, 0}, {1, 0}, {0, 1}},
            {{0, 0}, {1, 0}, {1, 1}},
            {{0, 0}, {1, 0}, {2, 0}, {2, 1}},
            {{0, 1}, {1, 1}, {2, 1}, {2, 0}},
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}}
    };
    private static final int[] PALETTE = {0xFFE74C3C, 0xFF3498DB, 0xFF2ECC71, 0xFFF1C40F, 0xFF9B59B6, 0xFFE67E22};

    private final Random random = new Random();
    private final int[][] grid = new int[N][N];
    private final int[][][] tray = new int[TRAY][][];
    private final int[] trayColors = new int[TRAY];

    private int score;
    private boolean gameOver;
    private int dragging = -1;
    private double dragX;
    private double dragY;

    public BlockBlastScreen(Screen parent) {
        super("Block Blast", "blockblast", parent);
        restart();
    }

    @Override
    protected String[] instructions() {
        return new String[]{"Ziehe Blöcke ins Raster", "Fülle Reihen oder Spalten", "Maus = Platzieren",
                "Game Over: R = Neustart", "ESC = Zurück"};
    }

    @Override
    protected int score() {
        return score;
    }

    private void restart() {
        saveScore();
        for (int[] column : grid) java.util.Arrays.fill(column, 0);
        score = 0;
        gameOver = false;
        dragging = -1;
        refill();
    }

    private void refill() {
        for (int i = 0; i < TRAY; i++) {
            tray[i] = SHAPES[random.nextInt(SHAPES.length)];
            trayColors[i] = PALETTE[random.nextInt(PALETTE.length)];
        }
        gameOver = !anyMove();
    }

    private int cellSize() {
        int available = width - RAIL_WIDTH - PLAYFIELD_GAP - 20;
        int trayCell = (available - (TRAY - 1) * 10) / (TRAY * 5);
        int boardCell = (available - 20) / N;
        int verticalCell = (height - 120) / N;
        return Math.max(8, Math.min(30, Math.min(trayCell, Math.min(boardCell, verticalCell))));
    }

    private int boardX() {
        return centeredPlayfieldX(N * cellSize());
    }

    private int boardY() {
        return height / 2 - N * cellSize() / 2 - 20;
    }

    private int trayY() {
        return boardY() + N * cellSize() + 16;
    }

    private int trayX(int index) {
        int slot = 5 * cellSize();
        return centeredPlayfieldX(TRAY * slot + (TRAY - 1) * 10) + index * (slot + 10);
    }

    private boolean fits(int[][] shape, int col, int row) {
        for (int[] c : shape) {
            int x = col + c[0];
            int y = row + c[1];
            if (x < 0 || x >= N || y < 0 || y >= N || grid[x][y] != 0) return false;
        }
        return true;
    }

    private boolean anyMove() {
        for (int[][] shape : tray) {
            if (shape == null) continue;
            for (int col = 0; col < N; col++) {
                for (int row = 0; row < N; row++) {
                    if (fits(shape, col, row)) return true;
                }
            }
        }
        return false;
    }

    private void place(int index, int col, int row) {
        for (int[] c : tray[index]) grid[col + c[0]][row + c[1]] = trayColors[index];
        score += tray[index].length;
        tray[index] = null;

        boolean[] fullRows = new boolean[N];
        boolean[] fullCols = new boolean[N];
        int cleared = 0;
        for (int i = 0; i < N; i++) {
            boolean rowFull = true;
            boolean columnFull = true;
            for (int j = 0; j < N; j++) {
                rowFull &= grid[j][i] != 0;
                columnFull &= grid[i][j] != 0;
            }
            fullRows[i] = rowFull;
            fullCols[i] = columnFull;
            if (rowFull) cleared++;
            if (columnFull) cleared++;
        }
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (fullRows[i]) grid[j][i] = 0;
                if (fullCols[i]) grid[i][j] = 0;
            }
        }
        if (cleared > 0) score += cleared * 10 * (cleared + 1);

        boolean empty = true;
        for (int[][] shape : tray) empty &= shape == null;
        if (empty) refill();
        else gameOver = !anyMove();
    }

    private int dropColumn() {
        return (int) Math.round((dragX - boardX()) / (double) cellSize());
    }

    private int dropRow() {
        return (int) Math.round((dragY - boardY()) / (double) cellSize());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && !gameOver) {
            int slot = 5 * cellSize();
            for (int i = 0; i < TRAY; i++) {
                if (tray[i] == null) continue;
                int sx = trayX(i);
                int sy = trayY();
                if (event.x() >= sx && event.x() <= sx + slot && event.y() >= sy && event.y() <= sy + slot) {
                    dragging = i;
                    dragX = event.x();
                    dragY = event.y();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging < 0) return super.mouseDragged(event, dx, dy);
        dragX = event.x();
        dragY = event.y();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging < 0) return super.mouseReleased(event);
        if (fits(tray[dragging], dropColumn(), dropRow())) place(dragging, dropColumn(), dropRow());
        dragging = -1;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_R && gameOver) {
            restart();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int cs = cellSize();
        int bx = boardX();
        int by = boardY();
        int size = N * cs;

        g.fill(bx - 4, by - 4, bx + size + 4, by + size + 4, 0xCC0C0C12);
        g.fill(bx - 4, by - 4, bx + size + 4, by - 3, Theme.BORDER);
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                int px = bx + x * cs;
                int py = by + y * cs;
                if (grid[x][y] == 0) {
                    g.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, 0x33FFFFFF);
                } else {
                    g.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, grid[x][y]);
                    g.fill(px + 1, py + 1, px + cs - 1, py + 3, 0x66FFFFFF);
                }
            }
        }

        int slot = 5 * cs;
        for (int i = 0; i < TRAY; i++) {
            if (tray[i] == null || i == dragging) continue;
            int sx = trayX(i);
            int sy = trayY();
            g.fill(sx, sy, sx + slot, sy + slot, 0x33000000);
            int[] bounds = bounds(tray[i]);
            int offX = sx + (slot - bounds[0] * cs) / 2;
            int offY = sy + (slot - bounds[1] * cs) / 2;
            for (int[] c : tray[i]) {
                int px = offX + c[0] * cs;
                int py = offY + c[1] * cs;
                g.fill(px, py, px + cs - 2, py + cs - 2, trayColors[i]);
            }
        }

        if (dragging >= 0) {
            int[][] shape = tray[dragging];
            int col = dropColumn();
            int row = dropRow();
            if (fits(shape, col, row)) {
                for (int[] c : shape) {
                    int px = bx + (col + c[0]) * cs;
                    int py = by + (row + c[1]) * cs;
                    g.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, (trayColors[dragging] & 0xFFFFFF) | 0x88000000);
                }
            }
            for (int[] c : shape) {
                int px = (int) dragX + c[0] * cs;
                int py = (int) dragY + c[1] * cs;
                g.fill(px, py, px + cs, py + cs, trayColors[dragging]);
            }
        }

        g.centeredText(font, "§ePunkte: §f" + score + "   §7Highscore: §f" + highscore(), playfieldCenterX(), by - 22, Theme.TEXT);
        if (gameOver) overlay(g, bx - 4, by - 4, size + 8, size + 8, 0xAA, "§c§lGAME OVER", "§7R = Neustart");
    }

    private static int[] bounds(int[][] shape) {
        int w = 0;
        int h = 0;
        for (int[] c : shape) {
            w = Math.max(w, c[0] + 1);
            h = Math.max(h, c[1] + 1);
        }
        return new int[]{w, h};
    }
}
