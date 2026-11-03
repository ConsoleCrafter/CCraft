package de.consolecrafter.ccraft.feature;

import de.consolecrafter.ccraft.CCraftMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatLog {

    public record Entry(String time, String text, boolean chat) {}

    private static final int MAX_ENTRIES = 3000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("ccraft_chat.log");

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static BufferedWriter writer;
    private static boolean loaded;
    private static boolean failed;

    private ChatLog() {}

    public static synchronized List<Entry> entries() {
        load();
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
    }

    public static synchronized void add(String text, boolean chat) {
        load();
        Entry entry = new Entry(LocalDateTime.now().format(TIME), text.replace('\n', ' '), chat);
        ENTRIES.add(entry);
        if (ENTRIES.size() > MAX_ENTRIES) ENTRIES.subList(0, ENTRIES.size() - MAX_ENTRIES).clear();
        write(entry);
    }

    public static synchronized void clear() {
        ENTRIES.clear();
        close();
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("Chat log could not be cleared", e);
        }
    }

    public static synchronized Path export(List<Entry> entries) throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path out = FILE.resolveSibling("ccraft_chat_export_" + stamp + ".txt");
        List<String> lines = new ArrayList<>(entries.size());
        for (Entry e : entries) lines.add("[" + e.time() + "] " + (e.chat() ? "Chat" : "Sys") + " " + e.text());
        Files.write(out, lines);
        return out;
    }

    public static synchronized void close() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException ignored) {
        }
        writer = null;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(FILE)) return;
        try {
            List<String> lines = Files.readAllLines(FILE);
            int start = Math.max(0, lines.size() - MAX_ENTRIES);
            for (String line : lines.subList(start, lines.size())) {
                String[] parts = line.split("\t", 3);
                if (parts.length == 3) ENTRIES.add(new Entry(parts[0], parts[2], parts[1].equals("C")));
            }
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("Chat log could not be read", e);
        }
    }

    private static void write(Entry entry) {
        if (failed) return;
        try {
            if (writer == null) {
                Files.createDirectories(FILE.getParent());
                writer = Files.newBufferedWriter(FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            writer.write(entry.time() + "\t" + (entry.chat() ? "C" : "S") + "\t" + entry.text());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            failed = true;
            CCraftMod.LOGGER.warn("Chat log could not be written, logging to file is disabled", e);
        }
    }
}
