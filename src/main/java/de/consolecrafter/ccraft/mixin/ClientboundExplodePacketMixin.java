package de.consolecrafter.ccraft.mixin;

import de.consolecrafter.ccraft.util.ExplosionPacketAccess;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundExplodePacket.class)
public abstract class ClientboundExplodePacketMixin implements ExplosionPacketAccess {

    @Shadow
    @Final
    @Mutable
    private int blockCount;

    @Override
    public void ccraft$setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }
}
