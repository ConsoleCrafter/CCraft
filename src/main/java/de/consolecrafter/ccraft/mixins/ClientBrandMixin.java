package de.consolecrafter.ccraft.mixins;

import net.minecraft.network.packet.BrandCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Erzwingt die Client-Brand "CCraft".
 *
 * In 1.21.10 wird die Brand als {@link BrandCustomPayload} verschickt (kein
 * {@code write}-Pfad mehr wie in älteren Versionen). Der Konstruktor ist der
 * einzige Ort, an dem der Brand-Text in das Paket gelangt — egal ob von Vanilla
 * (Login) oder von einer anderen Mod (z.B. Meteor). Indem der String-Parameter
 * hier bedingungslos ersetzt wird, trägt das Paket IMMER "CCraft", und jede
 * spätere Überschreibung durch andere Mods wird wieder überschrieben.
 */
@Mixin(BrandCustomPayload.class)
public class ClientBrandMixin {

    private static final String CCRAFT_BRAND = "CCraft";

    @ModifyVariable(method = "<init>(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private static String ccraft$forceBrand(String brand) {
        return CCRAFT_BRAND;
    }
}
