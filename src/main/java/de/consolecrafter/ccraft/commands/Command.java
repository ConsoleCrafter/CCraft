package de.consolecrafter.ccraft.commands;

import net.minecraft.client.network.ClientPlayerEntity;

public interface Command {
    String getName();
    String getDescription();
    String getUsage();

    void execute(ClientPlayerEntity player, String[] args);
}
