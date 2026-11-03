package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void ccraft$themedButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof Button button)) return;
        int state = !button.active ? Theme.BUTTON_DISABLED : button.isHoveredOrFocused() ? Theme.BUTTON_HOVER : Theme.BUTTON_NORMAL;
        Theme.button(graphics, Minecraft.getInstance().font, button.getX(), button.getY(), button.getWidth(), button.getHeight(), button.getMessage(), state);
        ci.cancel();
    }
}
