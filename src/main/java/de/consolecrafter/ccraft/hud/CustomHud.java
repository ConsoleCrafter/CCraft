package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.ModInfo;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gl.RenderPipelines;

import java.util.ArrayList;
import java.util.List;

public class CustomHud {

    private static final Identifier[] LOGOS = {
            Identifier.of("ccraft", "textures/gui/ccraft_logo.png"),
            Identifier.of("ccraft", "textures/gui/CLASSIC.png"),
            Identifier.of("ccraft", "textures/gui/MODERN.png"),
            Identifier.of("ccraft", "textures/gui/CRINGE.png"),
            Identifier.of("ccraft", "textures/gui/OG.png")
    };

    /** Quellmasse der Logos, in derselben Reihenfolge wie {@link #LOGOS}. */
    private static final int[] LOGO_SRC_WIDTHS  = {1024, 1024, 476, 666, 331};
    private static final int[] LOGO_SRC_HEIGHTS = { 522,  522, 101, 375,  77};

    /** Gewaehltes Logo, begrenzt auf den gueltigen Bereich. */
    public static Identifier logo() {
        int index = Math.max(0, Math.min(LOGOS.length - 1, CraftConfig.logoChoice));
        return LOGOS[index];
    }

    /**
     * Anzeigehoehe des gewaehlten Logos bei gegebener Breite.
     *
     * <p>Die Varianten haben unterschiedliche Seitenverhaeltnisse (MODERN ist
     * 476x101, CRINGE 666x375, OG 331x77). Ohne diese Umrechnung wuerden sie auf
     * die feste Hoehe des Standard-Logos gestaucht.</p>
     */
    public static int logoHeightForWidth(int width) {
        int index = Math.max(0, Math.min(LOGOS.length - 1, CraftConfig.logoChoice));
        int srcW = LOGO_SRC_WIDTHS[index];
        int srcH = LOGO_SRC_HEIGHTS[index];
        return Math.max(1, Math.round(width * (srcH / (float) srcW)));
    }
    private static final Identifier SID_TRAIN_TEXTURE = Identifier.of("ccraft", "textures/sidtrain.png");
    private static final Identifier DEVIN_TRAIN_TEXTURE = Identifier.of("ccraft", "textures/devintrain.png");
    private static final Identifier MELON_TRAIN_TEXTURE = Identifier.of("ccraft", "textures/melontrain.png");
    private static final Identifier DANIEL_TRAIN_TEXTURE = Identifier.of("ccraft", "textures/danieltrain.png");

    private static final int LOGO_WIDTH_DISPLAY = 120;

    /** Farbe der ▶-Pfeile in den HUD-Zeilen (immer weiss). */
    private static final int ARROW_COLOR = 0xFFFFFF;
    /** Textfarbe der HUD-Zeilen (hellblau, gut lesbar auf dunklem Hintergrund). */
    private static final int TEXT_BLUE = 0x55AAFF;

    private static MutableText arrow() {
        return Text.literal("▶ ").styled(s -> s.withColor(ARROW_COLOR));
    }

    private static MutableText blue(String s) {
        return Text.literal(s).styled(st -> st.withColor(TEXT_BLUE));
    }

    private static long tpsWindowStart = System.currentTimeMillis();
    private static float currentTPS = 20.0f;
    private static int tpsTicks = 0;

    /** Zeitpunkt des letzten Server-/Weltbeitritts (für "Online"-Zeit). */
    private static long sessionStart = System.currentTimeMillis();
    private static java.util.UUID lastPlayerUuid = null;

    /** Wird bei jedem Client-Tick aufgerufen und misst die echte Tickrate. */
    public void onClientTick() {
        tpsTicks++;
        long now = System.currentTimeMillis();
        long elapsed = now - tpsWindowStart;
        if (elapsed >= 1000L) {
            currentTPS = Math.min(20.0f, tpsTicks * 1000.0f / elapsed);
            tpsTicks = 0;
            tpsWindowStart = now;
        }
    }

