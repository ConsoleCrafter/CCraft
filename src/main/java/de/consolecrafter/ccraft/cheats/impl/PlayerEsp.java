package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerEsp — hebt Spieler hervor.
 *
 * <p>Zwei voneinander unabhaengige Anzeigen: der Vanilla-Glow-Umriss (auch
 * durch Waende sichtbar) und ein zusaetzlicher 2D-Kasten mit Name und
 * Entfernung. Der Glow wird ueber {@code mixins.OutlineColorMixin} in der
 * gewuenschten Farbe gezeichnet.</p>
 */
public class PlayerEsp extends Cheat {

    public static final String MODE_BOX = "Box";
    public static final String MODE_GLOW = "Glow";
    public static final String MODE_BOTH = "Beide";

    private final Option mode = choice("mode", "Anzeige", MODE_BOTH, MODE_BOX, MODE_GLOW, MODE_BOTH);
    private final Option color = number("color", "Farbe (RGB)", 16711680, 0, 16777215, 1);
    private final Option showNames = bool("names", "Namen anzeigen", true);
    private final Option showDistance = bool("distance", "Entfernung anzeigen", true);
    private final Option maxDistance = number("maxDistance", "Max. Entfernung (m)", 128.0, 8.0, 512.0, 8.0);
    private final Option ownTeam = bool("ownTeam", "Eigenes Team ignorieren", false);

    /** Spieler, die aktuell gluehen (fuer die Ruecknahme beim Deaktivieren). */
    private final List<Entity> glowing = new ArrayList<>();

    public PlayerEsp() {
        super("playeresp", "PlayerESP", "Hebt Spieler mit Umriss, Name und Entfernung hervor.");
    }

    @Override
    protected void onDisable() {
        for (Entity entity : glowing) {
            if (entity != null) entity.setGlowing(false);
        }
        glowing.clear();
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean useGlow = usesGlow();
        List<Entity> nowGlowing = new ArrayList<>();

        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == client.player || target == client.getCameraEntity()) continue;
            if (ownTeam.asBoolean() && sameTeam(client.player, target)) continue;

            if (useGlow) {
                target.setGlowing(true);
                nowGlowing.add(target);
            }
        }

        // Glow bei Spielern zuruecknehmen, die nicht mehr hervorgehoben werden.
        for (Entity entity : glowing) {
            if (!nowGlowing.contains(entity)) entity.setGlowing(false);
        }
        glowing.clear();
        glowing.addAll(nowGlowing);
    }

    @Override
    protected void onHudRender(DrawContext ctx) {
        if (!usesBox()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int rgb = color.asInt();
        int border = EspRender.withAlpha(rgb, 0xFF);
        double max = maxDistance.asNumber();

        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == client.player || target == client.getCameraEntity()) continue;
            if (ownTeam.asBoolean() && sameTeam(client.player, target)) continue;
            if (client.player.distanceTo(target) > max) continue;

            double[] rect = EspRender.projectBox(target.getBoundingBox());
            if (rect == null) continue;

            EspRender.drawBoxOutline(ctx, rect, border);
            drawLabel(ctx, target, rect, border);
        }
    }

    private void drawLabel(DrawContext ctx, PlayerEntity target, double[] rect, int border) {
        if (!showNames.asBoolean() && !showDistance.asBoolean()) return;

        StringBuilder label = new StringBuilder();
        if (showNames.asBoolean()) label.append(target.getName().getString());
        if (showDistance.asBoolean()) {
            if (label.length() > 0) label.append(' ');
            label.append(String.format(java.util.Locale.ROOT, "[%.0fm]",
                    MinecraftClient.getInstance().player.distanceTo(target)));
        }

        var renderer = MinecraftClient.getInstance().textRenderer;
        int x = (int) Math.round((rect[0] + rect[2]) / 2.0 - renderer.getWidth(label.toString()) / 2.0);
        int y = (int) Math.round(rect[1]) - 10;
        ctx.drawTextWithShadow(renderer, label.toString(), x, y, border);
    }

    private boolean usesBox() {
        String mode = this.mode.asChoice();
        return MODE_BOX.equals(mode) || MODE_BOTH.equals(mode);
    }

    private boolean usesGlow() {
        String mode = this.mode.asChoice();
        return MODE_GLOW.equals(mode) || MODE_BOTH.equals(mode);
    }

    private boolean sameTeam(PlayerEntity self, PlayerEntity other) {
        var team = self.getScoreboardTeam();
        return team != null && team == other.getScoreboardTeam();
    }

    /** Farbe des Glow-Umrisses, oder {@code null} wenn kein Glow aktiv ist. */
    public static Integer glowColor(Entity entity) {
        Cheat cheat = CheatManager.byId("playeresp");
        if (!(cheat instanceof PlayerEsp esp) || !esp.isEnabled() || !esp.usesGlow()) return null;
        if (!(entity instanceof PlayerEntity)) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || entity == client.player) return null;
        if (esp.ownTeam.asBoolean() && esp.sameTeam(client.player, (PlayerEntity) entity)) return null;
        return esp.color.asInt();
    }
}
