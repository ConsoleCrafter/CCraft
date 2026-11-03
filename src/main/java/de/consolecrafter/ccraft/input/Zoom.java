package de.consolecrafter.ccraft.input;

import net.minecraft.client.Minecraft;

public final class Zoom {

    private static final float ZOOM_FACTOR = 0.3f;
    private static final float SPEED = 14.0f;

    private static float current = 1.0f;
    private static long lastNanos;

    private Zoom() {}

    public static float apply(float fov) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean active = Keybinds.zoom != null && Keybinds.zoom.isDown() && minecraft.gui.screen() == null;
        float target = active ? ZOOM_FACTOR : 1.0f;

        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.0f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000.0f);
        lastNanos = now;
        current += (target - current) * (1.0f - (float) Math.exp(-SPEED * dt));
        if (Math.abs(current - target) < 0.001f) current = target;

        return fov * current;
    }
}
