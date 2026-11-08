package ua.zentix.incoming.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import ua.zentix.incoming.client.fx.particle.Fx;
import ua.zentix.incoming.client.fx.particle.FxBudget;

/**
 * Presentation: a sparse sample of hot blast debris, with the white-gold to orange-red look of titanium-shell sparks.
 * These are visual tracers, not a claim that a warhead contains titanium or that all casing fragments glow.
 * Flight uses the existing fragment law (9.81 m/s², quadratic drag toward world wind); size, burn time and launch
 * distribution are cinematic estimates. One quad per ember, no child particles, no new textures or renderer.
 */
final class EmberBurst {
    static final int MAX_PER_BLAST = 64;
    static final double RANGE = 512;

    private EmberBurst() {}

    /** Independent stream: adding visual embers cannot move the blast's original debris or change its random choices. */
    static RandomSource random(long blastSeed) {
        return RandomSource.create(blastSeed ^ 0x454D424552L);
    }

    /** Near details fade by distance; zero charge and distant impacts must not create a particle shower. */
    static int count(double chargeKg, double density, double distance) {
        if (!(chargeKg > 0) || !(density > 0) || distance >= RANGE) return 0;
        double detail = Math.min(1, Math.max(0, (RANGE - distance) / (RANGE - 96)));
        double base = Math.min(MAX_PER_BLAST, 12 + 9 * Math.cbrt(chargeKg));
        return Math.max(0, (int) Math.round(base * Math.min(1, density) * detail));
    }

    record Launch(Vec3 velocity, double terminalMps, int ticks, float size, float streak) {}

    static Launch launch(RandomSource random, boolean surface) {
        Vec3 direction = Explosions.dir(random, surface ? 0.12 : -1);
        boolean light = random.nextFloat() < 0.3f;
        // (est.) Smaller burning grains couple to wind faster and hang longer than the brighter, heavier pieces.
        double terminal = light ? 4.5 + random.nextDouble() * 2.5 : 8 + random.nextDouble() * 4;
        double speed = 1.8 + random.nextDouble() * 2.8;
        return new Launch(direction.scale(speed), terminal, 64 + random.nextInt(49),
                (float) (light ? 0.035 + random.nextDouble() * 0.02 : 0.05 + random.nextDouble() * 0.03),
                0.35f + random.nextFloat() * 0.35f);
    }

    static void emit(ClientLevel level, Vec3 pos, double chargeKg, boolean surface, RandomSource random) {
        double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(pos);
        int n = count(chargeKg, Fx.density(pos), distance);
        Vec3 origin = surface ? pos.add(0, 0.6, 0) : pos;
        for (int i = 0; i < n; i++) {
            Launch s = launch(random, surface);
            Fx.spark().vel(s.velocity()).ballistic(s.terminalMps()).wind(1).collide()
                    .size(s.size(), 0.008f).life(s.ticks()).streak(s.streak())
                    .color(0xFFF3D3, 0x9B2108).colorCurve(0.7f).alpha(0.85f)
                    .fadeIn(0).fadeFrom(0.45f).budget(FxBudget.EMBER).spawn(level, origin);
        }
    }
}