    public final Train sidTrain = new Train(SID_TRAIN_TEXTURE, 50, 549, 105);
    public final Train devinTrain = new Train(DEVIN_TRAIN_TEXTURE, 80, 549, 111);
    public final Train melonTrain = new Train(MELON_TRAIN_TEXTURE, 110, 553, 113);
    // Quelle: danieltrain.png ist 550x108. Die Werte muessen exakt zur Datei
    // passen, sonst wird der Zug gestaucht statt skaliert.
    public final Train danielTrain = new Train(DANIEL_TRAIN_TEXTURE, 140, 550, 108);

    /** Ein über den Bildschirm fahrender Zug. */
    public static class Train {
        /** Geschwindigkeit in GUI-Pixeln pro Sekunde (framerate-unabhängig, für alle Züge gleich). */
        private static final float TRAIN_SPEED = 150f;
        private static final int TRAIN_W = 183;   // Anzeigebreite (Quelle ist 549 breit)

        private final Identifier texture;
        private final int y;
        private final int srcW, srcH;
        private final int dispW, dispH;
        private boolean active = false;
        private float x;
        private long lastNanos = 0;

        Train(Identifier texture, int y, int srcW, int srcH) {
            this.texture = texture;
            this.y = y;
            this.srcW = srcW;
            this.srcH = srcH;
            this.dispW = TRAIN_W;
            this.dispH = Math.max(1, Math.round(TRAIN_W * (srcH / (float) srcW)));
            this.x = -dispW;
        }

        public void start() {
            active = true;
            x = -dispW;
            lastNanos = 0;
        }
    }

    public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return;

        BlockPos pos = player.getBlockPos();
        String serverAddress = client.getCurrentServerEntry() != null
                ? client.getCurrentServerEntry().address
                : "Singleplayer";
        int ping = client.getNetworkHandler() != null
                && client.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null
                ? client.getNetworkHandler().getPlayerListEntry(player.getUuid()).getLatency()
                : 0;

        long maxMemory = Runtime.getRuntime().maxMemory() / 1048576L;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;

        // Online-Zeit: bei Server-/Weltwechsel neu starten.
        if (lastPlayerUuid == null || !lastPlayerUuid.equals(player.getUuid())) {
            lastPlayerUuid = player.getUuid();
            sessionStart = System.currentTimeMillis();
        }

        String serverBrand = client.getNetworkHandler() != null && client.getNetworkHandler().getBrand() != null
                ? client.getNetworkHandler().getBrand()
                : "Vanilla";

        // Das Logo bleibt IMMER fest oben links — unabhängig von Stil/Position.
        drawLogo(context, 2, 2);

        int infoX = 2;
        int infoY = logoHeightForWidth(LOGO_WIDTH_DISPLAY) + 4;

