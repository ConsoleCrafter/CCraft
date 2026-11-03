package de.consolecrafter.ccraft.commands.impl;

import de.consolecrafter.ccraft.commands.Command;
import de.consolecrafter.ccraft.commands.CommandRegistry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class HelpCommand implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Zeigt alle verfügbaren Befehle";
    }

    @Override
    public String getUsage() {
        return "#help";
    }

    @Override
    public void execute(ClientPlayerEntity player, String[] args) {
        player.sendMessage(Text.literal("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§6§l           CCraft Befehle"), false);
        player.sendMessage(Text.literal("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);

        for (Command command : CommandRegistry.getAllCommands().values()) {
            player.sendMessage(Text.literal("  §e#" + command.getName() + " §7- §f" + command.getDescription()), false);
        }

        player.sendMessage(Text.literal("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }
}
