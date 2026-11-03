package de.consolecrafter.ccraft.commands;

import de.consolecrafter.ccraft.CCraftMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class CommandHandler {

    public static boolean handleCommand(ClientPlayerEntity player, String message) {
        if (!message.startsWith("#")) {
            return false;
        }

        String commandString = message.substring(1).trim();

        if (commandString.isEmpty()) {
            player.sendMessage(Text.literal("§eProbier mal §6#help§e, um alle Befehle zu sehen!"), false);
            return true;
        }

        String[] parts = commandString.split(" ");
        String commandName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Command command = CommandRegistry.getCommand(commandName);

        if (command == null) {
            player.sendMessage(Text.literal("§cUnbekannter Befehl: #" + commandName), false);
            return true;
        }

        try {
            command.execute(player, args);
            CCraftMod.LOGGER.info("Player " + player.getName().getString() + " executed command: #" + commandName);
        } catch (Exception e) {
            player.sendMessage(Text.literal("§cFehler beim Ausführen des Befehls!"), false);
            CCraftMod.LOGGER.error("Error executing command #" + commandName, e);
        }

        return true;
    }
}
