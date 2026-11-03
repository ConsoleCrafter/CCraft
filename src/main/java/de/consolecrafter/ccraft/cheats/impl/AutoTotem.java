package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.OptionalInt;

/**
 * AutoTotem — legt im Notfall automatisch einen Totem des Unsterblichkeits in
 * die Zweithand.
 *
 * <p>Das Inventar wird nur ueber den Netzwerk-Handler angefasst
 * ({@code clickSlot} mit {@link SlotActionType#SWAP} und Button
 * {@code 40}). Direkte Schreibzugriffe auf das Inventar wuerden beim naechsten
 * Server-Abgleich zurueckspringen; der Klick-Pfad ist derselbe, den auch das
 * Vanilla-Inventar benutzt, und wird vom Server bestaetigt.</p>
 *
 * <p>Getauscht wird nur bei Bedarf: Ist die Zweithand belegt oder liegt kein
 * Totem im Inventar, passiert nichts. Ein {@code cooldown} verhindert, dass in
 * einem Treffer mehrere Klicks hintereinander abgesetzt werden.</p>
 */
public class AutoTotem extends Cheat {

    private final Option healthThreshold = number("healthThreshold", "Auslösen ab Leben (HP)", 8.0, 1.0, 20.0, 1.0);
    private final Option cooldown = number("cooldown", "Pause zwischen Tausch (Ticks)", 10.0, 1.0, 100.0, 1.0);
    private final Option onlyWhenThreatened = bool("onlyWhenThreatened", "Nur bei Gefahr tauschen", true);

    private int cooldownTicks;

    public AutoTotem() {
        super("autototem", "AutoTotem",
                "Legt automatisch einen Totem in die Zweithand, bevor man stirbt.");
    }

    @Override
    protected void onEnable() {
        cooldownTicks = 0;
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (onlyWhenThreatened.asBoolean() && player.getHealth() > healthThreshold.asNumber()) return;

        int inventorySlot = findTotem(player.getInventory());
        if (inventorySlot < 0) return;

        ClientPlayerInteractionManager interaction = client.interactionManager;
        if (interaction == null) return;

        // clickSlot verlangt den syncId des aktuell offenen Handlers und
        // ignoriert den Klick sonst. getSlotIndex uebersetzt den Inventarindex
        // in die Slot-Id dieses Handlers — das ist derselbe Weg wie im
        // Vanilla-Inventar und damit unabhaengig von der Slot-Reihenfolge.
        ScreenHandler handler = player.currentScreenHandler;
        OptionalInt screenSlot = handler.getSlotIndex(player.getInventory(), inventorySlot);
        if (screenSlot.isEmpty()) return;

        // SWAP mit Button 40 tauscht den angeklickten Slot mit der Zweithand.
        interaction.clickSlot(
                handler.syncId,
                screenSlot.getAsInt(),
                PlayerInventory.OFF_HAND_SLOT,
                SlotActionType.SWAP,
                player);

        cooldownTicks = (int) Math.round(cooldown.asNumber());
    }

    /** Inventarslot mit einem Totem, oder {@code -1}. */
    private int findTotem(PlayerInventory inventory) {
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String statusText() {
        return cooldownTicks > 0 ? "§7wartet" : "";
    }

    /** Bleibt aktiv: der Tausch soll auch nach einem Weltwechsel sofort greifen. */
    @Override
    protected boolean keepOnDisconnect() {
        return true;
    }
}
