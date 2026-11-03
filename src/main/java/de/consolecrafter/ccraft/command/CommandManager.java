package de.consolecrafter.ccraft.command;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.command.impl.HelpCommand;
import de.consolecrafter.ccraft.command.impl.TrainCommand;
import de.consolecrafter.ccraft.hud.Trains;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CommandManager {

    public static final String PREFIX = "#";

    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

    private CommandManager() {}

    public static void init() {
        register(new HelpCommand());
        for (Trains.Kind kind : Trains.Kind.values()) register(new TrainCommand(kind));
    }

    private static void register(Command command) {
        COMMANDS.put(command.name().toLowerCase(Locale.ROOT), command);
    }

    public static Collection<Command> all() {
        return Collections.unmodifiableCollection(COMMANDS.values());
    }

    public static boolean isCommand(String message) {
        return message.startsWith(PREFIX);
    }

    public static void run(LocalPlayer player, String message) {
        String input = message.substring(PREFIX.length()).trim();
        if (input.isEmpty()) {
            player.sendSystemMessage(Component.literal("§eProbier mal §6" + PREFIX + "help§e, um alle Befehle zu sehen!"));
            return;
        }
        String[] parts = input.split("\\s+");
        String name = parts[0].toLowerCase(Locale.ROOT);
        Command command = COMMANDS.get(name);
        if (command == null) {
            player.sendSystemMessage(Component.literal("§cUnbekannter Befehl: " + PREFIX + name));
            return;
        }
        try {
            command.execute(player, Arrays.copyOfRange(parts, 1, parts.length));
        } catch (RuntimeException e) {
            player.sendSystemMessage(Component.literal("§cFehler beim Ausführen des Befehls!"));
            CCraftMod.LOGGER.error("Command {} failed", name, e);
        }
    }
}
