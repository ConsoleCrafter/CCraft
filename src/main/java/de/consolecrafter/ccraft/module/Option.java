package de.consolecrafter.ccraft.module;

import java.util.Locale;

public final class Option {

    public enum Kind { BOOLEAN, NUMBER, CHOICE, TEXT }

    public final Kind kind;
    public final String key;
    public final String label;

    private final Module owner;
    private final String defaultValue;
    private final String[] allowed;
    private final double min;
    private final double max;
    private final double step;

    Option(Module owner, Kind kind, String key, String label, String defaultValue,
           String[] allowed, double min, double max, double step) {
        this.owner = owner;
        this.kind = kind;
        this.key = key;
        this.label = label;
        this.defaultValue = defaultValue;
        this.allowed = allowed;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    private String raw() {
        String value = owner.stored(key);
        return value == null ? defaultValue : value;
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(raw());
    }

    public double asNumber() {
        try {
            return Math.max(min, Math.min(max, Double.parseDouble(raw())));
        } catch (NumberFormatException e) {
            return Double.parseDouble(defaultValue);
        }
    }

    public int asInt() {
        return (int) Math.round(asNumber());
    }

    public String asChoice() {
        String value = raw();
        for (String option : allowed) {
            if (option.equals(value)) return value;
        }
        return defaultValue;
    }

    public String asText() {
        return raw();
    }

    public void set(Object value) {
        owner.store(key, String.valueOf(value));
    }

    public void cycle() {
        if (kind == Kind.BOOLEAN) {
            set(!asBoolean());
        } else if (kind == Kind.CHOICE) {
            String current = asChoice();
            int index = 0;
            for (int i = 0; i < allowed.length; i++) {
                if (allowed[i].equals(current)) index = i;
            }
            set(allowed[(index + 1) % allowed.length]);
        }
    }

    public void step(int direction) {
        if (kind != Kind.NUMBER) return;
        double value = Math.max(min, Math.min(max, asNumber() + direction * step));
        set(step >= 1.0 ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value));
    }

    public void reset() {
        owner.store(key, null);
    }

    public boolean isDefault() {
        return switch (kind) {
            case NUMBER -> asNumber() == Double.parseDouble(defaultValue);
            case CHOICE -> asChoice().equals(defaultValue);
            default -> raw().equals(defaultValue);
        };
    }

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
