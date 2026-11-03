package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.feature.AccountStore;
import de.consolecrafter.ccraft.hud.Logos;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.Random;

public final class LoginScreen extends Screen {

    private static boolean loggedIn;

    private static final int LOGO_WIDTH = 120;
    private static final int PARTICLES = 40;

    private final Random random = new Random();
    private final float[] px = new float[PARTICLES];
    private final float[] py = new float[PARTICLES];
    private final float[] pvx = new float[PARTICLES];
    private final float[] pvy = new float[PARTICLES];
    private final int[] psize = new int[PARTICLES];
    private boolean particlesReady;

    private EditBox nameField;
    private EditBox passwordField;
    private String draftName = "";
    private String draftPassword = "";
    private String message = "";
    private volatile boolean busy;

    private int panelX;
    private int panelY;
    private int panelW;
    private int fieldX;
    private int nameY;
    private int passwordY;
    private int buttonY;
    private int logoY;

    public LoginScreen() {
        super(Component.literal("Login"));
    }

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    @Override
    protected void init() {
        if (!particlesReady) resetParticles();

        int cx = width / 2;
        int logoH = Logos.heightFor(LOGO_WIDTH);
        int panelH = 172;
        panelW = Math.min(260, width - 40);
        logoY = Math.max(6, (height - (logoH + 26 + panelH + 34)) / 2);
        panelY = logoY + logoH + 10;
        panelX = cx - panelW / 2;
        fieldX = panelX + 20;
        nameY = panelY + 42;
        passwordY = panelY + 84;
        buttonY = panelY + 122;

        ClientConfig config = ClientConfig.get();
        nameField = new EditBox(font, fieldX, nameY, panelW - 40, 20, Component.literal("Name"));
        nameField.setMaxLength(50);
        nameField.setHint(Component.literal("Name"));
        nameField.setValue(!draftName.isEmpty() ? draftName : config.saveLogin ? config.savedUser : "");
        nameField.setResponder(value -> draftName = value);
        addRenderableWidget(nameField);

        passwordField = new EditBox(font, fieldX, passwordY, panelW - 40, 20, Component.literal("Passwort"));
        passwordField.setMaxLength(50);
        passwordField.setHint(Component.literal("Passwort"));
        passwordField.setValue(draftPassword);
        passwordField.setResponder(value -> draftPassword = value);
        passwordField.addFormatter((text, start) -> FormattedCharSequence.forward("•".repeat(text.length()), Style.EMPTY));
        addRenderableWidget(passwordField);

        int half = (panelW - 44) / 2;
        addRenderableWidget(Button.builder(Component.literal("Login"), b -> attemptLogin())
                .bounds(fieldX, buttonY, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Beenden"), b -> minecraft.stop())
                .bounds(fieldX + half + 4, buttonY, half, 20).build());
        addRenderableWidget(Button.builder(
                        Component.literal("Merken: " + (config.saveLogin ? "an" : "aus")), b -> {
                            config.saveLogin = !config.saveLogin;
                            if (!config.saveLogin) config.savedUser = "";
                            ClientConfig.save();
                            rebuildWidgets();
                        })
                .bounds(fieldX, buttonY + 24, panelW - 40, 16).build());

        setInitialFocus(nameField.getValue().isEmpty() ? nameField : passwordField);
    }

    private void attemptLogin() {
        if (busy) return;
        String username = nameField.getValue().trim();
        char[] password = passwordField.getValue().toCharArray();
        if (username.isEmpty() || password.length == 0) {
            message = "Bitte fülle alle Felder aus!";
            return;
        }
        busy = true;
        message = "Prüfe...";
        AccountStore.verify(username, password).whenComplete((ok, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            if (error != null) {
                CCraftMod.LOGGER.warn("Login request failed", error);
                message = "Server nicht erreichbar!";
                return;
            }
            if (!ok) {
                message = "Falscher Benutzername oder Passwort!";
                return;
            }
            loggedIn = true;
            ClientConfig config = ClientConfig.get();
            config.savedUser = config.saveLogin ? username : "";
            ClientConfig.save();
            minecraft.gui.setScreen(new MainMenuScreen());
        }));
    }

    private void resetParticles() {
        for (int i = 0; i < PARTICLES; i++) {
            px[i] = random.nextFloat() * Math.max(1, width);
            py[i] = random.nextFloat() * Math.max(1, height);
            pvx[i] = random.nextFloat() * 0.6f - 0.3f;
            pvy[i] = -0.2f - random.nextFloat() * 0.6f;
            psize[i] = 1 + random.nextInt(3);
        }
        particlesReady = true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Theme.background(g, width, height);
        int particleColor = Theme.alpha(0x66, Theme.ACCENT);
        for (int i = 0; i < PARTICLES; i++) {
            px[i] += pvx[i];
            py[i] += pvy[i];
            if (py[i] < -5) {
                py[i] = height + 5;
                px[i] = random.nextFloat() * width;
            }
            if (px[i] < -5) px[i] = width + 5;
            if (px[i] > width + 5) px[i] = -5;
            g.fill((int) px[i], (int) py[i], (int) px[i] + psize[i], (int) py[i] + psize[i], particleColor);
        }
        int glow = Theme.alpha(0x33, Theme.ACCENT);
        g.fillGradient(panelX - 20, panelY - 20, panelX + panelW + 20, panelY, 0x00000000, glow);
        g.fillGradient(panelX - 20, panelY + 172, panelX + panelW + 20, panelY + 192, glow, 0x00000000);
        Theme.panel(g, panelX, panelY, panelW, 172);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 3, Theme.ACCENT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int logoH = Logos.draw(g, cx - LOGO_WIDTH / 2, logoY, LOGO_WIDTH);
        int lineY = logoY + logoH + 2;
        g.fill(cx - 70, lineY, cx + 70, lineY + 1, Theme.BORDER);
        g.fill(cx - 24, lineY + 1, cx + 24, lineY + 2, Theme.ACCENT);
        g.text(font, "Name", fieldX, nameY - 11, Theme.TEXT_DIM, true);
        g.text(font, "Passwort", fieldX, passwordY - 11, Theme.TEXT_DIM, true);
        if (!message.isEmpty()) g.centeredText(font, message, cx, buttonY + 48, busy ? Theme.TEXT_DIM : Theme.DANGER);
        g.text(font, "CCraft Login", 4, height - 12, Theme.TEXT_FAINT, true);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
