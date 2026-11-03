package de.consolecrafter.ccraft.cheats;

import de.consolecrafter.ccraft.cheats.impl.AimEr;
import de.consolecrafter.ccraft.cheats.impl.AntiParticleCrash;
import de.consolecrafter.ccraft.cheats.impl.AutoTotem;
import de.consolecrafter.ccraft.cheats.impl.ChestEsp;
import de.consolecrafter.ccraft.cheats.impl.FastPlace;
import de.consolecrafter.ccraft.cheats.impl.Flight;
import de.consolecrafter.ccraft.cheats.impl.Fullbright;
import de.consolecrafter.ccraft.cheats.impl.NameProtect;
import de.consolecrafter.ccraft.cheats.impl.NoFall;
import de.consolecrafter.ccraft.cheats.impl.PlayerEsp;
import de.consolecrafter.ccraft.cheats.impl.Scaffold;
import de.consolecrafter.ccraft.cheats.impl.Speed;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zentrale Verwaltung aller Cheats: Registrierung, Keybinds und Tick.
 *
 * <p>Der Manager ist die einzige Stelle, die weiss, welche Cheats existieren.
 * Menue und Einstellungsseite arbeiten ausschliesslich ueber {@link #all()}.</p>
 */
public final class CheatManager {

    /** Eigene Kategorie in den Steuerungseinstellungen. */
    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("ccraft", "cheats"));

    private static final List<Cheat> CHEATS = new ArrayList<>();

    private CheatManager() {}

    /** Legt alle Cheats an, laedt die Config und registriert die Keybinds. */
    public static void register() {
        if (!CHEATS.isEmpty()) return;
        CheatConfig.load();

        add(new AimEr());
        add(new Flight());
        add(new Speed());
        add(new NoFall());
        add(new Scaffold());
        add(new FastPlace());
        add(new Fullbright());
        add(new PlayerEsp());
        add(new ChestEsp());
        add(new AntiParticleCrash());
        add(new AutoTotem());
        add(new NameProtect());

        for (Cheat cheat : CHEATS) {
            String stored = CheatConfig.getRaw(cheat.id, "enabled");
            cheat.restore(stored != null ? Boolean.parseBoolean(stored) : cheat.defaultEnabled());
            registerKeybind(cheat);
        }
    }

    private static void add(Cheat cheat) {
        CHEATS.add(cheat);
    }

    /** Alle registrierten Cheats in Anzeigereihenfolge. */
    public static List<Cheat> all() {
        return Collections.unmodifiableList(CHEATS);
    }

    public static Cheat byId(String id) {
        for (Cheat cheat : CHEATS) {
            if (cheat.id.equals(id)) return cheat;
        }
        return null;
    }

    /**
     * Der Partikel-Filter, oder {@code null} solange nicht registriert.
     *
     * <p>Der Mixin greift auf den Netzwerk-Handler zu, der theoretisch vor
     * {@link #register()} laufen kann — deshalb hier kein direkter Cast.</p>
     */
    public static AntiParticleCrash antiParticleCrash() {
        Cheat cheat = byId("antiparticlecrash");
        return cheat instanceof AntiParticleCrash apc ? apc : null;
    }

    /**
     * Alle registrierten Cheats, gefiltert nach einem Suchbegriff.
     *
     * @param query Suchtext; leer bedeutet "kein Filter"
     */
    public static List<Cheat> search(String query) {
        if (query == null || query.isBlank()) return all();
        String needle = query.toLowerCase(java.util.Locale.ROOT).trim();
        List<Cheat> hits = new ArrayList<>();
        for (Cheat cheat : CHEATS) {
            if (cheat.name.toLowerCase(java.util.Locale.ROOT).contains(needle)
                    || cheat.id.contains(needle)
                    || cheat.description.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                hits.add(cheat);
            }
        }
        return Collections.unmodifiableList(hits);
    }

    // ── Keybinds ─────────────────────────────────────────────────────────────
    private static void registerKeybind(Cheat cheat) {
        // Gespeichert wird der Translation-Key der Taste (z. B.
        // "key.keyboard.g"), damit die Belegung auch nach einem MC-Update gilt.
        InputUtil.Key key = resolveKey(CheatConfig.getRaw(cheat.id, "key"));
        KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ccraft.cheat." + cheat.id,
                InputUtil.Type.KEYSYM,
                key.getCode(),
                CATEGORY));
        cheat.setKeybind(binding);
    }

    /** Loest einen gespeicherten Translation-Key in eine Eingabetaste auf. */
    private static InputUtil.Key resolveKey(String stored) {
        if (stored == null || stored.isBlank()) return InputUtil.UNKNOWN_KEY;
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(stored);
            return key == null ? InputUtil.UNKNOWN_KEY : key;
        } catch (IllegalArgumentException e) {
            // Unbekannte Taste (z. B. nach einem MC-Update) -> unbelegt.
            return InputUtil.UNKNOWN_KEY;
        }
    }

    /**
     * Speichert einen neuen Keybind fuer einen Cheat und wendet ihn sofort an.
     * Gespeichert wird der Translation-Key, damit die Belegung stabil bleibt.
     */
    public static void setKeybind(Cheat cheat, InputUtil.Key key) {
        InputUtil.Key effective = key == null ? InputUtil.UNKNOWN_KEY : key;
        CheatConfig.set(cheat.id, "key", effective.getTranslationKey());
        if (cheat.keybind() != null) {
            cheat.keybind().setBoundKey(effective);
            // setBoundKey() aendert nur das Feld. Die Zuordnung Taste->Keybind
            // wird in KeyBinding.KEY_TO_BINDINGS gehalten und nur beim Laden der
            // Optionen bzw. im Vanilla-Steuerungsmenue neu aufgebaut. Ohne diesen
            // Aufruf bleibt die neue Taste wirkungslos, waehrend die alte weiter
            // ausloest.
            KeyBinding.updateKeysByCode();
        }
        writeOptions();
    }

    /**
     * Schreibt die Belegung zusaetzlich in {@code options.txt}.
     *
     * <p>Ohne diesen Schritt war jede im Menue gesetzte Taste nach einem
     * Neustart wieder weg: Der Fabric-Entrypoint (und damit
     * {@link #register()}) laeuft im {@code MinecraftClient}-Konstruktor
     * <em>vor</em> {@code new GameOptions(...)}. Vanilla laedt {@code options.txt}
     * erst dort und ueberschreibt dabei jede Taste, die nicht auch in der Datei
     * steht, wieder mit dem alten Wert. {@code CheatConfig} allein reicht also
     * nicht — die Datei ist die Persistenz, die Vanilla tatsaechlich liest.</p>
     */
    private static void writeOptions() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.write();
        }
    }

    /**
     * Uebertraegt die gespeicherten Belegungen auf die Keybinds.
     *
     * <p>Wird einmal nach dem Laden von {@code options.txt} gerufen. Vanilla
     * ueberschreibt beim Laden jede Taste mit dem Wert aus der Datei; steht dort
     * noch ein veralteter Eintrag (z. B. {@code key.keyboard.unknown}), waere die
     * CCraft-Belegung sonst nach jedem Neustart wieder weg. Angewendet wird nur,
     * wenn die Taste aktuell unbelegt ist — eine bewusst im Vanilla-Menue
     * gesetzte Taste bleibt dadurch unangetastet.</p>
     */
    public static void restoreKeybinds() {
        boolean changed = false;
        for (Cheat cheat : CHEATS) {
            KeyBinding binding = cheat.keybind();
            if (binding == null || !binding.isUnbound()) continue;

            String stored = CheatConfig.getRaw(cheat.id, "key");
            if (stored == null || stored.isBlank()) continue;
            InputUtil.Key key = resolveKey(stored);
            if (key == InputUtil.UNKNOWN_KEY) continue;

            binding.setBoundKey(key);
            changed = true;
        }
        if (changed) {
            KeyBinding.updateKeysByCode();
            writeOptions();
        }
    }

    /** Keybind eines Cheats auf die Werkseinstellung (unbelegt) setzen. */
    public static void resetKeybind(Cheat cheat) {
        setKeybind(cheat, InputUtil.UNKNOWN_KEY);
    }

    /**
     * Ob die Taste bereits anderweitig belegt ist.
     *
     * <p>Geprueft werden die uebrigen Cheats, das CCraft-Menue und die
     * Vanilla-Keybinds. Unbelegte Tasten gelten nie als Konflikt.</p>
     */
    public static boolean hasKeybindConflict(Cheat cheat) {
        KeyBinding self = cheat.keybind();
        if (self == null || self.isUnbound()) return false;

        for (Cheat other : CHEATS) {
            if (other == cheat) continue;
            KeyBinding binding = other.keybind();
            if (binding != null && !binding.isUnbound() && binding.equals(self)) return true;
        }

        KeyBinding menu = de.consolecrafter.ccraft.keybinds.Keybinds.toggleCommandMenu;
        if (menu != null && !menu.isUnbound() && menu.equals(self)) return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            for (KeyBinding vanilla : client.options.allKeys) {
                // Fabric haengt die eigenen Keybinds ebenfalls in allKeys; ohne
                // diesen Filter wuerde sich jeder Cheat mit sich selbst melden.
                if (isOwnBinding(vanilla)) continue;
                if (vanilla != null && !vanilla.isUnbound() && vanilla.equals(self)) return true;
            }
        }
        return false;
    }

    /** Ob der Keybind von CCraft selbst stammt (Cheats oder Menue). */
    private static boolean isOwnBinding(KeyBinding binding) {
        return binding != null && binding.getId().startsWith("key.ccraft.");
    }

    // ── Tick ─────────────────────────────────────────────────────────────────
    /** Jeden Client-Tick: Keybinds pruefen und aktive Cheats ticken. */
    public static void tick(MinecraftClient client) {
        for (Cheat cheat : CHEATS) {
            cheat.pollKeybind();
            if (!cheat.isEnabled()) continue;
            if (client.player == null || client.world == null) continue;
            try {
                cheat.onTick();
            } catch (Throwable t) {
                cheat.setEnabled(false);
                de.consolecrafter.ccraft.CCraftMod.LOGGER.warn(
                        "Cheat {} wurde wegen eines Fehlers deaktiviert.", cheat.id, t);
            }
        }
    }

    /**
     * Setzt alle Cheats auf AUS, ohne den gespeicherten Zustand zu aendern.
     *
     * <p>Wird beim Verlassen der Welt gerufen. Der Zustand in der Config bleibt
     * erhalten, damit {@link #restoreSaved()} beim naechsten Beitritt den Cheat
     * wieder einschalten kann. Vorher rief diese Methode {@code setEnabled(false)},
     * was {@code enabled=false} schrieb — dadurch konnte ein Cheat faktisch nie
     * beim Start aktiv sein.</p>
     */
    public static void disableAll() {
        for (Cheat cheat : CHEATS) {
            if (!cheat.keepOnDisconnect()) cheat.disableInMemory();
        }
    }

    /**
     * Stellt den in der Config gespeicherten An/Aus-Zustand her.
     *
     * <p>Wird beim Beitritt zu einer Welt gerufen, damit ein im Menue
     * eingeschalteter Cheat nach einem Weltwechsel weiterlaeuft.</p>
     */
    public static void restoreSaved() {
        for (Cheat cheat : CHEATS) {
            String stored = CheatConfig.getRaw(cheat.id, "enabled");
            boolean wanted = stored != null ? Boolean.parseBoolean(stored) : cheat.defaultEnabled();
            if (wanted && !cheat.isEnabled()) {
                cheat.setEnabled(true);
            }
        }
    }

    // ── HUD ──────────────────────────────────────────────────────────────────
    /** Jeden Frame: aktive Cheats koennen 2D-Overlays (ESP) zeichnen. */
    public static void renderHud(net.minecraft.client.gui.DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        for (Cheat cheat : CHEATS) {
            if (!cheat.isEnabled()) continue;
            try {
                cheat.onHudRender(ctx);
            } catch (Throwable t) {
                cheat.setEnabled(false);
                de.consolecrafter.ccraft.CCraftMod.LOGGER.warn(
                        "Cheat {} wurde beim HUD-Rendering deaktiviert.", cheat.id, t);
            }
        }
    }
}
