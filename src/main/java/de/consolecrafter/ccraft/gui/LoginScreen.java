package de.consolecrafter.ccraft.gui;

import at.favre.lib.crypto.bcrypt.BCrypt;
import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.util.AccountStore;
import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoginScreen extends Screen {
    public static final LoginScreen INSTANCE = new LoginScreen();
    public static boolean loggedIn = false;

    private static final Identifier CCRAFT_LOGO = Identifier.of("ccraft", "textures/gui/ccraft_logo.png");
    private static final int LOGO_WIDTH  = 120;
    private static final int LOGO_HEIGHT = 61;

    private TextFieldWidget nameField;
    private TextFieldWidget passwordField;
    private String errorMessage = "";
    private volatile boolean busy = false;

    // Eingaben überleben Fenstergröße-/Vollbild-Wechsel (init() wird dabei neu
    // aufgerufen und würde die Felder sonst zurücksetzen).
    private String draftName = "";
    private String draftPass = "";

    // Panel-Geometrie (im init berechnet, im render wiederverwendet)
    private int panelX, panelY, panelW, panelH;
    private int fieldX, nameY, passY, btnY, errY;
    private int logoY;

    // Schwebende Partikel im Hintergrund
    private static final int PARTICLES = 40;
    private final float[] px = new float[PARTICLES];
    private final float[] py = new float[PARTICLES];
    private final float[] pvx = new float[PARTICLES];
    private final float[] pvy = new float[PARTICLES];
    private final float[] psize = new float[PARTICLES];
    private boolean particlesInit = false;

    private LoginScreen() {
        super(Text.literal("Login"));
    }

    private void initParticles() {
        for (int i = 0; i < PARTICLES; i++) {
            px[i] = (float) (Math.random() * this.width);
            py[i] = (float) (Math.random() * this.height);
            pvx[i] = (float) (Math.random() * 0.6 - 0.3);
            pvy[i] = (float) (-0.2 - Math.random() * 0.6);
            psize[i] = 1f + (float) (Math.random() * 2.5);
        }
        particlesInit = true;
    }

    @Override
    protected void init() {
        super.init();
        if (!particlesInit || this.width == 0) initParticles();

        final int cx = this.width / 2;

        // Auf kleine Fenster (z.B. 854x480) begrenzen, damit nichts abgeschnitten wird.
        panelW = Math.min(260, this.width - 40);
        panelH = 172;

        int topMargin = 6;
        int logoBlockH = LOGO_HEIGHT + 26; // Logo + Titel + Trennlinie
        int availableTop = Math.max(topMargin, (this.height - (logoBlockH + panelH + 34)) / 2);
        logoY = availableTop;
        panelY = logoY + LOGO_HEIGHT + 10;

        panelX = cx - panelW / 2;
        fieldX = panelX + 20;

        nameY = panelY + 42;
        passY = panelY + 84;
        btnY  = panelY + 122;
        errY  = btnY + 48;

        nameField = new TextFieldWidget(
                this.textRenderer, fieldX, nameY, panelW - 40, 20, Text.literal("Name"));
        nameField.setMaxLength(50);
        nameField.setPlaceholder(Text.literal("Name"));
        if (!draftName.isEmpty()) {
            nameField.setText(draftName);
        } else if (CraftConfig.saveLogin && !CraftConfig.savedUser.isEmpty()) {
            nameField.setText(CraftConfig.savedUser);
        }
        this.addDrawableChild(nameField);

        passwordField = new TextFieldWidget(
                this.textRenderer, fieldX, passY, panelW - 40, 20, Text.literal("Passwort"));
        passwordField.setMaxLength(50);
        passwordField.setPlaceholder(Text.literal("Passwort"));
        passwordField.setText(draftPass);
        // Passwort als Punkte maskieren (Vanilla-Weg).
        passwordField.addFormatter((text, firstChar) ->
                OrderedText.styledForwardsVisitedString("•".repeat(text.length()), net.minecraft.text.Style.EMPTY));
        this.addDrawableChild(passwordField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"), b -> attemptLogin())
                .position(fieldX, btnY).size((panelW - 44) / 2, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Beenden"), b ->
                MinecraftClient.getInstance().stop())
                .position(fieldX + (panelW - 44) / 2 + 4, btnY)
                .size((panelW - 44) / 2, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Merken: " + (CraftConfig.saveLogin ? "an" : "aus")), b -> {
            CraftConfig.saveLogin = !CraftConfig.saveLogin;
            if (!CraftConfig.saveLogin) CraftConfig.savedUser = "";
            CraftConfig.save();
            this.clearAndInit();
        }).position(fieldX, btnY + 24).size(panelW - 40, 16).build());
    }

    private void attemptLogin() {
        if (busy) return;

        final String username = nameField.getText().trim();
        final char[] password = passwordField.getText().toCharArray();

        if (username.isEmpty() || password.length == 0) {
            errorMessage = "Bitte fülle alle Felder aus!";
            return;
        }

        busy = true;
        errorMessage = "Prüfe...";

        CompletableFuture<Map<String, String>> request;
        try {
            request = AccountStore.fetch();
        } catch (Throwable t) {
            // fetch() kann schon beim Aufbau der Anfrage werfen (z. B. defekte
            // URL). Ohne dieses catch bliebe busy=true stehen und der
            // Login-Button waere dauerhaft gesperrt.
            CCraftMod.LOGGER.warn("Login-Anfrage konnte nicht gestartet werden.", t);
            busy = false;
            errorMessage = "Server nicht erreichbar!";
            return;
        }

        request
                .thenApplyAsync(accounts -> {
                    String hash = accounts.get(username);
                    return hash != null && BCrypt.verifyer().verify(password, hash).verified;
                })
                .whenComplete((ok, err) -> MinecraftClient.getInstance().execute(() -> {
                    busy = false;
                    if (err != null) {
                        errorMessage = "Server nicht erreichbar!";
                    } else if (ok) {
                        loggedIn = true;
                        errorMessage = "";
                        draftName = "";
                        draftPass = "";
                        CraftConfig.savedUser = CraftConfig.saveLogin ? username : "";
                        CraftConfig.save();
                        MinecraftClient.getInstance().setScreen(new CustomMainMenuScreen());
                    } else {
                        errorMessage = "Falscher Benutzername oder Passwort!";
                    }
                }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Eingaben vor dem Zeichnen sichern (überlebt Fenster-/Vollbild-Wechsel).
        if (nameField != null) draftName = nameField.getText();
        if (passwordField != null) draftPass = passwordField.getText();

        drawParticles(context);
        drawGlow(context);

        // Panel ZUERST zeichnen, damit Felder und Buttons darüber liegen.
        Theme.panel(context, panelX, panelY, panelW, panelH);
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 3, Theme.ACCENT);

        // Logo + Titel über dem Panel
        int cx = this.width / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, CCRAFT_LOGO,
                cx - LOGO_WIDTH / 2, logoY,
                0, 0, LOGO_WIDTH, LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);

        context.drawCenteredTextWithShadow(textRenderer, "§lCCraft Login", cx, logoY - 10, Theme.TEXT);

        int lineY = logoY + LOGO_HEIGHT + 2;
        context.fill(cx - 70, lineY, cx + 70, lineY + 1, Theme.BORDER);
        context.fill(cx - 24, lineY + 1, cx + 24, lineY + 2, Theme.ACCENT);

        // Labels
        context.drawText(textRenderer, "Name",     fieldX, nameY - 11, Theme.TEXT_DIM, true);
        context.drawText(textRenderer, "Passwort", fieldX, passY - 11, Theme.TEXT_DIM, true);

        // Widgets (Felder + Buttons)
        super.render(context, mouseX, mouseY, delta);

        // Fehlermeldung — Platz ist eingeplant (errY), damit sie nicht abgeschnitten wird.
        if (!errorMessage.isEmpty()) {
            int color = busy ? Theme.TEXT_DIM : Theme.DANGER;
            context.drawCenteredTextWithShadow(textRenderer, errorMessage, cx, errY, color);
        }

        context.drawText(textRenderer, "CCraft Login", 4, this.height - 12, Theme.TEXT_FAINT, true);
    }

    /** Weich leuchtender Akzent-Schein hinter dem Panel. */
    private void drawGlow(DrawContext context) {
        int glow = (CraftConfig.accent & 0xFFFFFF) | 0x33000000;
        context.fillGradient(panelX - 20, panelY - 20, panelX + panelW + 20, panelY,
                glow, 0x00000000);
        context.fillGradient(panelX - 20, panelY + panelH, panelX + panelW + 20, panelY + panelH + 20,
                0x00000000, glow);
    }

    /** Langsam aufsteigende Akzent-Partikel. */
    private void drawParticles(DrawContext context) {
        int color = (CraftConfig.accent & 0xFFFFFF) | 0x66000000;
        for (int i = 0; i < PARTICLES; i++) {
            px[i] += pvx[i];
            py[i] += pvy[i];
            if (py[i] < -5) {
                py[i] = this.height + 5;
                px[i] = (float) (Math.random() * this.width);
            }
            if (px[i] < -5) px[i] = this.width + 5;
            if (px[i] > this.width + 5) px[i] = -5;
            int s = Math.round(psize[i]);
            context.fill((int) px[i], (int) py[i], (int) px[i] + s, (int) py[i] + s, color);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
