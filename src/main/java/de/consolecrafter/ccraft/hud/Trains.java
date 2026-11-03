package de.consolecrafter.ccraft.hud;

import de.consolecrafter.ccraft.CCraftMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class Trains {

    public enum Kind {
        SID("sidtrain", "SidTrain", 50, 549, 105),
        DEVIN("devintrain", "DevinTrain", 80, 549, 111),
        MELON("melontrain", "MelonTrain", 110, 553, 113),
        DANIEL("danieltrain", "DanielTrain", 140, 550, 108);

        private final String command;
        private final String displayName;
        private final Identifier texture;
        private final int y;
        private final int srcW;
        private final int srcH;

        Kind(String command, String displayName, int y, int srcW, int srcH) {
            this.command = command;
            this.displayName = displayName;
            this.texture = Identifier.fromNamespaceAndPath(CCraftMod.MOD_ID, "textures/" + command + ".png");
            this.y = y;
            this.srcW = srcW;
            this.srcH = srcH;
        }

        public String command() {
            return command;
        }

        public String displayName() {
            return displayName;
        }
    }

    private static final float SPEED = 150.0f;
    private static final int WIDTH = 183;

    private static final float[] X = new float[Kind.values().length];
    private static final boolean[] ACTIVE = new boolean[Kind.values().length];
    private static final long[] LAST = new long[Kind.values().length];

    private Trains() {}

    public static void start(Kind kind) {
        int i = kind.ordinal();
        ACTIVE[i] = true;
        X[i] = -WIDTH;
        LAST[i] = 0;
    }

    public static void extract(GuiGraphicsExtractor g) {
        for (Kind kind : Kind.values()) {
            int i = kind.ordinal();
            if (!ACTIVE[i]) continue;
            int height = Math.max(1, Math.round(WIDTH * (kind.srcH / (float) kind.srcW)));
            g.blit(RenderPipelines.GUI_TEXTURED, kind.texture, (int) X[i], kind.y, 0.0f, 0.0f,
                    WIDTH, height, kind.srcW, kind.srcH, kind.srcW, kind.srcH);

            long now = System.nanoTime();
            float dt = LAST[i] == 0 ? 0.0f : Math.min(0.1f, (now - LAST[i]) / 1_000_000_000.0f);
            LAST[i] = now;
            X[i] += SPEED * dt;
            if (X[i] > g.guiWidth()) ACTIVE[i] = false;
        }
    }
}
