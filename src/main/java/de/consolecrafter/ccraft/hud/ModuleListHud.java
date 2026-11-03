package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.module.Module;
import de.consolecrafter.ccraft.module.ModuleManager;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ModuleListHud {

    private static final int MARGIN = 4;
    private static final int LINE_HEIGHT = 11;

    private ModuleListHud() {}

    public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden()) return;

        int right = g.guiWidth() - MARGIN;
        int y = MARGIN + 4;
        if (ClientConfig.get().keystrokes) {
            int ksX = KeystrokesHud.x(g.guiWidth());
            int ksY = KeystrokesHud.y(g.guiHeight());
            if (ksX + KeystrokesHud.width() > right - 40 && ksY < y) y = ksY + KeystrokesHud.height() + 2;
        }

        for (Module module : ModuleManager.all()) {
            if (!module.isEnabled()) continue;
            g.text(minecraft.font, module.name, right - minecraft.font.width(module.name), y, Theme.ACCENT, true);
            y += LINE_HEIGHT;
        }
    }
}
