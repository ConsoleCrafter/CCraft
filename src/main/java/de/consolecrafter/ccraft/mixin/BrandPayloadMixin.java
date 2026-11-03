package de.consolecrafter.ccraft.mixin;

import net.minecraft.network.protocol.common.custom.BrandPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BrandPayload.class)
public abstract class BrandPayloadMixin {

    @ModifyVariable(method = "<init>(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private static String ccraft$brand(String brand) {
        return "CCraft";
    }
}
