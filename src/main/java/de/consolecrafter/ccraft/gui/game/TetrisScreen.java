package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.CCraftMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;

public final class TetrisScreen extends GameScreen {

    private static final SoundEvent NORMAL_MUSIC = SoundEvent.createVariableRangeEvent(CCraftMod.id("tetris1"));
    private static final SoundEvent FAST_MUSIC = SoundEvent.createVariableRangeEvent(CCraftMod.id("tetris2"));

    private static final int BW = 10;
    private static final int BH = 20;
    private static final int CLEAR_ANIMATION_MS = 500;
    private static final int[][][] SHAPES = {
            {{-1, 0}, {0, 0}, {1, 0}, {2, 0}},
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
            {{-1, 0}, {0, 0}, {1, 0}, {0, 1}},
            {{0, 0}, {1, 0}, {-1, 1}, {0, 1}},
            {{-1, 0}, {0, 0}, {0, 1}, {1, 1}},
            {{-1, 0}, {0, 0}, {1, 0}, {1, 1}},
            {{-1, 0}, {0, 0}, {1, 0}, {-1, 1}}
    };
    private static final int[] COLORS = {
            0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF7FFF00, 0xFF00FF00, 0xFF00FF7F,
            0xFF00FFFF, 0xFF007FFF, 0xFF0000FF, 0xFF7F00FF, 0xFFFF00FF, 0xFFFF007F
    };

    private record Piece(int[][] cells, int color, boolean square) {
        Piece rotated() {
            int[][] next = new int[cells.length][2];
            for (int i = 0; i < cells.length; i++) {
                next[i][0] = cells[i][1];
                next[i][1] = -cells[i][0];
            }
            return new Piece(next, color, square);
        }
    }

    private final Random random = new Random();
    private final int[][] board = new int[BW][BH];
    private List<Integer> clearing = new ArrayList<>();

    private Piece current;
    private Piece next;
    private int px;
    private int py;
    private boolean paused;
    private boolean gameOver;
    private int score;
    private int lines;
    private int level = 1;
    private long dropInterval = 500;
    private long lastDrop = System.currentTimeMillis();
    private long lastTick = System.currentTimeMillis();
    private long roundTime;
    private int clearTimer;

    private SimpleSoundInstance music;
    private int musicTrack = -1;

    public TetrisScreen(Screen parent) {
        super("Tetris", "tetris", parent);
        next = randomPiece();
        spawn();
    }

    @Override
    protected String[] instructions() {
        return new String[]{"A = Links", "D = Rechts", "W = Runter", "S = Drop", "Leertaste / Shift = Drehen",
                "P = Pause", "Game Over: Leertaste = Neustart", "ESC = Zurück"};
    }

    @Override
    protected int score() {
        return score;
    }

    @Override
    public void removed() {
        stopMusic();
        super.removed();
    }

    private Piece randomPiece() {
        int type = random.nextInt(SHAPES.length);
        return new Piece(SHAPES[type], COLORS[random.nextInt(COLORS.length)], type == 1);
    }

    private void spawn() {
        current = next;
        px = BW / 2;
        py = 0;
        next = randomPiece();
        if (!fits(current, px, py)) gameOver = true;
    }

    private void restart() {
        for (int[] column : board) java.util.Arrays.fill(column, 0);
        saveScore();
        score = 0;
        lines = 0;
        level = 1;
        dropInterval = 500;
        gameOver = false;
        paused = false;
        clearing.clear();
        clearTimer = 0;
        roundTime = 0;
        lastTick = System.currentTimeMillis();
        next = randomPiece();
        spawn();
    }

    @Override
    public void tick() {
        updateMusic();
        long now = System.currentTimeMillis();
        long dt = now - lastTick;
        lastTick = now;
        if (paused || gameOver) return;
        roundTime += dt;

        if (clearTimer > 0) {
            clearTimer -= (int) dt;
            if (clearTimer <= 0) {
                removeLines();
                applyGravity();
                findLines();
            }
            return;
        }

        if (now - lastDrop >= dropInterval) {
            if (fits(current, px, py + 1)) {
                py++;
            } else {
                lock();
                spawn();
            }
            lastDrop = now;
        }
    }

