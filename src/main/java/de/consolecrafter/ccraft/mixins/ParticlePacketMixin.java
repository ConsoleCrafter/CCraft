package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.cheats.impl.AntiParticleCrash;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filtert Partikel-Pakete, bevor der Client Partikel erzeugt.
 *
 * <p>Der {@code @Inject} sitzt auf {@code HEAD} des Netzwerk-Handlers und
 * beendet die Methode per {@code ci.cancel()}, sobald ein Paket als Angriff
 * eingestuft wird. Dadurch entstehen gar keine Partikel — das ist der
 * entscheidende Unterschied zu einem nachtraeglichen Aufraeumen, bei dem der
 * Speicher bereits belegt ist.</p>
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ParticlePacketMixin {

    @Inject(method = "onParticle(Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;)V",
            at = @At("HEAD"), cancellable = true)
    private void ccraft$filterParticles(ParticleS2CPacket packet, CallbackInfo ci) {
        AntiParticleCrash cheat = CheatManager.antiParticleCrash();
        if (cheat == null || !cheat.isEnabled()) return;

        int count = packet.getCount();
        float offsetX = packet.getOffsetX();
        float offsetY = packet.getOffsetY();
        float offsetZ = packet.getOffsetZ();

        // Paket-Flut zuerst: greift auch, wenn jedes Einzelpaket harmlos aussieht.
        boolean flooding = cheat.notePacket();
        if (flooding || cheat.isDangerous(count, offsetX, offsetY, offsetZ)) {
            cheat.countBlocked();
            if (cheat.dropsWholePacket()) ci.cancel();
        }
    }

    /**
     * Begrenzt die Partikelzahl, wenn das Paket NICHT komplett verworfen wird.
     *
     * <p>Ohne diese Begrenzung war die Option "Gefaehrliches Paket verwerfen"
     * ein Alles-oder-Nichts-Schalter: auf {@code false} gestellt, lief das
     * gefaehrliche Paket unveraendert durch und der Schutz war komplett aus.
     * Jetzt wird stattdessen die Anzahl auf {@code maxPerPacket} gedeckelt.</p>
     *
     * <p>Angefasst wird nur die Schleifengrenze ({@code ordinal = 1}); die
     * vorgelagerte Abfrage {@code count == 0} bleibt unveraendert, damit
     * Einzelpartikel-Pakete weiterhin ihren eigenen Pfad nehmen.</p>
     */
    @Redirect(
            method = "onParticle(Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getCount()I",
                    ordinal = 1))
    private int ccraft$capParticleCount(ParticleS2CPacket packet) {
        int count = packet.getCount();
        AntiParticleCrash cheat = CheatManager.antiParticleCrash();
        if (cheat == null || !cheat.isEnabled()) return count;
        if (cheat.isDangerous(count, packet.getOffsetX(), packet.getOffsetY(), packet.getOffsetZ())) {
            return Math.min(count, cheat.particleLimit());
        }
        return count;
    }

    /**
     * Deckelt den Umfang von Explosions-Paketen.
     *
     * <p>Explosionen sind der zweite Weg, ueber den ein Server beliebig viele
     * Partikel erzeugen kann. Angefasst wird nur das Argument {@code blockCount}
     * des Block-Partikel-Aufrufs; Knall, Druckwelle und Knockback bleiben
     * erhalten. Das Paket selbst ist ein Record mit finalen Feldern und laesst
     * sich nicht nachtraeglich aendern — deshalb der Hebel am Aufruf.</p>
     */
    @ModifyArg(
            method = "onExplosion(Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;addBlockParticleEffects(Lnet/minecraft/util/math/Vec3d;FILnet/minecraft/util/collection/Pool;)V"),
            index = 2)
    private int ccraft$limitExplosionBlockCount(int blockCount) {
        AntiParticleCrash cheat = CheatManager.antiParticleCrash();
        if (cheat == null || !cheat.isEnabled()) return blockCount;
        if (blockCount <= cheat.explosionBlockLimit()) return blockCount;

        cheat.countBlocked();
        return cheat.explosionBlockLimit();
    }
}
