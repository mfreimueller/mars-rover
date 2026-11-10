package marsrover.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SessionTest {

    @Test
    void keepsBestStarsPerLevelAndSumsThem() {
        Session s = new Session("Max", 1_000);

        s.recordSolved("a", 1);
        s.recordSolved("a", 3);
        s.recordSolved("a", 2);
        s.recordSolved("b", 2);

        assertEquals(3, s.stars("a"));
        assertEquals(0, s.stars("c"));
        assertEquals(5, s.totalStars());
        assertEquals(2, s.levelsSolved());
    }

    @Test
    void resultMeasuresTimeSinceStart() {
        Session s = new Session("Max", 1_000);
        s.recordSolved("a", 3);

        assertEquals(new Session.Result("Max", 3, 64_000, 1), s.result(65_000));
    }

    @Test
    void advancesThroughLevels() {
        Session s = new Session("Max", 0);
        s.advance();
        s.advance();

        assertEquals(2, s.currentLevel());
    }
}
