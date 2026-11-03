package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.gui.CustomMainMenuScreen;
import de.consolecrafter.ccraft.gui.LoginScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void openLoginOrMainMenu(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(LoginScreen.loggedIn ? new CustomMainMenuScreen() : LoginScreen.INSTANCE);
        ci.cancel();
    }
}
