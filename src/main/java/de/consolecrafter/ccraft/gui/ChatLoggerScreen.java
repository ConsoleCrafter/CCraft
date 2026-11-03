package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Ausgebauter Chat-Logger: Suche, Quellen-Filter (Chat/System), Export,
 * Zeitstempel-Schalter und scrollbare Liste.
 */
public class ChatLoggerScreen extends Screen {

    private final Screen parent;

    /** Ein Log-Eintrag. {@code chat} = aus dem Chat (sonst System/Game). */
    public static final class Entry {
        final String time;
        final String text;
        final boolean chat;
        Entry(String time, String text, boolean chat) {
            this.time = time; this.text = text; this.chat = chat;
        }
    }

    public static final List<Entry> LOG = new ArrayList<>();
    /** Obergrenze im Speicher, damit lange Sessions keinen RAM fressen. */
    private static final int MAX_ENTRIES = 3000;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static File LOG_FILE;

    private static final int LINE_H = 10;
    private static final int PADDING = 6;

    private TextFieldWidget searchField;
    private String query = "";
    private int scrollOffset = 0;

    public ChatLoggerScreen(Screen parent) {
        super(Text.literal("Chat Logger"));
        this.parent = parent;
    }

    // ── Wird vom ChatListener aufgerufen ─────────────────────────────────────
    public static void addMessage(String message, boolean chat) {
        String time = LocalDateTime.now().format(FMT);
        Entry entry = new Entry(time, message, chat);
        LOG.add(entry);
        if (LOG.size() > MAX_ENTRIES) {
            LOG.subList(0, LOG.size() - MAX_ENTRIES).clear();
        }
        appendToFile(entry);
    }

