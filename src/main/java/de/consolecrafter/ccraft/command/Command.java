package de.consolecrafter.ccraft.command;

import net.minecraft.client.player.LocalPlayer;

public interface Command {

    String name();

    String description();

    void execute(LocalPlayer player, String[] args);
}
