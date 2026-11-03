package de.consolecrafter.ccraft.gui;

import de.consolecrafter.ccraft.hud.CustomHud;
import de.consolecrafter.ccraft.util.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

public class CustomMainMenuScreen extends Screen {

    private static final int LOGO_WIDTH  = 150;

    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 6;
    private static final int BTN_COUNT = 5;

    // In init() gefüllt, in render() gelesen. Null-sicher, damit render() nie
    // NPEt, falls init() nicht (oder mit Fehler) gelaufen ist.
    private int worldCount = 0;
    private int serverCount = 0;

    public CustomMainMenuScreen() {
        super(Text.literal("CCraft Main Menu"));
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.width / 2 - BTN_W / 2;
        int logoBottomY = 20 + CustomHud.logoHeightForWidth(LOGO_WIDTH);
        int totalH = BTN_COUNT * BTN_H + (BTN_COUNT - 1) * BTN_GAP;

        int available = this.height - logoBottomY - 40;
        int startY = logoBottomY + (available - totalH) / 2;
        startY = Math.max(logoBottomY + 10, startY);

        worldCount  = client.getLevelStorage().getLevelList().levels().size();
        serverCount = getServerCount();

        addBtn(cx, startY, 0, "Singleplayer", worldCount + " Welten",
            () -> client.setScreen(new SelectWorldScreen(this)));

        addBtn(cx, startY, 1, "Multiplayer", serverCount + " Server",
            () -> {
                if (client.options.skipMultiplayerWarning)
                    client.setScreen(new MultiplayerScreen(this));
                else
                    client.setScreen(new MultiplayerWarningScreen(this));
            });

        addBtn(cx, startY, 2, "Optionen", null,
            () -> client.setScreen(new OptionsScreen(this, client.options)));

        addBtn(cx, startY, 3, "CCraft", null,
            () -> client.setScreen(new CCraftSettingsScreen(this)));

        addBtn(cx, startY, 4, "Spiel beenden", null,
            () -> client.scheduleStop());
    }

    private void addBtn(int x, int baseY, int index, String label, String sub, Runnable action) {
        String text = label + (sub != null ? " §7(" + sub + ")§r" : "");
        addDrawableChild(ButtonWidget.builder(
            Text.literal(text), b -> action.run()
        ).position(x, baseY + index * (BTN_H + BTN_GAP)).size(BTN_W, BTN_H).build());
    }

    private int getServerCount() {
        try {
            ServerList list = new ServerList(client);
            list.loadFile();
            return list.size();
        } catch (Exception e) { return 0; }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;

        int logoX = cx - LOGO_WIDTH / 2;
        int logoH = CustomHud.logoHeightForWidth(LOGO_WIDTH);
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED, CustomHud.logo(),
            logoX, 20, 0f, 0f,
            LOGO_WIDTH, logoH, LOGO_WIDTH, logoH
        );

        int lineY = 20 + logoH + 8;
        context.fill(cx - 90, lineY, cx + 90, lineY + 1, Theme.BORDER);
        context.fill(cx - 30, lineY + 1, cx + 30, lineY + 2, Theme.ACCENT);

        String version = "Minecraft " + client.getGameVersion();
        context.drawText(textRenderer, version, 4, this.height - 12, Theme.TEXT_FAINT, true);

        String modInfo = "CCraft  |  " + FabricLoader.getInstance().getAllMods().size() + " Mods geladen";
        int modInfoX = this.width - textRenderer.getWidth(modInfo) - 4;
        context.drawText(textRenderer, modInfo, modInfoX, this.height - 12, Theme.ACCENT, true);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