        List<Text> all = new ArrayList<>();
        all.add(formatLine("XYZ", String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ())));
        all.add(formatLine("IGN", player.getName().getString()));
        all.add(formatLineFps("FPS", client.getCurrentFps()));
        all.add(formatLine("IP", serverAddress));
        all.add(formatLineTPS("TPS", currentTPS));
        all.add(formatLine("Software", serverBrand));
        all.add(formatLine("Ping", ping + "ms"));
        all.add(formatLine("Memory", usedMemory + "MB/" + maxMemory + "MB"));
        all.add(formatLine("Uhr", clockText()));
        all.add(formatLine("Online", onlineText()));

        List<Text> lines = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (i < CraftConfig.hudLines.length && CraftConfig.hudLines[i]) lines.add(all.get(i));
        }

        String header = "CCraft Client : " + ModInfo.display();
        switch (CraftConfig.hudStyle) {
            case 1 -> renderCompactHud(context, client, lines, infoX, infoY);
            case 2 -> renderBoxedHud(context, client, lines, infoX, infoY + 6);
            case 3 -> renderAccentHud(context, client, lines, infoX, infoY, header);
            case 4 -> renderMinimalHud(context, client, lines, infoX, infoY);
            case 5 -> renderNeonHud(context, client, lines, infoX, infoY, header);
            default -> renderClassicHud(context, client, lines, infoX, infoY);
        }

        renderTrain(context, sidTrain);
        renderTrain(context, devinTrain);
        renderTrain(context, melonTrain);
        renderTrain(context, danielTrain);

    }

    private static String clockText() {
        return java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static String onlineText() {
        long secs = (System.currentTimeMillis() - sessionStart) / 1000L;
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void drawLogo(DrawContext context, int x, int y) {
        int displayH = logoHeightForWidth(LOGO_WIDTH_DISPLAY);
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            logo(),
            x, y,
            0f, 0f,
            LOGO_WIDTH_DISPLAY, displayH,
            LOGO_WIDTH_DISPLAY, displayH
        );
    }

    /** Stil 0: Überschrift + Liste mit ▶-Zeilen. */
    private void renderClassicHud(DrawContext context, MinecraftClient client, List<Text> lines, int x, int y) {
        int lineHeight = client.textRenderer.fontHeight + 1;
        int cy = y;
        context.drawText(client.textRenderer, Text.literal("CCraft Client : " + ModInfo.display()).formatted(Formatting.BOLD),
                x, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
        cy += lineHeight;
        for (Text line : lines) {
            context.drawText(client.textRenderer, line, x, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
            cy += lineHeight;
        }
    }

    /** Stil 1: Kompakt — Key und Wert ohne Einrueckung, halber Zeilenabstand. */
    private void renderCompactHud(DrawContext context, MinecraftClient client, List<Text> lines, int x, int y) {
        int lineHeight = client.textRenderer.fontHeight;
        int cy = y;
        context.drawText(client.textRenderer, Text.literal("CCraft " + ModInfo.display()).formatted(Formatting.BOLD),
                x, cy, Theme.ACCENT, CraftConfig.hudShadow);
        cy += lineHeight + 1;
        for (Text line : lines) {
            context.drawText(client.textRenderer, line, x, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
            cy += lineHeight;
        }
    }

    /** Stil 3: Akzent — farbiger Balken links, Werte in Akzentfarbe. */
    private void renderAccentHud(DrawContext context, MinecraftClient client, List<Text> lines,
                                 int x, int y, String header) {
        int lineHeight = client.textRenderer.fontHeight + 1;
        int w = client.textRenderer.getWidth(header);
        for (Text line : lines) w = Math.max(w, client.textRenderer.getWidth(line));

        int pad = 5;
        int boxH = (lines.size() + 1) * lineHeight + 8;
        // Dunkle Flaeche mit Akzentkante links statt rundem Panel.
        context.fill(x, y - 4, x + w + pad * 2, y - 4 + boxH, 0x99000000);
        context.fill(x, y - 4, x + 2, y - 4 + boxH, Theme.ACCENT);

        int tx = x + pad + 3;
        int cy = y;
        context.drawText(client.textRenderer, Text.literal(header).formatted(Formatting.BOLD),
                tx, cy, Theme.ACCENT, CraftConfig.hudShadow);
        cy += lineHeight;
        for (Text line : lines) {
            context.drawText(client.textRenderer, line, tx, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
            cy += lineHeight;
        }
    }

    /** Stil 4: Minimal — nur die Werte, ohne Ueberschrift und ohne Deko. */
    private void renderMinimalHud(DrawContext context, MinecraftClient client, List<Text> lines, int x, int y) {
        int lineHeight = client.textRenderer.fontHeight + 1;
        int cy = y;
        for (Text line : lines) {
            context.drawText(client.textRenderer, line, x, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
            cy += lineHeight;
        }
    }

    /** Stil 5: Neon — Akzentrahmen mit dunkler Flaeche und leuchtender Schrift. */
    private void renderNeonHud(DrawContext context, MinecraftClient client, List<Text> lines,
                               int x, int y, String header) {
        int lineHeight = client.textRenderer.fontHeight + 1;
        int w = client.textRenderer.getWidth(header);
        for (Text line : lines) w = Math.max(w, client.textRenderer.getWidth(line));

        int pad = 6;
        int boxW = w + pad * 2;
        int boxH = (lines.size() + 1) * lineHeight + 8;
        int left = x - 2;
        int top = y - 4;

        context.fill(left, top, left + boxW, top + boxH, 0xCC0A0505);
        // 2px-Akzentrahmen.
        context.fill(left, top, left + boxW, top + 2, Theme.ACCENT);
        context.fill(left, top + boxH - 2, left + boxW, top + boxH, Theme.ACCENT);
        context.fill(left, top, left + 2, top + boxH, Theme.ACCENT);
        context.fill(left + boxW - 2, top, left + boxW, top + boxH, Theme.ACCENT);

        int tx = x + pad;
        int cy = y;
        context.drawText(client.textRenderer, Text.literal(header).formatted(Formatting.BOLD),
                tx, cy, 0xFFFFFFFF, true);
        cy += lineHeight;
        for (Text line : lines) {
            context.drawText(client.textRenderer, line, tx, cy, CraftConfig.hudColor(), true);
            cy += lineHeight;
        }
    }

    /** Stil 2: Liste in einem abgerundeten Panel. */
    private void renderBoxedHud(DrawContext context, MinecraftClient client, List<Text> lines, int x, int y) {
        String header = "CCraft Client : " + ModInfo.display();
        int w = client.textRenderer.getWidth(header);
        for (Text line : lines) w = Math.max(w, client.textRenderer.getWidth(line));

        int lineHeight = client.textRenderer.fontHeight + 1;
        int pad = 5;
        int boxW = w + pad * 2;
        int boxH = (lines.size() + 1) * lineHeight + 8;
        // Panel beginnt auf x (nicht x-3), damit der linke Rand vollständig sichtbar ist.
        Theme.panel(context, x, y - 4, boxW, boxH);

        int tx = x + pad;
        int cy = y;
        context.drawText(client.textRenderer, Text.literal(header).formatted(Formatting.BOLD),
                tx, cy, Theme.ACCENT, CraftConfig.hudShadow);
        cy += lineHeight;
        for (Text line : lines) {
            context.drawText(client.textRenderer, line, tx, cy, CraftConfig.hudColor(), CraftConfig.hudShadow);
            cy += lineHeight;
        }
    }

    private void renderTrain(DrawContext context, Train train) {
        if (!train.active) return;

        // Reihenfolge: x, y, u, v, Breite, Höhe, Quellregion, Texturgröße.
        // (Falsche Reihenfolge -> Ausschnitt statt Skalierung.)
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            train.texture,
            (int) train.x,
            train.y,
            0f,
            0f,
            train.dispW,
            train.dispH,
            train.srcW,
            train.srcH,
            train.srcW,
            train.srcH
        );

        long now = System.nanoTime();
        float dt = train.lastNanos == 0 ? 0f : (now - train.lastNanos) / 1_000_000_000f;
        train.lastNanos = now;
        dt = Math.min(dt, 0.1f); // Lag-Spitzen nicht als Sprung darstellen

        if (train.x > context.getScaledWindowWidth()) {
            train.active = false;
            train.x = -train.dispW;
            train.lastNanos = 0;
        } else {
            train.x += Train.TRAIN_SPEED * dt;
        }
    }

    // Pfeil (weiss) und Text (blau) tragen eigene Farben im Text-Style. Diese
    // ueberschreiben den Farbparameter von drawText, daher wirkt die HUD-Textfarbe
    // aus der Config auf diese Zeilen nicht mehr. Bei FPS/TPS bleibt die Ampel.
    private static MutableText formatLine(String key, String value) {
        return Text.empty()
                .append(arrow())
                .append(blue(key + ": " + value));
    }

    private static MutableText formatLineTPS(String key, float tps) {
        Formatting tpsColor;
        if (tps >= 19.0f) tpsColor = Formatting.GREEN;
        else if (tps >= 15.0f) tpsColor = Formatting.YELLOW;
        else tpsColor = Formatting.RED;

        return Text.empty()
                .append(arrow())
                .append(blue(key + ": "))
                .append(Text.literal(String.format("%.1f", tps)).formatted(tpsColor));
    }

    /** FPS mit Farbampel: grün ab 120, gelb ab 60, sonst rot. */
    private static MutableText formatLineFps(String key, int fps) {
        Formatting color = fps >= 120 ? Formatting.GREEN
                : fps >= 60 ? Formatting.YELLOW
                : Formatting.RED;
        return Text.empty()
                .append(arrow())
                .append(blue(key + ": "))
                .append(Text.literal(String.valueOf(fps)).formatted(color));
    }
}