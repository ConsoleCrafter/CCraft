package de.consolecrafter.ccraft.commands;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.commands.impl.HelpCommand;
import de.consolecrafter.ccraft.commands.impl.TrainCommand;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private static final Map<String, Command> commands = new HashMap<>();

    public static void registerCommands() {
        CCraftMod.LOGGER.info("Registering commands...");

        registerCommand(new HelpCommand());
        registerCommand(new TrainCommand(TrainCommand.Kind.SID));
        registerCommand(new TrainCommand(TrainCommand.Kind.DEVIN));
        registerCommand(new TrainCommand(TrainCommand.Kind.MELON));
        registerCommand(new TrainCommand(TrainCommand.Kind.DANIEL));

        CCraftMod.LOGGER.info("Registered " + commands.size() + " command(s)");
    }

    public static void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        CCraftMod.LOGGER.info("Registered command: " + command.getName());
    }

    public static Command getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public static Map<String, Command> getAllCommands() {
        return new HashMap<>(commands);
    }

    public static boolean hasCommand(String name) {
        return commands.containsKey(name.toLowerCase());
    }
}