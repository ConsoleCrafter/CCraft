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

public class DoodleJumpScreen extends Screen {

    private final Screen parent;
    private DoodleGame game;

    public DoodleJumpScreen(Screen parent) {
        super(Text.literal("Doodle Jump"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (game == null) game = new DoodleGame();

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
        Theme.header(context, textRenderer, "Doodle Jump", GameTutorialRenderer.playfieldCenterX(this.width));
        if (game != null) game.render(context);
        GameTutorialRenderer.draw(context, textRenderer, this.height,
            "A / ← = Links", "D / → = Rechts", "P = Pause", "Leertaste = Start / Neustart",
            "ESC = Zurück",
            "Grün = Normal", "Blau = Bewegend", "Gold = Feder x2", "Rot = Zerbricht");
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (game != null) game.keyDown(key);
        if (key == 256) {
            game.saveHighScore();
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        if (game != null) game.keyUp(keyInput.key());
        return super.keyReleased(keyInput);
    }

    @Override
    public void removed() {
        super.removed();
        if (game != null) game.saveHighScore();
        game = null;
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Doodle Game ───────────────────────────────────────────────────────────

    private class DoodleGame {

        // Virtuelles Spielfeld
        private static final int VW = 200;
        private static final int VH = 300;

        // Spieler
        private float px = VW / 2f - 8;
        private float py = VH - 60f;
        private float pvx = 0f;
        private float pvy = 0f;
        private static final int   PW         = 16;
        private static final int   PH         = 16;
        private static final float GRAVITY    = 0.35f;
        private static final float JUMP_VEL   = -8f;
        private static final float BOOST_VEL  = -13f;
        private static final float MOVE_SPEED = 2.5f;

        // Kamera
        private float camY = 0f;  // wie weit gescrollt (in virtuellen Einheiten)
        private float maxCamY = 0f;

        // Score
        private int score = 0;

        // Input
        private boolean leftDown  = false;
        private boolean rightDown = false;
        private boolean paused    = false;
        private boolean gameOver  = false;
        private boolean started   = false;

        // Plattformen: {x, y, type, state}
        // type: 0=normal, 1=moving, 2=spring, 3=crumbling
        // state: für moving = Richtung, für crumbling = 0=ok 1=crumbling 2=gone
        private final List<float[]> platforms = new ArrayList<>();
        private static final int PLT_W = 30;
        private static final int PLT_H = 5;

        // Moving platform Richtung & Speed
        private static final float MOV_SPEED = 0.8f;

        private final Random rng = new Random();

        // Highscore
        private static int     highScore = 0;
        private static boolean loadedHS  = false;
        private static File    HS_FILE;

        public DoodleGame() {
            loadHighScore();
            generateInitialPlatforms();
            // Spieler auf erste Plattform setzen
            float[] first = platforms.get(0);
            px = first[0] + PLT_W / 2f - PW / 2f;
            py = first[1] - PH;
            pvy = JUMP_VEL;
        }

        private void generateInitialPlatforms() {
            platforms.clear();
            // Startplattform direkt unter Spieler
            platforms.add(new float[]{VW / 2f - PLT_W / 2f, VH - 40f, 0, 0});

            float y = VH - 80f;
            while (y > -VH * 10) {
                spawnPlatformAt(y);
                y -= 28 + rng.nextInt(20);
            }
        }

        private void spawnPlatformAt(float y) {
            float x = rng.nextInt(VW - PLT_W);
            int type = 0;
            float difficulty = Math.min(1f, score / 80f);
            float r = rng.nextFloat();
            if      (r < 0.08f + difficulty * 0.12f) type = 3; // crumbling
            else if (r < 0.18f + difficulty * 0.15f) type = 1; // moving
            else if (r < 0.25f)                       type = 2; // spring
            platforms.add(new float[]{x, y, type, 1}); // state=1 = moving dir
        }

        public void update() {
            if (!started || paused || gameOver) return;

            // Input → Bewegung
            if (leftDown)  pvx = -MOVE_SPEED;
            else if (rightDown) pvx = MOVE_SPEED;
            else pvx *= 0.7f;

            // Wrap around
            px += pvx;
            if (px + PW < 0)  px = VW;
            if (px > VW)      px = -PW;

            // Schwerkraft
            pvy += GRAVITY;
            py  += pvy;

            // Kamera nach oben scrollen
            float screenMid = camY + VH / 3f;
            if (py < screenMid) {
                float diff = screenMid - py;
                camY -= diff;
                py    = screenMid;
            }
            if (camY < maxCamY) {
                score = (int)(-camY / 10);
                if (score > highScore) highScore = score;
                maxCamY = camY;
            }

            // Plattformen updaten
            for (float[] p : platforms) {
                if (p[2] == 1) { // moving
                    p[0] += MOV_SPEED * p[3];
                    if (p[0] <= 0 || p[0] + PLT_W >= VW) p[3] = -p[3];
                }
            }

            // Kollision mit Plattformen (nur beim Fallen)
            if (pvy > 0) {
                for (float[] p : platforms) {
                    float worldY = p[1] - camY; // Plattform in Weltkoordinaten
                    // Spieler fällt auf Plattform
                    if (px + PW > p[0] && px < p[0] + PLT_W
                            && py + PH >= p[1] && py + PH <= p[1] + PLT_H + pvy + 2
                            && p[2] != 3 || (p[2] == 3 && p[3] == 0)) {

                        if (p[2] == 3) { // crumbling
                            if (p[3] == 0) {
                                p[3] = 1; // beginnt zu bröckeln
                                pvy = JUMP_VEL;
                                py = p[1] - PH;
                            }
                        } else if (p[2] == 2) { // spring
                            pvy = BOOST_VEL;
                            py = p[1] - PH;
                        } else {
                            pvy = JUMP_VEL;
                            py = p[1] - PH;
                        }
                    }
                }
            }

            // Crumbling Plattformen zerstören
            platforms.removeIf(p -> p[2] == 3 && p[3] == 2);
            for (float[] p : platforms) {
                if (p[2] == 3 && p[3] == 1) p[3] = 2;
            }

            // Alte Plattformen entfernen, neue spawnen
            platforms.removeIf(p -> p[1] > camY + VH + 20);
            float topMost = platforms.stream().map(p -> p[1]).min(Float::compare).orElse(camY);
            while (topMost > camY - 20) {
                topMost -= 28 + rng.nextInt(20);
                spawnPlatformAt(topMost);
            }

            // Game Over wenn Spieler zu weit unten
            if (py - camY > VH + 20) {
                gameOver = true;
            }
        }

        public void render(DrawContext context) {
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

            // Hintergrund (Nachthimmel-Gradient)
            context.fill(ox, oy, ox + rw, oy + rh, 0xFF0A0A2A);
            // Sterne (deterministisch)
            for (int i = 0; i < 30; i++) {
                int sx = ox + (i * 37 + 13) % rw;
                int sy = oy + (i * 53 + 7)  % rh;
                context.fill(sx, sy, sx + 1, sy + 1, 0xAAFFFFFF);
            }

            // Rand
            context.fill(ox - 1, oy - 1, ox + rw + 1, oy,      0xFF55FFFF);
            context.fill(ox - 1, oy + rh, ox + rw + 1, oy + rh + 1, 0xFF55FFFF);
            context.fill(ox - 1, oy,     ox,           oy + rh, 0xFF55FFFF);
            context.fill(ox + rw, oy,    ox + rw + 1,  oy + rh, 0xFF55FFFF);

            // Plattformen
            for (float[] p : platforms) {
                float screenY = p[1] - camY;
                if (screenY < -PLT_H || screenY > VH + PLT_H) continue;

                int px2 = ox + (int)(p[0] * scale);
                int py2 = oy + (int)(screenY * scale);
                int pw2 = (int)(PLT_W * scale);
                int ph2 = Math.max(3, (int)(PLT_H * scale));

                int col, colDark;
                switch ((int) p[2]) {
                    case 1  -> { col = 0xFF4466FF; colDark = 0xFF2244CC; } // moving - blau
                    case 2  -> { col = 0xFFFFAA00; colDark = 0xFFCC8800; } // spring - gold
                    case 3  -> { col = p[3] == 1 ? 0xFFAA2222 : 0xFFCC3333; colDark = 0xFF881111; } // crumbling - rot
                    default -> { col = 0xFF44BB44; colDark = 0xFF228822; } // normal - grün
                }

                context.fill(px2, py2, px2 + pw2, py2 + ph2, col);
                context.fill(px2, py2, px2 + pw2, py2 + Math.max(1, ph2/3), colDark); // highlight oben

                // Feder bei Spring-Plattform
                if (p[2] == 2) {
                    int sx = px2 + pw2/2 - 1;
                    context.fill(sx, py2 - (int)(8*scale), sx + (int)(3*scale), py2, 0xFFFFFFFF);
                }
            }

            // Spieler (Doodler = kleiner grüner Kerl)
            int spx  = ox + (int)(px * scale);
            int spy  = oy + (int)((py - camY) * scale);
            int spw  = Math.max(8, (int)(PW * scale));
            int sph  = Math.max(8, (int)(PH * scale));

            // Körper
            context.fill(spx, spy, spx + spw, spy + sph, 0xFF44DD44);
            // Augen
            int ew = Math.max(2, spw / 5);
            context.fill(spx + spw/4,   spy + sph/4, spx + spw/4 + ew,   spy + sph/4 + ew, 0xFF000000);
            context.fill(spx + spw*3/5, spy + sph/4, spx + spw*3/5 + ew, spy + sph/4 + ew, 0xFF000000);
            // Lächeln
            context.fill(spx + spw/4, spy + sph*2/3, spx + spw*3/4, spy + sph*2/3 + Math.max(1, ew/2), 0xFF000000);

            // Info-Panel rechts
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

            context.drawText(textRenderer, "Höhe", ix, iy, 0xFFFFFFFF, true);
            iy += textRenderer.fontHeight + 1;
            context.drawText(textRenderer, (score / 10) + "m", ix, iy, 0xFF00AAFF, true);

            // Overlays
            if (!started) {
                context.fill(ox, oy, ox + rw, oy + rh, 0x88000000);
                context.drawCenteredTextWithShadow(textRenderer, "§a§lDoodle Jump",           ox + rw/2, oy + rh/2 - 16, 0xFF44FF44);
                context.drawCenteredTextWithShadow(textRenderer, "§7A/D zum Starten",         ox + rw/2, oy + rh/2,      0xFFAAAAAA);
            }

            if (paused) {
                context.fill(ox, oy, ox + rw, oy + rh, 0x88000000);
                context.drawCenteredTextWithShadow(textRenderer, "§e§lPAUSIERT",       ox + rw/2, oy + rh/2 - 8, 0xFFFFFF00);
                context.drawCenteredTextWithShadow(textRenderer, "§7P zum Fortfahren", ox + rw/2, oy + rh/2 + 6, 0xFFAAAAAA);
            }

            if (gameOver) {
                context.fill(ox, oy, ox + rw, oy + rh, 0x88000000);
                context.drawCenteredTextWithShadow(textRenderer, "§c§lGAME OVER",         ox + rw/2, oy + rh/2 - 18, 0xFFFF3333);
                context.drawCenteredTextWithShadow(textRenderer, "§eScore: §f" + score,    ox + rw/2, oy + rh/2 - 4,  0xFFFFFFFF);
                context.drawCenteredTextWithShadow(textRenderer, "§7Leertaste = Neustart", ox + rw/2, oy + rh/2 + 10, 0xFFAAAAAA);
            }
        }

        public void keyDown(int key) {
            switch (key) {
                case 65, 263 -> { leftDown = true;  if (!started) started = true; }
                case 68, 262 -> { rightDown = true; if (!started) started = true; }
                case 80 -> { if (!gameOver && started) paused = !paused; }
                case 32 -> { if (gameOver) restart(); else if (!started) started = true; }
            }
        }

        public void keyUp(int key) {
            switch (key) {
                case 65, 263 -> leftDown  = false;
                case 68, 262 -> rightDown = false;
            }
        }

        private void restart() {
            gameOver = false; paused = false; started = false;
            leftDown = false; rightDown = false;
            score = 0; camY = 0; maxCamY = 0;
            pvx = 0; pvy = 0;
            generateInitialPlatforms();
            float[] first = platforms.get(0);
            px = first[0] + PLT_W / 2f - PW / 2f;
            py = first[1] - PH;
            pvy = JUMP_VEL;
        }

        private void loadHighScore() {
            if (loadedHS) return;
            loadedHS = true;
            HS_FILE = new File(MinecraftClient.getInstance().runDirectory, "doodleHighscore.txt");
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
