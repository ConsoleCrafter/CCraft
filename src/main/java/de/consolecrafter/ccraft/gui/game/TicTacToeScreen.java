package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TicTacToeScreen extends GameScreen {

    private static final int N = 3;
    private static final int EMPTY = 0;
    private static final int PLAYER = 1;
    private static final int CRAFTIE = 2;
    private static final int DRAW = 3;
    private static final double MISTAKE_CHANCE = 0.12;

    private final Random random = new Random();
    private final int[][] board = new int[N][N];

    private int winner;
    private int victories;
    private int thinkTicks;
    private int[] pendingMove;

    public TicTacToeScreen(Screen parent) {
        super("Tic Tac Toe", "tictactoe", parent);
    }

    @Override
    protected void init() {
        super.init();
        button("Neustart", width - 220, height - 30, 100, BUTTON_HEIGHT, this::restart);
    }

    @Override
    protected String[] instructions() {
        return new String[]{"Klicke ein freies Feld", "Du spielst X", "R = Neustart", "ESC = Zurück"};
    }

    @Override
    protected int score() {
        return victories;
    }

    private void restart() {
        for (int[] column : board) java.util.Arrays.fill(column, EMPTY);
        winner = EMPTY;
        pendingMove = null;
        thinkTicks = 0;
    }

    private boolean thinking() {
        return pendingMove != null;
    }

    @Override
    public void tick() {
        if (!thinking() || winner != EMPTY || --thinkTicks > 0) return;
        board[pendingMove[0]][pendingMove[1]] = CRAFTIE;
        pendingMove = null;
        checkEnd();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (winner == EMPTY && !thinking() && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int cs = cellSize();
            int cx = (int) Math.floor((event.x() - boardX()) / cs);
            int cy = (int) Math.floor((event.y() - boardY()) / cs);
            if (cx >= 0 && cx < N && cy >= 0 && cy < N && board[cx][cy] == EMPTY) {
                board[cx][cy] = PLAYER;
                if (!checkEnd()) {
                    pendingMove = bestMove();
                    thinkTicks = 24 + random.nextInt(18);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_R) {
            restart();
            return true;
        }
        return super.keyPressed(event);
    }

    private int[] bestMove() {
        List<int[]> empties = empties();
        if (random.nextDouble() < MISTAKE_CHANCE) return empties.get(random.nextInt(empties.size()));
        int bestScore = Integer.MIN_VALUE;
        List<int[]> best = new ArrayList<>();
        for (int[] move : empties) {
            board[move[0]][move[1]] = CRAFTIE;
            int score = minimax(false, 0);
            board[move[0]][move[1]] = EMPTY;
            if (score > bestScore) {
                bestScore = score;
                best.clear();
            }
            if (score == bestScore) best.add(move);
        }
        return best.get(random.nextInt(best.size()));
    }

    private int minimax(boolean craftieTurn, int depth) {
        if (wins(CRAFTIE)) return 10 - depth;
        if (wins(PLAYER)) return depth - 10;
        List<int[]> empties = empties();
        if (empties.isEmpty()) return 0;
        int best = craftieTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int[] move : empties) {
            board[move[0]][move[1]] = craftieTurn ? CRAFTIE : PLAYER;
            int score = minimax(!craftieTurn, depth + 1);
            board[move[0]][move[1]] = EMPTY;
            best = craftieTurn ? Math.max(best, score) : Math.min(best, score);
        }
        return best;
    }

    private List<int[]> empties() {
        List<int[]> list = new ArrayList<>();
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                if (board[x][y] == EMPTY) list.add(new int[]{x, y});
            }
        }
        return list;
    }

    private boolean checkEnd() {
        if (wins(PLAYER)) {
            winner = PLAYER;
            victories++;
        } else if (wins(CRAFTIE)) {
            winner = CRAFTIE;
        } else if (empties().isEmpty()) {
            winner = DRAW;
        }
        return winner != EMPTY;
    }

    private boolean wins(int who) {
        for (int i = 0; i < N; i++) {
            if (board[i][0] == who && board[i][1] == who && board[i][2] == who) return true;
            if (board[0][i] == who && board[1][i] == who && board[2][i] == who) return true;
        }
        return board[0][0] == who && board[1][1] == who && board[2][2] == who
                || board[2][0] == who && board[1][1] == who && board[0][2] == who;
    }

    private int cellSize() {
        return Math.max(40, Math.min(90, (Math.min(width, height) - 120) / N));
    }

    private int boardX() {
        return centeredPlayfieldX(N * cellSize());
    }

    private int boardY() {
        return height / 2 - N * cellSize() / 2 - 14;
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int cs = cellSize();
        int bx = boardX();
        int by = boardY();
        g.fill(bx - 4, by - 4, bx + N * cs + 4, by + N * cs + 4, 0xCC0C0C12);

        boolean canHover = winner == EMPTY && !thinking();
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                int px = bx + x * cs;
                int py = by + y * cs;
                boolean hovered = canHover && mouseX >= px && mouseX < px + cs && mouseY >= py && mouseY < py + cs;
                g.fill(px + 3, py + 3, px + cs - 3, py + cs - 3, hovered ? 0x33FFFFFF : 0x22000000);
                if (board[x][y] == PLAYER) g.centeredText(font, "§lX", px + cs / 2, py + cs / 2 - 4, Theme.ACCENT);
                else if (board[x][y] == CRAFTIE) g.centeredText(font, "§lO", px + cs / 2, py + cs / 2 - 4, 0xFF6EC6FF);
            }
        }

        String status = switch (winner) {
            case PLAYER -> "§a§lDU GEWINNST!";
            case CRAFTIE -> "§c§lCRAFTIE GEWINNT";
            case DRAW -> "§e§lUNENTSCHIEDEN";
            default -> thinking() ? "§7Craftie überlegt…" : "§7Du bist §cX§7 — Craftie ist §bO§7 — klicke ein Feld";
        };
        g.centeredText(font, status, playfieldCenterX(), by - 22, Theme.TEXT);
        g.centeredText(font, "§7Siege: §f" + victories + "   §7Rekord: §f" + highscore(), playfieldCenterX(), by + N * cs + 10, Theme.TEXT);
    }
}
