package de.consolecrafter.ccraft.module.impl;

import de.consolecrafter.ccraft.module.Module;
import de.consolecrafter.ccraft.module.Option;

public final class AntiParticleCrash extends Module {

    private final Option maxPerPacket = number("maxPerPacket", "Max. Partikel pro Paket", 2000, 0, 100000, 100);
    private final Option maxOffset = number("maxOffset", "Max. Streuung (Blöcke)", 16, 0, 256, 1);
    private final Option maxPacketsPerTick = number("maxPacketsPerTick", "Max. Partikel-Pakete pro Tick", 400, 0, 10000, 50);
    private final Option maxExplosionBlocks = number("maxExplosionBlocks", "Max. Blöcke pro Explosion", 20000, 0, 1000000, 1000);

    private int blocked;
    private int packetsThisTick;

    public AntiParticleCrash() {
        super("antiparticlecrash", "AntiParticleCrash", "Blockiert Partikel-Pakete, die den Client crashen (z. B. Ash-Crash).");
    }

    @Override
    protected boolean defaultEnabled() {
        return true;
    }

    @Override
    public String statusText() {
        return blocked == 0 ? "" : "§c" + blocked + " blockiert";
    }

    public void resetTick() {
        packetsThisTick = 0;
    }

    public boolean shouldDropParticles(int count, double offsetX, double offsetY, double offsetZ) {
        if (!isEnabled()) return false;
        packetsThisTick++;
        boolean drop = packetsThisTick > maxPacketsPerTick.asNumber()
                || !Double.isFinite(offsetX) || !Double.isFinite(offsetY) || !Double.isFinite(offsetZ)
                || count > maxPerPacket.asNumber()
                || count > 100 && Math.max(Math.abs(offsetX), Math.max(Math.abs(offsetY), Math.abs(offsetZ))) > maxOffset.asNumber();
        if (drop) blocked++;
        return drop;
    }

    public int limitExplosionBlocks(int blockCount) {
        if (!isEnabled()) return blockCount;
        int limit = maxExplosionBlocks.asInt();
        if (blockCount <= limit) return blockCount;
        blocked++;
        return limit;
    }
}
