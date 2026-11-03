package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    @Unique
    private static final int BOX_H = 148;

    @Shadow
    @Final
    private Screen parent;

    @Shadow
    @Final
    private DisconnectionDetails details;

    @Shadow
    @Final
    private Component buttonText;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ccraft$rebuild(CallbackInfo ci) {
        clearWidgets();
        addRenderableWidget(Button.builder(buttonText, b -> minecraft.gui.setScreen(parent))
                .bounds(width / 2 - 100, height / 2 + 62, 200, 20)
                .build());
    }

    @Inject(method = "repositionElements", at = @At("HEAD"), cancellable = true, require = 0)
    private void ccraft$reposition(CallbackInfo ci) {
        rebuildWidgets();
        ci.cancel();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xA0000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int boxW = Math.min(400, width - 40);
        int x = width / 2 - boxW / 2;
        int top = height / 2 - 96;
        graphics.fill(x, top, x + boxW, top + BOX_H, 0xC0100A0A);
        Theme.outline(graphics, x, top, boxW, BOX_H, Theme.ACCENT);
        graphics.centeredText(font, "§l§cVerbindung unterbrochen", width / 2, top + 12, 0xFFFFFFFF);
        Theme.divider(graphics, x + 16, top + 30, boxW - 32);

        String reason = details != null && details.reason() != null ? details.reason().getString() : "";
        if (reason.isBlank()) reason = "Unbekannter Fehler";

        int y = top + 40;
        for (String line : ccraft$wrap(reason, boxW - 32)) {
            graphics.centeredText(font, "§f" + line, width / 2, y, 0xFFFFFFFF);
            y += font.lineHeight + 2;
        }
        y += 4;
        for (String hint : ccraft$hints(reason.toLowerCase(Locale.ROOT))) {
            graphics.text(font, "§7• " + hint, x + 20, y, 0xFFFFFFFF, true);
            y += font.lineHeight + 1;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Unique
    private static List<String> ccraft$hints(String reason) {
        if (reason.contains("timed out") || reason.contains("timeout")) {
            return List.of("Der Server hat nicht rechtzeitig geantwortet.", "Läuft der Server? Firewall/VPN prüfen?");
        }
        if (reason.contains("connection refused") || reason.contains("connect")) {
            return List.of("Der Server lehnt die Verbindung ab.", "Stimmen IP und Port? Ist der Server online?");
        }
        if (reason.contains("kick") || reason.contains("banned")) {
            return List.of("Du wurdest vom Server entfernt.", "Wende dich ggf. an einen Admin.");
        }
        if (reason.contains("outdated") || reason.contains("version") || reason.contains("protocol")) {
            return List.of("Client- und Server-Version passen nicht zusammen.", "Prüfe deine Minecraft-/Mod-Version.");
        }
        return List.of("Bitte prüfe die Serveradresse und versuche es erneut.");
    }

    @Unique
    private List<String> ccraft$wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out.size() > 4 ? out.subList(0, 4) : out;
    }
}
