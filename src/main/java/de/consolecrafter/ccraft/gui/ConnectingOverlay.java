package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ConnectingOverlay {

    private static final String[] STEPS = {"Ziel", "DNS", "Handshake", "Login", "Welt"};
    private static final int BOX_H = 158;

    private static final Deque<String> LOG = new ArrayDeque<>();
    private static String host = "";
    private static int port;
    private static String name = "";
    private static long startedAt;
    private static volatile String resolvedIp;
    private static volatile boolean resolving;

    private ConnectingOverlay() {}

    public static synchronized void start(String address, int addressPort, String serverName) {
        host = address == null ? "" : address;
        port = addressPort;
        name = serverName == null ? "" : serverName;
        startedAt = System.currentTimeMillis();
        resolvedIp = null;
        LOG.clear();
        if (host.isEmpty()) return;
        resolving = true;
        log("DNS-Auflösung gestartet");
        String target = host;
        Thread thread = new Thread(() -> {
            String ip = null;
            try {
                ip = InetAddress.getByName(target).getHostAddress();
            } catch (Exception ignored) {
            }
            String result = ip;
            Minecraft.getInstance().execute(() -> {
                if (!target.equals(host)) return;
                resolvedIp = result;
                resolving = false;
                log(result != null ? "IP: " + result : "IP-Auflösung fehlgeschlagen");
            });
        }, "ccraft-resolve");
        thread.setDaemon(true);
        thread.start();
    }

    private static synchronized void log(String entry) {
        if (entry.equals(LOG.peekLast())) return;
        LOG.addLast(entry);
        while (LOG.size() > 5) LOG.removeFirst();
    }

    public static void extract(GuiGraphicsExtractor g, Font font, int width, int bottomLimit) {
        long elapsed = System.currentTimeMillis() - startedAt;
        int step = host.isEmpty() ? 0 : elapsed < 900 ? 1 : elapsed < 2600 ? 2 : elapsed < 5000 ? 3 : 4;
        String phase = switch (step) {
            case 0 -> "Ziel wird ermittelt";
            case 1 -> "DNS-Auflösung läuft";
            case 2 -> "Verbindung wird aufgebaut";
            case 3 -> "Anmeldung am Server";
            default -> "Welt wird geladen";
        };

        int boxW = Math.min(420, width - 40);
        int x = width / 2 - boxW / 2;
        int y = Math.max(8, Math.min((bottomLimit - BOX_H) / 2 + 10, bottomLimit - BOX_H - 8));

        g.fill(x, y, x + boxW, y + BOX_H, 0xC0000000);
        Theme.outline(g, x, y, boxW, BOX_H, Theme.BORDER);
        g.centeredText(font, "§lServer betreten", width / 2, y + 10, Theme.ACCENT);
        Theme.divider(g, x + 14, y + 26, boxW - 28);

        int lineH = font.lineHeight + 3;
        int cy = y + 34;
        String target = (name.isEmpty() ? "" : name + "  ") + "§7" + host + (port > 0 ? ":" + port : "");
        g.text(font, "§7Ziel: §f" + (host.isEmpty() ? "?" : target), x + 16, cy, Theme.TEXT, true);
        cy += lineH;
        String ip = resolving ? "§7löst auf…" : resolvedIp != null ? "§a" + resolvedIp : "§cfehlgeschlagen";
        g.text(font, "§7Verbinde auf: §f" + ip, x + 16, cy, Theme.TEXT, true);
        cy += lineH;
        g.text(font, "§7Status: §e" + phase, x + 16, cy, Theme.TEXT, true);
        cy += lineH + 2;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < STEPS.length; i++) {
            bar.append(i <= step ? "§a●" : "§8○");
            if (i < STEPS.length - 1) bar.append("§8──");
        }
        g.text(font, bar.toString(), x + 16, cy, Theme.TEXT, true);
        cy += lineH;

        int barX = x + 16;
        int barW = boxW - 32;
        int chunk = barW / 4;
        int travel = Math.max(1, (barW - chunk) * 2);
        int phasePx = (int) (System.currentTimeMillis() / 8L % travel);
        int offset = phasePx <= barW - chunk ? phasePx : travel - phasePx;
        g.fill(barX, cy, barX + barW, cy + 5, 0xFF2A0E0E);
        g.fill(barX, cy, barX + barW, cy + 1, Theme.BORDER);
        g.fill(barX + offset, cy, barX + offset + chunk, cy + 5, Theme.ACCENT);

        int logY = y + BOX_H - 58;
        synchronized (ConnectingOverlay.class) {
            for (String entry : LOG) {
                g.text(font, "§8› §7" + entry, x + 16, logY, Theme.TEXT_FAINT, true);
                logY += font.lineHeight + 1;
            }
        }
    }
}
