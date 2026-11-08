package ua.zentix.incoming.client.fx;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import ua.zentix.incoming.client.fx.particle.FxBudget;
import ua.zentix.incoming.net.S2C;
import ua.zentix.incoming.strike.WeaponType;
import ua.zentix.incoming.warhead.GroundMaterial;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmberBurstTest {
    @Test
    void nuclearSecondaryPacketsRetainTheirLegacySparkPath() {
        for (int kind : new int[]{S2C.Blast.DRONE, S2C.Blast.MISSILE}) {
            S2C.Blast secondary = new S2C.Blast(kind, Vec3.ZERO, GroundMaterial.BRICK.ordinal(), 0, 5, Optional.empty(), S2C.Blast.NO_WEAPON);
            assertTrue(BlastSource.of(secondary).chargeKg() > 0, "legacy kind inference must not enable new embers");
            assertTrue(!BlastEffects.hotDebris(secondary));
            assertEquals(70, Explosions.sparkCount(4, 1, BlastEffects.hotDebris(secondary)));
        }
        S2C.Blast missile = new S2C.Blast(S2C.Blast.MISSILE, Vec3.ZERO, GroundMaterial.STONE.ordinal(), 0, 5, Optional.empty(), WeaponType.MISSILE.id());
        assertTrue(BlastEffects.hotDebris(missile));
        assertEquals(34, Explosions.sparkCount(4, 1, BlastEffects.hotDebris(missile)));
        S2C.Blast water = new S2C.Blast(S2C.Blast.MISSILE, Vec3.ZERO, GroundMaterial.WATER.ordinal(), 0, 5, Optional.empty(), WeaponType.MISSILE.id());
        assertTrue(!BlastEffects.hotDebris(water));
    }

    @Test
    void birthCountsAreBoundedAndRespectDistanceAndParticleSettings() {
        for (double charge : new double[]{0, 3, 50, 310, 2423, 15000000}) {
            for (double density : new double[]{0, 0.3, 0.6, 1}) {
                int previous = Integer.MAX_VALUE;
                for (int distance = 0; distance <= 1024; distance++) {
                    int count = EmberBurst.count(charge, density, distance);
                    assertTrue(count >= 0 && count <= EmberBurst.MAX_PER_BLAST);
                    assertTrue(count <= previous);
                    if (charge == 0 || density == 0 || distance >= EmberBurst.RANGE) assertEquals(0, count);
                    previous = count;
                }
            }
        }
        assertEquals(512, FxBudget.EMBER.limit());
        assertTrue(EmberBurst.count(310, 0.3, 30) < EmberBurst.count(310, 1, 30));
    }

    @Test
    void seededGroundDebrisRisesThenFallsWithFiniteEnergyAndShortLifetimes() {
        RandomSource random = RandomSource.create(1729);
        for (int i = 0; i < 1000; i++) {
            EmberBurst.Launch launch = EmberBurst.launch(random, true);
            assertTrue(launch.velocity().y > 0);
            assertTrue(launch.size() >= 0.035 && launch.size() <= 0.081);
            assertTrue(launch.ticks() >= 64 && launch.ticks() <= 112);
            double[] v = {launch.velocity().x, launch.velocity().y, launch.velocity().z};
            double y = 0.6, peak = y;
            for (int tick = 0; tick < launch.ticks(); tick++) {
                Fragments.step(v, 0, 0, 0, Fragments.drag(launch.terminalMps()));
                y += v[1];
                peak = Math.max(peak, y);
                for (double component : v) assertTrue(Double.isFinite(component));
            }
            assertTrue(peak > 0.6, "the ember must leave the blast upward");
            assertTrue(v[1] < 0, "the ember must fall rather than float upward");
            assertTrue(-v[1] * 20 < launch.terminalMps() * 1.04, "fall speed is bounded by air drag");
        }
    }

    @Test
    void crosswindBendsAnArcAndAirburstsAlsoThrowDebrisDownward() {
        double[] calm = {1, 1.5, 0}, windy = calm.clone();
        double drift = 0;
        for (int tick = 0; tick < 100; tick++) {
            Fragments.step(calm, 0, 0, 0, Fragments.drag(8));
            Fragments.step(windy, 0, 0, 0.25, Fragments.drag(8));
            drift += windy[2] - calm[2];
        }
        assertTrue(drift > 10, "a 5 m/s crosswind must carry the cooling debris sideways");
        RandomSource random = RandomSource.create(10);
        int down = 0;
        for (int i = 0; i < 100; i++) if (EmberBurst.launch(random, false).velocity().y < 0) down++;
        assertTrue(down > 20 && down < 80, "airbursts spread in all directions");
    }

    @Test
    void launchesRepeatFromTheExplosionSeed() {
        RandomSource a = RandomSource.create(123), b = RandomSource.create(123);
        for (int i = 0; i < 100; i++) assertEquals(EmberBurst.launch(a, true), EmberBurst.launch(b, true));
    }
}
