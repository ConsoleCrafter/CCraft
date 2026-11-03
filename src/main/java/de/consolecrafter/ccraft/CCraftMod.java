package de.consolecrafter.ccraft;

import de.consolecrafter.ccraft.command.CommandManager;
import de.consolecrafter.ccraft.config.ClientConfig;
import de.consolecrafter.ccraft.feature.ChatLog;
import de.consolecrafter.ccraft.feature.ResourcePacks;
import de.consolecrafter.ccraft.feature.TickRate;
import de.consolecrafter.ccraft.feature.WindowSetup;
import de.consolecrafter.ccraft.gui.MenuScreen;
import de.consolecrafter.ccraft.hud.InfoHud;
import de.consolecrafter.ccraft.hud.KeystrokesHud;
import de.consolecrafter.ccraft.hud.ModuleListHud;
import de.consolecrafter.ccraft.input.Keybinds;
import de.consolecrafter.ccraft.module.ModuleManager;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CCraftMod implements ClientModInitializer {

    public static final String MOD_ID = "ccraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        Theme.apply();
        ModuleManager.init();
        CommandManager.init();
        Keybinds.init();
        ResourcePacks.init(FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow());

        HudElementRegistry.addLast(id("info"), InfoHud::extract);
        HudElementRegistry.addLast(id("keystrokes"), KeystrokesHud::extract);
        HudElementRegistry.addLast(id("modules"), ModuleListHud::extract);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ChatLog.close());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            TickRate.reset();
            InfoHud.onJoin();
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!CommandManager.isCommand(message)) return true;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) CommandManager.run(minecraft.player, message);
            return false;
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, type, time) -> ChatLog.add(message.getString(), true));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) ChatLog.add(message.getString(), false);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof JoinMultiplayerScreen)) return;
            for (AbstractWidget widget : Screens.getWidgets(screen)) {
                String label = widget.getMessage().getString();
                if (label.contains("ViaFabricPlus") || label.contains("Via Fabric Plus")) {
                    widget.setMessage(Component.literal("Version"));
                }
            }
        });
    }

    private void tick(Minecraft minecraft) {
        WindowSetup.tick(minecraft);
        ResourcePacks.tick(minecraft);
        ModuleManager.tick(minecraft);
        while (Keybinds.menu.consumeClick()) {
            if (minecraft.gui.screen() == null) minecraft.gui.setScreen(new MenuScreen());
        }
    }
}
