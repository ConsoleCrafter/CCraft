package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.*;

public class TetrisScreen extends Screen {
    private static final SoundEvent NORMAL_MUSIC = SoundEvent.of(Identifier.of("ccraft", "tetris1"));
    private static final SoundEvent FAST_MUSIC = SoundEvent.of(Identifier.of("ccraft", "tetris2"));

    private TetrisGame tetrisGame;
    private final Screen parent;

    public TetrisScreen(Screen parent) {
        super(Text.literal("Tetris"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (tetrisGame == null) {
            tetrisGame = new TetrisGame();
        }
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Zurück"),
            button -> {
                if (tetrisGame != null) {
                    tetrisGame.saveHighScore();
                    tetrisGame.stopMusic();
                }
                client.setScreen(parent);
            }
        ).position(this.width - 110, this.height - 30).size(100, 20).build());
    }

    @Override
    public void removed() {
        super.removed();
        if (tetrisGame != null) {
            tetrisGame.saveHighScore();
            tetrisGame.stopMusic();
        }
        tetrisGame = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (tetrisGame != null) {
            tetrisGame.update();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Theme.header(context, textRenderer, "Tetris", GameTutorialRenderer.playfieldCenterX(this.width));
        if (tetrisGame != null) {
            tetrisGame.render(context);
        }
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "A = Links", "D = Rechts", "W = Runter", "S = Drop",
            "Leertaste / Shift = Drehen", "P = Pause", "Game Over: Leertaste = Neustart", "ESC = Zurück");
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        
        if (tetrisGame != null && tetrisGame.handleKeyPress(keyCode)) {
            return true;
        }
        if (keyCode == 256) {
            if (this.client != null) {
                if (tetrisGame != null) {
                    tetrisGame.saveHighScore();
                }
                this.client.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Tetromino {
        Empty, I, O, T, S, Z, J, L;

        public static Tetromino randomT() {
            Tetromino[] v = {I, O, T, S, Z, J, L};
            return v[(int) (Math.random() * v.length)];
        }
    }

    private record TetrominoData(int[][] shape, int color) {
        public TetrominoData rotate() {
            int[][] nc = new int[shape.length][2];
            for (int i = 0; i < shape.length; i++) {
                nc[i][0] = shape[i][1];
                nc[i][1] = -shape[i][0];
            }
            return new TetrominoData(nc, color);
        }
    }

    private record TetrominoWrapper(TetrominoData d) {
        public int[][] cells() {
            return d.shape();
        }

        public int color() {
            return d.color();
        }

        public TetrominoWrapper rotate() {
            return new TetrominoWrapper(d.rotate());
        }
    }

    private class TetrisGame {
        private static int highScore = 0;
        private static boolean loadedHS = false;
        private static File HS_FILE;
        private final int bw = 10, bh = 20;
        private final int[][] board = new int[bw][bh];
        private TetrominoWrapper cur, nxt;
        private int cx, cy;
        private boolean paused = false;
        private boolean gameOver = false;
        private long lastUpdate = System.currentTimeMillis();
        private long updateInt = 500;
        private int score = 0;
        private int linesClearedTotal = 0;
        private int level = 1;
        private PositionedSoundInstance music;
        private int musicTrack = -1;

        private long roundTimeMs = 0;
        private long lastTickTime = System.currentTimeMillis();

        private int clearAnimationTicks = 0;
        private List<Integer> linesToClear = new ArrayList<>();

        public TetrisGame() {
            loadHighScore();
            clearBoard();
            nxt = new TetrominoWrapper(randomData());
            spawnPiece();
        }

        private void loadHighScore() {
            if (loadedHS) return;
            loadedHS = true;
            HS_FILE = new File(MinecraftClient.getInstance().runDirectory, "tetrisHighscore.txt");
            if (!HS_FILE.exists()) {
                highScore = 0;
                return;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(HS_FILE))) {
                String line = br.readLine();
                if (line != null) {
                    highScore = Integer.parseInt(line.trim());
                }
            } catch (Exception e) {
                highScore = 0;
            }
        }

        public void saveHighScore() {
            if (score > highScore) {
                highScore = score;
            }
            if (HS_FILE == null) return;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(HS_FILE))) {
                bw.write(String.valueOf(highScore));
            } catch (IOException ignored) {
            }
        }

        private void clearBoard() {
            for (int x = 0; x < bw; x++) {
                for (int y = 0; y < bh; y++) {
                    board[x][y] = 0;
                }
            }
        }

        private void spawnPiece() {
            cur = nxt;
            cx = bw / 2;
            cy = 0;
            nxt = new TetrominoWrapper(randomData());
            if (!canMove(cur, cx, cy)) {
                gameOver = true;
                checkHighScore();
            }
        }

        public void update() {
            updateMusic();
            if (paused || gameOver) return;
            long now = System.currentTimeMillis();
            long dt = now - lastTickTime;
            lastTickTime = now;
            roundTimeMs += dt;

            if (clearAnimationTicks > 0) {
                clearAnimationTicks -= (int) dt;
                if (clearAnimationTicks <= 0) {
                    removeLines();
                    applyPartialGravity();
                    recheckAll();
                }
                return;
            }

            if (now - lastUpdate >= updateInt) {
                if (canMove(cur, cx, cy + 1)) {
                    cy++;
                } else {
                    lockPiece();
                    spawnPiece();
                }
                lastUpdate = now;
            }
        }

        private void recheckAll() {
            List<Integer> newLines = findFullLines();
            if (!newLines.isEmpty()) {
                linesToClear = newLines;
                clearAnimationTicks = 500;
            }
        }

        private void lockPiece() {
            for (int[] p : cur.cells()) {
                int x = cx + p[0];
                int y = cy + p[1];
                if (x >= 0 && x < bw && y >= 0 && y < bh) {
                    board[x][y] = cur.color();
                }
            }
            List<Integer> newLines = findFullLines();
            if (!newLines.isEmpty()) {
                linesToClear = newLines;
                clearAnimationTicks = 500;
            }
        }

        private List<Integer> findFullLines() {
            List<Integer> lines = new ArrayList<>();
            for (int row = 0; row < bh; row++) {
                boolean full = true;
                for (int col = 0; col < bw; col++) {
                    if (board[col][row] == 0) {
                        full = false;
                        break;
                    }
                }
                if (full) {
                    lines.add(row);
                }
            }
            return lines;
        }

        private void removeLines() {
            if (linesToClear.isEmpty()) return;
            int lines = linesToClear.size();
            linesClearedTotal += lines;
            addScoreNES(lines);
            updateLevel();
            checkHighScore();

            for (int row : linesToClear) {
                for (int sr = row; sr > 0; sr--) {
                    for (int col = 0; col < bw; col++) {
                        board[col][sr] = board[col][sr - 1];
                    }
                }
                for (int col = 0; col < bw; col++) {
                    board[col][0] = 0;
                }
            }
            linesToClear.clear();
        }

        private void applyPartialGravity() {
            boolean[][] visited = new boolean[bw][bh];
            for (int x = 0; x < bw; x++) {
                int y = bh - 1;
                if (board[x][y] != 0) {
                    bfsSupport(x, y, visited);
                }
            }
            for (int y = bh - 1; y >= 0; y--) {
                for (int x = 0; x < bw; x++) {
                    if (board[x][y] != 0 && !visited[x][y]) {
                        List<int[]> cluster = gatherCluster(x, y, visited);
                        dropCluster(cluster);
                    }
                }
            }
        }

        private void bfsSupport(int sx, int sy, boolean[][] visited) {
            Queue<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{sx, sy});
            visited[sx][sy] = true;
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                int cx = cur[0];
                int cy = cur[1];
                for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = cx + dir[0];
                    int ny = cy + dir[1];
                    if (nx >= 0 && nx < bw && ny >= 0 && ny < bh) {
                        if (!visited[nx][ny] && board[nx][ny] != 0) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }

        private List<int[]> gatherCluster(int sx, int sy, boolean[][] visited) {
            List<int[]> cluster = new ArrayList<>();
            Queue<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{sx, sy});
            visited[sx][sy] = true;
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                cluster.add(cur);
                int cx = cur[0];
                int cy = cur[1];
                for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = cx + dir[0];
                    int ny = cy + dir[1];
                    if (nx >= 0 && nx < bw && ny >= 0 && ny < bh) {
                        if (!visited[nx][ny] && board[nx][ny] != 0) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
            }
            return cluster;
        }

