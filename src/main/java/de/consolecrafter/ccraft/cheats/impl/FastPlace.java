package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;

/**
 * FastPlace — verkuerzt die Wartezeit zwischen zwei Blockplatzierungen.
 *
 * <p>Vanilla setzt am ANFANG von {@code MinecraftClient.doItemUse()} den
 * Cooldown {@code itemUseCooldown = 4}. Da das Feld privat ist, greift
 * {@code FastPlaceMixin} am Ende der Methode (RETURN) ein und ueberschreibt
 * den Wert ueber {@link #cooldown(int)}.</p>
 */
public class FastPlace extends Cheat {

    private final Option cooldown = number("cooldown", "Cooldown (Ticks)", 0.0, 0.0, 4.0, 1.0);
    private final Option onlyBlocks = bool("onlyBlocks", "Nur Bloecke", true);

    public FastPlace() {
        super("fastplace", "FastPlace", "Verkuerzt die Wartezeit beim Blockplatzieren.");
    }

    /**
     * Vom Mixin aufgerufen. Liefert den Cooldown, der statt {@code vanilla}
     * gesetzt werden soll.
     */
    public static int cooldown(int vanilla) {
        Cheat cheat = CheatManager.byId("fastplace");
        if (!(cheat instanceof FastPlace fastPlace) || !fastPlace.isEnabled()) return vanilla;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return vanilla;

        if (fastPlace.onlyBlocks.asBoolean()) {
            boolean holdingBlock =
                    player.getMainHandStack().getItem() instanceof BlockItem
                 || player.getOffHandStack().getItem() instanceof BlockItem;
            if (!holdingBlock) return vanilla;
        }
        return Math.min(vanilla, fastPlace.cooldown.asInt());
    }
}