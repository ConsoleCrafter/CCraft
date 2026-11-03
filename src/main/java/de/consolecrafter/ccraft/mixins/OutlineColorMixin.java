package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.cheats.impl.PlayerEsp;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Faerbt den Glow-Umriss (Outline) von Spielern fuer PlayerESP ein.
 *
 * <p>Vanilla setzt die Umrissfarbe am Ende von
 * {@code EntityRenderer.updateRenderState} aus der Teamfarbe. Dieser Mixin
 * ueberschreibt sie, wenn der Spieler hervorgehoben werden soll. Der Umriss
 * wird auch durch Waende gezeichnet, ist also als Wallhack nutzbar.</p>
 */
@Mixin(EntityRenderer.class)
public abstract class OutlineColorMixin {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void ccraft$colorOutline(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        Integer color = PlayerEsp.glowColor(entity);
        if (color != null) {
            state.outlineColor = color;
        }
    }
}