        private void dropCluster(List<int[]> cluster) {
            int color = board[cluster.get(0)[0]][cluster.get(0)[1]];
            for (int[] c : cluster) {
                board[c[0]][c[1]] = 0;
            }
            boolean canFall = true;
            int drop = 0;
            while (true) {
                for (int[] c : cluster) {
                    int nx = c[0];
                    int ny = c[1] + (drop + 1);
                    if (ny >= bh || board[nx][ny] != 0) {
                        canFall = false;
                        break;
                    }
                }
                if (!canFall) break;
                drop++;
            }
            for (int[] c : cluster) {
                int nx = c[0];
                int ny = c[1] + drop;
                board[nx][ny] = color;
            }
        }

        private void addScoreNES(int lines) {
            int points = switch (lines) {
                case 1 -> 40 * level;
                case 2 -> 100 * level;
                case 3 -> 300 * level;
                case 4 -> 1200 * level;
                default -> 0;
            };
            score += points;
        }

        private void updateLevel() {
            int newLevel = 1 + (linesClearedTotal / 10);
            if (newLevel > level) {
                level = newLevel;
                adjustSpeed();
            }
        }

        private void adjustSpeed() {
            updateInt = 500 - (level - 1) * 16L;
            if (updateInt < 100) {
                updateInt = 100;
            }
        }

