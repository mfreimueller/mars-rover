package marsrover.view;

import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import javafx.scene.Scene;
import marsrover.model.Level;
import marsrover.program.Block;
import marsrover.session.Leaderboard;
import marsrover.session.NameFilter;
import marsrover.session.Session;

/**
 * The session flow: start screen → levels in order → end screen → start screen.
 * Owns the scene root; screens only report back through callbacks.
 */
public final class Game {

    private final Scene scene;
    private final NameFilter nameFilter;
    private final Leaderboard leaderboard;
    private final LongSupplier clock;
    private final Map<String, List<Block>> preload; // dev only: -Ddemo=true
    private List<Level> levels;

    private Session session;
    private LevelScreen levelScreen;

    public Game(Scene scene, List<Level> levels, NameFilter nameFilter, Leaderboard leaderboard,
                LongSupplier clock, Map<String, List<Block>> preload) {
        this.scene = scene;
        this.leaderboard = leaderboard;
        this.levels = levels;
        this.nameFilter = nameFilter;
        this.clock = clock;
        this.preload = preload;
    }

    public void showStart() {
        session = null;
        levelScreen = null;
        scene.setRoot(new StartScreen(nameFilter, new LeaderboardView(leaderboard, 0), this::startSession));
    }

    public void startSession(String name) {
        session = new Session(name, clock.getAsLong());
        showLevel();
    }

    /** Dev shortcut: start straight at a level (0-based) with a placeholder name. */
    public LevelScreen startAt(int levelIndex) {
        startSession("Test");
        while (session.currentLevel() < levelIndex) {
            session.advance();
        }
        showLevel();
        return levelScreen;
    }

    /** Dev shortcut: the end screen with a made-up session. */
    public void showDemoEnd() {
        session = new Session("Ada", clock.getAsLong() - 187_000);
        int[] stars = {3, 2, 3, 1};
        for (int i = 0; i < levels.size(); i++) {
            session.recordSolved(levels.get(i).id(), stars[i % stars.length], clock.getAsLong());
        }
        endSession();
    }

    private void showLevel() {
        Level level = levels.get(session.currentLevel());
        String progress = Texts.progress(session.name(), session.currentLevel() + 1, levels.size());
        boolean last = session.currentLevel() == levels.size() - 1;
        levelScreen = new LevelScreen(level, preload.getOrDefault(level.id(), List.of()), progress, last,
                stars -> session.recordSolved(level.id(), stars, clock.getAsLong()),
                this::nextLevel);
        scene.setRoot(levelScreen);
    }

    private void nextLevel() {
        session.advance();
        if (session.currentLevel() < levels.size()) {
            showLevel();
        } else {
            endSession();
        }
    }

    /** Stores the result and shows the summary with the leaderboard. */
    private void endSession() {
        Session finished = session;
        Session.Result result = finished.result();
        int rank = store(result);
        session = null;
        levelScreen = null;
        scene.setRoot(new EndScreen(finished, result, rank, levels,
                new LeaderboardView(leaderboard, rank), this::showStart));
    }

    /**
     * Ends a session without the end screen (inactivity reset): the result still counts
     * if at least one level was solved. Then back to the start screen.
     */
    public void abortSession() {
        if (session != null) {
            store(session.result());
        }
        showStart();
    }

    /** Returns the rank, or 0 if nothing was solved and nothing was stored. */
    private int store(Session.Result result) {
        return result.levelsSolved() > 0 ? leaderboard.add(result) : 0;
    }

    public LevelScreen levelScreen() {
        return levelScreen;
    }
}
