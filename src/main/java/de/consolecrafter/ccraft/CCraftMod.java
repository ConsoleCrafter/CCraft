package de.consolecrafter.ccraft;

import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.commands.CommandRegistry;
import de.consolecrafter.ccraft.gui.CommandMenuScreen;
import de.consolecrafter.ccraft.hud.CustomHud;
import de.consolecrafter.ccraft.keybinds.Keybinds;
import de.consolecrafter.ccraft.listener.ChatListener;
import de.consolecrafter.ccraft.util.CraftConfig;
import de.consolecrafter.ccraft.util.ModInfo;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CCraftMod implements ClientModInitializer {
    public static final String MOD_ID = "ccraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int ZOOM_FOV = 30;
    private static final String[] FONT_PACKS = {"", "ccraft_font", "ccraft_helltown", "ccraft_impact", "ccraft_lazyarie"};
    private static final String[] FONT_NAMES = {"Vanilla", "CCraft", "Helltown", "Impact", "Lazyarie"};

    private boolean wasZooming = false;
    /** FOV des Spielers vor dem Zoom, damit sie exakt zurueckkommt. */
    private int savedFov = -1;
    private static boolean menuOpen = false;

    /** Vollbild wird beim ersten Tick einmalig erzwungen. */
    private boolean fullscreenApplied = false;
    private boolean customFontPackSynced = false;

    /**
     * Ob die CCraft-Tastenbelegungen nach dem Laden von {@code options.txt}
     * bereits wiederhergestellt wurden.
     */
    private boolean keybindsRestored = false;

    private StatusEffectInstance virtualNightVision;

    public static CustomHud customHud;

    /** Schließt das Rechts-Shift-Menü (auch aus dem Screen heraus aufrufbar). */
    public static void closeCommandMenu() {
        closeCommandMenu(null);
    }

    /** Schließt das Menü und öffnet stattdessen {@code target} (oder das Spiel). */
    public static void closeCommandMenu(Screen target) {
        menuOpen = false;
        MinecraftClient.getInstance().setScreen(target);
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing CCraft Mod (Client)...");

        CraftConfig.load();
        Theme.apply();

        var modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        for (int i = 1; i < FONT_PACKS.length; i++) {
            boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.of(MOD_ID, FONT_PACKS[i]), modContainer,
                    Text.literal("CCraft " + FONT_NAMES[i] + " Font"), ResourcePackActivationType.NORMAL);
            if (!registered) LOGGER.warn("Could not register CCraft font pack: {}", FONT_PACKS[i]);
        }

        customHud = new CustomHud();

        CheatManager.register();
        CommandRegistry.registerCommands();
        ChatListener.register();
        Keybinds.register();

        // ViaFabricPlus haengt im Multiplayer-Menue einen Button mit dem Label
        // "ViaFabricPlus" an. Der wird hier in "Version" umbenannt.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof MultiplayerScreen)) return;
            for (ClickableWidget widget : Screens.getButtons(screen)) {
                String label = widget.getMessage().getString();
                if (label.contains("ViaFabricPlus") || label.contains("Via Fabric Plus")) {
                    widget.setMessage(Text.literal("Version"));
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            customHud.renderHud(drawContext, tickCounter);
            de.consolecrafter.ccraft.hud.KeystrokesRenderer.render(drawContext);
            CheatManager.renderHud(drawContext);
            de.consolecrafter.ccraft.hud.ActiveCheatsHud.render(drawContext);
        });

        // Beim Verlassen der Welt alle Cheats deaktivieren, damit Glow-Umrisse,
        // Attribute und Flugzustand nicht in die naechste Sitzung uebergehen.
        // Die Aenderung wird nicht gespeichert: beim naechsten Beitritt soll der
        // zuvor gespeicherte Zustand wieder gelten (siehe restoreSaved()).
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CheatManager.disableAll());

        // Beim Betreten der Welt den gespeicherten Zustand wiederherstellen. Noetig,
        // weil disableAll() nur im Speicher abschaltet und der Client ohne
        // Neustart direkt wieder einer Welt beitreten kann.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CheatManager.restoreSaved());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            customHud.onClientTick();
            CheatManager.tick(client);
            updateVirtualNightVision(client);
            if (!customFontPackSynced) customFontPackSynced = syncCustomFontPack(client);

            // Einmalig nach dem Start: Vanilla hat options.txt inzwischen geladen
            // und dabei moeglicherweise veraltete Tasten ueberschrieben. Erst
            // hier kann die CCraft-Belegung sicher gewinnen.
            if (!keybindsRestored) {
                keybindsRestored = true;
                CheatManager.restoreKeybinds();
            }

            // Beim ersten Tick das Fenster maximieren (mit Titelleiste und
            // sichtbarer Taskleiste, kein echtes Vollbild).
            if (!fullscreenApplied) {
                fullscreenApplied = true;
                applyMaximizedWindow(client);
            }

            if (Keybinds.toggleCommandMenu.wasPressed()) {
                if (menuOpen) {
                    closeCommandMenu();
                } else {
                    menuOpen = true;
                    client.setScreen(new CommandMenuScreen());
                }
            }

            // Zoom nur im Spiel ohne offenen Screen: sonst loest jedes "c" in
            // einem Textfeld (Chat, Suchfeld) ungewollt den Zoom aus.
            if (client.player != null && client.options != null && client.currentScreen == null) {
                long window = client.getWindow().getHandle();
                boolean isCPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS;

                if (isCPressed && !wasZooming) {
                    // Die aktuelle FOV merken und beim Loslassen genau dorthin
                    // zurueckkehren. Ein fest eingetragener Wert (vorher 82)
                    // haette die FOV-Einstellung des Spielers dauerhaft
                    // ueberschrieben — auch wenn er sie bewusst anders gesetzt hat.
                    savedFov = client.options.getFov().getValue();
                    client.options.getFov().setValue(ZOOM_FOV);
                    wasZooming = true;
                } else if (!isCPressed && wasZooming) {
                    client.options.getFov().setValue(savedFov > 0 ? savedFov : ZOOM_FOV);
                    wasZooming = false;
                }
            }

            if (client.getWindow() != null) {
                String playerName = (client.player != null)
                        ? client.player.getName().getString()
                        : (client.getSession() != null ? client.getSession().getUsername() : "Loading...");
                client.getWindow().setTitle("CCraft " + ModInfo.display() + " (Minecraft "
                        + client.getGameVersion() + ") --> " + playerName);
            }

        });

        copyResourcePack();
        activateResourcePack();

        LOGGER.info("CCraft Mod initialized!");
    }

    public static String fontName(int choice) {
        return FONT_NAMES[Math.max(0, Math.min(FONT_NAMES.length - 1, choice))];
    }

    public static void setFontChoice(int choice) {
        CraftConfig.fontChoice = Math.max(0, Math.min(FONT_NAMES.length - 1, choice));
        CraftConfig.save();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> applyFontPack(client));
        }
    }

    private boolean syncCustomFontPack(MinecraftClient client) {
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();
        for (int i = 1; i < FONT_PACKS.length; i++) {
            if (!manager.hasProfile(Identifier.of(MOD_ID, FONT_PACKS[i]).toString())) return false;
        }
        applyFontPack(client);
        return true;
    }

    private static void applyFontPack(MinecraftClient client) {
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();
        String selectedPack = FONT_PACKS[CraftConfig.fontChoice];
        if (!selectedPack.isEmpty() && !manager.hasProfile(Identifier.of(MOD_ID, selectedPack).toString())) {
            LOGGER.warn("Selected CCraft font pack is not available: {}", selectedPack);
            return;
        }

        List<String> enabledPacks = new ArrayList<>(manager.getEnabledIds());
        boolean selectedWasEnabled = false;
        boolean changed = false;
        String selectedPackId = selectedPack.isEmpty() ? null : Identifier.of(MOD_ID, selectedPack).toString();
        for (int i = 1; i < FONT_PACKS.length; i++) {
            String packId = Identifier.of(MOD_ID, FONT_PACKS[i]).toString();
            if (enabledPacks.remove(packId)) {
                changed = true;
                if (packId.equals(selectedPackId)) selectedWasEnabled = true;
            }
        }
        if (selectedPackId != null && !selectedWasEnabled) {
            enabledPacks.add(selectedPackId);
            changed = true;
        }
        if (!changed) return;

        List<ResourcePackProfile> profiles = new ArrayList<>();
        for (String enabledId : enabledPacks) {
            ResourcePackProfile profile = manager.getProfile(enabledId);
            if (profile != null) profiles.add(profile);
        }
        manager.setEnabledProfiles(profiles.stream().map(ResourcePackProfile::getId).toList());
        client.reloadResources();
    }

    private void updateVirtualNightVision(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            virtualNightVision = null;
            return;
        }

        var player = client.player;
        StatusEffectInstance active = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (!CraftConfig.fullbright) {
            if (virtualNightVision != null && active == virtualNightVision) {
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
            virtualNightVision = null;
            return;
        }

        if (active == null || active == virtualNightVision && active.getDuration() <= 40) {
            virtualNightVision = new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 220, 0, true, false, false);
            player.setStatusEffect(virtualNightVision, null);
        } else if (active != virtualNightVision) {
            virtualNightVision = null;
        }
    }

    /**
     * Maximiert das Spielfenster beim Start: füllt die Arbeitsfläche (ohne
     * Taskleiste), behält aber die normale Titelleiste mit Minimieren/
     * Maximieren/Schließen. Kein echtes Vollbild.
     */
    private void applyMaximizedWindow(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        try {
            if (client.getWindow().isFullscreen()) {
                client.getWindow().toggleFullscreen();
            }
            GLFW.glfwMaximizeWindow(handle);
        } catch (Throwable t) {
            LOGGER.warn("Maximieren fehlgeschlagen, bleibe im Fenstermodus.", t);
        }
    }

    private void copyResourcePack() {
        try {
            Path resourcepacksDir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");

            if (!Files.exists(resourcepacksDir)) {
                Files.createDirectories(resourcepacksDir);
                LOGGER.info("[CCraft] Resourcepacks Ordner erstellt!");
            }

            String resourcePackName = "resourcepack.zip";
            InputStream resourcePackStream = getClass().getResourceAsStream("/assets/ccraft/resourcepacks/" + resourcePackName);
            Path targetResourcePack = resourcepacksDir.resolve(resourcePackName);

            if (resourcePackStream != null && !Files.exists(targetResourcePack)) {
                Files.copy(resourcePackStream, targetResourcePack);
                LOGGER.info("[CCraft] Resource Pack erfolgreich installiert: " + resourcePackName);
            } else if (Files.exists(targetResourcePack)) {
                LOGGER.info("[CCraft] Resource Pack bereits vorhanden.");
            } else {
                LOGGER.warn("[CCraft] Resource Pack-Datei nicht in Resources gefunden!");
            }

        } catch (Exception e) {
            LOGGER.error("[CCraft] Fehler beim Resource Pack kopieren: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void activateResourcePack() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();

            new Thread(() -> {
                try {
                    Thread.sleep(2000);

                    ResourcePackManager manager = client.getResourcePackManager();

                    manager.scanPacks();

                    List<String> enabledPacks = new ArrayList<>(manager.getEnabledIds());

                    String packFileName = "file/resourcepack.zip";
                    if (!enabledPacks.contains(packFileName)) {
                        enabledPacks.add(packFileName);

                        List<ResourcePackProfile> profiles = new ArrayList<>();
                        for (String packId : enabledPacks) {
                            ResourcePackProfile profile = manager.getProfile(packId);
                            if (profile != null) {
                                profiles.add(profile);
                            }
                        }

                        manager.setEnabledProfiles(profiles.stream().map(ResourcePackProfile::getId).toList());

                        LOGGER.info("[CCraft] Resource Pack automatisch aktiviert!");
                    } else {
                        LOGGER.info("[CCraft] Resource Pack bereits aktiv.");
                    }

                } catch (Exception e) {
                    LOGGER.error("[CCraft] Fehler beim Aktivieren des Resource Packs: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            LOGGER.error("[CCraft] Fehler beim Resource Pack Aktivierung: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
