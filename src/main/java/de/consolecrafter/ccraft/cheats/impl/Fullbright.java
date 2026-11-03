package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.util.CraftConfig;

/**
 * Fullbright — maximale Helligkeit, ohne dass der Spieler eine Nachtsicht
 * besitzen muss.
 *
 * <p>Der Cheat setzt lediglich {@link CraftConfig#fullbright}; die eigentliche
 * Wirkung erzeugen {@code mixins.FullbrightMixin} (Helligkeit) und der
 * Nachtsicht-Effekt in {@code CCraftMod}. Dadurch gibt es nur eine einzige
 * Quelle der Wahrheit fuer den Zustand.</p>
 */
public class Fullbright extends Cheat {

    public Fullbright() {
        super("fullbright", "Fullbright", "Maximale Helligkeit ohne Nachtsicht-Trank.");
    }

    @Override
    protected boolean defaultEnabled() {
        // Vor 0.4 lag Fullbright in CraftConfig; diesen Zustand einmalig uebernehmen.
        return CraftConfig.fullbright;
    }

    @Override
    protected void onEnable() {
        apply(true);
    }

    @Override
    protected void onDisable() {
        apply(false);
    }

    @Override
    protected void onRestore() {
        apply(isEnabled());
    }

    /** Beim Verlassen der Welt aktiv lassen — Fullbright ist kein Kampf-Cheat. */
    @Override
    protected boolean keepOnDisconnect() {
        return true;
    }

    private void apply(boolean on) {
        if (CraftConfig.fullbright == on) return;
        CraftConfig.fullbright = on;
        CraftConfig.save();
    }
}
