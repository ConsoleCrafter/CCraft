package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Scaffold — platziert automatisch Bloecke unter dem Spieler.
 *
 * <p>Es wird nur dann ein Block gesetzt, wenn sich der Spieler in der Luft
 * befindet, der Zielplatz leer ist und ein Block in der Hotbar liegt. Die
 * Blickrichtung bleibt unveraendert, damit die Steuerung nicht uebernommen
 * wird.</p>
 */
public class Scaffold extends Cheat {

    private final Option reach = number("reach", "Reichweite (m)", 4.5, 3.0, 6.0, 0.5);
    private final Option tower = bool("tower", "Turm-Modus (beim Springen)", false);
    private final Option swing = bool("swing", "Arm schwingen", true);

    public Scaffold() {
        super("scaffold", "Scaffold", "Platziert automatisch Bloecke unter dem Spieler.");
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        if (client.interactionManager == null) return;

        // Im Turm-Modus auch waehrend des Sprungs platzieren, sonst nur im Fall.
        boolean airborne = !player.isOnGround();
        boolean jumping = player.input.playerInput.jump();
        if (!airborne) return;
        if (!tower.asBoolean() && player.getVelocity().y > 0.0 && !jumping) return;

        BlockPos below = player.getBlockPos().down();
        if (!client.world.getBlockState(below).isReplaceable()) return;

        int slot = findBlockSlot(player);
        if (slot < 0) return;

        Vec3d hitPos = Vec3d.ofCenter(below).add(0.0, 0.5, 0.0);
        if (player.getEyePos().distanceTo(hitPos) > reach.asNumber()) return;

        BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, below, false);

        int previous = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        try {
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
            if (swing.asBoolean()) player.swingHand(Hand.MAIN_HAND);
        } finally {
            player.getInventory().setSelectedSlot(previous);
        }
    }

    /** Erster Hotbar-Platz mit einem Block, oder {@code -1}. */
    private int findBlockSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