        private void updateMusic() {
            int wantedTrack = paused || gameOver ? -1 : (level >= 5 ? 1 : 0);
            if (wantedTrack == musicTrack) return;
            stopMusic();
            if (wantedTrack < 0) return;

            SoundEvent event = wantedTrack == 1 ? FAST_MUSIC : NORMAL_MUSIC;
            music = PositionedSoundInstance.music(event, 0.45f);
            MinecraftClient.getInstance().getSoundManager().play(music);
            musicTrack = wantedTrack;
        }

        private void stopMusic() {
            if (music != null) {
                MinecraftClient.getInstance().getSoundManager().stop(music);
                music = null;
            }
            musicTrack = -1;
        }

        private void checkHighScore() {
            if (score > highScore) {
                highScore = score;
            }
        }

        private boolean canMove(TetrominoWrapper piece, int nx, int ny) {
            for (int[] p : piece.cells()) {
                int x = nx + p[0];
                int y = ny + p[1];
                if (x < 0 || x >= bw || y < 0 || y >= bh) {
                    return false;
                }
                if (board[x][y] != 0) {
                    return false;
                }
            }
            return true;
        }

        public void render(DrawContext context) {
            int margin = 40;
            int aw = width - margin - GameTutorialRenderer.RAIL_WIDTH - GameTutorialRenderer.PLAYFIELD_GAP;
            int ah = height - margin;
            int cs = Math.min(aw / bw, ah / bh);
            if (cs < 1) cs = 1;
            int bpw = bw * cs;
            int bph = bh * cs;
            int sx = GameTutorialRenderer.centeredPlayfieldX(width, bpw);
            int sy = (height - bph) / 2;

            context.fill(sx, sy, sx + bpw, sy + bph, 0xFF000000);

            if (!linesToClear.isEmpty()) {
                float fade = clearAnimationTicks / 500f;
                for (int y : linesToClear) {
                    for (int x = 0; x < bw; x++) {
                        int color = board[x][y];
                        if (color != 0) {
                            int origAlpha = (color >> 24) & 0xFF;
                            int newAlpha = (int) (origAlpha * fade);
                            int newColor = (newAlpha << 24) | (color & 0x00FFFFFF);
                            int px = sx + x * cs;
                            int py = sy + y * cs;
                            context.fill(px, py, px + cs, py + cs, newColor);
                        }
                    }
                }
            }

            for (int x = 0; x < bw; x++) {
                for (int y = 0; y < bh; y++) {
                    int color = board[x][y];
                    if (color != 0) {
                        if (linesToClear.contains(y)) {
                            continue;
                        }
                        int px = sx + x * cs;
                        int py = sy + y * cs;
                        context.fill(px, py, px + cs, py + cs, color);
                    }
                }
            }

            if (!gameOver && !paused && cur != null) {
                for (int[] p : cur.cells()) {
                    int x = cx + p[0];
                    int y = cy + p[1];
                    int px = sx + x * cs;
                    int py = sy + y * cs;
                    context.fill(px, py, px + cs, py + cs, cur.color());
                }
            }

            for (int x = 0; x <= bw; x++) {
                int px = sx + x * cs;
                context.fill(px, sy, px + 1, sy + bph, 0xFF444444);
            }
            for (int y = 0; y <= bh; y++) {
                int py = sy + y * cs;
                context.fill(sx, py, sx + bpw, py + 1, 0xFF444444);
            }

            int ix = sx + bpw + 10;
            int iy = sy + 4;

            context.drawText(textRenderer, "Highscore", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(highScore), ix, iy, 0xFFFFFF00, true);
            iy += textRenderer.fontHeight + 8;

            context.drawText(textRenderer, "Nächstes", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 2;

            int pcs = cs;
            int boxWidth = pcs * 5;
            int boxHeight = pcs * 4;
            int boxY = iy;

            context.fill(ix, boxY, ix + boxWidth, boxY + boxHeight, 0xC0000000);

            if (nxt != null) {
                int[][] shape = nxt.cells();
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                for (int[] p : shape) {
                    if (p[0] < minX) minX = p[0];
                    if (p[0] > maxX) maxX = p[0];
                    if (p[1] < minY) minY = p[1];
                    if (p[1] > maxY) maxY = p[1];
                }
                int shapeWidthInCells = (maxX - minX + 1);
                int shapeHeightInCells = (maxY - minY + 1);
                int shapeWidth = shapeWidthInCells * pcs;
                int shapeHeight = shapeHeightInCells * pcs;
                int centerX = ix + boxWidth / 2;
                int centerY = boxY + boxHeight / 2;
                int shapeLeft = centerX - shapeWidth / 2;
                int shapeTop = centerY - shapeHeight / 2;
                for (int[] p : shape) {
                    int xx = p[0] - minX;
                    int yy = p[1] - minY;
                    int px = shapeLeft + xx * pcs;
                    int py = shapeTop + yy * pcs;
                    context.fill(px, py, px + pcs, py + pcs, nxt.color());
                }
            }

            iy += boxHeight + 10;

            context.drawText(textRenderer, "Level", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(level), ix, iy, 0xFFFF0000, true);
            iy += textRenderer.fontHeight + 10;

            context.drawText(textRenderer, "Score", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(score), ix, iy, 0xFFFF8000, true);
            iy += textRenderer.fontHeight + 10;

            context.drawText(textRenderer, "Linien", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, String.valueOf(linesClearedTotal), ix, iy, 0xFFFF00FF, true);
            iy += textRenderer.fontHeight + 10;

            context.drawText(textRenderer, "Zeit", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            long totalMs = roundTimeMs;
            long h = totalMs / 3600000;
            long r = totalMs % 3600000;
            long m = r / 60000;
            r = r % 60000;
            long s = r / 1000;
            long ms = r % 1000;
            String timeStr = (h > 0)
                ? String.format("%02d:%02d:%02d.%03d", h, m, s, ms)
                : String.format("%02d:%02d.%03d", m, s, ms);
            context.drawText(textRenderer, timeStr, ix, iy, 0xFF00FF00, true);

            if (paused) {
                context.fill(sx, sy, sx + bpw, sy + bph, 0xA0000000);
                drawCenteredText(context, "PAUSIERT", 0xFFFFFFFF);
            } else if (gameOver) {
                context.fill(sx, sy, sx + bpw, sy + bph, 0xA0000000);
                String line1 = "GAME OVER";
                String line2 = "Leertaste = Neustart";
                int w1 = textRenderer.getWidth(line1);
                int w2 = textRenderer.getWidth(line2);
                int x1 = (width - w1) / 2;
                int x2 = (width - w2) / 2;
                int yMid = height / 2;
                context.drawText(textRenderer, line1, x1, yMid - 10, 0xFF00FFFF, true);
                context.drawText(textRenderer, line2, x2, yMid + 10, 0xFF0000FF, true);
            }
        }

        public boolean handleKeyPress(int key) {
            if (gameOver) {
                if (key == 32) {
                    restartGame();
                    return true;
                }
                return false;
            }
            if (paused) {
                if (key == 80) {
                    paused = false;
                    return true;
                }
                return false;
            }
            boolean handled = false;
            switch (key) {
                case 65 -> {
                    if (canMove(cur, cx - 1, cy)) {
                        cx--;
                        handled = true;
                    }
                }
                case 68 -> {
                    if (canMove(cur, cx + 1, cy)) {
                        cx++;
                        handled = true;
                    }
                }
                case 87 -> {
                    if (canMove(cur, cx, cy + 1)) {
                        cy++;
                        handled = true;
                    }
                }
                case 83 -> {
                    while (canMove(cur, cx, cy + 1)) {
                        cy++;
                    }
                    handled = true;
                }
                case 32, 340, 344 -> {
                    if (!isOShape(cur)) {
                        TetrominoWrapper r = cur.rotate();
                        if (canMove(r, cx, cy)) {
                            cur = r;
                            handled = true;
                        }
                    }
                }
                case 80 -> { // P
                    paused = true;
                    handled = true;
                }
            }
            return handled;
        }

        private boolean isOShape(TetrominoWrapper piece) {
            int[][] c = piece.cells();
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (int[] p : c) {
                if (p[0] < minX) minX = p[0];
                if (p[0] > maxX) maxX = p[0];
                if (p[1] < minY) minY = p[1];
                if (p[1] > maxY) maxY = p[1];
            }
            int width = (maxX - minX) + 1;
            int height = (maxY - minY) + 1;
            return (width == 2 && height == 2 && c.length == 4);
        }

        private void restartGame() {
            clearBoard();
            score = 0;
            linesClearedTotal = 0;
            level = 1;
            updateInt = 500;
            gameOver = false;
            paused = false;
            nxt = new TetrominoWrapper(randomData());
            spawnPiece();
            roundTimeMs = 0;
            lastTickTime = System.currentTimeMillis();
        }

        private void drawCenteredText(DrawContext context, String msg, int color) {
            int w = textRenderer.getWidth(msg);
            int xx = (width - w) / 2;
            int yy = height / 2;
            context.drawText(textRenderer, msg, xx, yy, color, true);
        }

        private TetrominoData randomData() {
            int[][] shape;
            switch (Tetromino.randomT()) {
                case I -> shape = new int[][]{{-1, 0}, {0, 0}, {1, 0}, {2, 0}};
                case O -> shape = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};
                case T -> shape = new int[][]{{-1, 0}, {0, 0}, {1, 0}, {0, 1}};
                case S -> shape = new int[][]{{0, 0}, {1, 0}, {-1, 1}, {0, 1}};
                case Z -> shape = new int[][]{{-1, 0}, {0, 0}, {0, 1}, {1, 1}};
                case J -> shape = new int[][]{{-1, 0}, {0, 0}, {1, 0}, {1, 1}};
                case L -> shape = new int[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}};
                default -> shape = new int[][]{{-1, 0}, {0, 0}, {1, 0}, {2, 0}};
            }
            int[] mainColors = {
                0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF7FFF00,
                0xFF00FF00, 0xFF00FF7F, 0xFF00FFFF, 0xFF007FFF,
                0xFF0000FF, 0xFF7F00FF, 0xFFFF00FF, 0xFFFF007F
            };
            int color = mainColors[(int) (Math.random() * mainColors.length)];
            return new TetrominoData(shape, color);
        }
    }
}
