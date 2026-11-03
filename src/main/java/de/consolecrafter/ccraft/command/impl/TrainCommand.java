package de.consolecrafter.ccraft.command.impl;

import de.consolecrafter.ccraft.command.Command;
import de.consolecrafter.ccraft.hud.Trains;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class TrainCommand implements Command {

    private final Trains.Kind kind;

    public TrainCommand(Trains.Kind kind) {
        this.kind = kind;
    }

    @Override
    public String name() {
        return kind.command();
    }

    @Override
    public String description() {
        return "Lässt den " + kind.displayName() + " über den Bildschirm fahren";
    }

    @Override
    public void execute(LocalPlayer player, String[] args) {
        Trains.start(kind);
        player.sendSystemMessage(Component.literal("§c§nCCraft§r §0» §6Tutuuuuuuuuuuuut der " + kind.displayName() + " kommt!!"));
    }
}
