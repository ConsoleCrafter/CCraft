package de.consolecrafter.ccraft.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.Blaze3D;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ScreenshotScreen extends CCraftScreen {

    private static final int COLS = 3;
    private static final int ROWS = 2;
    private static final int PER_PAGE = COLS * ROWS;
    private static final int TILE_W = 160;
    private static final int TILE_H = 90;
    private static final int GAP = 10;
    private static final Path DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("screenshots");

    private record Loaded(Identifier id, int width, int height) {}

    private final Map<Path, Loaded> textures = new HashMap<>();
    private final Map<Path, String> captions = new HashMap<>();
    private final List<Path> shots = new ArrayList<>();
    private int counter;
    private int page;
    private Path preview;
    private Path renaming;
    private EditBox captionField;
    private EditBox renameField;
    private String status = "";
    private int statusTicks;

    public ScreenshotScreen(Screen parent) {
        super("Screenshots", parent);
        reload();
    }

    @Override
    public void tick() {
        if (statusTicks > 0 && --statusTicks == 0) status = "";
    }

    private void reload() {
        releaseTextures();
        captions.clear();
        shots.clear();
        page = 0;
        if (!Files.isDirectory(DIRECTORY)) return;
        try (Stream<Path> files = Files.list(DIRECTORY)) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                    .forEach(shots::add);
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("Screenshots could not be listed", e);
        }
    }

    @Override
    protected void init() {
        captionField = null;
        renameField = null;
        int cx = width / 2;
        int y = height - 30;

        if (preview != null) {
            button("Zurück", cx - 235, y, 80, BUTTON_HEIGHT, () -> {
                preview = null;
                rebuildWidgets();
            });
            button("Umbenennen", cx - 150, y, 90, BUTTON_HEIGHT, () -> {
                renaming = preview;
                preview = null;
                rebuildWidgets();
            });
            button("Kopieren", cx - 55, y, 90, BUTTON_HEIGHT, this::copyPath);
            button("§cLöschen", cx + 40, y, 90, BUTTON_HEIGHT, this::deletePreview);
            button("Ordner", cx + 135, y, 90, BUTTON_HEIGHT, this::openFolder);

            captionField = new EditBox(font, cx - 160, height - 58, 320, 20, Component.literal("Bildunterschrift"));
            captionField.setMaxLength(200);
            captionField.setHint(Component.literal("Bildunterschrift... (Enter = speichern)"));
            captionField.setValue(caption(preview));
            addRenderableWidget(captionField);
            return;
        }

        backButton(cx - 155, y, 100);
        button("Ordner öffnen", cx - 50, y, 100, BUTTON_HEIGHT, this::openFolder);
        button("↻ Neu laden", cx + 55, y, 100, BUTTON_HEIGHT, () -> {
            reload();
            rebuildWidgets();
        });
        button("«", cx - 190, y, 30, BUTTON_HEIGHT, () -> {
            if (page > 0) page--;
        });
        button("»", cx + 160, y, 30, BUTTON_HEIGHT, () -> {
            if ((page + 1) * PER_PAGE < shots.size()) page++;
        });

        int index = renaming == null ? -1 : shots.indexOf(renaming);
        if (index >= 0) {
            int[] r = tile(index % PER_PAGE);
            renameField = new EditBox(font, r[0], r[1] + TILE_H / 2 - 10, TILE_W, 20, Component.literal("Name"));
            renameField.setMaxLength(80);
            renameField.setValue(renaming.getFileName().toString());
            addRenderableWidget(renameField);
            setInitialFocus(renameField);
        }
    }

    private void openFolder() {
        try {
            Files.createDirectories(DIRECTORY);
            Blaze3D.openPath(DIRECTORY);
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("Screenshot folder could not be opened", e);
        }
    }

    private void copyPath() {
        minecraft.keyboardHandler.setClipboard(preview.toAbsolutePath().toString());
        setStatus("§aPfad kopiert");
    }

    private void deletePreview() {
        try {
            Files.deleteIfExists(captionFile(preview));
            Files.deleteIfExists(preview);
            release(preview);
            shots.remove(preview);
        } catch (IOException e) {
            setStatus("§cLöschen fehlgeschlagen");
        }
        preview = null;
        rebuildWidgets();
    }

    private void finishRename() {
        String name = renameField.getValue().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!name.isEmpty()) {
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) name += ".png";
            Path target = renaming.resolveSibling(name);
            try {
                if (!target.equals(renaming)) {
                    Files.move(renaming, target, StandardCopyOption.REPLACE_EXISTING);
                    Path oldCaption = captionFile(renaming);
                    if (Files.exists(oldCaption)) Files.move(oldCaption, captionFile(target), StandardCopyOption.REPLACE_EXISTING);
                }
                setStatus("§aUmbenannt: §f" + name);
                reload();
            } catch (IOException e) {
                setStatus("§cUmbenennen fehlgeschlagen");
            }
        }
        renaming = null;
        rebuildWidgets();
    }

    private void saveCaption() {
        String caption = captionField.getValue().trim();
        Path file = captionFile(preview);
        try {
            if (caption.isEmpty()) Files.deleteIfExists(file);
            else Files.writeString(file, caption);
            captions.put(preview, caption);
            setStatus(caption.isEmpty() ? "§7Bildunterschrift entfernt" : "§aBildunterschrift gespeichert");
        } catch (IOException e) {
            setStatus("§cSpeichern fehlgeschlagen");
        }
    }

    private static Path captionFile(Path png) {
        String name = png.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return png.resolveSibling((dot > 0 ? name.substring(0, dot) : name) + ".txt");
    }

    private String caption(Path png) {
        return captions.computeIfAbsent(png, p -> {
            try {
                Path file = captionFile(p);
                return Files.exists(file) ? Files.readString(file).trim() : "";
            } catch (IOException e) {
                return "";
            }
        });
    }

    private Loaded texture(Path png) {
        Loaded cached = textures.get(png);
        if (cached != null) return cached;
        try (InputStream in = Files.newInputStream(png)) {
            NativeImage image = NativeImage.read(in);
            Identifier id = CCraftMod.id("screenshot/" + counter++);
            minecraft.getTextureManager().register(id, new DynamicTexture(() -> "ccraft_screenshot", image));
            Loaded loaded = new Loaded(id, image.getWidth(), image.getHeight());
            textures.put(png, loaded);
            return loaded;
        } catch (IOException e) {
            return null;
        }
    }

    private void release(Path png) {
        Loaded loaded = textures.remove(png);
        if (loaded != null) minecraft.getTextureManager().release(loaded.id());
    }

    private void releaseTextures() {
        for (Loaded loaded : textures.values()) minecraft.getTextureManager().release(loaded.id());
        textures.clear();
    }

    @Override
    public void removed() {
        releaseTextures();
        super.removed();
    }

    private void setStatus(String text) {
        status = text;
        statusTicks = 80;
    }

    private int[] tile(int index) {
        int gridW = COLS * TILE_W + (COLS - 1) * GAP;
        int x = (width - gridW) / 2 + index % COLS * (TILE_W + GAP);
        int y = 48 + index / COLS * (TILE_H + GAP);
        return new int[]{x, y, TILE_W, TILE_H};
    }

    private static boolean inside(double x, double y, int[] r) {
        return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3];
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean enter = event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER;
        if (renameField != null && enter) {
            finishRename();
            return true;
        }
        if (captionField != null && captionField.isFocused() && enter) {
            saveCaption();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (preview == null && renaming == null) {
            int start = page * PER_PAGE;
            for (int i = 0; i < PER_PAGE && start + i < shots.size(); i++) {
                if (inside(event.x(), event.y(), tile(i))) {
                    preview = shots.get(start + i);
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (renaming != null) {
            renaming = null;
            rebuildWidgets();
        } else if (preview != null) {
            preview = null;
            rebuildWidgets();
        } else {
            super.onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (preview != null) {
            extractPreview(g);
        } else {
            Theme.header(g, font, "Screenshots", width / 2);
            if (shots.isEmpty()) {
                g.centeredText(font, "§7Noch keine Screenshots in §fscreenshots/", width / 2, height / 2, Theme.TEXT_DIM);
            } else {
                extractGrid(g, mouseX, mouseY);
            }
        }
        if (!status.isEmpty()) g.centeredText(font, status, width / 2, height - 76, Theme.ACCENT);
    }

    private void extractGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < shots.size(); i++) {
            Path shot = shots.get(start + i);
            int[] r = tile(i);
            boolean highlight = shot.equals(renaming) || inside(mouseX, mouseY, r);
            Theme.roundedBox(g, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, 0xFF000000, highlight ? Theme.ACCENT : Theme.BORDER);
            if (shot.equals(renaming)) continue;

            Loaded loaded = texture(shot);
            if (loaded != null) {
                float scale = Math.min((float) TILE_W / loaded.width(), (float) TILE_H / loaded.height());
                int w = Math.max(1, (int) (loaded.width() * scale));
                int h = Math.max(1, (int) (loaded.height() * scale));
                g.blit(RenderPipelines.GUI_TEXTURED, loaded.id(), r[0] + (TILE_W - w) / 2, r[1] + (TILE_H - h) / 2,
                        0.0f, 0.0f, w, h, loaded.width(), loaded.height(), loaded.width(), loaded.height());
            }
            String caption = caption(shot);
            if (!caption.isEmpty()) {
                g.fill(r[0], r[1] + TILE_H - 11, r[0] + TILE_W, r[1] + TILE_H, 0xB0000000);
                g.text(font, "§f" + (caption.length() <= 22 ? caption : caption.substring(0, 21) + "…"), r[0] + 3, r[1] + TILE_H - 9, 0xFFFFFFFF, true);
            }
        }
        int pages = Math.max(1, (shots.size() + PER_PAGE - 1) / PER_PAGE);
        g.centeredText(font, "§7Seite " + (page + 1) + " / " + pages + "  ·  " + shots.size() + " Screenshots", width / 2, height - 48, Theme.TEXT_DIM);
    }

    private void extractPreview(GuiGraphicsExtractor g) {
        Loaded loaded = texture(preview);
        if (loaded == null) return;
        int maxW = width - 80;
        int maxH = height - 140;
        float scale = Math.min((float) maxW / loaded.width(), (float) maxH / loaded.height());
        int w = (int) (loaded.width() * scale);
        int h = (int) (loaded.height() * scale);
        int x = (width - w) / 2;
        int y = 30 + (maxH - h) / 2;
        Theme.roundedBox(g, x - 3, y - 3, w + 6, h + 6, 0xFF000000, Theme.ACCENT);
        g.blit(RenderPipelines.GUI_TEXTURED, loaded.id(), x, y, 0.0f, 0.0f, w, h, loaded.width(), loaded.height(), loaded.width(), loaded.height());
        g.centeredText(font, "§7" + preview.getFileName(), width / 2, 16, Theme.TEXT_DIM);
    }
}
