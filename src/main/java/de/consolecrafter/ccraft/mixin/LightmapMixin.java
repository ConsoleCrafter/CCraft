package de.consolecrafter.ccraft.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import de.consolecrafter.ccraft.module.ModuleManager;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {

    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ccraft$fullbright(LightmapRenderState renderState, CallbackInfo ci) {
        if (!ModuleManager.FULLBRIGHT.isEnabled()) return;
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, new Vector4f(1));
        ci.cancel();
    }
}
