package de.consolecrafter.ccraft.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class CCraftScreen extends Screen {

    protected static final int BUTTON_HEIGHT = 20;

    protected final Screen parent;

    protected CCraftScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    protected Button button(String label, int x, int y, int width, int height, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, width, height)
                .build());
    }

    protected Button backButton(int x, int y, int width) {
        return button("Zurück", x, y, width, BUTTON_HEIGHT, this::onClose);
    }

    protected void open(Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
