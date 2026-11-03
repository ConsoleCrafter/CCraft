package de.consolecrafter.ccraft.cheats.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Hilfsfunktionen fuer die 2D-Darstellung von Weltpositionen (ESP).
 *
 * <p>Weltkoordinaten werden ueber {@code GameRenderer.project} in den
 * normalisierten Geraeteraum und von dort in Bildschirmkoordinaten umgerechnet.
 * Punkte hinter der Kamera liefern {@code null}.</p>
 */
public final class EspRender {

    private EspRender() {}

    /** Projektionsergebnis in Bildschirmkoordinaten. */
    public record ScreenPoint(double x, double y) {}

    /** Projiziert einen Weltpunkt; {@code null}, wenn er hinter der Kamera liegt. */
    public static ScreenPoint project(Vec3d world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null) return null;

        Vec3d ndc = client.gameRenderer.project(world);
        if (ndc.z > 1.0 || ndc.z < -1.0) return null;

        double width = client.getWindow().getScaledWidth();
        double height = client.getWindow().getScaledHeight();
        double x = (ndc.x * 0.5 + 0.5) * width;
        double y = (1.0 - (ndc.y * 0.5 + 0.5)) * height;
        return new ScreenPoint(x, y);
    }

    /** Bildschirmrechteck, das die projizierte Box umschliesst; {@code null} bei Projektionsfehler. */
    public static double[] projectBox(Box box) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX : box.maxX;
            double y = (i & 2) == 0 ? box.minY : box.maxY;
            double z = (i & 4) == 0 ? box.minZ : box.maxZ;
            ScreenPoint point = project(new Vec3d(x, y, z));
            if (point == null) return null;
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
        }
        return new double[] { minX, minY, maxX, maxY };
    }

    /** Zeichnet den Umriss eines Rechtecks. */
    public static void drawBoxOutline(DrawContext ctx, double[] rect, int color) {
        if (rect == null) return;
        int x1 = (int) Math.round(rect[0]);
        int y1 = (int) Math.round(rect[1]);
        int x2 = (int) Math.round(rect[2]);
        int y2 = (int) Math.round(rect[3]);
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    /** Farbwert {@code 0xRRGGBB} mit Alpha zu {@code 0xAARRGGBB} ergaenzen. */
    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}
