package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.Random;

/**
 * Pong gegen den Computer. Linker Schläger = Spieler (W/S oder ↑/↓),
 * rechter Schläger = KI. Erster mit 7 Punkten gewinnt.
 */
public class PongScreen extends Screen {

    // Virtuelles Spielfeld (wird auf die Bildschirmgröße skaliert).
    private static final int FW = 320;
    private static final int FH = 200;
    private static final int WIN_SCORE = 7;

    private final Screen parent;
    private final Random rng = new Random();

    private double paddleW = 6, paddleH = 40;
    private double playerX = 10, playerY = (FH - 40) / 2.0;
    private double aiX = FW - 16, aiY = (FH - 40) / 2.0;

    private double ballX = FW / 2.0, ballY = FH / 2.0;
    private double ballVX, ballVY;
    private double ballSpeed = 4.2;

    private int playerScore = 0;
    private int aiScore = 0;
    private boolean paused = false;
    private boolean gameOver = false;
    private int serveDelay = 0; // in Ticks

    private static final double PLAYER_SPEED = 7.0;
    private static final double AI_SPEED = 4.6;
    private static final double BALL_R = 4.0;

    public PongScreen(Screen parent) {
        super(Text.literal("Pong"));
        this.parent = parent;
        resetBall(1);
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent))
                .position(this.width - 110, this.height - 30).size(100, 20).build());
    }

    private void resetBall(int dir) {
        ballX = FW / 2.0;
        ballY = FH / 2.0;
        double angle = (rng.nextDouble() - 0.5) * Math.toRadians(60);
        ballSpeed = 4.2;
        ballVX = Math.cos(angle) * ballSpeed * dir;
        ballVY = Math.sin(angle) * ballSpeed;
        serveDelay = 20;
    }

    @Override
    public void tick() {
        super.tick();
        if (paused || gameOver) return;

        if (serveDelay > 0) { serveDelay--; return; }

        // Spieler-Schläger folgt den Tasten (GLFW-Zustand).
        long win = client.getWindow().getHandle();
        boolean up = org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_W) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_UP) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean down = org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_S) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (up) playerY -= PLAYER_SPEED;
        if (down) playerY += PLAYER_SPEED;
        playerY = clamp(playerY, 0, FH - paddleH);

        // KI folgt dem Ball träge.
        double aiCenter = aiY + paddleH / 2;
        if (aiCenter < ballY - 4) aiY += AI_SPEED;
        else if (aiCenter > ballY + 4) aiY -= AI_SPEED;
        aiY = clamp(aiY, 0, FH - paddleH);

        // Ball bewegen.
        ballX += ballVX;
        ballY += ballVY;

        // Decke/Boden.
        if (ballY - BALL_R < 0 && ballVY < 0) { ballY = BALL_R; ballVY = -ballVY; }
        if (ballY + BALL_R > FH && ballVY > 0) { ballY = FH - BALL_R; ballVY = -ballVY; }

        // Spieler-Schläger (links).
        if (ballVX < 0 && ballX - BALL_R <= playerX + paddleW && ballX - BALL_R >= playerX
                && ballY >= playerY && ballY <= playerY + paddleH) {
            bounceOffPaddle(playerX + paddleW, playerY);
        }
        // KI-Schläger (rechts).
        if (ballVX > 0 && ballX + BALL_R >= aiX && ballX + BALL_R <= aiX + paddleW
                && ballY >= aiY && ballY <= aiY + paddleH) {
            bounceOffPaddle(aiX, aiY);
        }

        // Punkt.
        if (ballX < -BALL_R) {
            aiScore++;
            if (aiScore >= WIN_SCORE) { gameOver = true; }
            else resetBall(1);
        } else if (ballX > FW + BALL_R) {
            playerScore++;
            if (playerScore >= WIN_SCORE) { gameOver = true; }
            else resetBall(-1);
        }
    }

    private void bounceOffPaddle(double paddleX, double paddleCenterY) {
        double rel = (ballY - (paddleCenterY + paddleH / 2)) / (paddleH / 2); // -1..1
        rel = clamp(rel, -1, 1);
        double angle = rel * Math.toRadians(55);
        ballSpeed = Math.min(9.0, ballSpeed + 0.35);
        double dir = ballVX > 0 ? -1 : 1; // vom Schläger weg
        ballVX = Math.cos(angle) * ballSpeed * dir;
        ballVY = Math.sin(angle) * ballSpeed;
        // Ball aus dem Schläger schieben.
        ballX = paddleX + (dir > 0 ? BALL_R + 1 : -BALL_R - 1);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int margin = 60;
        double s = Math.min((this.width - margin - GameTutorialRenderer.RAIL_WIDTH - GameTutorialRenderer.PLAYFIELD_GAP) / (double) FW,
            (this.height - margin) / (double) FH);
        if (s < 0.4) s = 0.4;
        int fw = (int) (FW * s), fh = (int) (FH * s);
        int ox = GameTutorialRenderer.centeredPlayfieldX(this.width, fw);
        int oy = (this.height - fh) / 2;

        // Feld.
        context.fill(ox, oy, ox + fw, oy + fh, 0xFF0A0A0A);
        context.fill(ox, oy, ox + fw, oy + 2, Theme.ACCENT);
        context.fill(ox, oy + fh - 2, ox + fw, oy + fh, Theme.ACCENT);

        // Mittellinie (gestrichelt).
        for (int y = oy + 4; y < oy + fh - 4; y += 14) {
            context.fill(ox + fw / 2 - 1, y, ox + fw / 2 + 1, y + 8, 0xFF333333);
        }

        // Schläger.
        context.fill(
                ox + (int) (playerX * s), oy + (int) (playerY * s),
                ox + (int) ((playerX + paddleW) * s), oy + (int) ((playerY + paddleH) * s),
                Theme.ACCENT);
        context.fill(
                ox + (int) (aiX * s), oy + (int) (aiY * s),
                ox + (int) ((aiX + paddleW) * s), oy + (int) ((aiY + paddleH) * s),
                0xFF00C2FF);

        // Ball.
        context.fill(
                ox + (int) ((ballX - BALL_R) * s), oy + (int) ((ballY - BALL_R) * s),
                ox + (int) ((ballX + BALL_R) * s), oy + (int) ((ballY + BALL_R) * s),
                0xFFFFFFFF);

        // Punkte.
        context.drawCenteredTextWithShadow(textRenderer, "§f" + playerScore,
                ox + fw / 2 - 30, oy + 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "§b" + aiScore,
                ox + fw / 2 + 30, oy + 10, 0xFF00C2FF);

        Theme.header(context, textRenderer, "Pong", GameTutorialRenderer.playfieldCenterX(this.width));
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "W / ↑ = Hoch", "S / ↓ = Runter", "P = Pause", "Game Over: Leertaste = Neustart", "ESC = Zurück");

        if (paused) {
            context.fill(ox, oy, ox + fw, oy + fh, 0xA0000000);
            context.drawCenteredTextWithShadow(textRenderer, "§ePAUSIERT", ox + fw / 2, oy + fh / 2, 0xFFFFFF00);
        } else if (gameOver) {
            context.fill(ox, oy, ox + fw, oy + fh, 0xB0000000);
            boolean won = playerScore > aiScore;
            context.drawCenteredTextWithShadow(textRenderer,
                    won ? "§aGEWONNEN!" : "§cVERLOREN!",
                    ox + fw / 2, oy + fh / 2 - 12, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer,
                    "§7Endstand §f" + playerScore + " §7: §f" + aiScore,
                    ox + fw / 2, oy + fh / 2 + 4, 0xFFAAAAAA);
            context.drawCenteredTextWithShadow(textRenderer,
                    "§7Leertaste = Neustart", ox + fw / 2, oy + fh / 2 + 20, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (key == 256) { client.setScreen(parent); return true; }      // ESC
        if (key == 80) { if (!gameOver) paused = !paused; return true; } // P
        if (key == 32 && gameOver) { restart(); return true; }           // Leertaste
        return super.keyPressed(keyInput);
    }

    private void restart() {
        playerScore = 0;
        aiScore = 0;
        gameOver = false;
        paused = false;
        playerY = (FH - paddleH) / 2.0;
        aiY = (FH - paddleH) / 2.0;
        resetBall(rng.nextBoolean() ? 1 : -1);
    }

    @Override
    public boolean shouldPause() { return false; }
}
