package ua.zentix.incoming;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StrikePacingTest {
    @Test
    void defaultPacingKeepsApproachTimeButReducesWaiting() {
        assertEquals(15, IncomingConfig.SERVER.droneFlightTime.getDefault());
        assertEquals(20, IncomingConfig.SERVER.missileFlightTime.getDefault());
        assertEquals(10, IncomingConfig.SERVER.loiterTime.getDefault());
        assertEquals(40, IncomingConfig.SERVER.bomberFlightTime.getDefault());
        assertFalse(IncomingConfig.CLIENT.nukeTinnitus.getDefault());
    }
}
