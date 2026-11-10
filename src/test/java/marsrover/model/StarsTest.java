package marsrover.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StarsTest {

    private static final Level.StarThresholds T = new Level.StarThresholds(10, 3);

    @Test
    void rates() {
        assertEquals(0, Stars.rate(false, 1, 1, T));
        assertEquals(1, Stars.rate(true, 11, 1, T), "battery over target -> 1 even with few blocks");
        assertEquals(2, Stars.rate(true, 10, 4, T));
        assertEquals(3, Stars.rate(true, 10, 3, T));
        assertEquals(3, Stars.rate(true, 2, 1, T));
    }
}
