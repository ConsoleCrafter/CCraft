package de.consolecrafter.ccraft.cheats;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Basis aller Cheats.
 *
 * <p>Ein Cheat besitzt einen An/Aus-Zustand, einen optionalen Keybind und eine
 * Liste von Optionen, aus denen die Einstellungsseite automatisch ihre
 * Bedienelemente erzeugt. Unterklassen implementieren die eigentliche Wirkung
 * in {@link #onEnable()}, {@link #onDisable()} und {@link #onTick()}.</p>
 */
public abstract class Cheat {

    /** Eindeutige Id; zugleich Praefix aller Config-Schluessel. */
    public final String id;
    /** Anzeigename im Menue. */
    public final String name;
    /** Einzeilige Beschreibung fuer die Einstellungsseite. */
    public final String description;

    protected final List<Option> options = new ArrayList<>();

    private boolean enabled;
    private KeyBinding keybind;

    protected Cheat(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // ── Zustand ──────────────────────────────────────────────────────────────
    public final boolean isEnabled() {
        return enabled;
    }

    /** Schaltet den Cheat um und ruft die Lifecycle-Hooks. */
    public final void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        CheatConfig.set(id, "enabled", value);
        if (value) onEnable(); else onDisable();
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Schaltet ab, ohne den Zustand zu speichern.
     *
     * <p>Wird beim Verlassen der Welt benutzt: der Cheat soll sofort aufhoeren
     * zu wirken, aber beim naechsten Start wieder aktiv sein. Wuerde hier
     * {@link #setEnabled(boolean)} benutzt, schriebe der Aufruf {@code false} in
     * die Config und der gespeicherte Zustand waere dauerhaft weg.</p>
     */
    final void disableInMemory() {
        if (!enabled) return;
        enabled = false;
        onDisable();
    }

    /** Stellt den Zustand aus der Config her, ohne Hooks zu rufen (Startphase). */
    final void restore(boolean value) {
        this.enabled = value;
        onRestore();
    }

    /** Startwert, wenn die Config noch keinen Eintrag hat (z. B. nach einem Update). */
    protected boolean defaultEnabled() {
        return false;
    }

    // ── Keybind ──────────────────────────────────────────────────────────────
    public final KeyBinding keybind() {
        return keybind;
    }

    final void setKeybind(KeyBinding keybind) {
        this.keybind = keybind;
    }

    /** Loest den Keybind aus (wird vom Manager jeden Tick aufgerufen). */
    final void pollKeybind() {
        if (keybind != null && keybind.wasPressed()) {
            toggle();
            var client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("§c§lCCraft§r §8» " + name + ": "
                                + (enabled ? "§aAN" : "§cAUS")), true);
            }
        }
    }

    // ── Optionen ─────────────────────────────────────────────────────────────
    public final List<Option> options() {
        return options;
    }

    protected Option bool(String key, String label, boolean def) {
        return add(new Option(id, Option.Kind.BOOLEAN, key, label, def));
    }

    protected Option number(String key, String label, double def, double min, double max, double step) {
        Option option = new Option(id, Option.Kind.NUMBER, key, label, def);
        option.min = min;
        option.max = max;
        option.step = step;
        return add(option);
    }

    protected Option choice(String key, String label, String def, String... allowed) {
        Option option = new Option(id, Option.Kind.CHOICE, key, label, def);
        option.allowed = allowed;
        return add(option);
    }

    /** Freies Textfeld; der Wert wird unveraendert gespeichert. */
    protected Option text(String key, String label, String def) {
        return add(new Option(id, Option.Kind.TEXT, key, label, def));
    }

    private Option add(Option option) {
        options.add(option);
        return option;
    }

    // ── Lifecycle-Hooks ──────────────────────────────────────────────────────
    protected void onEnable() {}
    protected void onDisable() {}
    /** Nach dem Laden des Zustands aus der Config (Startphase, ohne Hooks). */
    protected void onRestore() {}
    /** Wird jeden Client-Tick gerufen, aber nur solange der Cheat aktiv ist. */
    protected void onTick() {}
    /** Wird jeden Frame ueber das HUD gerufen, aber nur solange der Cheat aktiv ist. */
    protected void onHudRender(net.minecraft.client.gui.DrawContext ctx) {}
    /** Ob der Cheat beim Verlassen der Welt aktiv bleiben soll. */
    protected boolean keepOnDisconnect() {
        return false;
    }

    /** Optionen zuruecksetzen und speichern. */
    public final void resetOptions() {
        for (Option option : options) option.reset();
        CheatConfig.save();
    }

    /** Kurzer Zusatztext im Menue (z. B. Zaehler); leer bedeutet "nichts anzeigen". */
    public String statusText() {
        return "";
    }
}
