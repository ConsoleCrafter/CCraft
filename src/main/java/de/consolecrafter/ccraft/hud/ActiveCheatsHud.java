package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeigt die aktiven Cheats als Liste oben rechts an.
 *
 * <p>Die Liste ist rein informativ: Sie liest den Zustand aus
 * {@link CheatManager} und zeichnet nur, wenn mindestens ein Cheat laeuft.
 * Bewusst ohne Hintergrundbalken, damit das Spielbild sichtbar bleibt.</p>
 */
public final class ActiveCheatsHud {

    private ActiveCheatsHud() {}

    /** Rechtsbuendiger Abstand zum Bildschirmrand. */
    private static final int MARGIN = 4;
    private static final int LINE_H = 11;

    public static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options != null && client.options.hudHidden) return;

        List<String> labels = new ArrayList<>();
        for (Cheat cheat : CheatManager.all()) {
            if (cheat.isEnabled()) labels.add(cheat.name);
        }
        if (labels.isEmpty()) return;

        int right = client.getWindow().getScaledWidth() - MARGIN;
        int y = MARGIN + 4;

        // Das Keystrokes-Overlay sitzt standardmaessig ebenfalls oben rechts und
        // ist frei verschiebbar. Ueberlappt es die Spalte der Cheat-Liste, rutscht
        // die Liste darunter — sonst waeren beide Texte unlesbar uebereinander.
        if (CraftConfig.keystrokes) {
            int ksX = KeystrokesRenderer.positionX(client.getWindow().getScaledWidth());
            int ksY = KeystrokesRenderer.positionY(client.getWindow().getScaledHeight());
            int ksRight = ksX + KeystrokesRenderer.overlayWidth();
            int ksBottom = ksY + KeystrokesRenderer.overlayHeight();
            if (ksRight > right - 40 && ksY < y) {
                y = Math.max(y, ksBottom + 2);
            }
        }

        for (String label : labels) {
            int x = right - client.textRenderer.getWidth(label);
            ctx.drawText(client.textRenderer, label, x, y, Theme.ACCENT, true);
            y += LINE_H;
        }
    }
}
