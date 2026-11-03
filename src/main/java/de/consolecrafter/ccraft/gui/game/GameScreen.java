package de.consolecrafter.ccraft.gui.game;

import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.gui.CCraftScreen;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public abstract class GameScreen extends CCraftScreen {

    protected static final int RAIL_WIDTH = 150;
    protected static final int PLAYFIELD_GAP = 20;

    private static final int RAIL_LEFT = 10;
    private static final int RAIL_TEXT_WIDTH = RAIL_WIDTH - RAIL_LEFT;

    private final String gameId;

    protected GameScreen(String title, String gameId, Screen parent) {
        super(title, parent);
        this.gameId = gameId;
    }

    @Override
    protected void init() {
        backButton(width - 110, height - 30, 100);
    }

    protected abstract String[] instructions();

    protected abstract void extractGame(GuiGraphicsExtractor g, int mouseX, int mouseY);

    protected abstract int score();

    protected boolean showHeader() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (showHeader()) Theme.header(g, font, title.getString(), playfieldCenterX());
        extractGame(g, mouseX, mouseY);
        extractInstructions(g);
    }

    @Override
    public void removed() {
        saveScore();
        super.removed();
    }

    protected void saveScore() {
        ClientConfig.get().submitScore(gameId, score());
    }

    protected int highscore() {
        return Math.max(ClientConfig.get().highscore(gameId), score());
    }

    protected int playfieldCenterX() {
        return RAIL_WIDTH + (width - RAIL_WIDTH) / 2;
    }

    protected int centeredPlayfieldX(int playfieldWidth) {
        return playfieldCenterX() - playfieldWidth / 2;
    }

    protected void stat(GuiGraphicsExtractor g, String label, String value, int x, int y, int color) {
        g.text(font, label, x, y, 0xFFFFFFFF, true);
        g.text(font, value, x, y + font.lineHeight + 1, color, true);
    }

    protected void overlay(GuiGraphicsExtractor g, int x, int y, int w, int h, int alpha, String... lines) {
        g.fill(x, y, x + w, y + h, alpha << 24);
        int lineHeight = font.lineHeight + 6;
        int top = y + h / 2 - lines.length * lineHeight / 2;
        for (int i = 0; i < lines.length; i++) {
            g.centeredText(font, lines[i], x + w / 2, top + i * lineHeight, 0xFFFFFFFF);
        }
    }

    private void extractInstructions(GuiGraphicsExtractor g) {
        List<String> wrapped = new ArrayList<>();
        for (String instruction : instructions()) {
            String line = "";
            for (String word : instruction.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.width(candidate) > RAIL_TEXT_WIDTH) {
                    wrapped.add(line);
                    line = word;
                } else {
                    line = candidate;
                }
            }
            if (!line.isEmpty()) wrapped.add(line);
        }

        int lineHeight = font.lineHeight + 3;
        int y = Math.max(42, (height - (font.lineHeight + 8 + wrapped.size() * lineHeight)) / 2);
        g.text(font, "STEUERUNG", RAIL_LEFT, y, Theme.ACCENT, true);
        y += font.lineHeight + 5;
        g.fill(RAIL_LEFT, y, RAIL_LEFT + RAIL_TEXT_WIDTH, y + 1, Theme.BORDER);
        y += 5;
        for (String line : wrapped) {
            g.text(font, line, RAIL_LEFT, y, Theme.TEXT_DIM, true);
            y += lineHeight;
        }
    }
}
