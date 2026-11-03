package de.consolecrafter.ccraft.feature;

public final class TickRate {

    private static final int SAMPLES = 5;
    private static final float[] RATES = new float[SAMPLES];

    private static int index;
    private static int filled;
    private static long lastUpdate = -1;

    private TickRate() {}

    public static void reset() {
        index = 0;
        filled = 0;
        lastUpdate = -1;
    }

    public static void onTimeUpdate() {
        long now = System.nanoTime();
        if (lastUpdate >= 0) {
            double seconds = (now - lastUpdate) / 1_000_000_000.0;
            if (seconds > 0) {
                RATES[index] = (float) Math.max(0.0, Math.min(20.0, 20.0 / seconds));
                index = (index + 1) % SAMPLES;
                filled = Math.min(SAMPLES, filled + 1);
            }
        }
        lastUpdate = now;
    }

    public static float tps() {
        if (filled == 0) return 20.0f;
        float sum = 0;
        for (int i = 0; i < filled; i++) sum += RATES[i];
        return sum / filled;
    }
}
