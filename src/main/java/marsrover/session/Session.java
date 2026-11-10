package marsrover.session;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One visitor's run through the levels. Keeps the best stars per level.
 * The session time runs from the start to the last solve, so idle time before an
 * inactivity reset does not count. Times are passed in (ms) so tests don't depend on the clock.
 */
public final class Session {

    /** What goes on the leaderboard. Lower {@code timeMillis} wins ties. */
    public record Result(String name, int stars, long timeMillis, int levelsSolved) {}

    private final String name;
    private final long startMillis;
    private final Map<String, Integer> bestStars = new LinkedHashMap<>();
    private int currentLevel;
    private long lastSolvedMillis;

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

    public void recordSolved(String levelId, int stars, long nowMillis) {
        bestStars.merge(levelId, stars, Math::max);
        lastSolvedMillis = nowMillis;
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

    public Result result() {
        long time = levelsSolved() == 0 ? 0 : Math.max(0, lastSolvedMillis - startMillis);
        return new Result(name, totalStars(), time, levelsSolved());
    }
}
