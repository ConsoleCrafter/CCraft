package de.consolecrafter.ccraft.module;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.module.impl.AntiParticleCrash;
import de.consolecrafter.ccraft.module.impl.Fullbright;
import de.consolecrafter.ccraft.module.impl.NameProtect;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ModuleManager {

    public static final Fullbright FULLBRIGHT = new Fullbright();
    public static final NameProtect NAME_PROTECT = new NameProtect();
    public static final AntiParticleCrash ANTI_PARTICLE_CRASH = new AntiParticleCrash();

    private static final List<Module> MODULES = List.of(FULLBRIGHT, NAME_PROTECT, ANTI_PARTICLE_CRASH);
    private static final Set<Integer> HELD_KEYS = new HashSet<>();

    private ModuleManager() {}

    public static void init() {
        for (Module module : MODULES) module.restore();
    }

    public static List<Module> all() {
        return MODULES;
    }

    public static List<Module> search(String query) {
        if (query == null || query.isBlank()) return MODULES;
        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Module> hits = new ArrayList<>();
        for (Module module : MODULES) {
            if (module.name.toLowerCase(Locale.ROOT).contains(needle)
                    || module.description.toLowerCase(Locale.ROOT).contains(needle)) {
                hits.add(module);
            }
        }
        return hits;
    }

    public static void tick(Minecraft minecraft) {
        ANTI_PARTICLE_CRASH.resetTick();
        if (minecraft.gui.screen() != null) {
            HELD_KEYS.clear();
            return;
        }
        for (Module module : MODULES) {
            if (!module.hasKey()) continue;
            InputConstants.Key key = module.key();
            if (key.getType() != InputConstants.Type.KEYBOARD) continue;
            int code = key.getValue();
            boolean down = InputConstants.isKeyDown(code);
            if (down && HELD_KEYS.add(code)) {
                module.toggle();
                playToggleSound(minecraft, module.isEnabled());
            } else if (!down) {
                HELD_KEYS.remove(code);
            }
        }
    }

    public static boolean conflicts(Module module) {
        if (!module.hasKey()) return false;
        InputConstants.Key key = module.key();
        for (Module other : MODULES) {
            if (other != module && other.hasKey() && other.key().equals(key)) return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null) {
            for (KeyMapping mapping : minecraft.options.keyMappings) {
                if (!mapping.isUnbound() && KeyMappingHelper.getBoundKeyOf(mapping).equals(key)) return true;
            }
        }
        return false;
    }

    private static void playToggleSound(Minecraft minecraft, boolean enabled) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, enabled ? 1.4f : 0.8f));
    }
}
