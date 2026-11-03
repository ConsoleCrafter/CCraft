package de.consolecrafter.ccraft.gui.game;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import java.util.Random;

public final class PongScreen extends GameScreen {

    private static final int FW = 320;
    private static final int FH = 200;
    private static final int WIN_SCORE = 7;
    private static final double PADDLE_W = 6;
    private static final double PADDLE_H = 40;
    private static final double PLAYER_X = 10;
    private static final double AI_X = FW - 16;
    private static final double PLAYER_SPEED = 7.0;
    private static final double AI_SPEED = 4.6;
    private static final double BALL_R = 4.0;

    private final Random random = new Random();

    private double playerY = (FH - PADDLE_H) / 2;
    private double aiY = (FH - PADDLE_H) / 2;
    private double ballX;
    private double ballY;
    private double ballVX;
    private double ballVY;
    private double ballSpeed;
    private int playerScore;
    private int aiScore;
    private int wins;
    private boolean paused;
    private boolean gameOver;
    private int serveDelay;
    private boolean up;
    private boolean down;

    public PongScreen(Screen parent) {
        super("Pong", "pong", parent);
        serve(1);
    }

    @Override
    protected String[] instructions() {
        return new String[]{"W / ↑ = Hoch", "S / ↓ = Runter", "P = Pause", "Game Over: Leertaste = Neustart", "ESC = Zurück"};
    }

    @Override
    protected int score() {
        return wins;
    }

    private void serve(int direction) {
        ballX = FW / 2.0;
        ballY = FH / 2.0;
        ballSpeed = 4.2;
        double angle = (random.nextDouble() - 0.5) * Math.toRadians(60);
        ballVX = Math.cos(angle) * ballSpeed * direction;
        ballVY = Math.sin(angle) * ballSpeed;
        serveDelay = 20;
    }

    private void restart() {
        playerScore = 0;
        aiScore = 0;
        gameOver = false;
        paused = false;
        playerY = (FH - PADDLE_H) / 2;
        aiY = (FH - PADDLE_H) / 2;
        serve(random.nextBoolean() ? 1 : -1);
    }

    @Override
    public void tick() {
        if (paused || gameOver) return;
        if (serveDelay > 0) {
            serveDelay--;
            return;
        }

        if (up) playerY -= PLAYER_SPEED;
        if (down) playerY += PLAYER_SPEED;
        playerY = clamp(playerY, 0, FH - PADDLE_H);

        double aiCenter = aiY + PADDLE_H / 2;
        if (aiCenter < ballY - 4) aiY += AI_SPEED;
        else if (aiCenter > ballY + 4) aiY -= AI_SPEED;
        aiY = clamp(aiY, 0, FH - PADDLE_H);

        ballX += ballVX;
        ballY += ballVY;
        if (ballY - BALL_R < 0 && ballVY < 0 || ballY + BALL_R > FH && ballVY > 0) {
            ballY = clamp(ballY, BALL_R, FH - BALL_R);
            ballVY = -ballVY;
        }

        if (ballVX < 0 && ballX - BALL_R <= PLAYER_X + PADDLE_W && ballX - BALL_R >= PLAYER_X
                && ballY >= playerY && ballY <= playerY + PADDLE_H) {
            bounce(PLAYER_X + PADDLE_W, playerY);
        }
        if (ballVX > 0 && ballX + BALL_R >= AI_X && ballX + BALL_R <= AI_X + PADDLE_W
                && ballY >= aiY && ballY <= aiY + PADDLE_H) {
            bounce(AI_X, aiY);
        }

        if (ballX < -BALL_R) {
            if (++aiScore >= WIN_SCORE) gameOver = true;
            else serve(1);
        } else if (ballX > FW + BALL_R) {
            if (++playerScore >= WIN_SCORE) {
                gameOver = true;
                wins++;
            } else {
                serve(-1);
            }
        }
    }

    private void bounce(double paddleEdge, double paddleY) {
        double relative = clamp((ballY - (paddleY + PADDLE_H / 2)) / (PADDLE_H / 2), -1, 1);
        double angle = relative * Math.toRadians(55);
        double direction = ballVX > 0 ? -1 : 1;
        ballSpeed = Math.min(9.0, ballSpeed + 0.35);
        ballVX = Math.cos(angle) * ballSpeed * direction;
        ballVY = Math.sin(angle) * ballSpeed;
        ballX = paddleEdge + (direction > 0 ? BALL_R + 1 : -BALL_R - 1);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_W, InputConstants.KEY_UP -> up = true;
            case InputConstants.KEY_S, InputConstants.KEY_DOWN -> down = true;
            case InputConstants.KEY_P -> {
                if (!gameOver) paused = !paused;
            }
            case InputConstants.KEY_SPACE -> {
                if (gameOver) restart();
            }
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_W, InputConstants.KEY_UP -> up = false;
            case InputConstants.KEY_S, InputConstants.KEY_DOWN -> down = false;
            default -> {
                return super.keyReleased(event);
            }
        }
        return true;
    }

    @Override
    protected void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        double s = Math.max(0.4, Math.min((width - 60 - RAIL_WIDTH - PLAYFIELD_GAP) / (double) FW, (height - 60) / (double) FH));
        int fw = (int) (FW * s);
        int fh = (int) (FH * s);
        int ox = centeredPlayfieldX(fw);
        int oy = (height - fh) / 2;

        g.fill(ox, oy, ox + fw, oy + fh, 0xFF0A0A0A);
        g.fill(ox, oy, ox + fw, oy + 2, Theme.ACCENT);
        g.fill(ox, oy + fh - 2, ox + fw, oy + fh, Theme.ACCENT);
        for (int y = oy + 4; y < oy + fh - 4; y += 14) g.fill(ox + fw / 2 - 1, y, ox + fw / 2 + 1, y + 8, 0xFF333333);

        rect(g, ox, oy, s, PLAYER_X, playerY, PADDLE_W, PADDLE_H, Theme.ACCENT);
        rect(g, ox, oy, s, AI_X, aiY, PADDLE_W, PADDLE_H, 0xFF00C2FF);
        rect(g, ox, oy, s, ballX - BALL_R, ballY - BALL_R, BALL_R * 2, BALL_R * 2, 0xFFFFFFFF);

        g.centeredText(font, "§f" + playerScore, ox + fw / 2 - 30, oy + 10, 0xFFFFFFFF);
        g.centeredText(font, "§b" + aiScore, ox + fw / 2 + 30, oy + 10, 0xFF00C2FF);

        if (paused) {
            overlay(g, ox, oy, fw, fh, 0xA0, "§ePAUSIERT");
        } else if (gameOver) {
            overlay(g, ox, oy, fw, fh, 0xB0, playerScore > aiScore ? "§aGEWONNEN!" : "§cVERLOREN!",
                    "§7Endstand §f" + playerScore + " §7: §f" + aiScore, "§7Leertaste = Neustart");
        }
    }

    private static void rect(GuiGraphicsExtractor g, int ox, int oy, double s, double x, double y, double w, double h, int color) {
        g.fill(ox + (int) (x * s), oy + (int) (y * s), ox + (int) ((x + w) * s), oy + (int) ((y + h) * s), color);
    }
}
