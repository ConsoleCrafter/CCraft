package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * NoFall — verhindert Sturzschaden.
 *
 * <p>Solange der Spieler faellt, meldet der Client dem Server "auf dem Boden",
 * wodurch der Server keinen Sturzschaden berechnet. Der echte Zustand bleibt
 * unveraendert, damit die Bewegung nicht beeinflusst wird.</p>
 */
public class NoFall extends Cheat {

    public static final String MODE_PACKET = "Paket";
    public static final String MODE_GROUND = "Boden-Flag";

    private final Option mode = choice("mode", "Modus", MODE_PACKET, MODE_PACKET, MODE_GROUND);
    private final Option minFall = number("minFall", "Ab Fallhoehe (m)", 3.0, 1.0, 20.0, 0.5);

    private boolean fakeGroundSent;

    public NoFall() {
        super("nofall", "NoFall", "Verhindert Sturzschaden.");
    }

    @Override
    protected void onDisable() {
        fakeGroundSent = false;
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        boolean falling = player.fallDistance > minFall.asNumber() && !player.isOnGround();

        if (MODE_GROUND.equals(mode.asChoice())) {
            // Nur das Flag setzen; der Server prueft die Hoehe selbst.
            if (falling) player.setOnGround(true);
            return;
        }

        if (falling) {
            if (player.networkHandler != null) {
                player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, player.horizontalCollision));
                fakeGroundSent = true;
            }
            player.fallDistance = 0.0;
        } else if (fakeGroundSent) {
            // Echten Zustand nachreichen, sobald die Gefahr vorbei ist.
            if (player.networkHandler != null) {
                player.networkHandler.sendPacket(
                        new PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround(), player.horizontalCollision));
            }
            fakeGroundSent = false;
        }
    }
}
