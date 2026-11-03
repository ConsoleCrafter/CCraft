package de.consolecrafter.ccraft.module;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.config.ClientConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {

    private static final String ENABLED = "enabled";
    private static final String KEY = "key";

    public final String id;
    public final String name;
    public final String description;

    private final List<Option> options = new ArrayList<>();
    private boolean enabled;

    protected Module(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        store(ENABLED, String.valueOf(value));
        if (value) onEnable(); else onDisable();
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    final void restore() {
        String stored = stored(ENABLED);
        enabled = stored != null ? Boolean.parseBoolean(stored) : defaultEnabled();
    }

    protected boolean defaultEnabled() {
        return false;
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public String statusText() {
        return "";
    }

    public final InputConstants.Key key() {
        String stored = stored(KEY);
        if (stored == null || stored.isBlank()) return InputConstants.UNKNOWN;
        try {
            return InputConstants.getKey(stored);
        } catch (IllegalArgumentException e) {
            return InputConstants.UNKNOWN;
        }
    }

    public final void setKey(InputConstants.Key key) {
        store(KEY, key == null || key.equals(InputConstants.UNKNOWN) ? null : key.getName());
    }

    public final boolean hasKey() {
        return !key().equals(InputConstants.UNKNOWN);
    }

    public final List<Option> options() {
        return Collections.unmodifiableList(options);
    }

    public final void resetOptions() {
        for (Option option : options) option.reset();
    }

    protected final Option bool(String key, String label, boolean def) {
        return add(new Option(this, Option.Kind.BOOLEAN, key, label, String.valueOf(def), new String[0], 0, 0, 0));
    }

    protected final Option number(String key, String label, double def, double min, double max, double step) {
        return add(new Option(this, Option.Kind.NUMBER, key, label, String.valueOf(def), new String[0], min, max, step));
    }

    protected final Option choice(String key, String label, String def, String... allowed) {
        return add(new Option(this, Option.Kind.CHOICE, key, label, def, allowed, 0, 0, 0));
    }

    protected final Option text(String key, String label, String def) {
        return add(new Option(this, Option.Kind.TEXT, key, label, def, new String[0], 0, 0, 0));
    }

    private Option add(Option option) {
        options.add(option);
        return option;
    }

    final String stored(String key) {
        Map<String, String> values = ClientConfig.get().modules.get(id);
        return values == null ? null : values.get(key);
    }

    final void store(String key, String value) {
        Map<String, Map<String, String>> all = ClientConfig.get().modules;
        if (value == null) {
            Map<String, String> values = all.get(id);
            if (values != null) values.remove(key);
        } else {
            all.computeIfAbsent(id, k -> new HashMap<>()).put(key, value);
        }
        ClientConfig.save();
    }
}
