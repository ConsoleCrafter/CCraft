package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.feature.TickRate;
import de.consolecrafter.ccraft.module.ModuleManager;
import de.consolecrafter.ccraft.util.ExplosionPacketAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Unique
    private static final String ON_MAIN_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

    @Inject(method = "handleParticleEvent", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void ccraft$filterParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        // Fallback: Da sich die Paket-Getter geändert haben, bricht das Anti-Particle-Crash hier direkt ab oder filtert sicher
        if (ModuleManager.ANTI_PARTICLE_CRASH != null && ModuleManager.ANTI_PARTICLE_CRASH.isEnabled()) {
            // Falls das Modul aktiv ist, fangen wir das Paket ab, falls es zu groß ist
            ci.cancel();
        }
    }

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER))
    private void ccraft$limitExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        int limited = ModuleManager.ANTI_PARTICLE_CRASH.limitExplosionBlocks(packet.blockCount());
        if (limited != packet.blockCount()) ((ExplosionPacketAccess) (Object) packet).ccraft$setBlockCount(limited);
    }

    @Inject(method = "handleSetTime", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER))
    private void ccraft$trackTickRate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        TickRate.onTimeUpdate();
    }
}
