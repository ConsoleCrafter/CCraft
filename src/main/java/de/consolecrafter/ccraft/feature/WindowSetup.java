package de.consolecrafter.ccraft.feature;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.module.ModuleManager;
import de.consolecrafter.ccraft.util.ModInfo;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLVideo;

public final class WindowSetup {

    private static boolean done;

    private WindowSetup() {}

    public static void tick(Minecraft minecraft) {
        if (done) return;
        done = true;
        if (minecraft.options.fullscreen().get()) return;
        try {
            SDLVideo.SDL_MaximizeWindow(minecraft.getWindow().handle());
        } catch (Throwable t) {
            CCraftMod.LOGGER.warn("Window could not be maximized", t);
        }
    }

    public static String title() {
        Minecraft minecraft = Minecraft.getInstance();
        String name = minecraft.getUser() != null ? ModuleManager.NAME_PROTECT.replace(minecraft.getUser().getName()) : "";
        return "CCraft " + ModInfo.display() + " (Minecraft " + ModInfo.minecraftVersion() + ") --> " + name;
    }
}
