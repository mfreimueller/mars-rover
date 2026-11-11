package marsrover.view;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import marsrover.model.Level;
import marsrover.model.LevelLoader;
import marsrover.program.Block;
import marsrover.session.Leaderboard;
import marsrover.session.NameFilter;
import marsrover.session.Session;

/**
 * The session flow: start screen → levels in order → end screen → start screen.
 * Owns the scene root (a fixed shell): screens are swapped inside it, and the admin
 * panel (Ctrl+Shift+A) can sit on top of any screen. Screens report back through callbacks.
 */
public final class Game {

    /** Below this size the whole UI is laid out at this size and scaled down to fit. */
    private static final double MIN_WIDTH = 1600;
    private static final double MIN_HEIGHT = 900;

    private final StackPane shell = new StackPane();
    private final Path levelsFolder;
    private final String adminPin;
    private AdminOverlay admin;
    private final NameFilter nameFilter;
    private final Leaderboard leaderboard;
    private final LongSupplier clock;
    private final Map<String, List<Block>> preload; // dev only: -Ddemo=true
    private List<Level> levels;

    private Session session;
    private LevelScreen levelScreen;

    public Game(Scene scene, Path levelsFolder, List<Level> levels, NameFilter nameFilter,
                Leaderboard leaderboard, String adminPin, LongSupplier clock, Map<String, List<Block>> preload) {
        this.levelsFolder = levelsFolder;
        this.adminPin = adminPin;
        this.leaderboard = leaderboard;
        this.levels = levels;
        this.nameFilter = nameFilter;
        this.clock = clock;
        this.preload = preload;
        scene.setRoot(scaledToFit(scene, shell));
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.isShiftDown() && e.getCode() == KeyCode.A) {
                openAdmin();
                e.consume();
            }
        });
    }

    /**
     * Small screens (laptops, 125 % Windows scaling) get the same layout, just smaller,
     * instead of clipped text and squeezed panels.
     */
    private static Pane scaledToFit(Scene scene, Region content) {
        Scale scale = new Scale(1, 1, 0, 0);
        content.getTransforms().add(scale);
        Pane root = new Pane(content);
        Runnable fit = () -> {
            double w = scene.getWidth();
            double h = scene.getHeight();
            double f = Math.min(1, Math.min(w / MIN_WIDTH, h / MIN_HEIGHT));
            scale.setX(f);
            scale.setY(f);
            content.setMinSize(w / f, h / f);
            content.setPrefSize(w / f, h / f);
            content.setMaxSize(w / f, h / f);
        };
        scene.widthProperty().addListener(o -> fit.run());
        scene.heightProperty().addListener(o -> fit.run());
        fit.run();
        return root;
    }

    private void setScreen(Node screen) {
        if (shell.getChildren().isEmpty()) {
            shell.getChildren().add(screen);
        } else {
            shell.getChildren().set(0, screen);
        }
    }

    // ---- admin ------------------------------------------------------------------------------

    public void openAdmin() {
        if (admin != null) {
            return;
        }
        admin = new AdminOverlay(adminPin, new AdminOverlay.Actions() {
            @Override
            public LevelLoader.Result reloadLevels() {
                return Game.this.reloadLevels();
            }

            @Override
            public void clearLeaderboard() {
                leaderboard.clear();
                if (session == null) {
                    showStart(); // refresh the visible board
                }
            }

            @Override
            public void exit() {
                Platform.exit();
                System.exit(0);
            }

            @Override
            public void close() {
                closeAdmin();
            }
        });
        shell.getChildren().add(admin);
    }

    /** Dev only: the admin menu without PIN, for snapshots. */
    public void openAdminUnlockedForDev() {
        openAdmin();
        admin.unlockForDev();
    }

    private void closeAdmin() {
        shell.getChildren().remove(admin);
        admin = null;
    }

    /**
     * Re-reads the levels folder. With at least one valid level the new set is used and any
     * running session ends (its result still counts); otherwise the old levels stay.
     */
    private LevelLoader.Result reloadLevels() {
        LevelLoader.Result result = LevelLoader.loadFolder(levelsFolder);
        if (!result.levels().isEmpty()) {
            abortSessionKeepAdmin();
            levels = result.levels();
            showStart();
        }
        return result;
    }

    // ---- flow -------------------------------------------------------------------------------

    public void showStart() {
        session = null;
        levelScreen = null;
        setScreen(new StartScreen(nameFilter, new LeaderboardView(leaderboard, 0), this::startSession));
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
        session = new Session(System.getProperty("name", "Ada"), clock.getAsLong() - 187_000);
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
        setScreen(levelScreen);
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
        setScreen(new EndScreen(finished, result, rank, levels,
                new LeaderboardView(leaderboard, rank), this::showStart));
    }

    /**
     * Ends a session without the end screen (inactivity reset): the result still counts
     * if at least one level was solved. Then back to the start screen.
     */
    public void abortSession() {
        if (admin != null) {
            closeAdmin(); // never leave the admin panel open on an unattended kiosk
        }
        abortSessionKeepAdmin();
        showStart();
    }

    private void abortSessionKeepAdmin() {
        if (session != null) {
            store(session.result());
            session = null;
        }
    }

    /** Returns the rank, or 0 if nothing was solved and nothing was stored. */
    private int store(Session.Result result) {
        return result.levelsSolved() > 0 ? leaderboard.add(result) : 0;
    }

    public LevelScreen levelScreen() {
        return levelScreen;
    }
}
