package ua.zentix.incoming.client.fx.layer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import ua.zentix.incoming.client.far.FarView;
import ua.zentix.incoming.client.far.Sight;
import ua.zentix.incoming.client.fx.particle.Fx;
import ua.zentix.incoming.client.fx.particle.FxBudget;
import ua.zentix.incoming.client.fx.particle.FxPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmberDrawBudgetTest {
    @BeforeAll
    static void boot() {
        SharedConstants.tryDetectVersion();
        if (LoadingModList.get() == null) LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
    }

    @Test
    void aHundredBurstsCannotDrawMoreThan512EmberQuads() {
        FxPool pool = FxPool.INSTANCE;
        pool.clear();
        ClientLevel level = mock(ClientLevel.class);
        for (int i = 0; i < 100 * 64; i++) {
            Fx.spark().vel(0.3, 1, 0).ballistic(8).fadeIn(0).budget(FxBudget.EMBER).spawn(level, 0, 65, 0);
        }
        FarView view = new FarView(new Vec3(0, 64, 20), new Vector3f(0, 0, -1), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0),
                0, 1000, 0.001, 1, 1, 1, new Vector3f(1, 1, 1), 20000, 192, 192, 0, Sight.Air.at(0, 20000));
        FrustumIntersection frustum = new FrustumIntersection(new Matrix4f().ortho(-100, 100, -100, 100, 0.1f, 1000));
        FxFrame frame = new FxFrame(view, frustum, new FxFrame.Fog());
        FxQuads quads = new FxQuads();
        quads.begin(view.left(), view.up());
        quads.glowTexel(view.pixel());
        pool.collect(frame, quads, 0);
        assertEquals(512, pool.live(FxBudget.EMBER));
        assertEquals(512, quads.size(), "one existing-renderer quad per ember; no secondary particle draws");
        assertTrue(quads.glows());
        for (int i = 0; i < quads.size(); i++) assertEquals(FxQuads.Part.PARTICLES, quads.partOf(i));
        pool.clear();
    }
}