    private boolean fits(Piece piece, int x, int y) {
        for (int[] cell : piece.cells()) {
            int cx = x + cell[0];
            int cy = y + cell[1];
            if (cx < 0 || cx >= BW || cy < 0 || cy >= BH || board[cx][cy] != 0) return false;
        }
        return true;
    }

    private void lock() {
        for (int[] cell : current.cells()) {
            int cx = px + cell[0];
            int cy = py + cell[1];
            if (cx >= 0 && cx < BW && cy >= 0 && cy < BH) board[cx][cy] = current.color();
        }
        findLines();
    }

    private void findLines() {
        List<Integer> full = new ArrayList<>();
        for (int row = 0; row < BH; row++) {
            boolean complete = true;
            for (int col = 0; col < BW && complete; col++) complete = board[col][row] != 0;
            if (complete) full.add(row);
        }
        if (!full.isEmpty()) {
            clearing = full;
            clearTimer = CLEAR_ANIMATION_MS;
        }
    }

    private void removeLines() {
        if (clearing.isEmpty()) return;
        int count = clearing.size();
        lines += count;
        int base = switch (count) {
            case 1 -> 40;
            case 2 -> 100;
            case 3 -> 300;
            default -> 1200;
        };
        score += base * level;
        int newLevel = 1 + lines / 10;
        if (newLevel > level) {
            level = newLevel;
            dropInterval = Math.max(100, 500 - (level - 1) * 16L);
        }
        for (int row : clearing) {
            for (int y = row; y > 0; y--) {
                for (int x = 0; x < BW; x++) board[x][y] = board[x][y - 1];
            }
            for (int x = 0; x < BW; x++) board[x][0] = 0;
        }
        clearing.clear();
    }

    private void applyGravity() {
        boolean[][] visited = new boolean[BW][BH];
        for (int x = 0; x < BW; x++) {
            if (board[x][BH - 1] != 0 && !visited[x][BH - 1]) flood(x, BH - 1, visited);
        }
        for (int y = BH - 1; y >= 0; y--) {
            for (int x = 0; x < BW; x++) {
                if (board[x][y] != 0 && !visited[x][y]) drop(flood(x, y, visited));
            }
        }
    }

