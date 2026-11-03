package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;

/**
 * Flight — einstellbarer Flug-Modus mit erweiterter Konfiguration.
 *
 * <p>{@code Kreativ} nutzt die normalen Flug-Faehigkeiten, {@code Vanilla-Hack}
 * simuliert Flug ueber Schwerkraft-Aufhebung und Geschwindigkeitsmanipulation
 * und funktioniert auch ausserhalb des Kreativmodus.</p>
 */
public class Flight extends Cheat {

    public static final String MODE_CREATIVE = "Kreativ";
    public static final String MODE_VANILLA = "Vanilla-Hack";

    private final Option mode = choice("mode", "Modus", MODE_CREATIVE, MODE_CREATIVE, MODE_VANILLA);
    private final Option speed = number("speed", "Flugtempo", 1.0, 0.5, 5.0, 0.25);
    private final Option vertical = bool("vertical", "Vertikal fliegen", true);
    private final Option antiKick = bool("antiKick", "Anti-Kick (sanft sinken)", true);
    private final Option sprintBoost = bool("sprintBoost", "Sprint-Boost", false);
    private final Option damage = bool("damage", "Sturzschaden vermeiden", true);

    // Vor dem Aktivieren gesicherte Werte, damit beim Ausschalten exakt
    // wiederhergestellt werden kann.
    private boolean savedAllowFlying;
    private boolean savedFlying;
    private boolean savedNoGravity;
    private float savedFlySpeed = 0.05F;
    private boolean saved;

    private int antiKickTicks;

    public Flight() {
        super("flight", "Flight", "Einstellbarer Flug-Modus mit erweiterter Konfiguration.");
    }

    @Override
    protected void onEnable() {
        antiKickTicks = 0;
        saved = false;
    }

    @Override
    protected void onDisable() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !saved) return;

        PlayerAbilities abilities = player.getAbilities();
        abilities.allowFlying = savedAllowFlying;
        abilities.flying = savedFlying;
        abilities.setFlySpeed(savedFlySpeed);
        player.setNoGravity(savedNoGravity);
        player.fallDistance = 0.0F;
        player.sendAbilitiesUpdate();
        saved = false;
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        PlayerAbilities abilities = player.getAbilities();
        if (!saved) {
            savedAllowFlying = abilities.allowFlying;
            savedFlying = abilities.flying;
            savedFlySpeed = abilities.getFlySpeed();
            savedNoGravity = player.hasNoGravity();
            saved = true;
        }

        if (MODE_CREATIVE.equals(mode.asChoice())) {
            tickCreative(player, abilities);
        } else {
            tickVanillaHack(player, abilities);
        }
    }

    /** Kreativmodus: Faehigkeiten setzen und Tempo laufend nachziehen. */
    private void tickCreative(ClientPlayerEntity player, PlayerAbilities abilities) {
        boolean changed = false;
        if (!abilities.allowFlying) { abilities.allowFlying = true; changed = true; }
        if (!abilities.flying) { abilities.flying = true; changed = true; }

        float wanted = (float) (savedFlySpeed * speed.asNumber());
        if (Math.abs(abilities.getFlySpeed() - wanted) > 1.0E-4F) {
            abilities.setFlySpeed(wanted);
            changed = true;
        }
        if (changed) player.sendAbilitiesUpdate();

        if (damage.asBoolean()) player.fallDistance = 0.0F;
    }

    /**
     * Vanilla-Hack: hebt die Schwerkraft auf und setzt die Geschwindigkeit
     * gemaess Eingabe. Ohne Tastendruck bleibt die Position stabil, damit kein
     * dauerhaftes Sinken entsteht.
     */
    private void tickVanillaHack(ClientPlayerEntity player, PlayerAbilities abilities) {
        if (!player.hasNoGravity()) player.setNoGravity(true);
        if (abilities.flying) {
            abilities.flying = false;
            player.sendAbilitiesUpdate();
        }
        if (damage.asBoolean()) player.fallDistance = 0.0F;

        var input = player.input;
        boolean forward = input.playerInput.forward();
        boolean backward = input.playerInput.backward();
        boolean left = input.playerInput.left();
        boolean right = input.playerInput.right();
        boolean jumping = input.playerInput.jump();
        boolean sneaking = input.playerInput.sneak();

        double base = speed.asNumber();
        double boost = sprintBoost.asBoolean() && player.isSprinting() ? 1.5 : 1.0;

        double yaw = Math.toRadians(player.getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = forwardZ;
        double rightZ = -forwardX;

        double motionX = 0.0;
        double motionZ = 0.0;
        if (forward) { motionX += forwardX; motionZ += forwardZ; }
        if (backward) { motionX -= forwardX; motionZ -= forwardZ; }
        if (right) { motionX += rightX; motionZ += rightZ; }
        if (left) { motionX -= rightX; motionZ -= rightZ; }

        double motionY = 0.0;
        if (vertical.asBoolean()) {
            if (jumping) motionY += 1.0;
            if (sneaking) motionY -= 1.0;
        }

        boolean moving = motionX != 0.0 || motionZ != 0.0 || motionY != 0.0;
        if (moving) {
            antiKickTicks = 0;
            double horizontal = Math.hypot(motionX, motionZ);
            if (horizontal > 0.0) {
                motionX = motionX / horizontal * base * boost;
                motionZ = motionZ / horizontal * base * boost;
            }
            if (motionY != 0.0) motionY = Math.signum(motionY) * base * boost;
        } else if (antiKick.asBoolean()) {
            // Anti-Kick: regelmaessig minimal sinken, damit der Server die
            // Position als plausibel ansieht.
            if (++antiKickTicks >= 40) {
                motionY = -0.04;
                antiKickTicks = 0;
            }
        } else {
            antiKickTicks = 0;
        }

        player.setVelocity(motionX, motionY, motionZ);
        player.velocityModified = true;
        // Nur bei echter Vertikalbewegung als "in der Luft" melden; sonst
        // wuerde das Flag beim Stehen jeden Tick umspringen.
        if (motionY != 0.0) player.setOnGround(false);
    }
}
