package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.gui.ConnectingOverlay;
import io.netty.channel.ChannelFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ersetzt das Vanilla-"Joining Server"-Overlay durch {@link ConnectingOverlay}
 * und macht ESC zum Abbrechen nutzbar.
 *
 * <p>{@code ConnectScreen.connect} ist {@code static} (Signatur:
 * {@code Screen, MinecraftClient, ServerAddress, ServerInfo, boolean,
 * CookieStorage}). Der Callback muss daher dieselbe Parameterliste haben und
 * selbst {@code static} sein. Der Ziel-Server wird im statischen Zustand von
 * {@link ConnectingOverlay} abgelegt.
 *
 * <p>ESC: Vanilla liefert in {@code shouldCloseOnEsc()} immer {@code false},
 * man kann nur den Cancel-Button klicken. {@link ConnectingOverlay} verspricht
 * aber "Esc zum Abbrechen", also erzwingen wir hier {@code true} und lassen
 * {@code close()} dieselbe Abbruch-Logik wie der Cancel-Button ausführen.
 */
@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Shadow @Final Screen parent;
    @Shadow volatile ClientConnection connection;
    @Shadow volatile ChannelFuture future;
    @Shadow volatile boolean connectingCancelled;

    /** Entry-Point: {@code static connect(Screen, MinecraftClient, ServerAddress, ServerInfo, boolean, CookieStorage)}. */
    @Inject(method = "connect", at = @At("HEAD"))
    private static void ccraft$captureAddress(Screen parent, MinecraftClient client, ServerAddress address,
                                              ServerInfo info, boolean quickPlay, CookieStorage cookies,
                                              CallbackInfo ci) {
        if (address != null) {
            ConnectingOverlay.setTarget(address.getAddress(), address.getPort(),
                    info != null ? info.name : "");
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ccraft$replaceWithConnectingOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ConnectingOverlay.render(context);
        ci.cancel();
    }

    /** Vanilla verbietet ESC hier; wir erlauben es, damit "Esc zum Abbrechen" stimmt. */
    @Inject(method = "shouldCloseOnEsc", at = @At("HEAD"), cancellable = true)
    private void ccraft$allowEscAbort(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * Ersetzt {@code Screen.close()} für diesen Screen: bricht die laufende
     * Verbindung ab (wie der Cancel-Button) und kehrt zum vorherigen Screen
     * zurück, statt nur {@code setScreen(null)} aufzurufen.
     */
    public void close() {
        synchronized (this) {
            this.connectingCancelled = true;
            if (this.future != null) {
                this.future.cancel(true);
                this.future = null;
            }
            if (this.connection != null) {
                this.connection.disconnect(ConnectScreen.ABORTED_TEXT);
            }
        }
        MinecraftClient.getInstance().setScreen(this.parent);
    }
}
