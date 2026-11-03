package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.cheats.impl.FastPlace;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class FastPlaceMixin {

    @Shadow
    private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void ccraft$fastPlace(CallbackInfo ci) {
        this.itemUseCooldown = FastPlace.cooldown(this.itemUseCooldown);
    }
}