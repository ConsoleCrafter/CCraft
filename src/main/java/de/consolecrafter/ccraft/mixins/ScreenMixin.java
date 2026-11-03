package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void ccraft$renderCustomBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var mc = MinecraftClient.getInstance();

        // In der Welt (z. B. Inventar) und beim Verbinden: normales Verhalten,
        // damit das Spiel im Hintergrund sichtbar bleibt.
        if (mc.world != null || (Object) this instanceof ConnectScreen) {
            return;
        }

        int width  = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        RenderUtil.drawBackground(context, width, height);
        ci.cancel();
    }
}
