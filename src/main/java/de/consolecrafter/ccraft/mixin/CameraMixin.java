package de.consolecrafter.ccraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.consolecrafter.ccraft.input.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float ccraft$zoom(float fov) {
        return Zoom.apply(fov);
    }
}
