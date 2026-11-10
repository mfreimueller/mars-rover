package marsrover.view;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.function.BooleanSupplier;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.DragEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Kiosk behavior: locked full screen, inactivity reset, and a safety net that returns to the
 * start screen when an unexpected exception happens.
 */
public final class Kiosk {

    private Kiosk() {
    }

    /** Full screen that only the admin "Exit" can leave. Esc, Alt+F4 and window close are ignored. */
    public static void lock(Stage stage) {
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreen(true);
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(Event::consume);
        // If the OS still drops full screen (e.g. after Alt+Tab), take it back.
        stage.fullScreenProperty().addListener((obs, was, now) -> {
            if (!now) {
                Platform.runLater(() -> stage.setFullScreen(true));
            }
        });
    }

    /**
     * Calls {@code onTimeout} after {@code seconds} without mouse/keyboard input.
     * While {@code busy} is true (a program is running), the timer keeps restarting.
     */
    public static void inactivityReset(Scene scene, int seconds, BooleanSupplier busy, Runnable onTimeout) {
        PauseTransition timer = new PauseTransition(Duration.seconds(seconds));
        timer.setOnFinished(e -> {
            if (busy.getAsBoolean()) {
                timer.playFromStart();
            } else {
                onTimeout.run();
                timer.playFromStart();
            }
        });
        Runnable activity = timer::playFromStart;
        scene.addEventFilter(MouseEvent.ANY, e -> activity.run());
        scene.addEventFilter(KeyEvent.ANY, e -> activity.run());
        scene.addEventFilter(ScrollEvent.ANY, e -> activity.run());
        scene.addEventFilter(DragEvent.ANY, e -> activity.run());
        timer.play();
    }

    /**
     * Any uncaught exception is written to {@code errorLog} and the game recovers via {@code recover}.
     * If errors pile up (5 within 10 s), it stops recovering to avoid an endless loop.
     */
    public static void safetyNet(Path errorLog, Runnable recover) {
        long[] recent = new long[5];
        int[] next = {0};
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log(errorLog, error);
            long now = System.currentTimeMillis();
            long oldest = recent[next[0]];
            recent[next[0]] = now;
            next[0] = (next[0] + 1) % recent.length;
            if (oldest != 0 && now - oldest < 10_000) {
                System.err.println("Zu viele Fehler hintereinander – keine automatische Erholung.");
                return;
            }
            Platform.runLater(recover);
        });
    }

    private static void log(Path file, Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String entry = "=== " + LocalDateTime.now() + " ===\n" + trace + "\n";
        System.err.print(entry);
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // nothing more we can do; it is already on stderr
        }
    }
}
