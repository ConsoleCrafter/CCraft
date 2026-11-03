package de.consolecrafter.ccraft.listener;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.commands.CommandHandler;
import de.consolecrafter.ccraft.gui.ChatLoggerScreen;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

public class ChatListener {

    public static void register() {
        CCraftMod.LOGGER.info("Registering ChatListener (Client-side)...");

        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            CCraftMod.LOGGER.info("Client chat message: " + message);

            if (message.startsWith("#")) {
                CCraftMod.LOGGER.info("Detected command: " + message);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    CommandHandler.handleCommand(client.player, message);
                }
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            String name = client.player != null ? client.player.getName().getString() : "Du";
            ChatLoggerScreen.addMessage("<" + name + "> " + message, true);

            return true;
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            ChatLoggerScreen.addMessage(message.getString(), true);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                ChatLoggerScreen.addMessage(message.getString(), false);
            }
        });

        CCraftMod.LOGGER.info("ChatListener registered successfully!");
    }
}
