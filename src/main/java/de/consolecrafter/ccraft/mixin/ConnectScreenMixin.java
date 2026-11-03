package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.gui.ConnectingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V", at = @At("HEAD"))
    private void ccraft$captureTarget(Minecraft minecraft, ServerAddress address, ServerData server, TransferState transferState, CallbackInfo ci) {
        ConnectingOverlay.start(address.getHost(), address.getPort(), server != null ? server.name : "");
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ccraft$extractOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int bottom = height;
        for (GuiEventListener child : children()) {
            if (child instanceof LayoutElement element) bottom = Math.min(bottom, element.getY());
        }
        ConnectingOverlay.extract(graphics, font, width, bottom);
        ci.cancel();
    }
}
