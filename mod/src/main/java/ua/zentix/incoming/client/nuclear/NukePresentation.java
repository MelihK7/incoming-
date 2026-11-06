package ua.zentix.incoming.client.nuclear;

/** Game presentation, not physical: keep the blast readable and restore visibility within twenty seconds. */
public final class NukePresentation {
    public static final double CLEAR_TICKS = 20 * 20;
    private static final double FADE_TICKS = 5 * 20;
    /** Thin ground smoke: at least 325 m visibility and no opaque overhead blanket. */
    private static final double MAX_SIGMA = 0.012, MAX_DEPTH = 0.65;

    private NukePresentation() {}

    public static double obscuration(double ticks) {
        double u = Math.max(0, Math.min(1, (ticks - FADE_TICKS) / (CLEAR_TICKS - FADE_TICKS)));
        return 1 - u * u * (3 - 2 * u);
    }

    static double density(double sigma, double top, double ticks) {
        return Math.max(0, Math.min(sigma * 0.25, Math.min(MAX_SIGMA, MAX_DEPTH / Math.max(1, top)))) * obscuration(ticks);
    }

    static double depth(double tau, double ticks) {
        return Math.max(0, Math.min(MAX_DEPTH, tau * 0.25)) * obscuration(ticks);
    }

    static double dazzleSeconds(double physicalSeconds, double pulseTicks) {
        return Math.max(0, Math.min(physicalSeconds, (CLEAR_TICKS - pulseTicks) / 20));
    }

    static void wall(float[] wall, double ticks) {
        wall[3] = (float) density(wall[3], wall[2], ticks);
        wall[7] = (float) density(wall[7], wall[2] * wall[6], ticks);
        wall[18] *= (float) (0.25 * obscuration(ticks));
    }

    /** Limit light on smoke in frame units, keeping the fireball's own emission unchanged. */
    static void glow(float[] glow, double exposure) {
        double peak = Math.max(glow[4], Math.max(glow[5], glow[6])) * exposure;
        double gain = Math.min(1, 1.5 / Math.max(peak, 1e-9));
        for (int i = 4; i < 7; i++) glow[i] *= (float) gain;
        glow[1] *= 0.35f;
    }
}
