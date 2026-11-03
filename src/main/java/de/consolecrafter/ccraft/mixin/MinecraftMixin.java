package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.feature.WindowSetup;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @ModifyArg(method = "updateTitle", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setTitle(Ljava/lang/String;)V"))
    private String ccraft$title(String original) {
        return WindowSetup.title();
    }
}
