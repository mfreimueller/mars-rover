package marsrover.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SessionTest {

    @Test
    void keepsBestStarsPerLevelAndSumsThem() {
        Session s = new Session("Max", 1_000);

        s.recordSolved("a", 1, 2_000);
        s.recordSolved("a", 3, 3_000);
        s.recordSolved("a", 2, 4_000);
        s.recordSolved("b", 2, 5_000);

        assertEquals(3, s.stars("a"));
        assertEquals(0, s.stars("c"));
        assertEquals(5, s.totalStars());
        assertEquals(2, s.levelsSolved());
    }

    @Test
    void timeRunsUntilTheLastSolveNotUntilTheReset() {
        Session s = new Session("Max", 1_000);
        s.recordSolved("a", 3, 65_000);

        assertEquals(new Session.Result("Max", 3, 64_000, 1), s.result());
    }

    @Test
    void nothingSolvedMeansZeroTime() {
        assertEquals(new Session.Result("Max", 0, 0, 0), new Session("Max", 1_000).result());
    }

    @Test
    void advancesThroughLevels() {
        Session s = new Session("Max", 0);
        s.advance();
        s.advance();

        assertEquals(2, s.currentLevel());
    }
}
