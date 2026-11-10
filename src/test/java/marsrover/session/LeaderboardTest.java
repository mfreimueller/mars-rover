package marsrover.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import marsrover.session.Leaderboard.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeaderboardTest {

    @TempDir
    Path dir;

    private static Session.Result result(String name, int stars, long time) {
        return new Session.Result(name, stars, time, 1);
    }

    private static List<String> names(Leaderboard b) {
        return b.top().stream().map(Entry::name).toList();
    }

    @Test
    void ordersByStarsThenTimeThenFirstCome() {
        Leaderboard b = Leaderboard.load(dir.resolve("board.json"));

        assertEquals(1, b.add(result("slow", 9, 300_000)));
        assertEquals(1, b.add(result("fast", 9, 100_000)));
        assertEquals(1, b.add(result("best", 12, 400_000)));
        assertEquals(4, b.add(result("few", 3, 10_000)));
        assertEquals(3, b.add(result("tie", 9, 100_000)), "same as 'fast' -> behind it");

        assertEquals(List.of("best", "fast", "tie", "slow", "few"), names(b));
    }

    @Test
    void showsTopTenButRanksEveryone() {
        Leaderboard b = Leaderboard.load(dir.resolve("board.json"));
        for (int i = 0; i < 12; i++) {
            b.add(result("p" + i, 10, 1000L * i));
        }

        assertEquals(10, b.top().size());
        assertEquals(13, b.add(result("late", 10, 999_999)));
        assertEquals(13, b.size());
    }

    @Test
    void survivesARestart() {
        Path file = dir.resolve("data/board.json");
        Leaderboard b = Leaderboard.load(file);
        b.add(result("Max", 7, 90_000));
        b.add(result("Ada", 12, 200_000));

        Leaderboard reloaded = Leaderboard.load(file);

        assertEquals(List.of(new Entry("Ada", 12, 200_000), new Entry("Max", 7, 90_000)), reloaded.top());
        assertTrue(Files.notExists(dir.resolve("data/board.json.tmp")));
    }

    @Test
    void clearEmptiesTheFileToo() {
        Path file = dir.resolve("board.json");
        Leaderboard b = Leaderboard.load(file);
        b.add(result("Max", 7, 90_000));

        b.clear();

        assertEquals(0, Leaderboard.load(file).size());
    }

    @Test
    void missingFileIsEmpty() {
        assertEquals(0, Leaderboard.load(dir.resolve("nope.json")).size());
    }

    @Test
    void brokenFileIsEmptyAndGetsReplacedOnNextSave() throws IOException {
        Path file = dir.resolve("board.json");
        Files.writeString(file, "{ this is not json");

        Leaderboard b = Leaderboard.load(file);
        assertEquals(0, b.size());
        b.add(result("Max", 7, 90_000));

        assertEquals(1, Leaderboard.load(file).size());
    }
}
