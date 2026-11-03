package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Overlay auf dem Verbindungs-Screen. Ersetzt das Vanilla-"Joining Server"
 * durch eine eigene Box, die live zeigt, was gerade passiert: Auflösen der
 * Adresse zur IP, Verbindungsaufbau, Login … bis zum Server-Beitritt.
 *
 * <p>Der Zustand ist statisch, weil {@code ConnectScreen.connect} eine
 * statische Methode ist (der Mixin-Handler kann daher nichts Instanzbezogenes
 * speichern).
 */
public final class ConnectingOverlay {

    private ConnectingOverlay() {}

    private static final Deque<String> LOG = new ArrayDeque<>();
    private static String lastKey = "";
    private static String resolvedIp = null;
    private static volatile boolean resolving = false;
    private static String host = "";
    private static int port = 25565;
    private static String name = "";
    private static long startedAt = System.currentTimeMillis();

    /** Vom {@code ConnectScreenMixin} gesetzt — von JEDER Thread aufrufbar. */
    public static void setTarget(String address, int addressPort, String serverName) {
        String h = address == null ? "" : address;
        if (h.equals(host) && addressPort == port) return;
        host = h;
        port = addressPort;
        name = serverName == null ? "" : serverName;
        lastKey = "";
        LOG.clear();
        resolvedIp = null;
        startedAt = System.currentTimeMillis();
    }

    private static void log(String entry) {
        if (!LOG.isEmpty() && LOG.peekLast().equals(entry)) return;
        LOG.addLast(entry);
        while (LOG.size() > 5) LOG.removeFirst();
    }

    /**
     * Löst den Host hinter der Adresse asynchron auf, damit "Verbinde auf …"
     * wirklich eine IP zeigt (nicht die Domain). Wird pro Adresse nur einmal
     * gestartet.
     */
    private static void resolveAsync(MinecraftClient client, final String toResolve) {
        if (toResolve.equals(lastKey)) return;
        lastKey = toResolve;
        resolving = true;
        log("DNS-Auflösung gestartet");
        Thread t = new Thread(() -> {
            String ip = null;
            try {
                ip = InetAddress.getByName(toResolve).getHostAddress();
            } catch (Exception ignored) {
            }
            final String result = ip;
            client.execute(() -> {
                resolvedIp = result;
                resolving = false;
                log(result != null ? "IP: " + result : "IP-Auflösung fehlgeschlagen");
            });
        }, "ccraft-resolve");
        t.setDaemon(true);
        t.start();
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width  = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        var tr = client.textRenderer;

        if (!host.isEmpty()) resolveAsync(client, host);

        // Phasen-Fortschritt (grob geschätzt über die verstrichene Zeit).
        long elapsed = System.currentTimeMillis() - startedAt;
        String phase = host.isEmpty()
                ? "Ziel wird ermittelt"
                : elapsed < 900 ? "DNS-Auflösung läuft"
                : elapsed < 2600 ? "Verbindung wird aufgebaut"
                : elapsed < 5000 ? "Anmeldung am Server"
                : "Welt wird geladen";
        int step = host.isEmpty() ? 0 : elapsed < 900 ? 1 : elapsed < 2600 ? 2 : elapsed < 5000 ? 3 : 4;
        String[] STEPS = {"Ziel", "DNS", "Handshake", "Login", "Welt"};

        int boxW = Math.min(420, width - 40);
        int boxH = 158;
        int x = width / 2 - boxW / 2;
        int y = height / 2 - boxH / 2;

        // Box
        context.fill(x, y, x + boxW, y + boxH, 0xC0000000);
        context.fill(x, y, x + boxW, y + 1, Theme.BORDER);
        context.fill(x, y + boxH - 1, x + boxW, y + boxH, Theme.BORDER);
        context.fill(x, y, x + 1, y + boxH, Theme.BORDER);
        context.fill(x + boxW - 1, y, x + boxW, y + boxH, Theme.BORDER);

        // Kopfzeile
        context.drawCenteredTextWithShadow(tr, "§lServer betreten", width / 2, y + 10, Theme.ACCENT);
        Theme.divider(context, x + 14, y + 26, boxW - 28);

        int cy = y + 34;
        int lineH = tr.fontHeight + 3;

        // Ziel-Server
        String target = (name.isEmpty() ? "" : name + "  ") + "§7" + host + (port > 0 ? ":" + port : "");
        context.drawTextWithShadow(tr, "§7Ziel: §f" + (host.isEmpty() ? "?" : target), x + 16, cy, Theme.TEXT);
        cy += lineH;

        // Auflösung zur IP — genau die IP, nicht die Domain.
        String ipLine;
        if (resolving) ipLine = "§7löst auf…";
        else if (resolvedIp != null) ipLine = "§a" + resolvedIp;
        else ipLine = "§cfehlgeschlagen";
        context.drawTextWithShadow(tr, "§7Verbinde auf: §f" + ipLine, x + 16, cy, Theme.TEXT);
        cy += lineH;

        // Aktuelle Phase
        context.drawTextWithShadow(tr, "§7Status: §e" + phase, x + 16, cy, Theme.TEXT);
        cy += lineH + 2;

        // Punkte-Fortschritt
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < STEPS.length; i++) {
            bar.append(i <= step ? "§a●" : "§8○");
            if (i < STEPS.length - 1) bar.append("§8──");
        }
        context.drawTextWithShadow(tr, bar.toString(), x + 16, cy, Theme.TEXT);
        cy += lineH;

        // Animierter, hin- und herlaufender Balken
        int barX = x + 16;
        int barW = boxW - 32;
        context.fill(barX, cy, barX + barW, cy + 5, 0xFF2A0E0E);
        context.fill(barX, cy, barX + barW, cy + 1, Theme.BORDER);
        int chunk = barW / 4;
        int travel = Math.max(1, (barW - chunk) * 2);
        int phasePx = (int) ((System.currentTimeMillis() / 8L) % travel);
        int offset = phasePx <= (barW - chunk) ? phasePx : travel - phasePx;
        context.fill(barX + offset, cy, barX + offset + chunk, cy + 5, Theme.ACCENT);

        // Log der letzten Schritte
        int logY = y + boxH - 58;
        for (String entry : LOG) {
            context.drawTextWithShadow(tr, "§8› §7" + entry, x + 16, logY, Theme.TEXT_FAINT);
            logY += tr.fontHeight + 1;
        }

        context.drawCenteredTextWithShadow(tr, "§7Esc zum Abbrechen", width / 2, y + boxH - 14, Theme.TEXT_FAINT);
    }
}
