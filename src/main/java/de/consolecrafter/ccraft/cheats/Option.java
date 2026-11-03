package de.consolecrafter.ccraft.cheats;

import java.util.Locale;

/**
 * Eine einzelne Einstellung eines Cheats (Schalter, Zahl oder Auswahl).
 *
 * <p>Die Einstellungsseite erzeugt pro Option automatisch das passende
 * Bedienelement; Werte werden sofort in {@link CheatConfig} gespeichert.</p>
 */
public final class Option {

    public enum Kind { BOOLEAN, NUMBER, CHOICE, TEXT }

    public final Kind kind;
    public final String key;
    public final String label;

    private final String ownerId;
    private final Object defaultValue;

    /** Erlaubte Werte bei {@link Kind#CHOICE}. */
    String[] allowed = new String[0];
    double min;
    double max;
    double step = 1.0;

    Option(String ownerId, Kind kind, String key, String label, Object defaultValue) {
        this.ownerId = ownerId;
        this.kind = kind;
        this.key = key;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    // ── Werte lesen ──────────────────────────────────────────────────────────
    public boolean asBoolean() {
        return CheatConfig.getBoolean(ownerId, key, (Boolean) defaultValue);
    }

    public double asNumber() {
        return CheatConfig.getDouble(ownerId, key, (Double) defaultValue, min, max);
    }

    public int asInt() {
        return (int) Math.round(asNumber());
    }

    public String asChoice() {
        return CheatConfig.getChoice(ownerId, key, (String) defaultValue, allowed);
    }

    /** Roher Textwert (nur bei {@link Kind#TEXT}). */
    public String asText() {
        String value = CheatConfig.getRaw(ownerId, key);
        return value == null ? (String) defaultValue : value;
    }

    // ── Werte schreiben ──────────────────────────────────────────────────────
    public void set(Object value) {
        CheatConfig.set(ownerId, key, value);
    }

    /** Schaltet auf den naechsten Wert (nur Schalter und Auswahl). */
    public void cycle() {
        if (kind == Kind.BOOLEAN) {
            set(!asBoolean());
        } else if (kind == Kind.CHOICE) {
            int index = 0;
            for (int i = 0; i < allowed.length; i++) {
                if (allowed[i].equals(asChoice())) index = i;
            }
            set(allowed[(index + 1) % allowed.length]);
        }
    }

    public void reset() {
        set(defaultValue);
    }

    /** Ob der aktuelle Wert noch dem Standardwert entspricht. */
    public boolean isDefault() {
        return switch (kind) {
            case BOOLEAN -> asBoolean() == (Boolean) defaultValue;
            case CHOICE -> asChoice().equals(defaultValue);
            case TEXT -> asText().equals(defaultValue);
            case NUMBER -> asNumber() == (Double) defaultValue;
        };
    }

    /** Erhoeht bzw. senkt eine Zahl um einen Schritt und begrenzt sie auf min/max. */
    public void step(int direction) {
        if (kind != Kind.NUMBER) return;
        double value = asNumber() + direction * step;
        set(Math.max(min, Math.min(max, value)));
    }

    /** Anzeigetext des aktuellen Werts, z. B. {@code "AN"}, {@code "4.50"} oder {@code "Spieler"}. */
    public String display() {
        return switch (kind) {
            case BOOLEAN -> asBoolean() ? "AN" : "AUS";
            case CHOICE -> asChoice();
            case TEXT -> asText();
            case NUMBER -> step >= 1.0
                    ? String.valueOf((long) asNumber())
                    : String.format(Locale.ROOT, "%.2f", asNumber());
        };
    }
}
