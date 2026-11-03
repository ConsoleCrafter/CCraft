package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.feature.ChatLog;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChatLoggerScreen extends CCraftScreen {

    private static final String[] FILTERS = {"Alle", "Chat", "System"};
    private static final int LINE_H = 10;
    private static final int PADDING = 6;
    private static final int AREA_TOP = 58;

    private record Line(String prefix, String text, boolean chat) {}

    private String query = "";
    private int scroll;
    private int total;
    private int matching;
    private String status = "";

    public ChatLoggerScreen(Screen parent) {
        super("Chat Logger", parent);
    }

    @Override
    protected void init() {
        EditBox search = new EditBox(font, PADDING + 2, 36, 200, 18, Component.literal("Suche"));
        search.setHint(Component.literal("Suchen…"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value.toLowerCase(Locale.ROOT);
            scroll = 0;
        });
        addRenderableWidget(search);

        ClientConfig config = ClientConfig.get();
        int y = height - 38;
        int w = 88;
        int gap = 3;
        backButton(PADDING, y, w);
        button("Quelle: " + FILTERS[config.chatFilter], PADDING + w + gap, y, w, BUTTON_HEIGHT, () -> {
            config.chatFilter = (config.chatFilter + 1) % FILTERS.length;
            ClientConfig.save();
            rebuildWidgets();
        });
        button("Zeit: " + (config.chatTimestamps ? "an" : "aus"), PADDING + (w + gap) * 2, y, w, BUTTON_HEIGHT, () -> {
            config.chatTimestamps = !config.chatTimestamps;
            ClientConfig.save();
            rebuildWidgets();
        });
        int right = width - PADDING - (w * 2 + gap);
        button("Export", right, y, w, BUTTON_HEIGHT, this::export);
        button("§cLeeren", right + w + gap, y, w, BUTTON_HEIGHT, () -> {
            ChatLog.clear();
            scroll = 0;
        });
    }

    private boolean passes(ChatLog.Entry entry) {
        int filter = ClientConfig.get().chatFilter;
        if (filter == 1 && !entry.chat() || filter == 2 && entry.chat()) return false;
        return query.isEmpty() || entry.text().toLowerCase(Locale.ROOT).contains(query);
    }

    private List<ChatLog.Entry> filtered() {
        List<ChatLog.Entry> out = new ArrayList<>();
        List<ChatLog.Entry> all = ChatLog.entries();
        total = all.size();
        for (ChatLog.Entry entry : all) if (passes(entry)) out.add(entry);
        matching = out.size();
        return out;
    }

    private void export() {
        try {
            status = "§aExportiert: " + ChatLog.export(filtered()).getFileName();
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("Chat log export failed", e);
            status = "§cExport fehlgeschlagen";
        }
    }

    private int visibleLines() {
        return Math.max(1, (height - AREA_TOP - 50) / LINE_H);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) (vertical * 3));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Theme.header(g, font, "Chat Logger", width / 2);

        int areaH = height - AREA_TOP - 44;
        g.fill(PADDING, AREA_TOP, width - PADDING, AREA_TOP + areaH, 0x88000000);

        boolean timestamps = ClientConfig.get().chatTimestamps;
        int maxW = width - PADDING * 2 - 8;
        List<Line> lines = new ArrayList<>();
        for (ChatLog.Entry entry : filtered()) {
            String prefix = timestamps ? "[" + entry.time() + "] " : "";
            List<String> parts = wrap(entry.text(), maxW - font.width(prefix));
            for (int i = 0; i < parts.size(); i++) lines.add(new Line(i == 0 ? prefix : "", parts.get(i), entry.chat()));
        }

        int visible = visibleLines();
        int maxScroll = Math.max(0, lines.size() - visible);
        scroll = Math.min(scroll, maxScroll);

        int y = AREA_TOP + 3;
        int indent = timestamps ? font.width("[00:00:00] ") : 0;
        for (int i = scroll; i < Math.min(lines.size(), scroll + visible); i++) {
            Line line = lines.get(i);
            if (!line.prefix().isEmpty()) g.text(font, line.prefix(), PADDING + 3, y, 0xFF888888, true);
            g.text(font, line.text(), PADDING + 3 + indent, y, line.chat() ? 0xFFFFFFFF : 0xFFFFD080, true);
            y += LINE_H;
        }

        if (lines.size() > visible) {
            int barX = width - PADDING - 5;
            int thumbH = Math.max(10, areaH * visible / lines.size());
            int thumbY = AREA_TOP + (int) ((float) scroll / maxScroll * (areaH - thumbH));
            g.fill(barX, AREA_TOP, barX + 4, AREA_TOP + areaH, 0x44FFFFFF);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xCCFFFFFF);
        }

        g.text(font, matching + " / " + total + " Einträge", PADDING, height - 52, Theme.TEXT_FAINT, true);
        if (!status.isEmpty()) g.text(font, status, PADDING + 120, height - 52, Theme.TEXT, true);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (font.width(text) <= maxWidth) {
            result.add(text);
            return result;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }
}
