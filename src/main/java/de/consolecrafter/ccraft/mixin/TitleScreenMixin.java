package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.gui.LoginScreen;
import de.consolecrafter.ccraft.gui.MainMenuScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void ccraft$replaceTitleScreen(CallbackInfo ci) {
        minecraft.gui.setScreen(LoginScreen.isLoggedIn() ? new MainMenuScreen() : new LoginScreen());
        ci.cancel();
    }
}
