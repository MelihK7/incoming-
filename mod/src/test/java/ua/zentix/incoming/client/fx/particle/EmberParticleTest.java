package ua.zentix.incoming.client.fx.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import ua.zentix.incoming.client.fx.Fragments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class EmberParticleTest {
    @BeforeAll
    static void boot() {
        SharedConstants.tryDetectVersion();
        if (LoadingModList.get() == null) LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
    }

    @Test
    void globalEmberBudgetCannotStealOrdinarySparkSlotsOrSpawnChildParticles() {
        FxPool pool = FxPool.INSTANCE;
        pool.clear();
        long refused = pool.refused(FxBudget.EMBER);
        Fx.Spec ember = Fx.spark().ballistic(8).budget(FxBudget.EMBER);
        assertEquals(1, ember.drag);
        assertEquals(Fragments.drag(8), ember.ballisticDrag);
        assertEquals(null, ember.trail);
        for (int i = 0; i < 10000; i++) pool.spawn(ember, null, 0, 64, 0, 1, 1);
        assertEquals(512, pool.live(FxBudget.EMBER));
        assertEquals(10000 - 512, pool.refused(FxBudget.EMBER) - refused);
        pool.spawn(Fx.spark(), null, 0, 64, 0, 1, 1);
        assertEquals(1, pool.live(FxBudget.SPARK));
        pool.clear();
        assertEquals(0, pool.count());
    }

    @Test
    void pooledParticlesResetBallisticStateAndEmbersExtinguishInWater() {
        ClientLevel level = mock(ClientLevel.class);
        FluidState water = mock(FluidState.class);
        when(level.getFluidState(any())).thenReturn(water);
        when(water.isEmpty()).thenReturn(false);
        FxParticle particle = new FxParticle();
        particle.init(Fx.spark().ballistic(8), 0, 64, 0, 1, 1, RandomSource.create(1), level);
        particle.tick(level, FxPool.INSTANCE);
        assertTrue(particle.dead, "the glow must extinguish upon entering water");
        particle.init(Fx.spark().vel(1, 0, 0).drag(1).gravity(0), 0, 64, 0, 1, 1, RandomSource.create(2), level);
        particle.tick(level, FxPool.INSTANCE);
        assertTrue(!particle.dead, "an ordinary reused spark must not retain the ember's fluid/motion mode");
        assertEquals(1, particle.x, 1e-9);
        assertEquals(64, particle.y, 1e-9);
    }

    @Test
    void invalidTerminalSpeedsCannotPoisonTheParticlePool() {
        for (double speed : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> Fx.spark().ballistic(speed));
        }
    }

    @Test
    void aFastEmberExtinguishesEvenWhenItCrossesWaterAndEndsInAir() {
        ClientLevel level = mock(ClientLevel.class);
        FluidState wet = mock(FluidState.class), dry = mock(FluidState.class);
        when(wet.isEmpty()).thenReturn(false);
        when(dry.isEmpty()).thenReturn(true);
        when(level.getFluidState(any())).thenAnswer(call -> ((BlockPos) call.getArgument(0)).getX() == 1 ? wet : dry);
        assertTrue(FxParticle.touchesFluid(level, new Vec3(0.2, 64, 0.2), new Vec3(3.5, 64, 0.2)));
        assertTrue(!FxParticle.touchesFluid(level, new Vec3(2.2, 64, 0.2), new Vec3(3.5, 64, 0.2)));
        FxParticle particle = new FxParticle();
        particle.init(Fx.spark().vel(4.6, 0.1, 0).ballistic(12), 0.2, 64, 0.2, 1, 1, RandomSource.create(1), level);
        particle.tick(level, FxPool.INSTANCE);
        assertTrue(particle.x > 2, "the entire water column was crossed in this tick");
        assertTrue(level.getFluidState(BlockPos.containing(particle.x, particle.y, particle.z)).isEmpty());
        assertTrue(particle.dead, "crossing water must quench the ember before its next frame");
    }

    @Test
    void aGroundContactStopsMovementAndCoolsTheParticleWithinHalfASecond() {
        ClientLevel level = mock(ClientLevel.class);
        FluidState dry = mock(FluidState.class);
        when(dry.isEmpty()).thenReturn(true);
        when(level.getFluidState(any())).thenReturn(dry);
        try (var collision = mockStatic(Entity.class)) {
            collision.when(() -> Entity.collideBoundingBox(any(), any(), any(), any(), any())).thenAnswer(call -> {
                Vec3 requested = call.getArgument(1);
                return new Vec3(requested.x, 0, requested.z);
            });
            FxParticle particle = new FxParticle();
            particle.init(Fx.spark().vel(1, -0.4, 0).ballistic(8).collide().life(100), 0, 64, 0, 1, 1, RandomSource.create(1), level);
            particle.tick(level, FxPool.INSTANCE);
            double landedX = particle.x;
            for (int i = 0; i < 11; i++) particle.tick(level, FxPool.INSTANCE);
            assertEquals(landedX, particle.x, 1e-9);
            assertEquals(64, particle.y, 1e-9);
            assertTrue(particle.dead, "ground contact must not leave long-lived floor glitter");
        }
    }
}
