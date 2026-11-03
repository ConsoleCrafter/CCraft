package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.cheats.impl.NameProtect;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ersetzt den eigenen Namen in der Tabliste.
 *
 * <p>Angefasst wird {@code getPlayerName}, weil die Methode den Namen fuer
 * Anzeige und Sortierung liefert. Der Eintrag wird nur veraendert, wenn er zum
 * lokalen Spieler gehoert; fremde Namen bleiben unberuehrt.</p>
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void ccraft$protectTabListName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text original = cir.getReturnValue();
        Text replaced = NameProtect.protectTabList(entry, original);
        if (replaced != original) {
            cir.setReturnValue(replaced);
        }
    }
}
