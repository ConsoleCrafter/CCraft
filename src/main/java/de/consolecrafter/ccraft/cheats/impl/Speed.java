package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Speed — erhoeht das Lauf-Tempo durch Skalierung der horizontalen
 * Geschwindigkeit.
 *
 * <p>Der Server uebernimmt die vom Client gemeldete Position, solange sie
 * plausibel ist. Ein moderater Multiplikator bleibt daher wirksam; zu hohe
 * Werte werden vom Server zurueckgesetzt ("moved too quickly").</p>
 */
public class Speed extends Cheat {

    private final Option multiplier = number("multiplier", "Multiplikator", 1.4, 1.05, 3.0, 0.05);
    private final Option onlyGround = bool("onlyGround", "Nur auf dem Boden", true);
    private final Option onlyForward = bool("onlyForward", "Nur beim Vorwaertsgehen", true);

    public Speed() {
        super("speed", "Speed", "Erhoeht das Lauf-Tempo.");
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        if (onlyGround.asBoolean() && !player.isOnGround()) return;

        var input = player.input;
        boolean moving = onlyForward.asBoolean()
                ? input.playerInput.forward()
                : input.hasForwardMovement() || input.playerInput.backward()
                        || input.playerInput.left() || input.playerInput.right();
        if (!moving) return;

        Vec3d velocity = player.getVelocity();
        double horizontal = Math.hypot(velocity.x, velocity.z);
        if (horizontal < 1.0E-4) return;

        // Nur die Horizontalkomponente skalieren, damit Springen/Fallen
        // unbeeinflusst bleibt.
        double factor = multiplier.asNumber();
        player.setVelocity(velocity.x * factor, velocity.y, velocity.z * factor);
        player.velocityModified = true;
    }
}
