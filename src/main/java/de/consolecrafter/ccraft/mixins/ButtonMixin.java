package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ersetzt das Vanilla-Button-Aussehen global durch abgerundete,
 * transluzente rote CCraft-Buttons — auch bei Mod-Buttons
 * (z. B. ViaFabricPlus im Multiplayer-Menü).
 *
 * Bewusst ohne @Shadow: alle benoetigten Zugriffe laufen ueber die
 * öffentliche ClickableWidget-API des Instanz-Casts, damit die Mixin
 * unabhaengig von der Vererbungshierarchie bleibt.
 */
@Mixin(net.minecraft.client.gui.widget.PressableWidget.class)
public abstract class ButtonMixin {

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void ccraft$renderRoundedButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ClickableWidget self = (ClickableWidget) (Object) this;

        // Checkboxen behalten ihr eigenes Aussehen.
        if (self.getClass().getName().contains("Checkbox")) {
            return;
        }

        int state;
        if (!self.active) state = 3;
        else if (self.isHovered()) state = 1;
        else state = 0;

        Text label = self.getMessage();
        Theme.button(context, MinecraftClient.getInstance().textRenderer,
                self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                label, state);

        ci.cancel();
    }
}