    private List<int[]> flood(int sx, int sy, boolean[][] visited) {
        List<int[]> cluster = new ArrayList<>();
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy});
        visited[sx][sy] = true;
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            cluster.add(cell);
            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = cell[0] + dir[0];
                int ny = cell[1] + dir[1];
                if (nx >= 0 && nx < BW && ny >= 0 && ny < BH && !visited[nx][ny] && board[nx][ny] != 0) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return cluster;
    }

    private void drop(List<int[]> cluster) {
        int[] colors = new int[cluster.size()];
        for (int i = 0; i < cluster.size(); i++) {
            int[] c = cluster.get(i);
            colors[i] = board[c[0]][c[1]];
            board[c[0]][c[1]] = 0;
        }
        int distance = 0;
        while (true) {
            boolean blocked = false;
            for (int[] c : cluster) {
                int ny = c[1] + distance + 1;
                if (ny >= BH || board[c[0]][ny] != 0) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) break;
            distance++;
        }
        for (int i = 0; i < cluster.size(); i++) {
            int[] c = cluster.get(i);
            board[c[0]][c[1] + distance] = colors[i];
        }
    }

    private void updateMusic() {
        int wanted = paused || gameOver ? -1 : level >= 5 ? 1 : 0;
        if (wanted == musicTrack) return;
        stopMusic();
        if (wanted < 0) return;
        music = SimpleSoundInstance.forMusic(wanted == 1 ? FAST_MUSIC : NORMAL_MUSIC, 0.45f);
        minecraft.getSoundManager().play(music);
        musicTrack = wanted;
    }

    private void stopMusic() {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }
        musicTrack = -1;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (handleKey(event.key())) return true;
        return super.keyPressed(event);
    }

    private boolean handleKey(int key) {
        if (gameOver) {
            if (key != InputConstants.KEY_SPACE) return false;
            restart();
            return true;
        }
        if (key == InputConstants.KEY_P) {
            paused = !paused;
            return true;
        }
        if (paused) return false;
        switch (key) {
            case InputConstants.KEY_A -> {
                if (fits(current, px - 1, py)) px--;
            }
            case InputConstants.KEY_D -> {
                if (fits(current, px + 1, py)) px++;
            }
            case InputConstants.KEY_W -> {
                if (fits(current, px, py + 1)) py++;
            }
            case InputConstants.KEY_S -> {
                while (fits(current, px, py + 1)) py++;
            }
            case InputConstants.KEY_SPACE, InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> {
                if (!current.square()) {
                    Piece rotated = current.rotated();
                    if (fits(rotated, px, py)) current = rotated;
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int cs = Math.max(1, Math.min((width - 40 - RAIL_WIDTH - PLAYFIELD_GAP) / BW, (height - 40) / BH));
        int bpw = BW * cs;
        int bph = BH * cs;
        int sx = centeredPlayfieldX(bpw);
        int sy = (height - bph) / 2;

        g.fill(sx, sy, sx + bpw, sy + bph, 0xFF000000);

        float fade = clearTimer / (float) CLEAR_ANIMATION_MS;
        for (int x = 0; x < BW; x++) {
            for (int y = 0; y < BH; y++) {
                int color = board[x][y];
                if (color == 0) continue;
                if (clearing.contains(y)) color = ((int) (((color >>> 24) & 0xFF) * fade) << 24) | (color & 0xFFFFFF);
                g.fill(sx + x * cs, sy + y * cs, sx + x * cs + cs, sy + y * cs + cs, color);
            }
        }

        if (!gameOver && !paused && current != null) {
            for (int[] cell : current.cells()) {
                int x = sx + (px + cell[0]) * cs;
                int y = sy + (py + cell[1]) * cs;
                g.fill(x, y, x + cs, y + cs, current.color());
            }
        }

        for (int x = 0; x <= BW; x++) g.fill(sx + x * cs, sy, sx + x * cs + 1, sy + bph, 0xFF444444);
        for (int y = 0; y <= BH; y++) g.fill(sx, sy + y * cs, sx + bpw, sy + y * cs + 1, 0xFF444444);

        int ix = sx + bpw + 10;
        int iy = sy + 4;
        int step = font.lineHeight * 2 + 9;
        stat(g, "Highscore", String.valueOf(highscore()), ix, iy, 0xFFFFFF00);
        iy += step;

        g.text(font, "Nächstes", ix, iy, 0xFFFFFFFF, true);
        iy += font.lineHeight + 2;
        int boxW = cs * 5;
        int boxH = cs * 4;
        g.fill(ix, iy, ix + boxW, iy + boxH, 0xC0000000);
        extractPreview(g, next, ix + boxW / 2, iy + boxH / 2, cs);
        iy += boxH + 10;

        stat(g, "Level", String.valueOf(level), ix, iy, 0xFFFF0000);
        iy += step;
        stat(g, "Score", String.valueOf(score), ix, iy, 0xFFFF8000);
        iy += step;
        stat(g, "Linien", String.valueOf(lines), ix, iy, 0xFFFF00FF);
        iy += step;
        stat(g, "Zeit", formatTime(roundTime), ix, iy, 0xFF00FF00);

        if (paused) overlay(g, sx, sy, bpw, bph, 0xA0, "§fPAUSIERT");
        else if (gameOver) overlay(g, sx, sy, bpw, bph, 0xA0, "§bGAME OVER", "§9Leertaste = Neustart");
    }

    private void extractPreview(GuiGraphicsExtractor g, Piece piece, int centerX, int centerY, int cs) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] c : piece.cells()) {
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
            minY = Math.min(minY, c[1]);
            maxY = Math.max(maxY, c[1]);
        }
        int left = centerX - (maxX - minX + 1) * cs / 2;
        int top = centerY - (maxY - minY + 1) * cs / 2;
        for (int[] c : piece.cells()) {
            int x = left + (c[0] - minX) * cs;
            int y = top + (c[1] - minY) * cs;
            g.fill(x, y, x + cs, y + cs, piece.color());
        }
    }

    private static String formatTime(long ms) {
        long h = ms / 3_600_000;
        long m = ms % 3_600_000 / 60_000;
        long s = ms % 60_000 / 1000;
        long rest = ms % 1000;
        return h > 0
                ? String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", h, m, s, rest)
                : String.format(Locale.ROOT, "%02d:%02d.%03d", m, s, rest);
    }
}
