package ua.zentix.incoming.client.nuclear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukePresentationTest {
    @Test
    void landDustAndWaterSprayBothClearAndSmokeLightStaysBounded() {
        for (int tick : new int[]{0, 200, 399, 400, 1200}) {
            float[] wall = new float[20];
            wall[2] = 100;
            wall[3] = wall[7] = 1000;
            wall[6] = 0.4f;
            wall[18] = 1;
            NukePresentation.wall(wall, tick);
            assertTrue(wall[3] <= 0.0121 && wall[7] <= 0.0121);
            if (tick >= 400) assertEquals(0, wall[3] + wall[7] + wall[18]);
        }
        for (double exposure : new double[]{1, 1200}) {
            float[] glow = {100, 1, 0, 500, 1000, 500, 100, 100};
            NukePresentation.glow(glow, exposure);
            assertTrue(glow[4] * exposure <= 1.50001);
            assertEquals(0.5, glow[5] / glow[4], 1e-6);
        }
    }

    @Test
    void visibilityReturnsWithinTwentySecondsAtEveryDensity() {
        for (double density : new double[]{0, 0.001, 1, 1e6}) {
            for (double top : new double[]{1, 40, 400}) {
                double last = Double.POSITIVE_INFINITY;
                for (int tick = 0; tick <= 400; tick++) {
                    double now = NukePresentation.density(density, top, tick);
                    assertTrue(now >= 0 && now <= last);
                    assertTrue(now <= 0.012 && now * top <= 0.65 + 1e-12);
                    last = now;
                }
                assertEquals(0, last);
                assertEquals(0, NukePresentation.density(density, top, 12000));
            }
        }
        assertEquals(1, NukePresentation.obscuration(100));
        assertEquals(0, NukePresentation.obscuration(400));
    }

    @Test
    void dazzleAndAftereffectsCannotRunForMinutes() {
        for (double seconds : new double[]{0, 2, 20, 60, 1000}) {
            for (double pulse : new double[]{0, 3, 20, 500}) {
                double duration = NukePresentation.dazzleSeconds(seconds, pulse);
                assertTrue(duration >= 0 && duration <= seconds);
                assertTrue(duration * 20 + Math.min(pulse, 400) <= 400);
            }
        }
        assertEquals(0, NukePresentation.depth(1e6, 400));
    }
}
