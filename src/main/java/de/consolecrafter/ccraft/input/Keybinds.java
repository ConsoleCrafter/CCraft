package de.consolecrafter.ccraft.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.consolecrafter.ccraft.CCraftMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class Keybinds {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CCraftMod.MOD_ID, "main"));

    public static KeyMapping menu;
    public static KeyMapping zoom;

    private Keybinds() {}

    public static void init() {
        menu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ccraft.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, CATEGORY));
        zoom = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ccraft.zoom", InputConstants.Type.KEYBOARD, InputConstants.KEY_C, CATEGORY));
    }
}
