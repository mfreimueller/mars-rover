package marsrover;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import marsrover.model.Action;
import marsrover.model.Level;
import marsrover.model.LevelLoader;
import marsrover.model.Sensor;
import marsrover.program.Block;
import marsrover.program.Block.Command;
import marsrover.program.Block.Condition;
import marsrover.program.Block.If;
import marsrover.program.Block.Repeat;
import marsrover.program.Block.While;
import marsrover.view.DevSnapshot;
import marsrover.view.Fonts;
import marsrover.view.Game;
import marsrover.view.LevelScreen;
import javafx.scene.layout.Pane;
import marsrover.session.NameFilter;

/**
 * Dev options (system properties):
 * {@code -Dkiosk=false} windowed instead of full screen,
 * {@code -Dlevel=N} skip the start screen and start at level N (1-based),
 * {@code -Ddemo=true} preload the reference solution,
 * {@code -Dautorun=true} press Run right away,
 * {@code -Dscreen=end} show the end screen with a made-up session,
 * {@code -Dsnapshot=file.png} see {@link DevSnapshot}.
 */
public class App extends Application {

    public static final double WIDTH = 1920;
    public static final double HEIGHT = 1080;

    /** Reference solutions, only preloaded with {@code -Ddemo=true} (dev snapshots). */
    private static final Block VOR = new Command(Action.VOR);
    private static final Map<String, List<Block>> DEMO_PROGRAMS = Map.of(
            "01-landing", List.of(VOR, VOR, new Command(Action.PROBE_NEHMEN), new Command(Action.LINKS_UM), VOR, VOR),
            "02-long-road", List.of(new Repeat(9, List.of(VOR))),
            "03-crater-field", List.of(new Repeat(6, List.of(
                    new If(new Condition(Sensor.VORN_KRATER, false), List.of(
                            new Command(Action.LINKS_UM), VOR, new Command(Action.RECHTS_UM)), false, List.of()),
                    VOR))),
            "04-unknown-distance", List.of(
                    new While(new Condition(Sensor.VORN_FREI, false), List.of(VOR)),
                    new Command(Action.PROBE_NEHMEN), new Command(Action.RECHTS_UM),
                    new While(new Condition(Sensor.VORN_FREI, false), List.of(VOR))));

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        LevelLoader.Result loaded = LevelLoader.loadFolder(Path.of("levels"));
        loaded.errors().forEach(e -> System.err.println("Level " + e.file() + ": " + e.reason()));
        List<Level> levels = loaded.levels();
        if (levels.isEmpty()) {
            System.err.println("Keine Levels gefunden.");
            Platform.exit();
            return;
        }
        Scene scene = new Scene(new Pane(), WIDTH, HEIGHT);
        scene.getStylesheets().addAll(
                App.class.getResource("/marsrover/style.css").toExternalForm(),
                Fonts.monoStylesheet());

        Game game = new Game(scene, levels, NameFilter.fromFile(Path.of("data", "badwords.txt")),
                System::currentTimeMillis, Boolean.getBoolean("demo") ? DEMO_PROGRAMS : Map.of());
        Integer level = Integer.getInteger("level");
        if ("end".equals(System.getProperty("screen"))) {
            game.showDemoEnd();
        } else if (level == null) {
            game.showStart();
        } else {
            LevelScreen screen = game.startAt(Math.clamp(level - 1, 0, levels.size() - 1));
            if (Boolean.getBoolean("autorun")) {
                screen.run();
            }
        }
        if (DevSnapshot.scheduleIfRequested(scene)) {
            return; // render off-screen at exactly 1920×1080
        }

        stage.setTitle("Mars Rover");
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(!"false".equals(System.getProperty("kiosk")));
        stage.show();
    }
}
