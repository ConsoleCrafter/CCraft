package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

final class GameTutorialRenderer {

    static final int RAIL_WIDTH = 150;
    static final int PLAYFIELD_GAP = 20;
    private static final int LEFT = 10;
    private static final int MAX_TEXT_WIDTH = RAIL_WIDTH - LEFT;

    private GameTutorialRenderer() {}

    static int playfieldCenterX(int screenWidth) {
        return RAIL_WIDTH + (screenWidth - RAIL_WIDTH) / 2;
    }

    static int centeredPlayfieldX(int screenWidth, int playfieldWidth) {
        return playfieldCenterX(screenWidth) - playfieldWidth / 2;
    }

    static void draw(DrawContext context, TextRenderer renderer, int screenHeight, String... instructions) {
        List<String> wrapped = new ArrayList<>();
        for (String instruction : instructions) {
            String line = "";
            for (String word : instruction.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && renderer.getWidth(candidate) > MAX_TEXT_WIDTH) {
                    wrapped.add(line);
                    line = word;
                } else {
                    line = candidate;
                }
            }
            if (!line.isEmpty()) wrapped.add(line);
        }

        int lineHeight = renderer.fontHeight + 3;
        int totalHeight = renderer.fontHeight + 8 + wrapped.size() * lineHeight;
        int y = Math.max(42, (screenHeight - totalHeight) / 2);
        context.drawTextWithShadow(renderer, "STEUERUNG", LEFT, y, Theme.ACCENT);
        y += renderer.fontHeight + 5;
        context.fill(LEFT, y, LEFT + MAX_TEXT_WIDTH, y + 1, Theme.BORDER);
        y += 5;
        for (String line : wrapped) {
            context.drawTextWithShadow(renderer, line, LEFT, y, Theme.TEXT_DIM);
            y += lineHeight;
        }
    }
}