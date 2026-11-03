package de.consolecrafter.ccraft.keybinds;

import de.consolecrafter.ccraft.cheats.CheatManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static KeyBinding toggleCommandMenu;

    public static void register() {
        toggleCommandMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ccraft.toggle_command_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CheatManager.CATEGORY
        ));
    }
}
