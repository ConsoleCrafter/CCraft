package de.consolecrafter.ccraft.commands.impl;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.commands.Command;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/** Startet einen der Züge, die über den Bildschirm fahren. */
public class TrainCommand implements Command {

    public enum Kind {
        SID("sidtrain", "Lässt den SidTrain über den Bildschirm fahren", "Tutuuuuuuuuuuuut der SidTrain kommt!!"),
        DEVIN("devintrain", "Lässt den DevinTrain über den Bildschirm fahren", "Tutuuuuuuuuuuuut der DevinTrain kommt!!"),
        MELON("melontrain", "Lässt den MelonTrain über den Bildschirm fahren", "Tutuuuuuuuuuuuut der MelonTrain kommt!!"),
        DANIEL("danieltrain", "Lässt den DanielTrain über den Bildschirm fahren", "Tutuuuuuuuuuuuut der DanielTrain kommt!!");

        final String name;
        final String description;
        final String message;

        Kind(String name, String description, String message) {
            this.name = name;
            this.description = description;
            this.message = message;
        }
    }

    private final Kind kind;

    public TrainCommand(Kind kind) {
        this.kind = kind;
    }

    @Override
    public String getName() {
        return kind.name;
    }

    @Override
    public String getDescription() {
        return kind.description;
    }

    @Override
    public String getUsage() {
        return "#" + kind.name;
    }

    @Override
    public void execute(ClientPlayerEntity player, String[] args) {
        switch (kind) {
            case SID -> CCraftMod.customHud.sidTrain.start();
            case DEVIN -> CCraftMod.customHud.devinTrain.start();
            case MELON -> CCraftMod.customHud.melonTrain.start();
            case DANIEL -> CCraftMod.customHud.danielTrain.start();
        }
        player.sendMessage(Text.literal("§c§nCCraft§r §0» §6" + kind.message), false);
    }
}