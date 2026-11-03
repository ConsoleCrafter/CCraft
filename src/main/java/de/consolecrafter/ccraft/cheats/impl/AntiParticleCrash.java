package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;

/**
 * AntiParticleCrash — wehrt Partikel-Lag-Bomben ab.
 *
 * <p>Die verbreiteten Client-Crashes funktionieren alle gleich: Der Server
 * schickt ein Paket mit absurden Werten, der Client rechnet daraufhin Millionen
 * Partikel und stirbt an Speicher oder Rechenzeit. Bekannte Varianten sind der
 * Ash-Crash (Partikelzahl), Explosions-Pakete mit riesiger Blockzahl und
 * Paket-Fluten aus sehr vielen kleinen Paketen.</p>
 *
 * <p>Geprueft wird in {@code mixins.ParticlePacketMixin} direkt im
 * Netzwerk-Handler, also bevor der Client ueberhaupt Partikel erzeugt. Ein
 * Paket mit vielen Partikeln ist fuer sich normal (Feuerwerk); gefaehrlich wird
 * erst die Kombination aus hoher Anzahl und weiter Streuung.</p>
 */
public class AntiParticleCrash extends Cheat {

    private final Option maxPerPacket = number("maxPerPacket", "Max. Partikel pro Paket", 2000.0, 0.0, 100000.0, 100.0);
    private final Option maxOffset = number("maxOffset", "Max. Streuung (Bloecke)", 16.0, 0.0, 256.0, 1.0);
    private final Option maxExplosionBlocks = number("maxExplosionBlocks", "Max. Bloecke pro Explosion", 20000.0, 0.0, 1000000.0, 1000.0);
    private final Option maxPacketsPerTick = number("maxPacketsPerTick", "Max. Partikel-Pakete pro Tick", 400.0, 0.0, 10000.0, 50.0);
    private final Option dropPacket = bool("dropPacket", "Gefaehrliches Paket verwerfen", true);

    /** Zaehler der abgewehrten Pakete (nur Anzeige). */
    private int blocked;

    /** Pakete im laufenden Tick. */
    private int packetsThisTick;

    public AntiParticleCrash() {
        super("antiparticlecrash", "AntiParticleCrash",
                "Blockiert Partikel-Pakete, die den Client crashen (z. B. Ash-Crash).");
    }

    /** Standardmaessig an: der Cheat schuetzt nur, er veraendert das Spiel nicht. */
    @Override
    protected boolean defaultEnabled() {
        return true;
    }

    /**
     * Bleibt beim Weltwechsel an.
     *
     * <p>Ein Schutz, den man nach jedem Weltwechsel neu einschalten muss, waere
     * nutzlos — die Angriffe kommen vom Server, nicht aus der eigenen Welt.</p>
     */
    @Override
    protected boolean keepOnDisconnect() {
        return true;
    }

    @Override
    protected void onTick() {
        packetsThisTick = 0;
    }

    @Override
    public String statusText() {
        return blocked == 0 ? "" : "§c" + blocked + " Pakete blockiert";
    }

    /**
     * Ob ein eingehendes Partikel-Paket als Angriff gilt.
     *
     * @param count   Anzahl der Partikel im Paket
     * @param offsetX Streuung in X (Bloecke)
     * @param offsetY Streuung in Y (Bloecke)
     * @param offsetZ Streuung in Z (Bloecke)
     */
    public boolean isDangerous(int count, double offsetX, double offsetY, double offsetZ) {
        if (!isEnabled()) return false;

        // Kaputte Werte: eine NaN-Streuung pflanzt sich in jede Positionsrechnung
        // fort und kann das Rendering in einen Endloszustand schicken.
        if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY) || !Double.isFinite(offsetZ)) {
            return true;
        }

        // Absurde Anzahl: der klassische Ash-Crash schickt Millionen Partikel
        // in einem einzigen Paket.
        if (count > maxPerPacket.asNumber()) return true;

        // Viele Partikel mit weiter Streuung: hier explodiert die Rechenlast,
        // weil jedes Partikel einzeln gestreut wird.
        double spread = Math.max(Math.abs(offsetX), Math.max(Math.abs(offsetY), Math.abs(offsetZ)));
        return count > 100 && spread > maxOffset.asNumber();
    }

    /** Obergrenze fuer die Block-Partikel einer Explosion. */
    public int explosionBlockLimit() {
        return Math.max(0, (int) Math.round(maxExplosionBlocks.asNumber()));
    }

    /**
     * Zaehlt ein eingehendes Partikel-Paket fuer die Flut-Erkennung.
     *
     * <p>Bewusst eine eigene Methode statt eines Nebeneffekts in
     * {@link #isFlooding()}: eine Abfrage, die den Zaehler veraendert, zaehlt
     * bei jedem zusaetzlichen Aufruf doppelt.</p>
     *
     * @return {@code true}, wenn in diesem Tick die Obergrenze erreicht ist
     */
    public boolean notePacket() {
        packetsThisTick++;
        return isFlooding();
    }

    /**
     * Ob in diesem Tick zu viele Partikel-Pakete eintreffen.
     *
     * <p>Die Flut-Variante schickt keine einzelnen Riesenpakete, sondern sehr
     * viele kleine. Jedes Paket fuer sich ist unauffaellig, in Summe bricht die
     * Partikelverwaltung ein. Der Zaehler wird jeden Tick zurueckgesetzt.</p>
     */
    public boolean isFlooding() {
        return isEnabled() && packetsThisTick > maxPacketsPerTick.asNumber();
    }

    /** Ob das Paket ganz verworfen oder nur auf eine sichere Menge begrenzt wird. */
    public boolean dropsWholePacket() {
        return dropPacket.asBoolean();
    }

    /** Wirksame Obergrenze fuer die Partikelanzahl eines Pakets. */
    public int particleLimit() {
        return Math.max(1, (int) Math.round(maxPerPacket.asNumber()));
    }

    public int blockedCount() {
        return blocked;
    }

    public void countBlocked() {
        blocked++;
    }
}
