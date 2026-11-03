package de.consolecrafter.ccraft.command.impl;

import de.consolecrafter.ccraft.command.Command;
import de.consolecrafter.ccraft.command.CommandManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class HelpCommand implements Command {

    private static final String LINE = "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Zeigt alle verfügbaren Befehle";
    }

    @Override
    public void execute(LocalPlayer player, String[] args) {
        player.sendSystemMessage(Component.literal(LINE));
        player.sendSystemMessage(Component.literal("§6§l           CCraft Befehle"));
        player.sendSystemMessage(Component.literal(LINE));
        for (Command command : CommandManager.all()) {
            player.sendSystemMessage(Component.literal("  §e" + CommandManager.PREFIX + command.name() + " §7- §f" + command.description()));
        }
        player.sendSystemMessage(Component.literal(LINE));
    }
}
