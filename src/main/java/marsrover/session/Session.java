package marsrover.session;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One visitor's run through the levels. Keeps the best stars per level.
 * Time is passed in (milliseconds) so tests don't depend on the clock.
 */
public final class Session {

    /** What goes on the leaderboard. Lower {@code timeMillis} wins ties. */
    public record Result(String name, int stars, long timeMillis, int levelsSolved) {}

    private final String name;
    private final long startMillis;
    private final Map<String, Integer> bestStars = new LinkedHashMap<>();
    private int currentLevel;

    public Session(String name, long startMillis) {
        this.name = name;
        this.startMillis = startMillis;
    }

    public String name() {
        return name;
    }

    /** 0-based index of the level being played. */
    public int currentLevel() {
        return currentLevel;
    }

    public void advance() {
        currentLevel++;
    }

    public void recordSolved(String levelId, int stars) {
        bestStars.merge(levelId, stars, Math::max);
    }

    public int stars(String levelId) {
        return bestStars.getOrDefault(levelId, 0);
    }

    public int totalStars() {
        return bestStars.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int levelsSolved() {
        return bestStars.size();
    }

    public Result result(long nowMillis) {
        return new Result(name, totalStars(), Math.max(0, nowMillis - startMillis), levelsSolved());
    }
}
