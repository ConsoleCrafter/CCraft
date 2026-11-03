package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tic-Tac-Toe: 3x3-Feld, du gegen Craftie (das CCraft-Maskottchen).
 * Linksklick setzt dein X, Craftie antwortet mit O — nach kurzem Überlegen.
 */
public class TicTacToeScreen extends Screen {

    private static final int N = 3;
    /** Chance, dass Craftie absichtlich daneben greift (bleibt schlagbar). */
    private static final double MISTAKE_CHANCE = 0.12;

    private final Screen parent;
    private final Game game = new Game();

    public TicTacToeScreen(Screen parent) {
        super(Text.literal("Tic Tac Toe"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent))
                .position(this.width - 110, this.height - 30).size(100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Neustart"), b -> game.restart())
            .position(this.width - 220, this.height - 30).size(100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        game.render(context, mouseX, mouseY);
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "Klicke ein freies Feld", "Du spielst X", "R = Neustart", "ESC = Zurück");
    }

    @Override
    public void tick() {
        super.tick();
        game.tick();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (game.handleClick(click.x(), click.y())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        switch (keyInput.key()) {
            case 82 -> { game.restart(); return true; }   // R
            case 256 -> { client.setScreen(parent); return true; }
            default -> { return super.keyPressed(keyInput); }
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Spiel ─────────────────────────────────────────────────────────────────

    private class Game {

        /** 0 = leer, 1 = X (Spieler), 2 = O (Craftie). */
        private final int[][] board = new int[N][N];
        private int winner = 0;        // 0 = offen, 1 = X, 2 = O, 3 = Unentschieden
        private boolean playerTurn = true;

        // Craftie "überlegt": die Antwort wird verzögert gesetzt.
        private boolean thinking = false;
        private int thinkTicks = 0;
        private int thinkDelay = 30;
        private int[] pendingMove = null;

        private final Random rng = new Random();

        Game() {
            restart();
        }

        void restart() {
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++) board[x][y] = 0;
            winner = 0;
            playerTurn = true;
            thinking = false;
            thinkTicks = 0;
            pendingMove = null;
        }

        /** Wird jeden Screen-Tick aufgerufen; setzt Crafties Antwort nach der Denkpause. */
        void tick() {
            if (!thinking || winner != 0) return;
            thinkTicks++;
            if (thinkTicks >= thinkDelay) {
                thinking = false;
                if (pendingMove != null) board[pendingMove[0]][pendingMove[1]] = 2;
                pendingMove = null;
                playerTurn = true;
                checkEnd();
            }
        }

        boolean handleClick(double mx, double my) {
            if (winner != 0 || !playerTurn || thinking) return false;
            int cs = cellSize();
            int bx = boardX();
            int by = boardY();
            int cx = (int) ((mx - bx) / cs);
            int cy = (int) ((my - by) / cs);
            if (cx < 0 || cx >= N || cy < 0 || cy >= N) return false;
            if (board[cx][cy] != 0) return false;

            board[cx][cy] = 1;
            if (!checkEnd()) startThinking();
            return true;
        }

        private void startThinking() {
            pendingMove = bestMove();
            thinking = true;
            thinkTicks = 0;
            thinkDelay = 24 + rng.nextInt(18); // ~1,2–2,1 Sekunden
            playerTurn = false;
        }

        /**
         * Spielt (fast) optimal per Minimax. Mit kleiner Wahrscheinlichkeit
         * greift Craftie absichtlich daneben, damit er schlagbar bleibt und
         * man nicht mit einem festen Muster gewinnt.
         */
        private int[] bestMove() {
            List<int[]> empties = empties();
            if (empties.isEmpty()) return null;

            if (rng.nextDouble() < MISTAKE_CHANCE) {
                return empties.get(rng.nextInt(empties.size()));
            }

            int bestScore = Integer.MIN_VALUE;
            List<int[]> best = new ArrayList<>();
            for (int[] m : empties) {
                board[m[0]][m[1]] = 2;
                int score = minimax(false, 0);
                board[m[0]][m[1]] = 0;
                if (score > bestScore) { bestScore = score; best.clear(); best.add(m); }
                else if (score == bestScore) { best.add(m); }
            }
            return best.get(rng.nextInt(best.size()));
        }

        /** Minimax: 2 = Craftie (maximiert), 1 = Spieler (minimiert). */
        private int minimax(boolean craftieTurn, int depth) {
            if (hasWinner(2)) return 10 - depth;
            if (hasWinner(1)) return depth - 10;

            List<int[]> empties = empties();
            if (empties.isEmpty()) return 0;

            if (craftieTurn) {
                int best = Integer.MIN_VALUE;
                for (int[] m : empties) {
                    board[m[0]][m[1]] = 2;
                    best = Math.max(best, minimax(false, depth + 1));
                    board[m[0]][m[1]] = 0;
                }
                return best;
            } else {
                int best = Integer.MAX_VALUE;
                for (int[] m : empties) {
                    board[m[0]][m[1]] = 1;
                    best = Math.min(best, minimax(true, depth + 1));
                    board[m[0]][m[1]] = 0;
                }
                return best;
            }
        }

        private List<int[]> empties() {
            List<int[]> list = new ArrayList<>();
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++) if (board[x][y] == 0) list.add(new int[]{x, y});
            return list;
        }

        private boolean checkEnd() {
            if (hasWinner(1)) { winner = 1; return true; }
            if (hasWinner(2)) { winner = 2; return true; }
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++) if (board[x][y] == 0) { playerTurn = true; return false; }
            winner = 3;
            return true;
        }

        private boolean hasWinner(int who) {
            for (int i = 0; i < N; i++) {
                if (board[i][0] == who && board[i][1] == who && board[i][2] == who) return true;
                if (board[0][i] == who && board[1][i] == who && board[2][i] == who) return true;
            }
            if (board[0][0] == who && board[1][1] == who && board[2][2] == who) return true;
            return board[2][0] == who && board[1][1] == who && board[0][2] == who;
        }

        // ── Layout ────────────────────────────────────────────────────────────

        private int cellSize() {
            int avail = Math.min(width, height) - 120;
            return Math.max(40, Math.min(90, avail / N));
        }

        private int boardX() { return GameTutorialRenderer.centeredPlayfieldX(width, N * cellSize()); }
        private int boardY() { return height / 2 - (N * cellSize()) / 2 - 14; }

        // ── Rendering ─────────────────────────────────────────────────────────

        void render(DrawContext ctx, int mouseX, int mouseY) {
            int cs = cellSize();
            int bx = boardX();
            int by = boardY();

            Theme.header(ctx, textRenderer, "Tic Tac Toe", GameTutorialRenderer.playfieldCenterX(width));

            ctx.fill(bx - 4, by - 4, bx + N * cs + 4, by + N * cs + 4, 0xCC0C0C12);

            boolean hover = winner == 0 && playerTurn && !thinking;
            for (int x = 0; x < N; x++)
                for (int y = 0; y < N; y++) {
                    int px = bx + x * cs;
                    int py = by + y * cs;
                    int x0 = px + 3, y0 = py + 3, x1 = px + cs - 3, y1 = py + cs - 3;

                    boolean isHover = hover && mouseX >= px && mouseX < px + cs
                            && mouseY >= py && mouseY < py + cs;
                    ctx.fill(x0, y0, x1, y1, isHover ? 0x33FFFFFF : 0x22000000);

                    int v = board[x][y];
                    if (v == 1) {
                        ctx.drawCenteredTextWithShadow(textRenderer, "§lX", px + cs / 2, py + cs / 2 - 4, Theme.ACCENT);
                    } else if (v == 2) {
                        ctx.drawCenteredTextWithShadow(textRenderer, "§lO", px + cs / 2, py + cs / 2 - 4, 0xFF6EC6FF);
                    }
                }

            String status = switch (winner) {
                case 1 -> "§a§lDU GEWINNST!";
                case 2 -> "§c§lCRAFTIE GEWINNT";
                case 3 -> "§e§lUNENTSCHIEDEN";
                default -> thinking
                        ? "§7Craftie überlegt…"
                        : "§7Du bist §cX§7 — Craftie ist §bO§7 — klicke ein Feld";
            };
            ctx.drawCenteredTextWithShadow(textRenderer, status, GameTutorialRenderer.playfieldCenterX(width), by - 22, Theme.TEXT);
        }
    }
}
