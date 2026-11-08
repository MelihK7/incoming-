package ua.zentix.incoming.client.nuclear;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeafnessTest {
    @Test
    void repeatedBlastsCannotStartRingingOrSuppressAudio() {
        for (int i = 0; i < 1000; i++) {
            Deafness.before(null);
            Deafness.blast(1000, 1000, i);
            Deafness.tick(null);
            assertEquals(1, Deafness.gain());
            assertEquals(1, Deafness.highs());
            assertEquals(1, Deafness.gain(null));
            assertEquals(1, Deafness.highs(null));
        }
        Deafness.reset();
        assertTrue(Deafness.describe().contains("ringing false"));
    }
}