    private static void appendToFile(Entry e) {
        try {
            if (LOG_FILE == null) LOG_FILE = file();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                bw.write(e.time + "\t" + (e.chat ? "C" : "S") + "\t" + e.text);
                bw.newLine();
            }
        } catch (IOException ex) {
            // Frueher stumm verschluckt: das Log verschwand dann lautlos und es
            // gab keinerlei Hinweis, warum. Einmal pro Fehlerart loggen reicht,
            // sonst flutet jede Chat-Zeile das Log.
            warnOnce(ex);
        }
    }

    private static boolean warnedWrite = false;

    private static void warnOnce(IOException ex) {
        if (warnedWrite) return;
        warnedWrite = true;
        CCraftMod.LOGGER.warn("ccraft_chat.log konnte nicht geschrieben werden "
                + "(weitere Meldungen unterdrueckt).", ex);
    }

    public static void loadFromFile() {
        try {
            File f = file();
            LOG_FILE = f;
            if (!f.exists()) return;
            LOG.clear();
            for (String line : Files.readAllLines(f.toPath())) LOG.add(parse(line));
        } catch (IOException ex) {
            CCraftMod.LOGGER.warn("ccraft_chat.log konnte nicht gelesen werden.", ex);
        }
    }

    private static Entry parse(String line) {
        String[] parts = line.split("\t", 3);
        if (parts.length == 3) return new Entry(parts[0], parts[2], parts[1].equals("C"));
        // Altes Format ohne Typ: heuristisch als Chat erkennen
        if (line.startsWith("[") && line.contains("] ")) {
            int split = line.indexOf("] ");
            return new Entry(line.substring(1, split), line.substring(split + 2), line.contains("<"));
        }
        return new Entry("--:--:--", line, false);
    }

    private static File file() {
        return new File(MinecraftClient.getInstance().runDirectory, "ccraft_chat.log");
    }

    // ── Screen ────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        if (LOG.isEmpty()) loadFromFile();

        searchField = new TextFieldWidget(textRenderer, PADDING + 2, 36, 200, 18, Text.literal("Suche"));
        searchField.setPlaceholder(Text.literal("Suchen…"));
        searchField.setChangedListener(s -> { query = s.toLowerCase(); scrollOffset = 0; });
        searchField.setText(query);
        addDrawableChild(searchField);

        int y = this.height - 38;
        int bw = 88, bg = 3;
        int rx = this.width - PADDING - (bw * 5 + bg * 4);
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent))
                .position(PADDING, y).size(bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(filterName()), b -> {
            CraftConfig.chatFilter = (CraftConfig.chatFilter + 1) % 3;
            CraftConfig.save();
            this.clearAndInit();
        }).position(PADDING + (bw + bg), y).size(bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zeit: " + (CraftConfig.chatTimestamps ? "an" : "aus")), b -> {
            CraftConfig.chatTimestamps = !CraftConfig.chatTimestamps;
            CraftConfig.save();
            this.clearAndInit();
        }).position(PADDING + (bw + bg) * 2, y).size(bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Export"), b -> exportFiltered())
                .position(rx, y).size(bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§cLeeren"), b -> {
            LOG.clear();
            try { if (LOG_FILE != null) new FileWriter(LOG_FILE, false).close(); } catch (IOException ignored) {}
            scrollOffset = 0;
        }).position(rx + (bw + bg), y).size(bw, 20).build());
    }

    private String filterName() {
        return "Quelle: " + new String[]{"Alle", "Chat", "System"}[CraftConfig.chatFilter];
    }

    private void exportFiltered() {
        try {
            File out = new File(MinecraftClient.getInstance().runDirectory,
                    "ccraft_chat_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                for (Entry e : LOG) {
                    if (!passes(e)) continue;
                    bw.write("[" + e.time + "] " + (e.chat ? "Chat" : "Sys") + " " + e.text);
                    bw.newLine();
                }
            }
        } catch (IOException ignored) {}
    }

    private boolean passes(Entry e) {
        int f = CraftConfig.chatFilter;
        if (f == 1 && !e.chat) return false;
        if (f == 2 && e.chat) return false;
        return query.isEmpty() || e.text.toLowerCase().contains(query);
    }

    private List<Entry> filtered() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : LOG) if (passes(e)) out.add(e);
        return out;
    }

    private int visibleLines() {
        return Math.max(1, (this.height - 60 - 32) / LINE_H);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        int max = Math.max(0, totalWrapped() - visibleLines());
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) (vAmount * 3)));
        return true;
    }

    private List<String> wrapped;

    private int totalWrapped() {
        return wrapped != null ? wrapped.size() : 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        Theme.header(context, textRenderer, "Chat Logger", cx);

        int areaY = 58;
        int areaH = this.height - areaY - 44;
        context.fill(PADDING, areaY, this.width - PADDING, areaY + areaH, 0x88000000);

        // Filtern + umbrechen
        List<Entry> entries = filtered();
        int maxW = this.width - PADDING * 2 - 8;
        wrapped = new ArrayList<>();
        List<Entry> owners = new ArrayList<>();
        List<String> metas = new ArrayList<>();
        for (Entry e : entries) {
            String prefix = CraftConfig.chatTimestamps ? "[" + e.time + "] " : "";
            String full = prefix + e.text;
            List<String> parts = wrapLine(full, maxW);
            for (int i = 0; i < parts.size(); i++) {
                wrapped.add(parts.get(i));
                owners.add(e);
                metas.add((i == 0 ? prefix : "    ") + "|" + (e.chat ? "1" : "0"));
            }
        }

        int total = wrapped.size();
        int visible = visibleLines();
        int maxScroll = Math.max(0, total - visible);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int y = areaY + 3;
        for (int i = scrollOffset; i < Math.min(scrollOffset + visible, total); i++) {
            String line = wrapped.get(i);
            String meta = metas.get(i);
            boolean chat = meta.endsWith("1");
            String prefix = meta.substring(0, meta.lastIndexOf('|'));

            int color = chat ? 0xFFFFFFFF : 0xFFFFD080;
            if (!prefix.isBlank()) {
                context.drawText(textRenderer, prefix, PADDING + 3, y, 0xFF888888, true);
                context.drawText(textRenderer, line.substring(prefix.length()),
                        PADDING + 3 + textRenderer.getWidth(prefix), y, color, true);
            } else {
                context.drawText(textRenderer, line, PADDING + 3, y, color, true);
            }
            y += LINE_H;
        }

        if (total > visible) {
            int barX = this.width - PADDING - 5;
            int thumbH = Math.max(10, areaH * visible / total);
            int thumbY = areaY + (int) ((float) scrollOffset / Math.max(1, total - visible) * (areaH - thumbH));
            context.fill(barX, areaY, barX + 4, areaY + areaH, 0x44FFFFFF);
            context.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xCCFFFFFF);
        }

        context.drawText(textRenderer, entries.size() + " / " + LOG.size() + " Einträge",
                PADDING, this.height - 52, Theme.TEXT_FAINT, true);
    }

    private List<String> wrapLine(String line, int maxW) {
        List<String> result = new ArrayList<>();
        if (textRenderer.getWidth(line) <= maxW) {
            result.add(line);
            return result;
        }
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (textRenderer.getWidth(test) > maxW) {
                if (!current.isEmpty()) result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }

    @Override
    public boolean shouldPause() { return false; }
}
