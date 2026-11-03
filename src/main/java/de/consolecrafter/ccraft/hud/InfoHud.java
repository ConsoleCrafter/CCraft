package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.feature.TickRate;
import de.consolecrafter.ccraft.mixin.MinecraftAccessor;
import de.consolecrafter.ccraft.util.ModInfo;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InfoHud {

    public static final String[] LINE_NAMES = {"XYZ", "IGN", "FPS", "IP", "TPS", "Software", "Ping", "Memory", "Uhr", "Online"};

    private static final int LOGO_WIDTH = 120;
    private static final int ARROW_COLOR = 0xFFFFFF;
    private static final int TEXT_BLUE = 0x55AAFF;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static long sessionStart = System.currentTimeMillis();

    private InfoHud() {}

    public static void onJoin() {
        sessionStart = System.currentTimeMillis();
    }

    public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.hud.isHidden()) return;

        int top = 2 + Logos.draw(g, 2, 2, LOGO_WIDTH) + 2;
        List<Component> lines = lines(minecraft, player);
        Font font = minecraft.font;
        ClientConfig config = ClientConfig.get();
        String header = "CCraft Client : " + ModInfo.display();

        switch (config.hudStyle) {
            case 1 -> list(g, font, Component.literal("CCraft " + ModInfo.display()).withStyle(ChatFormatting.BOLD), Theme.ACCENT, lines, 2, top, font.lineHeight);
            case 2 -> boxed(g, font, header, lines, 2, top + 6);
            case 3 -> accent(g, font, header, lines, 2, top);
            case 4 -> list(g, font, null, 0, lines, 2, top, font.lineHeight + 1);
            case 5 -> neon(g, font, header, lines, 2, top);
            default -> list(g, font, Component.literal(header).withStyle(ChatFormatting.BOLD), config.hudColor(), lines, 2, top, font.lineHeight + 1);
        }

        Trains.extract(g);
    }

    private static List<Component> lines(Minecraft minecraft, LocalPlayer player) {
        BlockPos pos = player.blockPosition();
        ClientPacketListener connection = minecraft.getConnection();
        PlayerInfo info = connection != null ? connection.getPlayerInfo(player.getUUID()) : null;
        String brand = connection != null && connection.serverBrand() != null ? connection.serverBrand() : "Vanilla";
        String address = minecraft.getCurrentServer() != null ? minecraft.getCurrentServer().ip : "Singleplayer";
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / 1048576L;
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        int fps = MinecraftAccessor.ccraft$getFps();
        float tps = TickRate.tps();

        List<Component> all = List.of(
                line("XYZ", pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                line("IGN", player.getName().getString()),
                rated("FPS", String.valueOf(fps), fps >= 120 ? ChatFormatting.GREEN : fps >= 60 ? ChatFormatting.YELLOW : ChatFormatting.RED),
                line("IP", address),
                rated("TPS", String.format(Locale.ROOT, "%.1f", tps), tps >= 19 ? ChatFormatting.GREEN : tps >= 15 ? ChatFormatting.YELLOW : ChatFormatting.RED),
                line("Software", brand),
                line("Ping", (info != null ? info.getLatency() : 0) + "ms"),
                line("Memory", used + "MB/" + max + "MB"),
                line("Uhr", LocalTime.now().format(CLOCK)),
                line("Online", online()));

        boolean[] enabled = ClientConfig.get().hudLines;
        List<Component> visible = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (i < enabled.length && enabled[i]) visible.add(all.get(i));
        }
        return visible;
    }

    private static String online() {
        long seconds = (System.currentTimeMillis() - sessionStart) / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private static MutableComponent arrow() {
        return Component.literal("▶ ").withColor(ARROW_COLOR);
    }

    private static MutableComponent line(String key, String value) {
        return Component.empty().append(arrow()).append(Component.literal(key + ": " + value).withColor(TEXT_BLUE));
    }

    private static MutableComponent rated(String key, String value, ChatFormatting color) {
        return Component.empty().append(arrow())
                .append(Component.literal(key + ": ").withColor(TEXT_BLUE))
                .append(Component.literal(value).withStyle(color));
    }

    private static int widest(Font font, String header, List<Component> lines) {
        int w = font.width(header);
        for (Component line : lines) w = Math.max(w, font.width(line));
        return w;
    }

    private static void list(GuiGraphicsExtractor g, Font font, Component header, int headerColor,
                             List<Component> lines, int x, int y, int lineHeight) {
        ClientConfig config = ClientConfig.get();
        int cy = y;
        if (header != null) {
            g.text(font, header, x, cy, headerColor, config.hudShadow);
            cy += lineHeight + 1;
        }
        for (Component line : lines) {
            g.text(font, line, x, cy, config.hudColor(), config.hudShadow);
            cy += lineHeight;
        }
    }

    private static void boxed(GuiGraphicsExtractor g, Font font, String header, List<Component> lines, int x, int y) {
        int lineHeight = font.lineHeight + 1;
        int pad = 5;
        Theme.panel(g, x, y - 4, widest(font, header, lines) + pad * 2, (lines.size() + 1) * lineHeight + 8);
        list(g, font, Component.literal(header).withStyle(ChatFormatting.BOLD), Theme.ACCENT, lines, x + pad, y, lineHeight);
    }

    private static void accent(GuiGraphicsExtractor g, Font font, String header, List<Component> lines, int x, int y) {
        int lineHeight = font.lineHeight + 1;
        int pad = 5;
        int boxH = (lines.size() + 1) * lineHeight + 8;
        g.fill(x, y - 4, x + widest(font, header, lines) + pad * 2 + 3, y - 4 + boxH, 0x99000000);
        g.fill(x, y - 4, x + 2, y - 4 + boxH, Theme.ACCENT);
        list(g, font, Component.literal(header).withStyle(ChatFormatting.BOLD), Theme.ACCENT, lines, x + pad + 3, y, lineHeight);
    }

    private static void neon(GuiGraphicsExtractor g, Font font, String header, List<Component> lines, int x, int y) {
        int lineHeight = font.lineHeight + 1;
        int pad = 6;
        int boxW = widest(font, header, lines) + pad * 2;
        int boxH = (lines.size() + 1) * lineHeight + 8;
        g.fill(x - 2, y - 4, x - 2 + boxW, y - 4 + boxH, 0xCC0A0505);
        for (int i = 0; i < 2; i++) Theme.outline(g, x - 2 + i, y - 4 + i, boxW - 2 * i, boxH - 2 * i, Theme.ACCENT);
        list(g, font, Component.literal(header).withStyle(ChatFormatting.BOLD), 0xFFFFFFFF, lines, x + pad, y, lineHeight);
    }
}
