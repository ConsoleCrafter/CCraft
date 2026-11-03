package de.consolecrafter.ccraft.mixins;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verhindert, dass ein Doppelklick auf einen Server-Eintrag sofort verbindet.
 * Vanilla verbindet nur bei Doppelklick; ein einzelner Klick markiert den
 * Eintrag. Da der erste Klick den Eintrag bereits ausgewählt hat, wird der
 * zweite Klick hier verworfen (Rückgabe {@code true} = behandelt). Verbinden
 * läuft damit bewusst über den "Beitreten"-Button.
 */
@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public class MultiplayerServerEntryMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ccraft$noDoubleClickConnect(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (doubled) {
            cir.setReturnValue(true);
        }
    }
}
