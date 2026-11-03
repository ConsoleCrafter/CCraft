package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Ersetzt das Vanilla-"Verbindung verloren"-Fenster durch eine eigene,
 * aufgeräumte Fehleransicht: klare Überschrift, der Grund vom Server,
 * konkrete Hinweise (Timeout/offline/kick) und der Vanilla-Zurück-Button.
 */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    @Shadow @Final private Screen parent;
    @Shadow @Final private DisconnectionInfo info;
    @Shadow @Final private Text buttonLabel;

    protected DisconnectedScreenMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ccraft$rebuild(CallbackInfo ci) {
        // Vanilla-Inhalt (großer Reason-Text + Report-Buttons) entfernen und
        // nur unseren eigenen Zurück-Button setzen.
        this.clearChildren();
        addDrawableChild(ButtonWidget.builder(buttonLabel, b -> this.client.setScreen(parent))
                .position(this.width / 2 - 100, this.height / 2 + 62).size(200, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Eigenen Hintergrund statt Vanilla-Verlauf zeichnen.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);
        super.render(context, mouseX, mouseY, delta); // nur die Widgets (Zurück-Button)

        int boxW = Math.min(400, this.width - 40);
        int x = this.width / 2 - boxW / 2;
        int top = this.height / 2 - 96;

        context.fill(x, top, x + boxW, top + 148, 0xC0100A0A);
        context.fill(x, top, x + boxW, top + 1, 0xFFCC3333);
        context.fill(x, top + 147, x + boxW, top + 148, 0xFFCC3333);
        context.fill(x, top, x + 1, top + 148, 0xFFCC3333);
        context.fill(x + boxW - 1, top, x + boxW, top + 148, 0xFFCC3333);

        context.drawCenteredTextWithShadow(this.textRenderer, "§l§cVerbindung unterbrochen",
                this.width / 2, top + 12, 0xFFFFFFFF);
        Theme.divider(context, x + 16, top + 30, boxW - 32);

        String reason = this.info != null && this.info.reason() != null
                ? this.info.reason().getString()
                : "Unbekannter Fehler";
        if (reason.isBlank()) reason = "Unbekannter Fehler";

        // Grund des Servers, auf die Breite umgebrochen.
        List<String> wrapped = wrap(reason, boxW - 32);
        int cy = top + 40;
        for (String line : wrapped) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§f" + line, this.width / 2, cy, 0xFFFFFFFF);
            cy += this.textRenderer.fontHeight + 2;
        }

        // Konkrete Hinweise je nach Fehlerart.
        String lower = reason.toLowerCase();
        List<String> hints = new ArrayList<>();
        if (lower.contains("timed out") || lower.contains("timeout")) {
            hints.add("Der Server hat nicht rechtzeitig geantwortet.");
            hints.add("Läuft der Server? Firewall/VPN prüfen?");
        } else if (lower.contains("connection refused") || lower.contains("connect")) {
            hints.add("Der Server lehnt die Verbindung ab.");
            hints.add("Stimmen IP und Port? Ist der Server online?");
        } else if (lower.contains("kick") || lower.contains("banned")) {
            hints.add("Du wurdest vom Server entfernt.");
            hints.add("Wende dich ggf. an einen Admin.");
        } else if (lower.contains("outdated") || lower.contains("version") || lower.contains("protocol")) {
            hints.add("Client- und Server-Version passen nicht zusammen.");
            hints.add("Prüfe deine Minecraft-/Mod-Version.");
        } else {
            hints.add("Bitte prüfe die Serveradresse und versuche es erneut.");
        }

        cy += 4;
        for (String h : hints) {
            context.drawTextWithShadow(this.textRenderer, "§7• §7" + h, x + 20, cy, 0xFFFFFFFF);
            cy += this.textRenderer.fontHeight + 1;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (this.textRenderer.getWidth(candidate) > maxWidth && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.size() > 4) return out.subList(0, 4);
        return out;
    }
}
