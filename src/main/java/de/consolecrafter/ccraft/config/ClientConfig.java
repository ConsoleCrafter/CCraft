package de.consolecrafter.ccraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import de.consolecrafter.ccraft.CCraftMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ClientConfig {

    public static final int HUD_STYLE_COUNT = 6;
    public static final int LOGO_COUNT = 5;
    public static final int FONT_COUNT = 5;
    public static final int HUD_LINE_COUNT = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ccraft.json");

    private static ClientConfig instance = new ClientConfig();

    public boolean dark = true;
    public int accent = 0xFF4D4D;
    public String preset = "CCraft";
    public int fontChoice = 0;
    public boolean bundledPackInstalled = false;

    public int hudStyle = 0;
    public int logoChoice = 0;
    public int hudText = 0xFFFFFFFF;
    public boolean hudShadow = true;
    public boolean[] hudLines = {true, true, true, true, true, true, true, true, true, true};

    public boolean keystrokes = false;
    public int keystrokesSize = 24;
    public int keystrokesStyle = 0;
    public boolean keystrokesShowMouse = true;
    public boolean keystrokesShowSpace = true;
    public float keystrokesX = 1.0f;
    public float keystrokesY = 0.0f;

    public int chatFilter = 0;
    public boolean chatTimestamps = true;

    public String savedUser = "";
    public boolean saveLogin = true;

    public Map<String, Map<String, String>> modules = new HashMap<>();
    public Map<String, Integer> highscores = new HashMap<>();

    public static ClientConfig get() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
            if (loaded != null) instance = loaded.sanitized();
        } catch (IOException | JsonParseException e) {
            CCraftMod.LOGGER.warn("ccraft.json could not be read, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("ccraft.json could not be saved", e);
        }
    }

    public int hudColor() {
        return (hudText >>> 24) == 0 ? hudText | 0xFF000000 : hudText;
    }

    public int highscore(String game) {
        return highscores.getOrDefault(game, 0);
    }

    public void submitScore(String game, int score) {
        if (score > highscore(game)) {
            highscores.put(game, score);
            save();
        }
    }

    private ClientConfig sanitized() {
        fontChoice = clamp(fontChoice, 0, FONT_COUNT - 1);
        hudStyle = clamp(hudStyle, 0, HUD_STYLE_COUNT - 1);
        logoChoice = clamp(logoChoice, 0, LOGO_COUNT - 1);
        keystrokesStyle = clamp(keystrokesStyle, 0, 2);
        keystrokesSize = clamp(keystrokesSize, 18, 48);
        chatFilter = clamp(chatFilter, 0, 2);
        keystrokesX = clampUnit(keystrokesX);
        keystrokesY = clampUnit(keystrokesY);
        if (hudLines == null || hudLines.length != HUD_LINE_COUNT) {
            boolean[] fixed = new boolean[HUD_LINE_COUNT];
            java.util.Arrays.fill(fixed, true);
            if (hudLines != null) System.arraycopy(hudLines, 0, fixed, 0, Math.min(hudLines.length, HUD_LINE_COUNT));
            hudLines = fixed;
        }
        if (modules == null) modules = new HashMap<>();
        if (highscores == null) highscores = new HashMap<>();
        if (savedUser == null) savedUser = "";
        if (preset == null) preset = "CCraft";
        return this;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampUnit(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(1.0f, value)) : 0.0f;
    }
}
