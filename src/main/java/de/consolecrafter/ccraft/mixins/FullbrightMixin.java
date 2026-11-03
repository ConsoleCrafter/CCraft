package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.util.CraftConfig;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FullbrightMixin {

    @Inject(method = "getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F", at = @At("HEAD"), cancellable = true)
    private static void ccraft$virtualNightVision(LivingEntity entity, float tickProgress,
                                                   CallbackInfoReturnable<Float> cir) {
        if (CraftConfig.fullbright) cir.setReturnValue(1.0F);
    }
}