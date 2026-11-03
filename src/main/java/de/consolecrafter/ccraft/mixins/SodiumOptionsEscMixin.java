package de.consolecrafter.ccraft.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodiums Optionen-Screen blockiert ESC bewusst, solange ungespeicherte
 * Änderungen ausstehen ({@code shouldCloseOnEsc() == !hasPendingChanges}).
 * Dann kommt man ohne Maus nicht mehr heraus.
 *
 * <p>Dieser Mixin überschreibt {@code shouldCloseOnEsc} auf {@code true}, damit
 * ESC den Screen immer schließt. Der normale ESC-Pfad (inkl. Sodiums eigener
 * Prompt-Behandlung) bleibt erhalten, weil hier nur der Rückgabewert gesetzt
 * wird.
 *
 * <p>Der Mixin ist auf ein optional gebündeltes Mod (Sodium) gerichtet und
 * nutzt deshalb {@code targets}. Ist Sodium nicht vorhanden, wird die Klasse
 * nie geladen und der Mixin läuft nicht.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI")
public abstract class SodiumOptionsEscMixin extends Screen {

    protected SodiumOptionsEscMixin() {
        super(Text.empty());
    }

    // Produktions-Laufzeit nutzt Intermediary-Namen (method_25422).
    @Inject(method = "method_25422", at = @At("RETURN"), cancellable = true, require = 0)
    private void ccraft$forceCloseOnEscIntermediary(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    // Dev-Laufzeit nutzt benannte Namen (shouldCloseOnEsc).
    @Inject(method = "shouldCloseOnEsc", at = @At("RETURN"), cancellable = true, require = 0)
    private void ccraft$forceCloseOnEscNamed(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
