package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ccraft$themedBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null || (Object) this instanceof ConnectScreen) return;
        Theme.background(graphics, width, height);
        ci.cancel();
    }
}
