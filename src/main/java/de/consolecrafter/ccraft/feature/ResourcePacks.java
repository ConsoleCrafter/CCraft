package de.consolecrafter.ccraft.feature;

import de.consolecrafter.ccraft.CCraftMod;
import de.consolecrafter.ccraft.config.ClientConfig;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ResourcePacks {

    private static final String[] FONT_PACKS = {"", "ccraft_font", "ccraft_helltown", "ccraft_impact", "ccraft_lazyarie"};
    private static final String[] FONT_NAMES = {"Vanilla", "CCraft", "Helltown", "Impact", "Lazyarie"};
    private static final String BUNDLED_FILE = "resourcepack.zip";
    private static final String BUNDLED_ID = "file/" + BUNDLED_FILE;

    private static boolean enableBundled;
    private static boolean synced;

    private ResourcePacks() {}

    public static void init(ModContainer container) {
        for (int i = 1; i < FONT_PACKS.length; i++) {
            ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(CCraftMod.MOD_ID, FONT_PACKS[i]), container,
                    Component.literal("CCraft " + FONT_NAMES[i] + " Font"), PackActivationType.NORMAL);
        }
        installBundledPack();
    }

    public static String fontName(int choice) {
        return FONT_NAMES[Math.max(0, Math.min(FONT_NAMES.length - 1, choice))];
    }

    public static void cycleFont() {
        ClientConfig config = ClientConfig.get();
        config.fontChoice = (config.fontChoice + 1) % FONT_PACKS.length;
        ClientConfig.save();
        apply(Minecraft.getInstance());
    }

    public static void tick(Minecraft minecraft) {
        if (synced || minecraft.gui.overlay() != null) return;
        synced = true;
        apply(minecraft);
    }

    private static void installBundledPack() {
        ClientConfig config = ClientConfig.get();
        if (config.bundledPackInstalled) return;
        Path target = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(BUNDLED_FILE);
        try (InputStream in = ResourcePacks.class.getResourceAsStream("/assets/ccraft/resourcepacks/" + BUNDLED_FILE)) {
            if (in == null) return;
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) Files.copy(in, target);
            enableBundled = true;
            config.bundledPackInstalled = true;
            ClientConfig.save();
        } catch (IOException e) {
            CCraftMod.LOGGER.warn("Bundled resource pack could not be installed", e);
        }
    }

    private static void apply(Minecraft minecraft) {
        PackRepository repository = minecraft.getResourcePackRepository();
        repository.reload();

        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        List<String> before = List.copyOf(selected);

        if (enableBundled && repository.isAvailable(BUNDLED_ID) && !selected.contains(BUNDLED_ID)) {
            selected.add(BUNDLED_ID);
        }
        enableBundled = false;

        for (int i = 1; i < FONT_PACKS.length; i++) selected.remove(packId(i));
        String wanted = packId(ClientConfig.get().fontChoice);
        if (wanted != null && repository.isAvailable(wanted)) selected.add(wanted);

        if (selected.equals(before)) return;
        repository.setSelected(selected);
        minecraft.options.updateResourcePacks(repository);
    }

    private static String packId(int choice) {
        if (choice <= 0 || choice >= FONT_PACKS.length) return null;
        return Identifier.fromNamespaceAndPath(CCraftMod.MOD_ID, FONT_PACKS[choice]).toString();
    }
}
