package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Galerie aller Minecraft-Screenshots aus dem {@code screenshots/}-Ordner.
 * Klick oeffnet die Vollansicht mit Umbenennen, Bildunterschrift und Kopieren
 * (Dateipfad in die Zwischenablage).
 */
public class ScreenshotScreen extends Screen {

    private final Screen parent;

    private static final int COLS = 3;
    private static final int ROWS = 2;
    private static final int PER_PAGE = COLS * ROWS;
    private static final int TILE_W = 160;
    private static final int TILE_H = 90;
    private static final int GAP = 10;

    private static final Map<String, Identifier> TEX = new HashMap<>();
    private static final Map<String, int[]> DIM = new HashMap<>();
    private static int texCounter = 0;

    private final List<Path> shots = new ArrayList<>();
    private int page = 0;
    private Path preview = null; // null = Uebersicht

    private TextFieldWidget captionField;
    private String status = "";
    private int statusTicks = 0;

    // Inline-Umbenennen
    private Path renaming = null;
    private TextFieldWidget renameField;

    public ScreenshotScreen(Screen parent) {
        super(Text.literal("Screenshots"));
        this.parent = parent;
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0 && --statusTicks == 0) status = "";
    }

    @Override
    protected void init() {
        super.init();
        captionField = null;
        renameField = null;
        if (shots.isEmpty()) loadShots();

        if (preview != null) {
            addPreviewButtons();
            return;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> client.setScreen(parent))
                .position(this.width / 2 - 155, this.height - 30).size(100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ordner oeffnen"), b -> openFolder())
                .position(this.width / 2 - 50, this.height - 30).size(100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u21bb Neu laden"), b -> {
            TEX.clear(); DIM.clear(); shots.clear(); page = 0; loadShots(); this.clearAndInit();
        }).position(this.width / 2 + 55, this.height - 30).size(100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("\u00ab"), b -> { if (page > 0) { page--; this.clearAndInit(); } })
                .position(this.width / 2 - 190, this.height - 30).size(30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00bb"), b -> { if ((page + 1) * PER_PAGE < shots.size()) { page++; this.clearAndInit(); } })
                .position(this.width / 2 + 160, this.height - 30).size(30, 20).build());

        if (renaming != null) {
            int idx = shots.indexOf(renaming);
            if (idx >= 0) {
                int[] r = tileRect(idx % PER_PAGE);
                renameField = new TextFieldWidget(textRenderer, r[0], r[1] + TILE_H / 2 - 10, TILE_W, 20, Text.literal("Name"));
                renameField.setMaxLength(80);
                renameField.setText(renaming.getFileName().toString());
                renameField.setFocused(true);
                addDrawableChild(renameField);
            }
        }
    }

    private void addPreviewButtons() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> { preview = null; this.clearAndInit(); })
                .position(this.width / 2 - 235, this.height - 30).size(80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Umbenennen"), b -> startRename())
                .position(this.width / 2 - 150, this.height - 30).size(90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Kopieren"), b -> copyPath())
                .position(this.width / 2 - 55, this.height - 30).size(90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cLoeschen"), b -> deletePreview())
                .position(this.width / 2 + 40, this.height - 30).size(90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ordner"), b -> openFolder())
                .position(this.width / 2 + 135, this.height - 30).size(90, 20).build());

        captionField = new TextFieldWidget(textRenderer, this.width / 2 - 160, this.height - 58, 320, 20,
                Text.literal("Bildunterschrift"));
        captionField.setMaxLength(200);
        captionField.setPlaceholder(Text.literal("Bildunterschrift... (Enter = speichern)"));
        captionField.setText(readCaption(preview));
        addDrawableChild(captionField);
    }

    private Path shotsDir() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots");
    }

    private void loadShots() {
        Path dir = shotsDir();
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                    .forEach(shots::add);
        } catch (IOException ignored) {}
    }

    private void openFolder() {
        try {
            Files.createDirectories(shotsDir());
            Util.getOperatingSystem().open(shotsDir().toFile());
        } catch (IOException ignored) {}
    }

    private void startRename() {
        if (renaming != null) return;
        renaming = preview;
        preview = null;
        this.clearAndInit();
    }

    private void finishRename() {
        if (renaming == null || renameField == null) return;
        String name = renameField.getText().trim();
        if (name.isEmpty()) { cancelRename(); return; }
        if (!name.toLowerCase().endsWith(".png")) name += ".png";
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path target = renaming.resolveSibling(name);
        try {
            if (!target.equals(renaming)) {
                Files.move(renaming, target, StandardCopyOption.REPLACE_EXISTING);
                Path oldCap = captionFile(renaming);
                if (Files.exists(oldCap)) Files.move(oldCap, captionFile(target), StandardCopyOption.REPLACE_EXISTING);
            }
            setStatus("\u00a7aUmbenannt: \u00a7f" + name);
            TEX.clear(); DIM.clear(); shots.clear(); loadShots();
        } catch (IOException e) {
            setStatus("\u00a7cUmbenennen fehlgeschlagen");
        }
        renaming = null;
        renameField = null;
        this.clearAndInit();
    }

    private void cancelRename() {
        renaming = null;
        renameField = null;
        this.clearAndInit();
    }

    private void copyPath() {
        if (preview == null) return;
        String path = preview.toAbsolutePath().toString();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path), null);
            setStatus("\u00a7aPfad kopiert");
        } catch (Throwable t) {
            setStatus("\u00a7cKopieren nicht moeglich");
        }
    }

    private Path captionFile(Path png) {
        String n = png.getFileName().toString();
        int dot = n.lastIndexOf('.');
        String base = dot > 0 ? n.substring(0, dot) : n;
        return png.resolveSibling(base + ".txt");
    }

    private String readCaption(Path png) {
        Path f = captionFile(png);
        if (!Files.exists(f)) return "";
        try { return Files.readString(f).trim(); } catch (IOException e) { return ""; }
    }

    private void saveCaption() {
        if (preview == null || captionField == null) return;
        String caption = captionField.getText().trim();
        Path f = captionFile(preview);
        try {
            if (caption.isEmpty()) Files.deleteIfExists(f);
            else try (BufferedWriter w = Files.newBufferedWriter(f)) { w.write(caption); }
            setStatus(caption.isEmpty() ? "\u00a77Bildunterschrift entfernt" : "\u00a7aBildunterschrift gespeichert");
        } catch (IOException e) {
            setStatus("\u00a7cSpeichern fehlgeschlagen");
        }
    }

    private void deletePreview() {
        try {
            Files.deleteIfExists(captionFile(preview));
            Files.deleteIfExists(preview);
            shots.remove(preview);
        } catch (IOException ignored) {}
        preview = null;
        this.clearAndInit();
    }

    private Identifier textureFor(Path p) {
        String key = p.toString();
        if (TEX.containsKey(key)) return TEX.get(key);
        try (var in = Files.newInputStream(p)) {
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "ccraft_screenshot", img);
            String name = p.getFileName().toString().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
            Identifier id = Identifier.of("ccraft", "screenshot/" + (texCounter++) + "_" + name);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            TEX.put(key, id);
            DIM.put(key, new int[]{img.getWidth(), img.getHeight()});
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        if (renaming != null && renameField != null) {
            int key = keyInput.key();
            if (key == 257 || key == 335) { finishRename(); return true; }
            if (key == 256) { cancelRename(); return true; }
            return renameField.keyPressed(keyInput) || super.keyPressed(keyInput);
        }
        if (preview != null && captionField != null && captionField.isFocused()) {
            int key = keyInput.key();
            if (key == 257 || key == 335) { saveCaption(); return true; }
        }
        return super.keyPressed(keyInput);
    }

    private void setStatus(String s) {
        status = s;
        statusTicks = 80;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (renaming != null) return super.mouseClicked(click, doubled);
        if (preview == null) {
            int start = page * PER_PAGE;
            for (int i = 0; i < PER_PAGE && start + i < shots.size(); i++) {
                int[] r = tileRect(i);
                if (click.x() >= r[0] && click.x() <= r[0] + r[2]
                        && click.y() >= r[1] && click.y() <= r[1] + r[3]) {
                    preview = shots.get(start + i);
                    this.clearAndInit();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private int[] tileRect(int index) {
        int gridW = COLS * TILE_W + (COLS - 1) * GAP;
        int ox = (this.width - gridW) / 2;
        int oy = 48;
        int c = index % COLS, r = index / COLS;
        return new int[]{ox + c * (TILE_W + GAP), oy + r * (TILE_H + GAP), TILE_W, TILE_H};
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (preview != null) {
            renderPreview(context);
            drawStatus(context);
            return;
        }

        Theme.header(context, textRenderer, "Screenshots", this.width / 2);

        if (shots.isEmpty() && renaming == null) {
            context.drawCenteredTextWithShadow(textRenderer,
                    "\u00a77Noch keine Screenshots in \u00a7fscreenshots/", this.width / 2, this.height / 2, Theme.TEXT_DIM);
            drawStatus(context);
            return;
        }

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < shots.size(); i++) {
            Path p = shots.get(start + i);
            int[] r = tileRect(i);
            boolean hover = mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3];

            Theme.roundedBox(context, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4,
                    p.equals(renaming) ? Theme.ACCENT : (hover ? Theme.ACCENT : Theme.BORDER), 0xFF000000);

            if (!p.equals(renaming)) {
                Identifier id = textureFor(p);
                if (id != null) {
                    int[] dim = DIM.get(p.toString());
                    float scale = Math.min((float) TILE_W / dim[0], (float) TILE_H / dim[1]);
                    int dw = Math.max(1, (int) (dim[0] * scale));
                    int dh = Math.max(1, (int) (dim[1] * scale));
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, id, r[0] + (TILE_W - dw) / 2,
                            r[1] + (TILE_H - dh) / 2, 0f, 0f, dw, dh, dw, dh);
                }
                String caption = readCaption(p);
                if (!caption.isEmpty()) {
                    context.fill(r[0], r[1] + TILE_H - 11, r[0] + TILE_W, r[1] + TILE_H, 0xB0000000);
                    context.drawTextWithShadow(textRenderer, "\u00a7f" + trim(caption, 22),
                            r[0] + 3, r[1] + TILE_H - 9, 0xFFFFFFFF);
                }
            }
        }

        context.drawCenteredTextWithShadow(textRenderer,
                "\u00a77Seite " + (page + 1) + " / " + Math.max(1, (shots.size() + PER_PAGE - 1) / PER_PAGE)
                        + "  \u00b7  " + shots.size() + " Screenshots",
                this.width / 2, this.height - 48, Theme.TEXT_DIM);
        drawStatus(context);
    }

    private void renderPreview(DrawContext context) {
        Identifier id = textureFor(preview);
        if (id == null) return;
        int[] dim = DIM.get(preview.toString());

        int maxW = this.width - 80;
        int maxH = this.height - 140;
        float scale = Math.min((float) maxW / dim[0], (float) maxH / dim[1]);
        int dw = (int) (dim[0] * scale);
        int dh = (int) (dim[1] * scale);
        int dx = (this.width - dw) / 2;
        int dy = 30 + (maxH - dh) / 2;

        Theme.roundedBox(context, dx - 3, dy - 3, dw + 6, dh + 6, Theme.ACCENT, 0xFF000000);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, id, dx, dy, 0f, 0f, dw, dh, dw, dh);

        String fname = preview.getFileName().toString();
        context.drawCenteredTextWithShadow(textRenderer, "\u00a77" + fname, this.width / 2, 16, Theme.TEXT_DIM);
    }

    private void drawStatus(DrawContext context) {
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, this.width / 2, this.height - 76, Theme.ACCENT);
        }
    }

    private static String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (renaming != null) { cancelRename(); return; }
        if (preview != null) { preview = null; this.clearAndInit(); }
        else client.setScreen(parent);
    }
}
