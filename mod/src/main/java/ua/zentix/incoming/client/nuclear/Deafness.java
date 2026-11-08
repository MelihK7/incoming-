package ua.zentix.incoming.client.nuclear;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Compatibility facade for blast-audio callers. Hearing damage and tinnitus are disabled: explosions must not
 * leave a persistent tone or suppress weapon/world audio. Keep these entry points for playback and filter callers.
 */
public final class Deafness {
    private Deafness() {}

    public static void blast(double exposure, double kpa, long seed) {}
    public static void before(SoundInstance sound) {}
    static void tick(ClientLevel level) {}
    static void reset() {}

    public static float gain(SoundInstance sound) { return 1; }
    public static float highs(SoundInstance sound) { return 1; }
    public static float gain() { return 1; }
    public static float highs() { return 1; }

    static String describe() {
        return "hearing disabled gain 1.00 highs 1.00 ringing false";
    }
}
